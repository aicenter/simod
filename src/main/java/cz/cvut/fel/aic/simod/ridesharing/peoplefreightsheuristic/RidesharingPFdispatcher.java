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

	TimeProvider timeProvider;

	PositionUtil positionUtil;

	IdGenerator tripIdGenerator;

	protected final DefaultPFPlanCompRequest.DefaultPFPlanComputationRequestFactory requestFactory;

	private List<PlanComputationRequest> newRequests;

	private final LinkedHashSet<PlanComputationRequest> waitingRequests;

	private final Map<Integer, PlanComputationRequest> requestsMapByDemandEntities;

	private int requestCounter;

	@Inject
	public RidesharingPFdispatcher(OnDemandvehicleStationStorage onDemandvehicleStationStorage,
								   TypedSimulation eventProcessor,
								   SimodConfig config,
								   DARPSolver solver,
								   PeriodicTicker ticker,
								   DefaultPFPlanCompRequest.DefaultPFPlanComputationRequestFactory requestFactory,
								   TimeProvider timeProvider,
								   PositionUtil positionUtil,
								   IdGenerator tripIdGenerator) {
		super(onDemandvehicleStationStorage, eventProcessor, config, solver, ticker, null, timeProvider, positionUtil, tripIdGenerator);

		this.timeProvider = timeProvider;
		this.requestFactory = requestFactory;
		this.positionUtil = positionUtil;
		this.tripIdGenerator = tripIdGenerator;
		newRequests = new ArrayList<>();
		waitingRequests = new LinkedHashSet<>();
		requestsMapByDemandEntities = new HashMap<>();

		requestCounter = 0;

		setEventHandeling();
		solver.setDispatcher(this);
	}


	@Override
	protected void serveDemand(SimulationNode startNode, DemandData demandData) {
		SimulationNode requestStartPosition = demandData.locations[0];
		DefaultPFPlanCompRequest newRequest;
		// if request is Agent
		if (demandData.demandAgent != null)
		{
			newRequest = requestFactory.create(requestCounter++, requestStartPosition,
					demandData.locations[1], demandData.demandAgent);
		}
		// if request is Package
		else {
			newRequest = requestFactory.create(requestCounter++, requestStartPosition,
					demandData.locations[1], demandData.demandPackage);
		}

		waitingRequests.add(newRequest);	// TODO jak funguji waitingReuqests??? (viz replan() )
		newRequests.add(newRequest);
		requestsMapByDemandEntities.put(newRequest.getDemandAgent().getSimpleId(), newRequest);
		// TODO replan() ???
	}

	@Override
	protected void replan() {
		int droppedDemandsThisBatch = 0;

		// logger info
		int currentTimeSec = (int) Math.round(timeProvider.getCurrentSimTime() / 1000.0);
		LOGGER.info("Current sim time is: {} seconds", currentTimeSec);
		LOGGER.info("No. of new requests: {}", newRequests.size());
		LOGGER.info("No. of waiting requests: {}", waitingRequests.size());

		// dropping demands that waits too long
		Iterator<PlanComputationRequest> waitingRequestIterator = waitingRequests.iterator();
		while(waitingRequestIterator.hasNext()){
			PlanComputationRequest request = waitingRequestIterator.next();
			if(request.getMaxPickupTime() + 5  < currentTimeSec){
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
		long startTime = System.nanoTime();
		Map<RideSharingOnDemandVehicle, DriverPlan> newPlans
				= solver.solve(newRequests, new ArrayList<>(waitingRequests));
		long totalTime = System.nanoTime() - startTime;
		darpSolverComputationalTimes.add(totalTime);

		// executing new plans
		for(Map.Entry<RideSharingOnDemandVehicle,DriverPlan> entry: newPlans.entrySet()){
			RideSharingOnDemandVehicle vehicle = entry.getKey();
			DriverPlan plan = entry.getValue();
			vehicle.replan(plan);
		}

		// printing nice plans
		if(false){
			for(Map.Entry<RideSharingOnDemandVehicle,DriverPlan> entry: newPlans.entrySet()){
				RideSharingOnDemandVehicle vehicle = entry.getKey();
				DriverPlan plan = entry.getValue();

				int pickupCount = 0;
				int dropoffCount = 0;
				Set<SimulationNode> positions = new HashSet<>();
				boolean positionOverlaps = false;

				for(PlanAction planAction: plan){
					if(positions.contains(planAction.getPosition())){
						positionOverlaps = true;
						break;
					}
					positions.add(planAction.getPosition());
					if(planAction instanceof PlanActionPickup){
						pickupCount++;
					}
					else if(planAction instanceof PlanActionDropoff){
						dropoffCount++;
					}
				}

				if(!positionOverlaps && pickupCount > 1 && dropoffCount > 1){
					boolean nearby = false;

					for(SimulationNode node: positions){
						for(SimulationNode node2: positions){
							if(node != node2){
								int distance = (int) Math.round(positionUtil.getPosition(node).distance(
										positionUtil.getPosition(node2)));
								if(distance <  500){
									nearby = true;
									break;
								}
							}
							if(nearby){
								break;
							}
						}
					}
					if(!nearby){
						LOGGER.info(vehicle.getId() + ": " + plan.toString());
					}
				}
			}
		}

		// reseting new request for next iteration
		newRequests = new LinkedList<>();
	}


	public PlanComputationRequest getRequest(int demandId) {	// TODO mozna bude potreba upravit na PFPlanComputationRequest
		return requestsMapByDemandEntities.get(demandId);
	}
}
