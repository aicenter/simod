package cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.ticker.PeriodicTicker;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.utils.PositionUtil;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.simod.DemandData;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.event.DemandEvent;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEvent;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEventContent;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleStationsCentralEvent;
import cz.cvut.fel.aic.simod.ridesharing.DARPSolver;
import cz.cvut.fel.aic.simod.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.simod.ridesharing.RidesharingDispatcher;
import cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanAction;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanActionDropoff;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanActionPickup;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.simod.ridesharing.vga.VehicleGroupAssignmentSolver;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class RidesharingPFdispatcher extends RidesharingDispatcher {

//	TimeProvider timeProvider;

//	PositionUtil positionUtil;

//	IdGenerator tripIdGenerator;

//	protected final DefaultPFPlanCompRequest.DefaultPFPlanComputationRequestFactory requestFactory;

	protected final PlanComputationRequestPeople.PlanComputationRequestPeopleFactory peopleRequestFactory;

	protected final PlanComputationRequestFreight.PlanComputationRequestFreightFactory freightRequestFactory;

	private List<PlanComputationRequest> newRequestsPackages;

	private final LinkedHashSet<PlanComputationRequest> waitingRequestsPackages;

	private final Map<Integer, PlanComputationRequest> requestsMapByDemandPackages;

//	private DARPSolverPFShared solver;

//	private int requestCounter;

	@Inject
	public RidesharingPFdispatcher(OnDemandvehicleStationStorage onDemandvehicleStationStorage,
								   TypedSimulation eventProcessor,
								   SimodConfig config,
								   DARPSolverPFShared solver,
								   PeriodicTicker ticker,
								   PlanComputationRequestPeople.PlanComputationRequestPeopleFactory peopleRequestFactory,
								   PlanComputationRequestFreight.PlanComputationRequestFreightFactory freightRequestFactory,
								   TimeProvider timeProvider,
								   PositionUtil positionUtil,
								   IdGenerator tripIdGenerator) {
		super(onDemandvehicleStationStorage, eventProcessor, config, solver, ticker, null, timeProvider, positionUtil, tripIdGenerator);

//		this.timeProvider = timeProvider;
//		this.positionUtil = positionUtil;
		this.peopleRequestFactory = peopleRequestFactory;
		this.freightRequestFactory = freightRequestFactory;
		this.tripIdGenerator = tripIdGenerator;
		newRequestsPackages = new ArrayList<>();
		waitingRequestsPackages = new LinkedHashSet<>();
		requestsMapByDemandPackages = new HashMap<>();


		setEventHandeling();
		solver.setDispatcher(this);
	}


	@Override
	protected void serveDemand(SimulationNode startNode, DemandData demandData) {
		SimulationNode requestStartPosition = demandData.locations[0];
		DefaultPFPlanCompRequest newRequest;
		// if request is Agent
		if (demandData.demandAgent != null) {
			newRequest = this.peopleRequestFactory.create(requestCounter++, requestStartPosition,
					demandData.locations[1], demandData.demandAgent);
			waitingRequests.add(newRequest);
			newRequests.add(newRequest);
			requestsMapByDemandAgents.put(newRequest.getDemandEntity().getSimpleId(), newRequest);
		}
		// if request is Package
		else {
			newRequest = this.freightRequestFactory.create(requestCounter++, requestStartPosition,
					demandData.locations[1], demandData.demandPackage, demandData.demandPackage.getWeight());
			waitingRequestsPackages.add(newRequest);
			newRequestsPackages.add(newRequest);
			requestsMapByDemandPackages.put(newRequest.getDemandEntity().getSimpleId(), newRequest);
		}
	}


	@Override
	protected void replan() {
		int droppedDemandsThisBatch = 0;
		int droppedPackageDemandsThisBatch = 0;

		// logger info
		int currentTimeSec = (int) Math.round(timeProvider.getCurrentSimTime() / 1000.0);
		LOGGER.info("Current sim time is: {} seconds", currentTimeSec);
		LOGGER.info("No. of new requests: {}", newRequests.size() + newRequestsPackages.size());
		LOGGER.info("No. of waiting requests: {}", waitingRequests.size() + waitingRequestsPackages.size());


		// dropping demands that waits too long
		Iterator<PlanComputationRequest> waitingRequestIterator = waitingRequests.iterator();
		while(waitingRequestIterator.hasNext()){
			PlanComputationRequest request = waitingRequestIterator.next();
			if(request.getMaxPickupTime() + 5  < currentTimeSec){
				request.getDemandEntity().setDropped(true);
				numberOfDemandsDropped++;
				droppedDemandsThisBatch++;
				waitingRequestIterator.remove();
				eventProcessor.addEvent(DemandEvent.LEFT, null, null, request);
				LOGGER.info("Demand {} dropped", request.getId());
			}
		}
		LOGGER.info("People-Demands dropped in this batch: {}", droppedDemandsThisBatch);

		// dropping package-demands that waits too long
		Iterator<PlanComputationRequest> waitingPackageRequestIterator = waitingRequestsPackages.iterator();
		while (waitingRequestIterator.hasNext()) {
			PlanComputationRequest request = waitingPackageRequestIterator.next();
			if (request.getMaxPickupTime() + 5 < currentTimeSec) {
				request.getDemandEntity().setDropped(true);
				numberOfDemandsDropped++;
				droppedPackageDemandsThisBatch++;
				waitingRequestIterator.remove();
				eventProcessor.addEvent(DemandEvent.LEFT, null, null, request);
				LOGGER.info("Demand {} dropped", request.getId());
			}
		}
		LOGGER.info("Package-Demands dropped in this batch: {}", droppedPackageDemandsThisBatch);
		LOGGER.info("Total dropped demands count: {}", numberOfDemandsDropped);

		// DARP solving
		DARPSolverPFShared pfSolver = (DARPSolverPFShared) solver;
		long startTime = System.nanoTime();
		Map<PeopleFreightVehicle, DriverPlan> newPlans
				= pfSolver.solve(
				newRequests,
				new ArrayList<>(waitingRequests),
				newRequestsPackages,
				new ArrayList<>(waitingRequestsPackages));
		long totalTime = System.nanoTime() - startTime;
		darpSolverComputationalTimes.add(totalTime);

		// executing new plans
		for (Map.Entry<PeopleFreightVehicle, DriverPlan> entry : newPlans.entrySet()) {
			PeopleFreightVehicle vehicle = entry.getKey();
			DriverPlan plan = entry.getValue();
			if (plan.getLength() > 0) {
				vehicle.replan(plan);
			}
		}

		// printing nice plans
		if (false) {
			for (Map.Entry<PeopleFreightVehicle, DriverPlan> entry : newPlans.entrySet()) {
				RideSharingOnDemandVehicle vehicle = entry.getKey();
				DriverPlan plan = entry.getValue();

				int pickupCount = 0;
				int dropoffCount = 0;
				Set<SimulationNode> positions = new HashSet<>();
				boolean positionOverlaps = false;

				for (PlanAction planAction : plan) {
					if (positions.contains(planAction.getPosition())) {
						positionOverlaps = true;
						break;
					}
					positions.add(planAction.getPosition());
					if (planAction instanceof PlanActionPickup) {
						pickupCount++;
					}
					else if (planAction instanceof PlanActionDropoff) {
						dropoffCount++;
					}
				}

				if (!positionOverlaps && pickupCount > 1 && dropoffCount > 1) {
					boolean nearby = false;

					for (SimulationNode node : positions) {
						for (SimulationNode node2 : positions) {
							if (node != node2) {
								int distance = (int) Math.round(positionUtil.getPosition(node).distance(
										positionUtil.getPosition(node2)));
								if (distance < 500) {
									nearby = true;
									break;
								}
							}
							if (nearby) {
								break;
							}
						}
					}
					if (!nearby) {
						LOGGER.info(vehicle.getId() + ": " + plan.toString());
					}
				}
			}
		}

		// reseting new request for next iteration
		newRequests = new LinkedList<>();
		newRequestsPackages = new LinkedList<>();
	}

	@Override
	public void handleEvent(Event event) {
		// dispatcher common events
		if(event.getType() instanceof OnDemandVehicleStationsCentralEvent){
			super.handleEvent(event);
		}
		// pickup event
		else{
			OnDemandVehicleEvent eventType = (OnDemandVehicleEvent) event.getType();
			if(eventType == OnDemandVehicleEvent.PICKUP){
				OnDemandVehicleEventContent eventContent = (OnDemandVehicleEventContent) event.getContent();

				PlanComputationRequest request = requestsMapByDemandAgents.get(eventContent.getDemandId());
				if (request != null) {
					if(!waitingRequests.remove(request)){
						try {
							throw new cz.cvut.fel.aic.amodsim.SimodException("Request picked up but it is not present in the waiting request queue!");
						} catch (Exception ex) {
							Logger.getLogger(VehicleGroupAssignmentSolver.class.getName()).log(Level.SEVERE, null, ex);
						}
					}
					request.setOnboard(true);
				}
				else {
					PlanComputationRequest packageRequest = requestsMapByDemandPackages.get(eventContent.getDemandId());
					if(!waitingRequestsPackages.remove(packageRequest)){
						try {
							throw new cz.cvut.fel.aic.amodsim.SimodException("PackageRequest picked up but it is not present in the waiting request queue!");
						} catch (Exception ex) {
							Logger.getLogger(VehicleGroupAssignmentSolver.class.getName()).log(Level.SEVERE, null, ex);
						}
					}
					packageRequest.setOnboard(true);
				}
			}
		}
	}


	public PlanComputationRequest getRequest(int demandId) {    // TODO mozna bude potreba upravit na PFPlanComputationRequest
		return requestsMapByDemandPackages.get(demandId);
	}

	protected void setEventHandeling() {
		List<Enum> typesToHandle = new LinkedList<>();
		typesToHandle.add(OnDemandVehicleEvent.PICKUP);
		eventProcessor.addEventHandler(this, typesToHandle);
	}
}
