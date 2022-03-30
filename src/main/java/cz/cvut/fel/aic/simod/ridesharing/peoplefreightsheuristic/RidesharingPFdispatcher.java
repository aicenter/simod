package cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic;

import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.ticker.PeriodicTicker;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.utils.PositionUtil;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.simod.DemandData;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEvent;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEventContent;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleStationsCentralEvent;
import cz.cvut.fel.aic.simod.ridesharing.RidesharingDispatcher;
import cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic.DARPSolverPFShared;
import cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic.PFPlanComputationRequest;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.simod.ridesharing.vga.VehicleGroupAssignmentSolver;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RidesharingPFdispatcher extends RidesharingDispatcher {

	DARPSolverPFShared solver;

	TimeProvider timeProvider;

	PositionUtil positionUtil;

	IdGenerator tripIdGenerator;

	protected final PFPlanComputationRequest.PFPlanComputationRequestFactory requestFactory;

	private List<PlanComputationRequest> newRequests;

	private final LinkedHashSet<PlanComputationRequest> waitingRequests;

	private final Map<Integer, PlanComputationRequest> requestsMapByDemandEntities;

	private int requestCounter;

	@Inject
	public RidesharingPFdispatcher(OnDemandvehicleStationStorage onDemandvehicleStationStorage,
								   TypedSimulation eventProcessor,
								   SimodConfig config,
								   DARPSolverPFShared solver,
								   PeriodicTicker ticker,
								   PFPlanComputationRequest.PFPlanComputationRequestFactory requestFactory,
								   TimeProvider timeProvider,
								   PositionUtil positionUtil,
								   IdGenerator tripIdGenerator) {
		super(onDemandvehicleStationStorage, eventProcessor, config, null, ticker, null, timeProvider, positionUtil, tripIdGenerator);

		this.solver = solver;
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


	@Override protected void serveDemand(SimulationNode startNode, DemandData demandData) {
		SimulationNode requestStartPosition = demandData.locations[0];
		PFPlanComputationRequest newRequest;
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
		requestsMapByDemandEntities.put(newRequest.getDemandEntity().getSimpleId(), newRequest);
		// TODO replan() ???
	}


	public PlanComputationRequest getRequest(int demandId) {	// TODO mozna bude potreba upravit na PFPlanComputationRequest
		return requestsMapByDemandEntities.get(demandId);
	}
}
