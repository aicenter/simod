/* 
 * Copyright (C) 2019 Czech Technical University in Prague.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package cz.cvut.fel.aic.amodsim.ridesharing.offline;

import cz.cvut.fel.aic.amodsim.ridesharing.offline.vga.OfflineGurobiSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.vga.OfflineGroupGenerator;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.demand.Solution;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.demand.NormalDemand;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.demand.GroupNormalDemand;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.opencsv.CSVWriter;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.utils.Benchmark;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandler;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.event.DemandEvent;
import cz.cvut.fel.aic.amodsim.ridesharing.DARPSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.MathUtils;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.*;
import cz.cvut.fel.aic.amodsim.event.OnDemandVehicleEvent;
import cz.cvut.fel.aic.amodsim.event.OnDemandVehicleEventContent;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import java.io.IOException;

import java.util.*;
import org.slf4j.LoggerFactory;
import cz.cvut.fel.aic.amodsim.ridesharing.PlanCostProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.model.DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.entities.Car;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.entities.TripTaxify;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.io.Statistics;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.io.TripTransformTaxify;
import cz.cvut.fel.aic.geographtools.Graph;
import java.io.File;
import java.io.FileWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.apache.commons.io.FilenameUtils;



@Singleton
public class MyOfflineVgaSolver extends DARPSolver implements EventHandler{
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MyOfflineVgaSolver.class);
	

    private final AmodsimConfig config;
		
	private final TypedSimulation eventProcessor;
	
	private int groupGenerationTime;
			
	private int solverTime;
	    
    public final Graph<SimulationNode,SimulationEdge> graph;
    
    private final TripTransformTaxify tripTransform;
           
	private final OfflineGurobiSolver gurobiSolver;
    
    private OfflineGroupGenerator groupGenerator;
    
    NormalDemand normalDemand;

        
	@Inject
	public MyOfflineVgaSolver(TravelTimeProvider travelTimeProvider, PlanCostProvider travelCostProvider,
			OnDemandVehicleStorage vehicleStorage, AmodsimConfig config, TimeProvider timeProvider, 
			DefaultPlanComputationRequestFactory requestFactory, TypedSimulation eventProcessor, 
			OfflineGurobiSolver gurobiSolver,  TripTransformTaxify tripTransform) {
		super(vehicleStorage, travelTimeProvider, travelCostProvider, requestFactory);
		this.config = config;
		this.gurobiSolver = gurobiSolver;
		this.eventProcessor = eventProcessor;

        MathUtils.setTravelTimeProvider(travelTimeProvider);
		setEventHandeling();
        this.tripTransform = tripTransform;
        graph = tripTransform.getGraph();
        
    }
    
    //TODO move to TripTransform
    private List<TripTaxify<SimulationNode>> loadTrips(){
      
        List<TripTaxify<SimulationNode>>  trips = new ArrayList<>();
        try {
            trips = tripTransform.loadTripsFromCsv(new File(config.tripsPath));
        }catch (IOException|ParseException ex) {
            LOGGER.error("File IO exception: " + ex);
        }
        if(trips.isEmpty()){
            LOGGER.error("Empty trip list ");
            System.exit(-1);
        }
        return trips;
    }

    /**
     *
     * @param newRequests
     * @param waitingRequests
     * @return
     */
    @Override
	public Map<RideSharingOnDemandVehicle, DriverPlan> solve(List<PlanComputationRequest> newRequests, 
        List<PlanComputationRequest> waitingRequests) {
        
        // load demand
        List<TripTaxify<SimulationNode>> trips = loadTrips();//.subList(0, 1000);
//      writeTripStats(trips);
        
        Collections.sort(trips, Comparator.comparing(TripTaxify::getStartTime));
        normalDemand = new NormalDemand(travelTimeProvider, config, trips, graph);
        groupGenerator = new OfflineGroupGenerator(normalDemand, travelTimeProvider, config, graph); 
        List<int[]> allOptimalPlans = new ArrayList<>();
       
       //compute plans for demand batches
       int batchStartInd = 0;
       int batchStartTime = normalDemand.getStartTime(0);
       int batchPeriod = config.ridesharing.offline.batchPeriod;
       while(true){
           int batchEndTime = batchStartTime + batchPeriod;
           int batchEndInd = normalDemand.getIndexByStartTime(batchEndTime);
           int[] batchTripIds = normalDemand.getTripsSlice(batchStartInd, batchEndInd);
           if(batchTripIds.length == 0){
               LOGGER.debug("empty slice");
               break;
           }
            batchStartInd = batchEndInd;
            batchStartTime = batchEndTime;

            List<int[]> optimalPlans = computePlans(batchTripIds);
            allOptimalPlans.addAll(optimalPlans);
            LOGGER.debug("new optimal plans  " + optimalPlans.size());
//          LOGGER.debug("total optimal plans" + allOptimalPlans.size());
        }
        LOGGER.debug("Optimal plans size "+ allOptimalPlans.size());
        
        // chaining
        GroupNormalDemand groupDemand = new GroupNormalDemand(travelTimeProvider, config, allOptimalPlans, normalDemand, graph);
        Solution sol = new Solution(groupDemand, travelTimeProvider, config);
        sol.buildPaths();
        
       // write results 
        List<Car> cars = sol.getAllCars();
        Statistics stats = new Statistics(groupDemand, cars, travelTimeProvider, config);
        stats.writeResults(config.amodsimExperimentDir);
              
        return new HashMap<>();
    }
    
    
    private List<int[]> computePlans(int[] tripIds){
        
        List<int[]> groups = generateGroups(tripIds);

        Benchmark benchmark = new Benchmark();
        List<int[]> optimalPlans 
                = benchmark.measureTime(() -> gurobiSolver.assignOptimallyFeasiblePlans(groups));
        
        // ILP solver generation total time 
        solverTime = benchmark.getDurationMsInt();
        LOGGER.debug("Solver time " + solverTime);
        LOGGER.debug("new optimal plans  " + optimalPlans.size());
           
        return optimalPlans;
    }
    
    
    private List<int[]> generateGroups(int[] tripIds){
        
        Benchmark benchmark = new Benchmark();
        List<int[]> groups = benchmark.measureTime(() ->groupGenerator.generateGroups(tripIds));
        groupGenerationTime = benchmark.getDurationMsInt();
        LOGGER.debug("Group generation time " + groupGenerationTime);
        LOGGER.debug("total groups " + groups.size());
        
//      writeGroupsForDebugging(allGroups, 100);
        
        return groups;
    }
    
   // methods for debugging group generation
    private void writeGroupsForDebugging(List<int[]> feasibleGroupPlans, int limit){
       
        if (feasibleGroupPlans == null || feasibleGroupPlans.isEmpty()){
            LOGGER.error("No feasible group plans for saving");
            return;
        }
        long timeToStart = config.ridesharing.offline.timeToStart;
        int maxGroupSize = config.ridesharing.vga.maxGroupSize;
        long maxProlongation = config.ridesharing.maxProlongationInSeconds * 1000;
        // entry [plan id, tripId, actionType, 
        // earliestPossibleTime, actionTime, latestPossibleTime, diff1 , diff2
        // nodeId, lat, lon]
        List<String[]>[] plansToSave = new List[maxGroupSize+1];
        for (int i=0; i < plansToSave.length; i++){
            plansToSave[i] = new ArrayList<>();
        }
        
        int planCounter = 0;
        for(int[] plan :feasibleGroupPlans){

            int planSize = (plan.length-1)/2;
            SimulationNode node = null;
            long time = 0;
            for(int a = 1; a < plan.length; a ++){
                //for pickup action:  earliest possible arrival = request time, 
                //                    latest possible arrival = request time + max prolongation
                int action = plan[a];
                if( action >= 0 ){
                    int tripInd = action;
                    int tripId = normalDemand.indToTripId(tripInd);
                    String actionType = "Pickup";
                    long earliestPossibleTime = normalDemand.getStartTime(tripInd);
                    long latestPossibleTime = earliestPossibleTime + maxProlongation;
                    long actionTime = 0;
                    if (node != null){
                       long travelTime = travelTimeProvider.getExpectedTravelTime(node, normalDemand.getStartNode(tripInd));
                       actionTime = time + travelTime;
                    } else{
                        actionTime = earliestPossibleTime + timeToStart;
                    }
                    actionTime = actionTime >= earliestPossibleTime ? actionTime : earliestPossibleTime;
                    node = normalDemand.getStartNode(tripInd);
                    double lat = node.getLatitude();
                    double lon = node.getLongitude();
                    String[] planEntry = new String[]{
                        String.valueOf(planCounter), String.valueOf(tripId), actionType,
                        String.valueOf(earliestPossibleTime), String.valueOf(actionTime), String.valueOf(latestPossibleTime),
                        String.valueOf(actionTime - earliestPossibleTime), String.valueOf(latestPossibleTime - actionTime),
                        String.valueOf(node.id), String.valueOf(lat), String.valueOf(lon)
                    };
                    plansToSave[planSize].add(planEntry);
                    time = actionTime;
                    
                // earliest possible arrival = request time + shortest path time
                // latest possible arrival = request time + shortest path time + max prolongation
                } else {
                    int tripInd = -action - 1;
                    int tripId = normalDemand.indToTripId(tripInd);
                    String actionType = "Dropoff";
                    long earliestPossibleTime = normalDemand.getEndTime(tripInd);
                    long latestPossibleTime = earliestPossibleTime + maxProlongation;
                    long travelTime = travelTimeProvider.getExpectedTravelTime(node, normalDemand.getEndNode(tripInd));
                    long actionTime = time + travelTime;
                //     actionTime = actionTime >= earliestPossibleTime ? actionTime : earliestPossibleTime;
                    node = normalDemand.getEndNode(tripInd);
                    double lat = node.getLatitude();
                    double lon = node.getLongitude();
                    String[] planEntry = new String[]{
                        String.valueOf(planCounter), String.valueOf(tripId), actionType, 
                        String.valueOf(earliestPossibleTime), String.valueOf(actionTime), String.valueOf(latestPossibleTime),
                        String.valueOf(actionTime - earliestPossibleTime), String.valueOf(latestPossibleTime - actionTime),
                        String.valueOf(node.id),  String.valueOf(lat), String.valueOf(lon)
                    };
                    plansToSave[planSize].add(planEntry);
                    time = actionTime;
                }//action
            }//plan actions
            planCounter++;
        }//plans
 
        String dir = config.amodsimExperimentDir;
        //dir = dir.endsWith("/") ? dir : dir + "/";
        Date timeStamp = Calendar.getInstance().getTime();
        String timeString = new SimpleDateFormat("dd-MM-HH-mm").format(timeStamp);
        // [planId, tripId, actionType, earliest possible time, action time, latest possible time, nodeId, lat, lon]
        String[] header = new String[]{"planId", "tripId", "actionType", "earliest possible", 
            "action time", "latest possible","at-ept", "lpt-at", "nodeId", "latitude", "longitude"};
        
        
        for(int i = 1; i <= maxGroupSize; i++){
            String filename = String.format("group_generation_%s", i) + "_"+ timeString + ".csv";
            String filepath  =  FilenameUtils.concat(dir, filename);
            List<String[]> plans = plansToSave[i];
            plans.add(0, header);
            limit = limit < plans.size() ? limit : plans.size();
            try (CSVWriter csvWriter = new CSVWriter(new FileWriter(filepath))) {
                csvWriter.writeAll(plans.subList(0, limit));
            }
            catch(Exception ex){
                ex.printStackTrace();
            }
        }
    }    

    private void writeTripStats(List<TripTaxify<SimulationNode>> trips){
        
        List<String[]> result = new ArrayList<>();
        
        for (TripTaxify<SimulationNode> trip : trips){
            Long shortestPathInMs = travelTimeProvider.getExpectedTravelTime(trip.getStartNode(), trip.getEndNode());
            result.add(new String[]{
                String.valueOf(trip.id), 
                String.valueOf(trip.getStartTime()/1000), 
                String.valueOf(shortestPathInMs/1000.0),
                
                String.valueOf(trip.getStartNode().id),
                String.valueOf(trip.getStartNode().getLatitude()),
                String.valueOf(trip.getStartNode().getLongitude()),
                
                String.valueOf(trip.getEndNode().id),
                String.valueOf(trip.getEndNode().getLatitude()),
                String.valueOf(trip.getEndNode().getLongitude())
               
            });
        }
        
        result.add(0, new String[]{"tripId", "startTime", "lengthSec",
            "startNode",  "startLat", "startLon", "endNode", "endLat", "endLon"});
        String filename = "trip_stats_2h.csv";
        String filepath  =  FilenameUtils.concat(config.amodsimExperimentDir, filename);
        
        try (CSVWriter csvWriter = new CSVWriter(new FileWriter(filepath))) {
                csvWriter.writeAll(result);
        }catch(Exception ex){
                ex.printStackTrace();
        }
    }
    
    // unused inteface methods
    @Override
	public EventProcessor getEventProcessor() {
		return eventProcessor;
	}

	@Override
	public void handleEvent(Event event) {
        throw new UnsupportedOperationException("Not supported yet.");
	}
		
	private void setEventHandeling() {
		List<Enum> typesToHandle = new LinkedList<>();
		typesToHandle.add(OnDemandVehicleEvent.PICKUP);
		typesToHandle.add(OnDemandVehicleEvent.DROP_OFF);
		typesToHandle.add(DemandEvent.LEFT);
		eventProcessor.addEventHandler(this, typesToHandle);
	}
}
