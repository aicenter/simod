/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.entity.vehicle;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import cz.agents.amodsim.DemandSimulationEntityType;
import cz.agents.amodsim.OnDemandVehicleStationsCentral;
import cz.agents.amodsim.PlanningAgent;
import cz.agents.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.agents.agentpolis.siminfrastructure.description.DescriptionImpl;
import cz.agents.agentpolis.siminfrastructure.planner.trip.Trip;
import cz.agents.agentpolis.siminfrastructure.planner.trip.TripItem;
import cz.agents.agentpolis.siminfrastructure.planner.trip.VehicleTrip;
import cz.agents.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.agents.agentpolis.simmodel.Activity;
import cz.agents.agentpolis.simmodel.Agent;
import cz.agents.agentpolis.simmodel.IdGenerator;
import cz.agents.agentpolis.simmodel.activity.activityFactory.PhysicalVehicleDriveFactory;
import cz.agents.agentpolis.simmodel.agent.Driver;
import cz.agents.agentpolis.simmodel.environment.model.VehicleStorage;
import cz.agents.agentpolis.simmodel.environment.model.action.driving.DelayData;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.EGraphType;
import cz.agents.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.agents.alite.common.event.Event;
import cz.agents.alite.common.event.EventHandler;
import cz.agents.alite.common.event.EventProcessor;
import cz.agents.amodsim.DemandData;
import cz.agents.amodsim.config.Config;
import cz.agents.amodsim.entity.DemandAgent;
import cz.agents.amodsim.entity.OnDemandVehicleState;
import cz.agents.amodsim.entity.OnDemandVehicleStation;
import cz.agents.amodsim.statistics.OnDemandVehicleEvent;
import cz.agents.amodsim.statistics.OnDemandVehicleEventContent;
import cz.agents.amodsim.statistics.PickupEventContent;
import cz.agents.basestructures.Node;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import cz.agents.agentpolis.simmodel.entity.EntityType;
import cz.agents.agentpolis.simmodel.entity.vehicle.PhysicalTransportVehicle;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.SimulationNode;


/**
 *
 * @author fido
 */
public class OnDemandVehicle extends Agent implements EventHandler, PlanningAgent,
        Driver<PhysicalTransportVehicle>{
    
    private static final double LENGTH = 4;
    
    private static final int CAPACITY = 5;
    
    
    
    
    
    protected PhysicalTransportVehicle vehicle;
    
    protected final Map<Long,SimulationNode> nodesMappedByNodeSourceIds;
    
    protected final TripsUtil tripsUtil;
    
    private final boolean precomputedPaths;
    
    protected final OnDemandVehicleStationsCentral onDemandVehicleStationsCentral;
    
    protected final PhysicalVehicleDriveFactory driveFactory;
    
    private final PositionUtil positionUtil;
    
    private final EventProcessor eventProcessor;
    
    private final StandardTimeProvider timeProvider;
    
    private final IdGenerator rebalancingIdGenerator;
    
    private final Config config;
    
    
    private List<Node> demandNodes;
    
    protected OnDemandVehicleState state;
    
    protected OnDemandVehicleStation departureStation;
    
    protected OnDemandVehicleStation targetStation;
    
    protected VehicleTrip currentTrip;
    
    private VehicleTrip demandTrip;
	
	protected VehicleTrip tripToStation;
	
	private VehicleTrip completeTrip;
    
    protected int metersWithPassenger;
    
    protected int metersToStartLocation;
    
    protected int metersToStation;
    
    private int metersRebalancing;
    
    private SimulationNode targetNode;
    
    private DelayData delayData;
    
    protected DemandData currentlyServedDemmand;
    
    private int currentRebalancingId;
    
    
    
    public VehicleTrip getCurrentTrips() {
        return currentTrip;
    }

    public VehicleTrip getDemandTrip(DemandAgent agent) {
        return demandTrip.clone();
    }

    public OnDemandVehicleState getState() {
        return state;
    }

    public int getMetersWithPassenger() {
        return metersWithPassenger;
    }

    public int getMetersToStartLocation() {
        return metersToStartLocation;
    }

    public int getMetersToStation() {
        return metersToStation;
    }

    public int getMetersRebalancing() {
        return metersRebalancing;
    }

    // remove in future to be more agent-like
    public void setDepartureStation(OnDemandVehicleStation departureStation) {
        this.departureStation = departureStation;
    }

    
    
    
    
    @Inject
    public OnDemandVehicle(Map<Long,SimulationNode> nodesMappedByNodeSourceIds, VehicleStorage vehicleStorage, 
            TripsUtil tripsUtil, OnDemandVehicleStationsCentral onDemandVehicleStationsCentral, 
            PhysicalVehicleDriveFactory driveFactory, PositionUtil positionUtil, EventProcessor eventProcessor,
            StandardTimeProvider timeProvider, @Named("precomputedPaths") boolean precomputedPaths, 
            IdGenerator rebalancingIdGenerator, Config config, @Assisted String vehicleId,
            @Assisted SimulationNode startPosition) {
        super(vehicleId + " - autonomus agent", startPosition);
        this.nodesMappedByNodeSourceIds = nodesMappedByNodeSourceIds;
        this.tripsUtil = tripsUtil;
        this.precomputedPaths = precomputedPaths;
        this.onDemandVehicleStationsCentral = onDemandVehicleStationsCentral;
        this.driveFactory = driveFactory;
        this.positionUtil = positionUtil;
        this.eventProcessor = eventProcessor;
        this.timeProvider = timeProvider;
        this.rebalancingIdGenerator = rebalancingIdGenerator;
        this.config = config;
        
        vehicle = new PhysicalTransportVehicle(vehicleId + " - vehicle", 
                DemandSimulationEntityType.VEHICLE, LENGTH, CAPACITY, EGraphType.HIGHWAY, startPosition, 
                config.vehicleSpeedInMeters);
        
        vehicleStorage.addEntity(vehicle);
        state = OnDemandVehicleState.WAITING;
        
        metersWithPassenger = 0;
        metersToStartLocation = 0;
        metersToStation = 0;
        metersRebalancing = 0;
    }

    @Override
    public DescriptionImpl getDescription() {
        return null;
    }

    @Override
    public EventProcessor getEventProcessor() {
        return null;
    }
    
    public String getVehicleId(){
        return vehicle.getId();
    }

    @Override
    public void handleEvent(Event event) {
        currentlyServedDemmand = (DemandData) event.getContent();
        List<Long> locations = currentlyServedDemmand.locations;
        
        demandNodes = new ArrayList<>();
		if(precomputedPaths){
			for (Long location : locations) {
				demandNodes.add(nodesMappedByNodeSourceIds.get(location));
			}
		}
		else{
			demandNodes.add(nodesMappedByNodeSourceIds.get(locations.get(0)));
			demandNodes.add(nodesMappedByNodeSourceIds.get(locations.get(locations.size() - 1)));
		}
        leavingStationEvent();
        driveToDemandStartLocation();
    }
    
    public void finishedDriving() {
        switch(state){
            case DRIVING_TO_START_LOCATION:
                driveToTargetLocation();
                break;
            case DRIVING_TO_TARGET_LOCATION:
                dropOffDemand();
                driveToNearestStation();
                break;
            case DRIVING_TO_STATION:
                eventProcessor.addEvent(OnDemandVehicleEvent.REACH_NEAREST_STATION, null, null, 
                    new OnDemandVehicleEventContent(timeProvider.getCurrentSimTime(), 
                        currentlyServedDemmand.demandAgent.getSimpleId()));
                waitInStation();
                break;
            case REBALANCING:
                eventProcessor.addEvent(OnDemandVehicleEvent.FINISH_REBALANCING, null, null, 
                    new OnDemandVehicleEventContent(timeProvider.getCurrentSimTime(), 
                        currentRebalancingId));
                waitInStation();
                break;
        }
    }

    protected void driveToDemandStartLocation() {
        
        if(getPosition() == demandNodes.get(0)){
			currentTrip = null;
		}
		else{
			currentTrip = tripsUtil.createTrip(getPosition().id, 
                demandNodes.get(0).getId(), vehicle);
            metersToStartLocation += positionUtil.getTripLengthInMeters(currentTrip);
		}
        if(precomputedPaths){
            demandTrip = tripsUtil.locationsToVehicleTrip(demandNodes, precomputedPaths, vehicle);
        }
        else{
            demandTrip = tripsUtil.createTrip(demandNodes.get(0).getId(), demandNodes.get(1).getId(), vehicle);
        }
        metersWithPassenger += positionUtil.getTripLengthInMeters(demandTrip);
		
		Node demandEndNode = demandNodes.get(demandNodes.size() - 1);
		
		targetStation = onDemandVehicleStationsCentral.getNearestStation(demandEndNode);
		
		if(demandEndNode.getId() == targetStation.getPosition().getId()){
			tripToStation = null;
		}
		else{
			tripToStation = tripsUtil.createTrip(demandEndNode.getId(), 
					targetStation.getPosition().getId(), vehicle);
            metersToStation += positionUtil.getTripLengthInMeters(tripToStation);
		}
		
		completeTrip = TripsUtil.mergeTripsOld(currentTrip, demandTrip, tripToStation);
		
		if(currentTrip == null){
			driveToTargetLocation();
			return;
		}
		
		state = OnDemandVehicleState.DRIVING_TO_START_LOCATION;
				
//		driveVehicleActivity.drive(getId(), vehicle, currentTrip.clone(), this);
//        driveActivityFactory.create(this, vehicle, vehicleTripToTrip(currentTrip)).run();
        driveFactory.runActivity(this, vehicle, vehicleTripToTrip(currentTrip));
    }

    

    protected void driveToTargetLocation() {
        state = OnDemandVehicleState.DRIVING_TO_TARGET_LOCATION;
        pickupDemand();
        
        departureStation.release(this);
        
        currentTrip = demandTrip;
				
//		driveVehicleActivity.drive(getId(), vehicle, currentTrip.clone(), this);
//        driveActivityFactory.create(this, vehicle, vehicleTripToTrip(currentTrip)).run();
        driveFactory.runActivity(this, vehicle, vehicleTripToTrip(currentTrip));
        
    }

    protected void driveToNearestStation() {
		if(tripToStation == null){
			waitInStation();
			return;
		}
		
        state = OnDemandVehicleState.DRIVING_TO_STATION;

        currentTrip = tripToStation;  
				
//		driveVehicleActivity.drive(getId(), vehicle, currentTrip.clone(), this);
//        driveActivityFactory.create(this, vehicle, vehicleTripToTrip(currentTrip)).run();
        driveFactory.runActivity(this, vehicle, vehicleTripToTrip(currentTrip));
    }

    protected void waitInStation() {
        targetStation.parkVehicle(this);
        state = OnDemandVehicleState.WAITING;
		completeTrip = null;
    }

	@Override
	public VehicleTrip getCurrentPlan() {
		return completeTrip;
	}
	
	public Node getDemandTarget(){
        if(demandNodes != null){
            return demandNodes.get(demandNodes.size() - 1);
        }
		return null;
	}

    public void startRebalancing(OnDemandVehicleStation targetStation) {
        state = OnDemandVehicleState.REBALANCING;
        currentRebalancingId = rebalancingIdGenerator.getId();
        eventProcessor.addEvent(OnDemandVehicleEvent.START_REBALANCING, null, null, 
                new OnDemandVehicleEventContent(timeProvider.getCurrentSimTime(), 
                       currentRebalancingId));
        
        currentTrip = tripsUtil.createTrip(getPosition().id, 
                targetStation.getPosition().getId(), vehicle);
        metersRebalancing += positionUtil.getTripLengthInMeters(currentTrip);
        
        completeTrip = currentTrip.clone();
        
        this.targetStation = targetStation;
        
//        driveVehicleActivity.drive(getId(), vehicle, currentTrip.clone(), this);
//        driveActivityFactory.create(this, vehicle, vehicleTripToTrip(currentTrip)).run();
        driveFactory.runActivity(this, vehicle, vehicleTripToTrip(currentTrip));
    }

    @Override
    public PhysicalTransportVehicle getVehicle() {
        return vehicle;
    }

    @Override
    public double getVelocity() {
        return config.vehicleSpeedInMeters;
    }

//    @Override
//    public List<AgentPolisEntity> getTransportedEntities() {
//        return cargo;
//    }

    @Override
    public void setTargetNode(SimulationNode targetNode) {
        this.targetNode = targetNode;
    }

    @Override
    public SimulationNode getTargetNode() {
        return targetNode;
    }

    protected void leavingStationEvent() {
        eventProcessor.addEvent(OnDemandVehicleEvent.LEAVE_STATION, null, null, 
                new OnDemandVehicleEventContent(timeProvider.getCurrentSimTime(), 
                        currentlyServedDemmand.demandAgent.getSimpleId()));
    }

    private void pickupDemand() {
        currentlyServedDemmand.demandAgent.tripStarted();
        vehicle.pickUp(currentlyServedDemmand.demandAgent);
        eventProcessor.addEvent(OnDemandVehicleEvent.PICKUP, null, null, 
                new PickupEventContent(timeProvider.getCurrentSimTime(), 
                        currentlyServedDemmand.demandAgent.getSimpleId(), 
                        positionUtil.getTripLengthInMeters(demandTrip)));
    }
    
    protected void dropOffDemand() {
        currentlyServedDemmand.demandAgent.tripEnded();
        vehicle.dropOff(currentlyServedDemmand.demandAgent);
        eventProcessor.addEvent(OnDemandVehicleEvent.DROP_OFF, null, null, 
                new OnDemandVehicleEventContent(timeProvider.getCurrentSimTime(), 
                        currentlyServedDemmand.demandAgent.getSimpleId()));
    }
    
    // todo - repair path planner and remove this
    protected Trip<Node> vehicleTripToTrip(VehicleTrip<TripItem> vehicleTrip){
        LinkedList<Node>  locations = new LinkedList<>();
        for (TripItem tripItem : vehicleTrip.getLocations()) {
            locations.add(positionUtil.getNode(tripItem.tripPositionByNodeId));
        }
        Trip<Node> trip = new Trip<>(locations);
        
        return trip;
    }

    @Override
    protected void onActivityFinish(Activity activity) {
        super.onActivityFinish(activity);
        finishedDriving();
    }

    @Override
    public EntityType getType() {
        return DemandSimulationEntityType.ON_DEMAND_VEHICLE;
    }


    @Override
    public void startDriving(PhysicalTransportVehicle vehicle){
        this.vehicle = vehicle;
        vehicle.setDriver(this);
    }

    @Override
    public void setDelayData(DelayData delayData) {
        this.delayData = delayData;
    }

    @Override
    public DelayData getDelayData() {
        return delayData;
    }

    @Override
    public void endDriving() {
        
    }
    
    
}
