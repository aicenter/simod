/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.cvut.fel.aic.amodsim.entity;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.Trip;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.VehicleTrip;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.Agent;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.PhysicalVehicleDriveFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.agent.DelayData;
import cz.cvut.fel.aic.agentpolis.simmodel.agent.Driver;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.EntityType;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalTransportVehicle;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.amodsim.DemandSimulationEntityType;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.io.TimeTrip;
import cz.cvut.fel.aic.amodsim.statistics.DemandServiceStatistic;
import cz.cvut.fel.aic.amodsim.statistics.StatisticEvent;
import cz.cvut.fel.aic.amodsim.storage.DriveAgentStorage;
import cz.cvut.fel.aic.amodsim.storage.PhysicalTransportVehicleStorage;
import cz.cvut.fel.aic.geographtools.Node;
import java.util.List;

/**
 *
 * @author praveale
 */
public class DriveAgent extends Agent implements Driver<PhysicalTransportVehicle> {

    private DelayData delayData;

    private PhysicalTransportVehicle drivenCar;

    private SimulationNode targetNode;
    
    protected final PhysicalVehicleDriveFactory driveFactory;
    
    private final PhysicalTransportVehicleStorage vehicleStorage;
    
    private final DriveAgentStorage driveAgentStorage;
    
    private static final double LENGTH = 4;  
    
    protected PhysicalTransportVehicle vehicle;
    
    protected final TripsUtil tripsUtil;
    
    private final boolean precomputedPaths;
    
    private final PositionUtil positionUtil;
    
    protected final EventProcessor eventProcessor;
    
    protected final StandardTimeProvider timeProvider;
    
    private final AmodsimConfig config;   
    
    private List<Node> demandNodes;
   
    protected VehicleTrip currentTrip;
    
    protected VehicleTrip demandTrip;
	
    protected VehicleTrip tripToStation;

    private VehicleTrip completeTrip;
    
    private SimulationNode startNode;
    
    private SimulationNode finishNode;
    
    private long demandTime;
    
    private long minDemandServiceDuration;    

    @Inject
    public DriveAgent(PhysicalTransportVehicleStorage vehicleStorage, TripsUtil tripsUtil,
            PhysicalVehicleDriveFactory driveFactory, PositionUtil positionUtil, EventProcessor eventProcessor,
            StandardTimeProvider timeProvider, @Named("precomputedPaths") boolean precomputedPaths, 
            AmodsimConfig config, @Assisted String vehicleId, DriveAgentStorage driveAgentStorage,
            @Assisted SimulationNode startPosition) {
        super(vehicleId + " - autonomus agent", startPosition);
        this.startNode = startPosition;
        this.tripsUtil = tripsUtil;
        this.precomputedPaths = precomputedPaths;
        this.vehicleStorage = vehicleStorage;
        this.driveFactory = driveFactory;
        this.driveAgentStorage = driveAgentStorage;
        this.positionUtil = positionUtil;
        this.eventProcessor = eventProcessor;
        this.timeProvider = timeProvider;
        this.config = config;
        
        drivenCar = new PhysicalTransportVehicle(vehicleId + " - vehicle", 
                DemandSimulationEntityType.VEHICLE, LENGTH, config.amodsim.ridesharing.vehicleCapacity, 
				EGraphType.HIGHWAY, startPosition, 
                config.vehicleSpeedInMeters);
        
        vehicleStorage.addEntity(drivenCar);
        drivenCar.setDriver(this);
    }
    
    @Override
    public void born() {
        driveAgentStorage.addEntity(this);
        demandTime = timeProvider.getCurrentSimTime();
    }

    @Override
    public void die() {
        vehicleStorage.removeEntity(drivenCar);
        driveAgentStorage.removeEntity(this);
        
    }

    @Override
    public EntityType getType() {
        return DemandSimulationEntityType.DEMAND;
    }
    
    public long getDemandTime() {
        return demandTime;
    }
    
    public long getMinDemandServiceDuration() {
        return minDemandServiceDuration;
    }

    @Override
    public PhysicalTransportVehicle getVehicle() {
        return drivenCar;
    }

    @Override
    public void startDriving(PhysicalTransportVehicle vehicle) {
        this.drivenCar = vehicle;
        vehicle.setDriver(this);
    }

    @Override
    public void endDriving() {
        computeMinServiceDuration();
        if (isInsideArea()) {
            eventProcessor.addEvent(StatisticEvent.DEMAND_DROPPED_OFF, null, null, 
                new DemandServiceStatistic(demandTime, demandTime, timeProvider.getCurrentSimTime(),
						minDemandServiceDuration,
						getId(), drivenCar.getId()));
        }       
        die();
    }

    @Override
    public void setTargetNode(SimulationNode targetNode) {
        this.targetNode = targetNode;
    }

    @Override
    public SimulationNode getTargetNode() {
        return targetNode;
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
    public double getVelocity() {
        return 15;
    }

    public SimulationNode getStartNode() {
        return startNode;
    }

    public void setStartNode(SimulationNode startNode) {
        this.startNode = startNode;
    }
        
    public interface DriveAgentFactory {
        public DriveAgent create(String agentId, int id, SimulationNode startNode);
    } 
    
    private void computeMinServiceDuration() {
        Trip<SimulationNode> minTrip = tripsUtil.createTrip(startNode.id, getPosition().id);
        minDemandServiceDuration = (long) (tripsUtil.getTripDurationInSeconds(minTrip) * 1000);
    }
    
    private boolean isInsideArea() {
        Trip<SimulationNode> trip = tripsUtil.createTrip(startNode.id, getPosition().id);
        for (SimulationNode node : trip.getLocations()) {
            if (((node.lonE6 / 1E6 < 14.45981) && (node.lonE6 / 1E6 > 14.44041)) &&
                    ((node.latE6 / 1E6 < 50.11200) && (node.latE6 / 1E6 > 50.09640))) {
                return true;
            }
        }
        return false;
    }
}
