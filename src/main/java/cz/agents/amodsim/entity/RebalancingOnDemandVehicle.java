/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.entity;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import cz.agents.agentpolis.siminfrastructure.planner.trip.VehicleTrip;
import cz.agents.agentpolis.simmodel.agent.activity.movement.DriveVehicleActivity;
import cz.agents.agentpolis.simmodel.environment.model.VehiclePositionModel;
import cz.agents.agentpolis.simmodel.environment.model.VehicleStorage;
import cz.agents.agentpolis.simmodel.environment.model.entityvelocitymodel.EntityVelocityModel;
import cz.agents.agentpolis.simulator.visualization.visio.entity.EntityPositionUtil;
import cz.agents.agentpolis.simulator.visualization.visio.entity.VehiclePositionUtil;
import cz.agents.alite.common.event.Event;
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
public class RebalancingOnDemandVehicle extends OnDemandVehicle{
    
    private final LinkedList<Node> startNodes;
    
    private final LinkedList<Node> targetNodes;
    
    private final Map<DemandAgent,DemandData> demandsData;
    
    private Node target;
    
    
    @Inject
    public RebalancingOnDemandVehicle(DriveVehicleActivity driveVehicleActivity, Map<Long,Node> nodesMappedByNodeSourceIds, 
            VehicleStorage vehicleStorage, EntityVelocityModel entityVelocityModel, 
            VehiclePositionModel vehiclePositionModel, TripsUtil tripsUtil, 
            OnDemandVehicleStationsCentral onDemandVehicleStationsCentral, EntityPositionUtil entityPositionUtil,
            VehiclePositionUtil vehiclePositionUtil, @Named("precomputedPaths") boolean precomputedPaths, 
            @Assisted String vehicleId, @Assisted Node startPosition) {
        super(driveVehicleActivity, nodesMappedByNodeSourceIds, vehicleStorage, entityVelocityModel,
                vehiclePositionModel, tripsUtil, onDemandVehicleStationsCentral, entityPositionUtil, vehiclePositionUtil,
                precomputedPaths, vehicleId, startPosition);
        startNodes = new LinkedList<>();
        targetNodes = new LinkedList<>();
        demandsData = new HashMap<>();
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
        demandsData.put(demandData.demandAgent, new DemandData(demandData.demandAgent, demandTrip));
        
        if(state == OnDemandVehicleState.WAITING){
            state = OnDemandVehicleState.DRIVING_TO_START_LOCATION;
            chooseTarget();
            driveToDemandStartLocation();
        }
    }

    @Override
    protected void driveToDemandStartLocation() {
        if(vehiclePositionModel.getEntityPositionByNodeId(vehicle.getId()) == target.getId()){
			finishedDriving();
            return;
		}
		else{
			currentTrip = tripsUtil.createTrip(vehiclePositionModel.getEntityPositionByNodeId(vehicle.getId()), 
                target.getId(), vehicle);
            metersToStartLocation += entityPositionUtil.getTripLengthInMeters(currentTrip);
		}

		driveVehicleActivity.drive(getId(), vehicle, currentTrip.clone(), this);
    }

    @Override
    protected void driveToTargetLocation() {
        currentTrip = tripsUtil.createTrip(vehiclePositionModel.getEntityPositionByNodeId(vehicle.getId()), 
                target.getId(), vehicle);
        
        metersWithPassenger += entityPositionUtil.getTripLengthInMeters(currentTrip);
				
		driveVehicleActivity.drive(getId(), vehicle, currentTrip.clone(), this);
    }

    @Override
    protected void driveToNearestStation() {
        targetStation = onDemandVehicleStationsCentral.getNearestStation(target);
		
		if(target.getId() == targetStation.getPositionInGraph().getId()){
			currentTrip = null;
		}
		else{
			currentTrip = tripsUtil.createTrip(target.getId(), 
					targetStation.getPositionInGraph().getId(), vehicle);
            metersToStation += entityPositionUtil.getTripLengthInMeters(currentTrip);
		}
        
        if(currentTrip == null){
			finishedDriving();
			return;
		}
		
        
				
		driveVehicleActivity.drive(getId(), vehicle, currentTrip.clone(), this);
    }
    
    

    // change to find the nearest
    private void chooseTarget() {
        LinkedList<Node> collection = startNodes.isEmpty() ? targetNodes : startNodes;
        target = collection.poll();
    }

    @Override
    public void finishedDriving() {
        switch(state){
            case DRIVING_TO_START_LOCATION:
                boolean departure = startNodes.isEmpty();
                chooseTarget();
                if(departure){
                    state = OnDemandVehicleState.DRIVING_TO_TARGET_LOCATION;
                    departureStation.departure(this);
                    driveToTargetLocation();
                }
                else{
                    driveToDemandStartLocation();
                }
                break;
            case DRIVING_TO_TARGET_LOCATION:
                chooseTarget();
                if(target == null){
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
        
        private final VehicleTrip demandTrip;

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
        
        
        
    }
    
}
