package cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic;

import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.ticker.PeriodicTicker;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.utils.PositionUtil;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.simod.DemandData;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.event.DemandEvent;
import cz.cvut.fel.aic.simod.ridesharing.DARPSolver;
import cz.cvut.fel.aic.simod.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.simod.ridesharing.RidesharingDispatcher;
import cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanAction;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanActionDropoff;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanActionPickup;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;

import java.util.*;

public class RidesharingPFdispatcher extends RidesharingDispatcher {

//	TimeProvider timeProvider;

//	PositionUtil positionUtil;

//	IdGenerator tripIdGenerator;

	protected final DefaultPFPlanCompRequest.DefaultPFPlanComputationRequestFactory requestFactory;

	private List<PlanComputationRequest> newRequestsPackages;

	private final LinkedHashSet<PlanComputationRequest> waitingRequestsPackages;

	private final Map<Integer, PFPlanCompRequest> requestsMapByDemandPackages;

//	private DARPSolverPFShared solver;

//	private int requestCounter;

	@Inject
	public RidesharingPFdispatcher(OnDemandvehicleStationStorage onDemandvehicleStationStorage,
								   TypedSimulation eventProcessor,
								   SimodConfig config,
								   DARPSolverPFShared solver,
								   PeriodicTicker ticker,
								   DefaultPFPlanCompRequest.DefaultPFPlanComputationRequestFactory requestFactory,
								   TimeProvider timeProvider,
								   PositionUtil positionUtil,
								   IdGenerator tripIdGenerator) {
		super(onDemandvehicleStationStorage, eventProcessor, config, solver, ticker, null, timeProvider, positionUtil, tripIdGenerator);

//		this.timeProvider = timeProvider;
//		this.positionUtil = positionUtil;
		this.requestFactory = requestFactory;
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
			newRequest = this.requestFactory.create(requestCounter++, requestStartPosition,
					demandData.locations[1], demandData.demandAgent);
		}
		// if request is Package
		else {
			newRequest = this.requestFactory.create(requestCounter++, requestStartPosition,
					demandData.locations[1], demandData.demandPackage);
		}

		waitingRequestsPackages.add(newRequest);    // TODO jak funguji waitingReuqests??? (viz replan() )
		newRequestsPackages.add(newRequest);
		requestsMapByDemandPackages.put(newRequest.getDemandAgent().getSimpleId(), newRequest);
	}

	@Override
	protected void replan() {
		int droppedDemandsThisBatch = 0;

		// logger info
		int currentTimeSec = (int) Math.round(timeProvider.getCurrentSimTime() / 1000.0);
		LOGGER.info("Current sim time is: {} seconds", currentTimeSec);
		LOGGER.info("No. of new requests: {}", newRequestsPackages.size());
		LOGGER.info("No. of waiting requests: {}", waitingRequestsPackages.size());

		// dropping demands that waits too long
		Iterator<PlanComputationRequest> waitingRequestIterator = waitingRequestsPackages.iterator();
		while (waitingRequestIterator.hasNext()) {
			PlanComputationRequest request = waitingRequestIterator.next();
			if (request.getMaxPickupTime() + 5 < currentTimeSec) {
				request.getDemandAgent().setDropped(true);
				numberOfDemandsDropped++;
				droppedDemandsThisBatch++;
				waitingRequestIterator.remove();
				eventProcessor.addEvent(DemandEvent.LEFT, null, null, request);
				LOGGER.info("Demand {} dropped", request.getId());
			}
		}
		LOGGER.info("Demands dropped in this batch: {}", droppedDemandsThisBatch);
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
		newRequestsPackages = new LinkedList<>();
	}


	public PlanComputationRequest getRequest(int demandId) {    // TODO mozna bude potreba upravit na PFPlanComputationRequest
		return requestsMapByDemandPackages.get(demandId);
	}
}
