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

import cz.cvut.fel.aic.amodsim.ridesharing.offline.demand.Solution;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.demand.GroupDemand;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.demand.Demand;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanRequestAction;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.opencsv.CSVWriter;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.utils.Benchmark;
import cz.cvut.fel.aic.agentpolis.utils.CollectionUtil;
import cz.cvut.fel.aic.agentpolis.utils.FlexArray;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandler;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.amodsim.CsvWriter;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicleFactorySpec;
import cz.cvut.fel.aic.amodsim.event.DemandEvent;
import cz.cvut.fel.aic.amodsim.io.Common;
import cz.cvut.fel.aic.amodsim.ridesharing.DARPSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.IOptimalPlanVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.MathUtils;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.GroupGenerator;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.*;
import cz.cvut.fel.aic.amodsim.event.OnDemandVehicleEvent;
import cz.cvut.fel.aic.amodsim.event.OnDemandVehicleEventContent;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.amodsim.storage.OnDemandvehicleStationStorage;
import java.io.IOException;

import java.util.*;
import org.slf4j.LoggerFactory;
import cz.cvut.fel.aic.amodsim.ridesharing.PlanCostProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.model.DefaultPlanComputationRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.model.DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanAction;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanActionDropoff;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanActionPickup;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.entities.Car;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.entities.OfflineVirtualVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.entities.TripTaxify;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.io.Statistics;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.io.TripTransformTaxify;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.GurobiSolver;
import cz.cvut.fel.aic.amodsim.statistics.content.GroupSizeData;
import cz.cvut.fel.aic.amodsim.statistics.content.RidesharingBatchStatsVGA;
import cz.cvut.fel.aic.geographtools.Graph;
import java.io.File;
import java.io.FileWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.stream.Collectors;
import org.apache.commons.io.FilenameUtils;



@Singleton
public class OfflineVGASolver extends DARPSolver implements EventHandler{
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(OfflineVGASolver.class);
	
	private static final int MILLION = 1000000;
	
    
	private final LinkedHashSet<PlanComputationRequest> activeRequests;

	private final AmodsimConfig config;
		
	private final Map<String,VGAVehicle> vgaVehiclesMapBydemandOnDemandVehicles;
	
	private final TypedSimulation eventProcessor;
	
	private final OnDemandvehicleStationStorage onDemandvehicleStationStorage;

	private final TimeProvider timeProvider;
	
	private final Provider<GroupGenerator> groupGeneratorProvider;
	
	private List<VGAVehicle> vgaVehicles;
	
	private int planCount;
	
	private List<VehiclePlanList> feasiblePlans;
	
	private List<List<String>> logRecords;
	
	private int groupGenerationTime;
			
	private int solverTime;
	
	private int newRequestCount;
	
	private FlexArray groupCounts;
	
	private FlexArray groupCountsPlanExists;
	
	private FlexArray computationalTimes;
	
	private FlexArray computationalTimesPlanExists;
	
	private int[] usedVehiclesPerStation;
    
    public final Graph<SimulationNode,SimulationEdge> graph;
    
    private final TripTransformTaxify tripTransform;
    
    final DefaultPlanComputationRequestFactory requestFactory;
    
    final DemandAgent.DemandAgentFactory agentFactory;
	        
    private final Provider<OnDemandVehicleFactorySpec> vehicleFactoryProvider;
    
	
	private final GurobiSolver gurobiSolver;
    
    Statistics stats;
        
	@Inject
	public OfflineVGASolver(TravelTimeProvider travelTimeProvider, PlanCostProvider travelCostProvider,
			OnDemandVehicleStorage vehicleStorage, AmodsimConfig config, TimeProvider timeProvider, 
			DefaultPlanComputationRequestFactory requestFactory, TypedSimulation eventProcessor, 
			GurobiSolver gurobiSolver, Provider<GroupGenerator> groupGeneratorProvider,
			OnDemandvehicleStationStorage onDemandvehicleStationStorage, DemandAgent.DemandAgentFactory
            agentFactory, TripTransformTaxify tripTransform,
            Provider<OnDemandVehicleFactorySpec> vehicleFactoryProvider, 
            OnDemandVehicleStorage onDemandVehicleStorage) {
		super(vehicleStorage, travelTimeProvider, travelCostProvider, requestFactory);
		this.config = config;
		this.gurobiSolver = gurobiSolver;
		this.eventProcessor = eventProcessor;
		this.onDemandvehicleStationStorage = onDemandvehicleStationStorage;
		this.timeProvider = timeProvider;
		this.groupGeneratorProvider = groupGeneratorProvider;
		activeRequests = new LinkedHashSet<>();
		vgaVehiclesMapBydemandOnDemandVehicles = new HashMap<>();
		MathUtils.setTravelTimeProvider(travelTimeProvider);
		setEventHandeling();
        this.agentFactory = agentFactory;
        this.requestFactory = requestFactory;
        this.tripTransform = tripTransform;
        this.vehicleFactoryProvider = vehicleFactoryProvider;

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

    
    private List<List<PlanComputationRequest>>  groupRequests(List<TripTaxify<SimulationNode>> trips){
//        int counter = 0;
        List<List<PlanComputationRequest>> batches = new LinkedList<>();
        int batchPeriod = config.ridesharing.offline.batchPeriod;
//        int maxBatch = config.ridesharing.offline.batchMax == 0 ? Integer.MAX_VALUE : config.ridesharing.offline.batchMax;
//        int maxTrips = config.ridesharing.offline.batchTotal == 0 ? Integer.MAX_VALUE : config.ridesharing.offline.batchTotal;
//        LOGGER.debug ("Period " + (batchPeriod/1000) + ", max batch "+maxBatch + ", max total "+maxTrips);
        int start = (int) trips.get(0).getStartTime() + 10000;
        //FIXME it loses around 200 trips somewhere here
        
        int end = start + batchPeriod;
        List<PlanComputationRequest> batch = new ArrayList<>();
//        LOGGER.debug("start "+start+", end "+end);
        for(TripTaxify<SimulationNode> trip : trips){

//            counter++;
            DefaultPlanComputationRequest request = requestFactory.create(trip.id, trip.getStartNode(), 
                trip.getEndNode(), agentFactory.create("agent " + trip.id, trip.id, trip));
//            if(batch.size() >= maxBatch){
//                LOGGER.debug("Batch size "+ batch.size());
//                batches.add(batch);
//                batch = new ArrayList<>();
//                batch.add(request);
//            }
            if (trip.getStartTime() <= end ) {
                batch.add(request);
            }else  {
//                LOGGER.debug("Batch size "+ batch.size());
                batches.add(batch);
                batch = new ArrayList<>();
                batch.add(request);
                end += batchPeriod;
//                LOGGER.debug("start "+start+", end "+end);
            }
 
        }
        LOGGER.debug(batches.stream().mapToInt((b)-> b.size()).sum() +" trips in "+batches.size() + " batches");
        return batches;
    }
   

    
     
    @Override 
    public Map<RideSharingOnDemandVehicle, DriverPlan> solve(List<PlanComputationRequest> newRequests, 
        List<PlanComputationRequest> waitingRequests) {
        
        List<TripTaxify<SimulationNode>> trips = loadTrips();
       
        Collections.sort(trips, Comparator.comparing(TripTaxify::getStartTime));
        LOGGER.debug("Sorted trips "+trips.size());
         
        //NormalDemand nd = new NormalDemand(travelTimeProvider, config, trips, graph);
        
        List<List<PlanComputationRequest>> requestBatches = groupRequests(trips); //.subList(0,1000)
        vgaVehicles = new LinkedList<>();
        // initialize 1 OnDemandVehicle in any node
        initVehicle(requestBatches.get(0).get(0).getFrom());
        
        List<DriverPlan> allPlans = new LinkedList<>();
        LOGGER.debug( requestBatches.size() + " request batches in the list");


      //compute optimal plan for each batch
//        for(List<PlanComputationRequest> requests : requestBatches){
        for( List<PlanComputationRequest> requests: requestBatches){

           LOGGER.debug( requests.size() + " requests in batch");
            waitingRequests.addAll(requests);
            //TODO vehicles
            List<DriverPlan> plans = computePlans(requests, waitingRequests);
            LOGGER.debug("Empty plans "+ plans.stream().filter((p)->p.size() == 0).count());
            LOGGER.debug("New plans " + plans.size());
            allPlans.addAll(plans);
            LOGGER.debug("total plans " + allPlans.size());
            waitingRequests.clear();
        }
//        demand.printDemand();
        LOGGER.debug("Group generation finished, plans " + allPlans.size());
        LOGGER.debug(allPlans.stream().mapToInt(p -> p.size()).sum()/2 + 
            " trips in  " + allPlans.size() + " plans");
        LOGGER.debug(waitingRequests.size() + " trips in waitingRequests");
        
        // chain plans and assign vehicles
        
        Demand demand = new GroupDemand(travelTimeProvider, config, allPlans, graph);
        Solution sol = new Solution(demand, travelTimeProvider, config);
        sol.buildPaths();
      // write results 
       List<Car> cars = sol.getAllCars();
       stats = new Statistics(demand, cars, travelTimeProvider, config);
       stats.writeResults(config.amodsimExperimentDir);
      

       return new HashMap<>();
    }     
    
    
    private List<DriverPlan> computePlans(List<PlanComputationRequest> newRequests, 
        List<PlanComputationRequest> waitingRequests) {
		// statistic vars
		newRequestCount = newRequests.size();
        LOGGER.debug("New requests count: " + newRequestCount);
		if(config.ridesharing.vga.logPlanComputationalTime){
			groupCounts = new FlexArray();
			groupCountsPlanExists = new  FlexArray();
			computationalTimes = new FlexArray();
			computationalTimesPlanExists = new FlexArray();
		}
		
        logRecords = new ArrayList<>();

        activeRequests.clear();
        activeRequests.addAll(newRequests);
		
		LOGGER.info("No. of active requests: {}", activeRequests.size());
		/* Generating feasible plans for each vehicle */
		Benchmark benchmark = new Benchmark();
//      
        LOGGER.debug("VgaVehicles size " + vgaVehicles.size());
        
		benchmark.measureTime(() -> generateGroups(waitingRequests));	
        
		groupGenerationTime = benchmark.getDurationMsInt();
        
		LOGGER.debug("groupGenerationTime " + benchmark.getDurationMsInt());
		LOGGER.debug("feasible plans  " + feasiblePlans.size());
		LOGGER.debug("active requests  " + activeRequests.size());
		/* Using an ILP solver to optimally assign a group to each vehicle */
		benchmark = new Benchmark();

      List<Plan<IOptimalPlanVehicle>> optimalPlans	= benchmark.measureTime(() -> gurobiSolver.assignOptimallyFeasiblePlans(
						feasiblePlans, activeRequests, usedVehiclesPerStation));
		LOGGER.debug("optimal plans  " + optimalPlans.size());
        
		// ILP solver generation total time 
		solverTime = benchmark.getDurationMsInt();
        LOGGER.debug("Solver Time " + benchmark.getDurationMsInt());
		/* Filling the output with converted plans */
	
		Map<OnDemandVehicleStation,Integer> usedVehiclesPerStation = new HashMap<>();
		
		List<DriverPlan> driverPlans = new ArrayList<>();
		for(Plan<IOptimalPlanVehicle> plan : optimalPlans) {
			DriverPlan driverPlan = toDriverPlan(plan);
            driverPlans.add(driverPlan);
		}

		VGAVehicle.resetMapping();
		
		if(config.ridesharing.vga.logPlanComputationalTime){
			logRecords();
		}
        LOGGER.debug("DriverPlans size " + driverPlans.size());
        
        LOGGER.debug("Plans length  " + driverPlans.stream().filter(p -> p.getLength() == 0).count());
        return driverPlans;
	}
       
    private void  initVehicle(SimulationNode position){
               
        OnDemandVehicleFactorySpec vehicleFactory = vehicleFactoryProvider.get();
        OnDemandVehicle vehicle = vehicleFactory.create("0", position);
        VGAVehicle newVGAVehicle = VGAVehicle.newInstance((RideSharingOnDemandVehicle) vehicle);
        vgaVehicles.add(newVGAVehicle);
        vgaVehiclesMapBydemandOnDemandVehicles.put(vehicle.getId(), newVGAVehicle);
    }
    
 private void generateGroups(List<PlanComputationRequest> waitingRequests) {
        
        feasiblePlans = new ArrayList<>();
//		LOGGER.info("Generating groups for vehicle.");
		planCount = 0;
        
		VGAVehicle vGAVehicle = vgaVehicles.get(0);
//        LOGGER.debug("Vehicle position " + vGAVehicle.getPosition());
//        LOGGER.debug("Vehicle requests " + vGAVehicle.getRequestsOnBoard().size());
        // all waiting request can be assigned to 1  vehicle
		List<Plan> feasibleGroupPlans = computeGroupsForVehicle(vGAVehicle, waitingRequests);
        LOGGER.info("groups generated " +feasibleGroupPlans.size());

        OfflineVirtualVehicle virtualVehicle = new OfflineVirtualVehicle(vGAVehicle, waitingRequests.size());
        List<Plan> virtualVehiclePlans = new ArrayList<>(feasibleGroupPlans.size());
		for (Plan feasibleGroupPlan : feasibleGroupPlans) {
			virtualVehiclePlans.add(feasibleGroupPlan.duplicateForVehicle(virtualVehicle));
		}
        VehiclePlanList vehiclePlanList = new VehiclePlanList(virtualVehicle, virtualVehiclePlans);
		feasiblePlans.add(vehiclePlanList);
		planCount += feasibleGroupPlans.size();
       		
		LOGGER.info("planCount "+ planCount);
		if(true){
			printGroupStats(feasiblePlans);
            
            //GROUP GENERATION
//            writeGroupsForDebugging(feasibleGroupPlans, 100);

		}
	}
 
    private void writeGroupsForDebugging(List<Plan> feasibleGroupPlans, int limit){
        
        if (feasibleGroupPlans == null || feasibleGroupPlans.isEmpty()){
            LOGGER.error("No feasible group plans for saving");
            return;
        }
        long timeToStart = config.ridesharing.offline.timeToStart;
        int maxGroupSize = config.ridesharing.vga.maxGroupSize;
        long maxProlongation = config.ridesharing.maxProlongationInSeconds * 1000;
        List<String[]>[] plansToSave = new List[maxGroupSize+1];
        for (int i=0; i < plansToSave.length; i++){
            plansToSave[i] = new ArrayList<>();
        }
        
        int planCounter = 0;
        for (Plan plan : feasibleGroupPlans){
            List<PlanRequestAction> planActions = plan.getActions();
            int planSize = planActions.size()/2;
            if(plansToSave[planSize].size() >= limit){
                continue;
            }
            SimulationNode node = null;
            long time = 0;
            for(PlanRequestAction action : planActions){
                //for pickup action:  earliest possible arrival = request time, 
                //                    latest possible arrival = request time + max prolongation
                if( action instanceof  PlanActionPickup ){
                    PlanActionPickup pAction = (PlanActionPickup) action;
                    PlanComputationRequest actionRequest = pAction.request;
                    int tripId = actionRequest.getId();
                    String actionType = "Pickup";
                    long earliestPossibleTime = actionRequest.getDemandAgent().getDemandTime();
                    long latestPossibleTime = earliestPossibleTime + maxProlongation;
                    long actionTime = 0;
                    
                    if (node != null){
                       long travelTime = travelTimeProvider.getExpectedTravelTime(node, actionRequest.getFrom());
                       actionTime = time + travelTime;
                    } else{
                        actionTime = earliestPossibleTime + timeToStart;
                    }
                    node = actionRequest.getFrom();
                    double lat = node.getLatitude();
                    double lon = node.getLongitude();
                    String[] planEntry = new String[]{
                        String.valueOf(planCounter), String.valueOf(tripId), actionType,
                        String.valueOf(earliestPossibleTime), String.valueOf(actionTime), String.valueOf(latestPossibleTime),
                        String.valueOf(node.id), String.valueOf(lat), String.valueOf(lon)
                    };
                    plansToSave[planSize].add(planEntry);
                    time = actionTime;
                    
                } else if (action instanceof PlanActionDropoff){
                    PlanActionDropoff dAction = (PlanActionDropoff) action;
                    PlanComputationRequest actionRequest = dAction.request;
                    int tripId = actionRequest.getId();
                    String actionType = "Dropoff";
                    long requestTime = actionRequest.getDemandAgent().getDemandTime();
                    long planBestTime = travelTimeProvider.getExpectedTravelTime(
                        actionRequest.getFrom(), actionRequest.getTo());
                    long earliestPossibleTime = requestTime + planBestTime;
                    long latestPossibleTime = earliestPossibleTime + maxProlongation;
                    long travelTime = travelTimeProvider.getExpectedTravelTime(node, actionRequest.getTo());
                    long actionTime = time + travelTime;
                    node = actionRequest.getTo();
                    double lat = node.getLatitude();
                    double lon = node.getLongitude();
                    String[] planEntry = new String[]{
                        String.valueOf(planCounter), String.valueOf(tripId), actionType, 
                        String.valueOf(earliestPossibleTime), String.valueOf(actionTime), String.valueOf(latestPossibleTime),
                        String.valueOf(node.id),  String.valueOf(lat), String.valueOf(lon)
                    };
                    plansToSave[planSize].add(planEntry);
                    time = actionTime;
                }//action
            }//plan actions
            planCounter++;
        }//plans
 
        String dir = config.amodsimExperimentDir;
        Date timeStamp = Calendar.getInstance().getTime();
        String timeString = new SimpleDateFormat("dd-MM-HH-mm").format(timeStamp);
        String[] header = new String[]{"planId", "tripId", "actionType", "earliest possible", 
            "action time", "latest possible", "nodeId", "latitude", "longitude"};
        
        for(int i = 1; i <= maxGroupSize; i++){
            String filename = String.format("group_generation_%s", i) + "_"+ timeString + ".csv";
            String filepath  =  FilenameUtils.concat(dir, filename);
            List<String[]> plans = plansToSave[i];
            plans.add(0, header);
            try (CSVWriter csvWriter = new CSVWriter(new FileWriter(filepath))) {
                csvWriter.writeAll(plans);
            }
            catch(Exception ex){
                ex.printStackTrace();
            }
        }
    }

    private void writeGraphDat(){
        long totalLengthM=0;
        long totalTimeS =0;
    }
    
	private List<Plan> computeGroupsForVehicle(VGAVehicle vehicle, Collection<PlanComputationRequest> waitingRequests) {
		Benchmark benchmark = new Benchmark();
		GroupGenerator groupGenerator = groupGeneratorProvider.get();
		List<Plan> feasibleGroupPlans = benchmark.measureTime(() ->
					groupGenerator.generateGroupsForVehicle(vehicle, waitingRequests, 0));
		
		// log
		if(config.ridesharing.vga.logPlanComputationalTime){
			logPlansPerVehicle(groupGenerator, vehicle, feasibleGroupPlans, benchmark.durationNano);
		}
		
		return feasibleGroupPlans;
	}
    
    
    private DriverPlan toDriverPlan(Plan<IOptimalPlanVehicle> plan) {
        //TODO don't remove current position to actions
		//List<PlanAction> tasks = new ArrayList<>(plan.getActions().size()+1);
        List<PlanAction> tasks = new ArrayList<>(plan.getActions().size());
		//tasks.add(new PlanActionCurrentPosition(plan.getVehicle().getPosition()));
		for(PlanRequestAction action: plan.getActions()){
			tasks.add(action);
		}
		DriverPlan driverPlan = new DriverPlan(tasks, plan.getEndTime() - plan.getStartTime(), plan.getCost());
		
		return driverPlan;
	} 
      
	private void printGroupStats(List<VehiclePlanList> feasiblePlans) {
		Map<Integer,Integer> stats = new LinkedHashMap();
		for (VehiclePlanList feasiblePlan : feasiblePlans) {
			for (Plan feasibleGroupPlan : feasiblePlan.feasibleGroupPlans) {
				int size = Math.round(feasibleGroupPlan.getActions().size() / 2);
				CollectionUtil.incrementMapValue(stats, size, 1);
			}
		}
		
		for (Map.Entry<Integer, Integer> entry : stats.entrySet()) {
			Integer size = entry.getKey();
			Integer count = entry.getValue();
			LOGGER.info("{} groups of size {}", count, size);
		}
	}

	private synchronized void logPlansPerVehicle(GroupGenerator groupGenerator, VGAVehicle vehicle, 
			List<Plan> feasibleGroupPlans, long totalTimeNano) {
		// group generator statistic addition
		groupCounts.addArrayInPlace(groupGenerator.getGroupCounts());
		groupCountsPlanExists.addArrayInPlace(groupGenerator.getGroupCountsPlanExists());
		computationalTimes.addArrayInPlace(groupGenerator.getComputationalTimes());
		computationalTimesPlanExists.addArrayInPlace(groupGenerator.getComputationalTimesPlanExists());	
		
		List<String> record = new ArrayList<>(5);
		record.add(Integer.toString(0));
		record.add(vehicle.getRidesharingVehicle().getId());
		record.add(Long.toString(Math.round(totalTimeNano / MILLION)));
		record.add(Integer.toString(vehicle.getRequestsOnBoard().size()));
		
		int maxActionCount = groupCountsPlanExists.size() * 2 + 1;
		int[] counts = new int[maxActionCount];
		for (int i = 0; i < counts.length; i++) {
			counts[i] = 0;
			
		}
		for (Plan feasibleGroupPlan : feasibleGroupPlans) {
			counts[feasibleGroupPlan.getActions().size()]++;
			
			// global group stats
			int groupSize = feasibleGroupPlan.getActions().size() / 2;
		}
		for (int i = 0; i < counts.length; i++) {
			record.add(Integer.toString(counts[i]));
			
		}
		logRecords.add(record);
	}

	private void logRecords() {
		
		GroupSizeData[] groupSizeDataForAllGroupSizes = new GroupSizeData[groupCounts.size()];
		GroupSizeData[] groupSizeDataForAllGroupSizesPlanExists = new GroupSizeData[groupCountsPlanExists.size()];
		for(int i = 0; i < groupCounts.size(); i++){
			groupSizeDataForAllGroupSizes[i] = new GroupSizeData(computationalTimes.get(i), groupCounts.get(i));
		}
		for(int i = 0; i < groupCountsPlanExists.size(); i++){
			groupSizeDataForAllGroupSizesPlanExists[i] = new GroupSizeData(
					computationalTimesPlanExists.get(i), groupCountsPlanExists.get(i));
		}
		
		// batch records
		RidesharingBatchStatsVGA batchStatsVGA = new RidesharingBatchStatsVGA(activeRequests.size(), 
				groupGenerationTime, solverTime, gurobiSolver.getGap(), groupSizeDataForAllGroupSizes, 
				groupSizeDataForAllGroupSizesPlanExists, newRequestCount);
		ridesharingStats.add(batchStatsVGA);
		
		// per vehicle records
		try {
			CsvWriter writer = new CsvWriter(
					Common.getFileWriter(config.ridesharing.vga.groupGeneratorLogFilepath, true));
			for (List<String> record : logRecords) {
				writer.writeLine(record.toArray(new String[0]));
			}
			writer.close();
		} catch (IOException ex) {
			LOGGER.error(null, ex);
		}
	}
   
    
	@Override
	public EventProcessor getEventProcessor() {
		return eventProcessor;
	}

	@Override
	public void handleEvent(Event event) {
		if(event.getType() instanceof OnDemandVehicleEvent){
			OnDemandVehicleEvent eventType = (OnDemandVehicleEvent) event.getType();
			OnDemandVehicleEventContent eventContent = (OnDemandVehicleEventContent) event.getContent();
			PlanComputationRequest request = ridesharingDispatcher.getRequest(eventContent.getDemandId());
			VGAVehicle vehicle = vgaVehiclesMapBydemandOnDemandVehicles.get(eventContent.getOnDemandVehicleId());
			if(eventType == OnDemandVehicleEvent.PICKUP){
				vehicle.addRequestOnBoard(request);
			}
			else if(eventType == OnDemandVehicleEvent.DROP_OFF){
				vehicle.removeRequestOnBoard(request);
				activeRequests.remove(request);
			}
		}
		else if(event.isType(DemandEvent.LEFT)){
			PlanComputationRequest request = (PlanComputationRequest) event.getContent();
			activeRequests.remove(request);
		}
	}
		
	private void setEventHandeling() {
		List<Enum> typesToHandle = new LinkedList<>();
		typesToHandle.add(OnDemandVehicleEvent.PICKUP);
		typesToHandle.add(OnDemandVehicleEvent.DROP_OFF);
		typesToHandle.add(DemandEvent.LEFT);
		eventProcessor.addEventHandler(this, typesToHandle);
	}
}
