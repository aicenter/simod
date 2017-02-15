/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.entity;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.vividsolutions.jts.geom.Coordinate;
import cz.agents.amodsim.DemandData;
import cz.agents.amodsim.DemandSimulationEntityType;
import cz.agents.amodsim.storage.OnDemandvehicleStationStorage;
import cz.agents.amodsim.event.OnDemandVehicleStationEvent;
import cz.agents.amodsim.entity.OnDemandVehicle.OnDemandVehicleFactory;
import cz.agents.amodsim.io.TimeTrip;
import cz.agents.amodsim.storage.OnDemandVehicleStorage;
import cz.agents.agentpolis.siminfrastructure.description.DescriptionImpl;
import cz.agents.agentpolis.simmodel.entity.AgentPolisEntity;
import cz.agents.agentpolis.simmodel.entity.EntityType;
import cz.agents.agentpolis.simmodel.environment.model.AgentPositionModel;
import cz.agents.agentpolis.simmodel.environment.model.VehiclePositionModel;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.NearestElementUtils;
import cz.agents.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.agents.agentpolis.simulator.visualization.visio.entity.VehiclePositionUtil;
import cz.agents.agentpolis.utils.nearestelement.NearestElementUtil;
import cz.agents.alite.common.event.Event;
import cz.agents.alite.common.event.EventHandler;
import cz.agents.alite.common.event.EventProcessor;
import cz.agents.amodsim.OnDemandVehicleStationsCentral;
import cz.agents.basestructures.GPSLocation;
import cz.agents.basestructures.Node;
import cz.agents.geotools.Transformer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.javatuples.Pair;

/**
 *
 * @author fido
 */
public class OnDemandVehicleStation extends AgentPolisEntity implements EventHandler{

    private final LinkedList<OnDemandVehicle> parkedVehicles;
    
    private final EventProcessor eventProcessor;
    
    private final VehiclePositionModel vehiclePositionModel;
    
    private final Node positionInGraph;

    private final LinkedList<DepartureCard> departureCards;
    
    private final VehiclePositionUtil vehiclePositionUtil;
    
    private final Transformer transformer;
    
    private final PositionUtil positionUtil;
    
    private final OnDemandVehicleStationsCentral onDemandVehicleStationsCentral;
    
    private final Map<Long,Node> nodesMappedByNodeSourceIds;

    

    public Node getPositionInGraph() {
        return positionInGraph;
    }
    
    
    
    
    @Inject
    public OnDemandVehicleStation(EventProcessor eventProcessor, OnDemandVehicleFactory onDemandVehicleFactory, 
            NearestElementUtils nearestElementUtils, OnDemandvehicleStationStorage onDemandVehicleStationStorage,
			OnDemandVehicleStorage onDemandVehicleStorage, @Assisted String id, AgentPositionModel agentPositionModel,
			@Assisted Node node, @Assisted int initialVehicleCount, 
            VehiclePositionModel vehiclePositionModel, VehiclePositionUtil vehiclePositionUtil, Transformer transformer,
            PositionUtil positionUtil, OnDemandVehicleStationsCentral onDemandVehicleStationsCentral,
            Map<Long,Node> nodesMappedByNodeSourceIds) {
        super(id);
        this.eventProcessor = eventProcessor;
        positionInGraph = node;
        parkedVehicles = new LinkedList<>();
        for (int i = 0; i < initialVehicleCount; i++) {
			String onDemandVehicelId = String.format("%s-%d", id, i);
			OnDemandVehicle newVehicle = onDemandVehicleFactory.create(onDemandVehicelId, positionInGraph);
            parkedVehicles.add(newVehicle);
			onDemandVehicleStorage.addEntity(newVehicle);
			agentPositionModel.setNewEntityPosition(newVehicle.getId(), positionInGraph.getId());
        }
        onDemandVehicleStationStorage.addEntity(this);
        this.vehiclePositionModel = vehiclePositionModel;
        this.departureCards = new LinkedList<>();
        this.vehiclePositionUtil = vehiclePositionUtil;
        this.transformer = transformer;
        this.positionUtil = positionUtil;
        this.onDemandVehicleStationsCentral = onDemandVehicleStationsCentral;
        this.nodesMappedByNodeSourceIds = nodesMappedByNodeSourceIds;
    }
    
    
    

    @Override
    public EntityType getType() {
        return DemandSimulationEntityType.ON_DEMAND_VEHICLE_STATION;
    }

    @Override
    public DescriptionImpl getDescription() {
        return null;
    }
    
//    public void setNearestNode(final Node node){
//        positionInGraph = node;
//        
//    }

    @Override
    public EventProcessor getEventProcessor() {
        return null;
    }

    @Override
    public void handleEvent(Event event) {
        OnDemandVehicleStationEvent eventType = (OnDemandVehicleStationEvent) event.getType();
        switch(eventType){
            case TRIP:
                handleTripRequest((DemandData) event.getContent());
                break;
        }
    }
    
    public void parkVehicle(OnDemandVehicle onDemandVehicle){
        parkedVehicles.add(onDemandVehicle);
    }
    
    public int getParkedVehiclesCount(){
        return parkedVehicles.size();
    }
    

    private void handleTripRequest(DemandData demandData) {
        Node startLocation = nodesMappedByNodeSourceIds.get(demandData.locations.get(0));
        Node targetLocation = nodesMappedByNodeSourceIds.get(demandData.locations.get(demandData.locations.size() - 1));
        
        // hack for demands that starts and ends in the same position
        if(startLocation.equals(targetLocation)){
            return;
        }
        
        OnDemandVehicle vehicle = getVehicle(startLocation, targetLocation);
        
        if(vehicle != null){
            vehicle.setDepartureStation(this);
            eventProcessor.addEvent(null, vehicle, null, demandData);
            eventProcessor.addEvent(null, demandData.demandAgent, null, vehicle);
        }
    }

    public void rebalance(TimeTrip<OnDemandVehicleStation> rebalancingTrip) {
        OnDemandVehicle vehicle = getVehicle(this.getPositionInGraph(), 
                rebalancingTrip.getLocations().getLast().getPositionInGraph());
        if(vehicle != null){
//            if(rebalancingTrip.getLocations().getLast() == this){
//                System.out.println("com.mycompany.testsim.entity.OnDemandVehicleStation.rebalance()");
//            }
//            if(rebalancingTrip.getLocations().getLast().getPositionInGraph().getId()
//                    == vehiclePositionModel.getEntityPositionByNodeId(vehicle.getVehicleId())){
//                System.out.println("com.mycompany.testsim.entity.OnDemandVehicleStation.rebalance()");
//            }
//            if(this.getPositionInGraph() = vehicle)
            vehicle.driveToStation(rebalancingTrip.getLocations().getLast());
        }
    }
    
    public boolean isEmpty(){
        return parkedVehicles.isEmpty();
    }

    private OnDemandVehicle getVehicle(Node startLocation, Node targetLocation) {
        OnDemandVehicleStation targetStation 
                = onDemandVehicleStationsCentral.getNearestStation(targetLocation);
        NearestElementUtil<OnDemandVehicle> nearestElementUtil 
                = getNearestElementUtilForVehiclesBeforeDeparture(targetStation);
        
        OnDemandVehicle nearestVehicle;
        
        // ridesharing posibility test
        if(nearestElementUtil != null){
            nearestVehicle = nearestElementUtil.getNearestElement(startLocation);
        
            // currently cartesian distance check only!
            if(positionUtil.getPosition(nearestVehicle.getPosition()).distance(positionUtil.getPosition(startLocation))
                < positionUtil.getPosition(positionInGraph).distance(positionUtil.getPosition(startLocation))){
                // ridesharing successfully locked
                return nearestVehicle;
            }
        }

        nearestVehicle = parkedVehicles.poll();
        departureCards.add(new DepartureCard(nearestVehicle, targetStation));
        
        return nearestVehicle;
    }
    
    private NearestElementUtil<OnDemandVehicle> getNearestElementUtilForVehiclesBeforeDeparture(
            OnDemandVehicleStation targetStation) {
        List<Pair<Coordinate,OnDemandVehicle>> pairs = new ArrayList<>();
        
		for(DepartureCard departureCard : departureCards) {
            if(departureCard.getTargetStation() == targetStation){
                GPSLocation location = departureCard.getDemandVehicle().getPosition();

                pairs.add(new Pair<>(
                    new Coordinate(location.getLongitude(), location.getLatitude(), location.getElevation()),
                        departureCard.getDemandVehicle()));
            }
		}
        if(pairs.size() > 0){
            return new NearestElementUtil<>(pairs, transformer, new OnDemandVehicleStation.OnDemandVehicleArrayConstructor());
        }
        else{
            return null;
        }
    }
    
    public void departure(OnDemandVehicle onDemandVehicle){
        Iterator<DepartureCard> iterator = departureCards.iterator();
        while(iterator.hasNext()){
            DepartureCard departureCard = iterator.next();
            if(departureCard.demandVehicle == onDemandVehicle){
                iterator.remove();
                break;
            }
        }
//        departureCards.remove(onDemandVehicle);
    }
    
    
    
    public interface OnDemandVehicleStationFactory {
        public OnDemandVehicleStation create(String id, Node node, int initialVehicleCount);
    }
    
    
    private class DepartureCard{
        private final OnDemandVehicle demandVehicle;
        
        private final OnDemandVehicleStation targetStation;

        public OnDemandVehicle getDemandVehicle() {
            return demandVehicle;
        }

        public OnDemandVehicleStation getTargetStation() {
            return targetStation;
        }
        
        

        public DepartureCard(OnDemandVehicle demandVehicle, OnDemandVehicleStation targetStation) {
            this.demandVehicle = demandVehicle;
            this.targetStation = targetStation;
        }

    }
    
    private static class OnDemandVehicleArrayConstructor 
            implements NearestElementUtil.SerializableIntFunction<OnDemandVehicle[]>{

        @Override
        public OnDemandVehicle[] apply(int value) {
            return new OnDemandVehicle[value];
        }

    }
}
