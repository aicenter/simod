/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim;

import cz.cvut.fel.aic.amodsim.storage.OnDemandvehicleStationStorage;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.amodsim.event.OnDemandVehicleStationsCentralEvent;
import cz.cvut.fel.aic.amodsim.io.TimeTrip;
import com.vividsolutions.jts.geom.Coordinate;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandlerAdapter;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Node;
import cz.cvut.fel.aic.geographtools.util.NearestElementUtil;
import cz.cvut.fel.aic.geographtools.util.NearestElementUtil.SerializableIntFunction;
import cz.cvut.fel.aic.geographtools.util.NearestElementUtilPair;
import cz.cvut.fel.aic.geographtools.util.Transformer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author fido
 */
@Singleton
public class OnDemandVehicleStationsCentral extends EventHandlerAdapter{
    
    private final OnDemandvehicleStationStorage onDemandvehicleStationStorage;
    
    private final Transformer transformer;
    
    private final EventProcessor eventProcessor;
	
    private final AmodsimConfig config;
    
    private NearestElementUtil<OnDemandVehicleStation> nearestElementUtil;
    
    private int numberOfDemandsNotServedFromNearestStation;
    
    protected int numberOfDemandsDropped;
    
    private int demandsCount;
    
    private int rebalancingDropped;

    
    
    
    public int getNumberOfDemandsNotServedFromNearestStation() {
        return numberOfDemandsNotServedFromNearestStation;
    }

    public int getNumberOfDemandsDropped() {
        return numberOfDemandsDropped;
    }

    public int getDemandsCount() {
        return demandsCount;
    }

    public int getNumberOfRebalancingDropped() {
        return rebalancingDropped;
    }
    
 
    
    
    @Inject
    public OnDemandVehicleStationsCentral(OnDemandvehicleStationStorage onDemandvehicleStationStorage,
        EventProcessor eventProcessor, AmodsimConfig config, @Named("mapSrid") int srid) {
        this.onDemandvehicleStationStorage = onDemandvehicleStationStorage;
        this.eventProcessor = eventProcessor;
        this.config = config;
        transformer = new Transformer(srid);
        numberOfDemandsNotServedFromNearestStation = 0;
        numberOfDemandsDropped = 0;
        demandsCount = 0;
        rebalancingDropped = 0;
    }

        
    @Override
    public void handleEvent(Event event) {
        OnDemandVehicleStationsCentralEvent eventType = (OnDemandVehicleStationsCentralEvent) event.getType();
        
        switch(eventType){
            case DEMAND:
                processDemand(event);
                break;
            case REBALANCING:
                serveRebalancing(event);
                break;
        }
    }
    
    
    public OnDemandVehicleStation getNearestReadyStation(GPSLocation position){
        if(nearestElementUtil == null){
            nearestElementUtil = getNearestElementUtilForStations();
        }
        
        OnDemandVehicleStation[] onDemandVehicleStationsSorted 
                = (OnDemandVehicleStation[]) nearestElementUtil.getKNearestElements(position, 1);
        
//        OnDemandVehicleStation[] onDemandVehicleStationsSorted 
//                = (OnDemandVehicleStation[]) nearestElementUtil.getKNearestElements(position, getNumberOfstations() - 1);
        
        OnDemandVehicleStation nearestStation = null;
        int i = 0;
        while(i < onDemandVehicleStationsSorted.length){
            if(!onDemandVehicleStationsSorted[i].isEmpty()){
                if(i > 0){
                    numberOfDemandsNotServedFromNearestStation++;
                }
                nearestStation = onDemandVehicleStationsSorted[i];
                break;
            }
            i++;
        }
        
        return nearestStation;
    }
    
     public OnDemandVehicleStation getNearestStation(GPSLocation position){
        if(nearestElementUtil == null){
            nearestElementUtil = getNearestElementUtilForStations();
        }
        
        OnDemandVehicleStation nearestStation = nearestElementUtil.getNearestElement(position);
        return nearestStation;
    }
    
    protected NearestElementUtil<OnDemandVehicleStation> getNearestElementUtilForStations() {
        List<NearestElementUtilPair<Coordinate,OnDemandVehicleStation>> pairs = new ArrayList<>();
        
        OnDemandvehicleStationStorage.EntityIterator iterator = onDemandvehicleStationStorage.new EntityIterator();
		
        OnDemandVehicleStation station;
		while ((station = iterator.getNextEntity()) != null) {
            GPSLocation location = station.getPosition();
            
			pairs.add(new NearestElementUtilPair<>(new Coordinate(
                    location.getLongitude(), location.getLatitude(), location.elevation), station));
		}
		
		return new NearestElementUtil<>(pairs, transformer, new OnDemandVehicleStationArrayConstructor());
    }

    private void processDemand(Event event) {
        demandsCount++;
        DemandData demandData = (DemandData) event.getContent();
        List<SimulationNode> locations = demandData.locations;
        Node startNode = locations.get(0);
		
		serveDemand(startNode, demandData);
    }

    protected void serveRebalancing(Event event) {
        TimeTrip<OnDemandVehicleStation> rebalancingTrip = (TimeTrip<OnDemandVehicleStation>) event.getContent();
        OnDemandVehicleStation sourceStation = rebalancingTrip.getLocations().peek();
        boolean success = sourceStation.rebalance(rebalancingTrip);
        if(!success){
            rebalancingDropped++;
        }
    }

    private int getNumberOfstations() {
        return onDemandvehicleStationStorage.getEntityIds().size();
    }

	protected void serveDemand(Node startNode, DemandData demandData) {
            OnDemandVehicleStation nearestStation = getNearestReadyStation(startNode); 
            if(nearestStation != null){
		nearestStation.handleTripRequest(demandData);
            }
            else{
		numberOfDemandsDropped++;
            }
	}
    
    
    
    
    private static class OnDemandVehicleStationArrayConstructor 
        implements SerializableIntFunction<OnDemandVehicleStation[]>{

        @Override
        public OnDemandVehicleStation[] apply(int value) {
            return new OnDemandVehicleStation[value];
        }
    }
    
    
}
