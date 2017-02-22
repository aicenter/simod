/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.entity;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import cz.agents.agentpolis.siminfrastructure.planner.trip.TripItem;
import cz.agents.agentpolis.siminfrastructure.planner.trip.VehicleTrip;
import cz.agents.agentpolis.siminfrastructure.time.TimeProvider;
import cz.agents.agentpolis.simmodel.activity.activityFactory.DriveActivityFactory;
import cz.agents.agentpolis.simmodel.agent.activity.movement.DriveVehicleActivity;
import cz.agents.agentpolis.simmodel.environment.model.VehicleStorage;
import cz.agents.agentpolis.simmodel.environment.model.entityvelocitymodel.EntityVelocityModel;
import cz.agents.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.agents.alite.common.event.Event;
import cz.agents.alite.common.event.EventProcessor;
import cz.agents.amodsim.OnDemandVehicleStationsCentral;
import cz.agents.amodsim.tripUtil.TripsUtil;
import cz.agents.basestructures.Node;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author fido
 */
public class RideSharingOnDemandVehicle extends OnDemandVehicle{
    
    private final LinkedList<Node> startNodes;
    
    private final LinkedList<Node> targetNodes;
    
    private final LinkedList<DemandData> demands;
    
    private final LinkedList<DemandData> pickedDemands;
    
    private final Map<DemandAgent,DemandData> demandsData;
    
    private final PositionUtil positionUtil;
    
    private DemandData currentlyServedDemmand;
    
    
    @Inject
    public RideSharingOnDemandVehicle(DriveVehicleActivity driveVehicleActivity, Map<Long,Node> nodesMappedByNodeSourceIds, 
            VehicleStorage vehicleStorage, EntityVelocityModel entityVelocityModel, TripsUtil tripsUtil, 
            OnDemandVehicleStationsCentral onDemandVehicleStationsCentral, DriveActivityFactory driveActivityFactory, 
            PositionUtil positionUtil, EventProcessor eventProcessor, TimeProvider timeProvider,
            @Named("precomputedPaths") boolean precomputedPaths, 
            @Assisted String vehicleId, @Assisted Node startPosition) {
        super(driveVehicleActivity, nodesMappedByNodeSourceIds, vehicleStorage, entityVelocityModel,
                tripsUtil, onDemandVehicleStationsCentral,
                driveActivityFactory, positionUtil, eventProcessor, timeProvider, precomputedPaths, vehicleId,
                startPosition);
        this.positionUtil = positionUtil;
        startNodes = new LinkedList<>();
        targetNodes = new LinkedList<>();
        demandsData = new HashMap<>();
        demands = new LinkedList<>();
        pickedDemands = new LinkedList<>();
    }
    
        
    public boolean hasFreeCapacity(){
        return targetNodes.size() < vehicle.getCapacity();
    }

    @Override
    public void handleEvent(Event event) {
        cz.agents.amodsim.DemandData demandData = (cz.agents.amodsim.DemandData) event.getContent();
        List<Long> locations = demandData.locations;
        
        Node startNode = nodesMappedByNodeSourceIds.get(locations.get(0));
        Node targetNode = nodesMappedByNodeSourceIds.get(locations.get(locations.size() - 1));

        startNodes.add(startNode);
        targetNodes.add(targetNode);
        
        // for the UseVehicleAsPassangerAction
        VehicleTrip demandTrip = tripsUtil.createTrip(startNode.id, targetNode.id, vehicle);
//        demandsData.put(demandData.demandAgent, new DemandData(demandData.demandAgent, demandTrip));
        demands.add(new DemandData(demandData.demandAgent, demandTrip));
        
        // release from station in case of full car
        if(demands.size() + pickedDemands.size() == vehicle.getCapacity()){
            departureStation.release(this);
        }
        
        if(state == OnDemandVehicleState.WAITING){
            state = OnDemandVehicleState.DRIVING_TO_START_LOCATION;
            leavingStationEvent();
            chooseTarget();
            driveToDemandStartLocation();
        }
    }

    @Override
    protected void driveToDemandStartLocation() {
        if(getPosition().id == currentlyServedDemmand.getStartNodeId()){
			finishedDriving();
            return;
		}
		else{
			currentTrip = tripsUtil.createTrip(getPosition().id, 
                currentlyServedDemmand.getStartNodeId(), vehicle);
            metersToStartLocation += positionUtil.getTripLengthInMeters(currentTrip);
		}

//		driveVehicleActivity.drive(getId(), vehicle, currentTrip.clone(), this);
        driveActivityFactory.create(this, vehicle, vehicleTripToTrip(currentTrip)).run();
    }

    @Override
    protected void driveToTargetLocation() {
        if(getPosition().id == currentlyServedDemmand.getTargetNodeId()){
			finishedDriving();
            return;
		}
        
        currentTrip = tripsUtil.createTrip(getPosition().id, 
                currentlyServedDemmand.getTargetNodeId(), vehicle);
        
        metersWithPassenger += positionUtil.getTripLengthInMeters(currentTrip);
				
//		driveVehicleActivity.drive(getId(), vehicle, currentTrip.clone(), this);
        driveActivityFactory.create(this, vehicle, vehicleTripToTrip(currentTrip)).run();
    }

    @Override
    protected void driveToNearestStation() {
        targetStation = onDemandVehicleStationsCentral.getNearestStation(getPosition());
		
		if(getPosition().equals(targetStation.getPositionInGraph())){
			currentTrip = null;
		}
		else{
			currentTrip = tripsUtil.createTrip(getPosition().id, 
					targetStation.getPositionInGraph().getId(), vehicle);
            metersToStation += positionUtil.getTripLengthInMeters(currentTrip);
		}
        
        if(currentTrip == null){
			finishedDriving();
			return;
		}

//		driveVehicleActivity.drive(getId(), vehicle, currentTrip.clone(), this);
        driveActivityFactory.create(this, vehicle, vehicleTripToTrip(currentTrip)).run();
    }
    
    

    // change to find the nearest
    private void chooseTarget() {
//        LinkedList<Node> collection = startNodes.isEmpty() ? targetNodes : startNodes;
//        target = collection.poll();
        LinkedList<DemandData> collection = demands.isEmpty() ? pickedDemands : demands;
        currentlyServedDemmand = collection.poll();
    }

    @Override
    public void finishedDriving() {
        switch(state){
            case DRIVING_TO_START_LOCATION:
                cargo.add(currentlyServedDemmand.demandAgent);
                currentlyServedDemmand.getDemandAgent().tripStarted();
                pickedDemands.add(currentlyServedDemmand);
                boolean departure = demands.isEmpty();
                chooseTarget();
                if(departure){
                    state = OnDemandVehicleState.DRIVING_TO_TARGET_LOCATION;
                    
                    // release from station if not done yet
                    if(pickedDemands.size() < vehicle.getCapacity()){
                        departureStation.release(this);
                    }
                    driveToTargetLocation();
                }
                else{
                    driveToDemandStartLocation();
                }
                break;
            case DRIVING_TO_TARGET_LOCATION:
                cargo.remove(currentlyServedDemmand.demandAgent);
                currentlyServedDemmand.demandAgent.tripEnded();
                chooseTarget();
                if(currentlyServedDemmand == null){
                    state = OnDemandVehicleState.DRIVING_TO_STATION;
                    driveToNearestStation();
                }
                else{
                    driveToTargetLocation();
                }
                
                break;
            case DRIVING_TO_STATION:
            case REBALANCING:
                waitInStation();
                break;
        }
    }

    @Override
    public VehicleTrip getDemandTrip(DemandAgent agent) {
        return demandsData.get(agent).demandTrip;
    }
    
    
    
    private class DemandData{
        private final DemandAgent demandAgent;
        
        private final VehicleTrip<TripItem> demandTrip;

        public DemandAgent getDemandAgent() {
            return demandAgent;
        }

        public VehicleTrip getDemandTrip() {
            return demandTrip;
        }
        
        

        public DemandData(DemandAgent demandAgent, VehicleTrip demandTrip) {
            this.demandAgent = demandAgent;
            this.demandTrip = demandTrip;
        }
        
        public int getStartNodeId(){
            return demandTrip.getLocations().getFirst().tripPositionByNodeId;
        }
        
        public int getTargetNodeId(){
            return demandTrip.getLocations().getLast().tripPositionByNodeId;
        }
        
    }
    
}
