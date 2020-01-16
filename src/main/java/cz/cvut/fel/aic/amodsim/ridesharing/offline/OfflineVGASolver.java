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
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.HighwayNetwork;
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
import cz.cvut.fel.aic.amodsim.ridesharing.PlanCostProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.model.DefaultPlanComputationRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.model.DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanAction;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.entities.Car;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.entities.OfflineVirtualVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.entities.TripTaxify;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.io.Statistics;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.io.TripTransformTaxify;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.OfflineGurobiSolver;
import cz.cvut.fel.aic.amodsim.statistics.content.GroupSizeData;
import cz.cvut.fel.aic.amodsim.statistics.content.RidesharingBatchStatsVGA;
import cz.cvut.fel.aic.geographtools.Graph;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.*;
import org.slf4j.LoggerFactory;


@Singleton
public class OfflineVGASolver extends DARPSolver implements EventHandler{
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(OfflineVGASolver.class);
	
	private static final int MILLION = 1000000;
	
    private final LinkedHashSet<PlanComputationRequest> activeRequests;

	private final AmodsimConfig config;
		
	private final Map<String,VGAVehicle> vgaVehiclesMapBydemandOnDemandVehicles;
	
	private final TypedSimulation eventProcessor;
	
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
    	
	private final OfflineGurobiSolver gurobiSolver;
    
    Statistics stats;
        
	@Inject
	public OfflineVGASolver(TravelTimeProvider travelTimeProvider, PlanCostProvider travelCostProvider,
			OnDemandVehicleStorage vehicleStorage, AmodsimConfig config, TimeProvider timeProvider, 
			DefaultPlanComputationRequestFactory requestFactory, TypedSimulation eventProcessor, 
			OfflineGurobiSolver gurobiSolver, Provider<GroupGenerator> groupGeneratorProvider,
			DemandAgent.DemandAgentFactory agentFactory, TripTransformTaxify tripTransform,
            HighwayNetwork highwayNetwork, Provider<OnDemandVehicleFactorySpec> vehicleFactoryProvider) {
        
		super(vehicleStorage, travelTimeProvider, travelCostProvider, requestFactory);
		this.config = config;
		this.gurobiSolver = gurobiSolver;
		this.eventProcessor = eventProcessor;
		this.groupGeneratorProvider = groupGeneratorProvider;
		activeRequests = new LinkedHashSet<>();
		vgaVehiclesMapBydemandOnDemandVehicles = new HashMap<>();
		MathUtils.setTravelTimeProvider(travelTimeProvider);
		setEventHandeling();
        this.agentFactory = agentFactory;
        this.requestFactory = requestFactory;
        this.tripTransform = tripTransform;
        this.vehicleFactoryProvider = vehicleFactoryProvider;

        graph = highwayNetwork.getNetwork();
    }
    
    /**
     * Offline VGA + Chaining.
     * 
     * @param newRequests
     * @param waitingRequests
     * @return 
     */
       
    @Override 
    public Map<RideSharingOnDemandVehicle, DriverPlan> solve(List<PlanComputationRequest> newRequests, 
        List<PlanComputationRequest> waitingRequests) {

        List<TripTaxify<SimulationNode>> trips = tripTransform.loadTripsFromCsv();
        Collections.sort(trips, Comparator.comparing(TripTaxify::getStartTime));
        List<List<PlanComputationRequest>> requestBatches = groupRequests(trips);
       
        vgaVehicles = new LinkedList<>();

        initVehicle(requestBatches.get(0).get(0).getFrom());
        
        List<DriverPlan> allPlans = new LinkedList<>();

      // VGA
        for( List<PlanComputationRequest> requests: requestBatches){

            waitingRequests.addAll(requests);
            List<DriverPlan> plans = computePlans(requests, waitingRequests);
            allPlans.addAll(plans);
            waitingRequests.clear();
        }
        LOGGER.debug("Group generation finished, plans " + allPlans.size());
        LOGGER.debug(allPlans.stream().mapToInt(p -> p.size()).sum()/2 + 
            " trips in  " + allPlans.size() + " plans");

        
        // Chaining
        Demand demand = new GroupDemand(travelTimeProvider, config, allPlans, graph);
        Solution sol = new Solution(demand, travelTimeProvider, config);
        sol.buildPaths();
        
      // write results 
       List<Car> cars = sol.getAllCars();
       stats = new Statistics(demand, cars, travelTimeProvider, config);
       stats.writeResults(config.amodsimExperimentDir);
      

       return new HashMap<>();
    }     
   /**
    *  Divides requests into batches for VGA.
    *  Length of batch in defined in config.vga.offline.batchPeriod
    * 
    * @param trips
    * @return 
    */ 
    
    
    private List<List<PlanComputationRequest>>  groupRequests(List<TripTaxify<SimulationNode>> trips){

        List<List<PlanComputationRequest>> batches = new LinkedList<>();
        int batchPeriod = config.ridesharing.offline.batchPeriod;
        int start = (int) trips.get(0).getStartTime();
        
        int end = start + batchPeriod;
        List<PlanComputationRequest> batch = new ArrayList<>();
        for(TripTaxify<SimulationNode> trip : trips){

            DefaultPlanComputationRequest request = requestFactory.create(trip.id, trip.getStartNode(), 
                trip.getEndNode(), agentFactory.create("agent " + trip.id, trip.id, trip));
            if (trip.getStartTime() < end ) {
                batch.add(request);
            }else  {
                batches.add(batch);
                batch = new ArrayList<>();
                batch.add(request);
                end += batchPeriod;
             }
        }
        batches.add(batch);
        LOGGER.debug(batches.stream().mapToInt((b)-> b.size()).sum() +" trips in "+batches.size() + " batches");
        return batches;
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
        
		benchmark.measureTime(() -> generateGroups(waitingRequests));	
        
		groupGenerationTime = benchmark.getDurationMsInt();
        
		LOGGER.debug("groupGenerationTime " + benchmark.getDurationMsInt());
		LOGGER.debug("feasible plans  " + feasiblePlans.size());
		LOGGER.debug("active requests  " + activeRequests.size());
		/* Using an ILP solver to optimally assign a group to each vehicle */
		benchmark = new Benchmark();

        List<Plan<IOptimalPlanVehicle>> optimalPlans = 
            benchmark.measureTime(() -> gurobiSolver.assignOptimallyFeasiblePlans(
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
		planCount = 0;
        
		VGAVehicle vGAVehicle = vgaVehicles.get(0);

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

		}
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
        List<PlanAction> tasks = new ArrayList<>(plan.getActions().size());
        tasks.addAll(plan.getActions());
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
