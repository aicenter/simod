/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim;

import com.mycompany.testsim.storage.OnDemandvehicleStationStorage;
import com.google.inject.Inject;
import com.mycompany.testsim.event.OnDemandVehicleStationEvent;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.mycompany.testsim.entity.OnDemandVehicleStation;
import com.mycompany.testsim.event.OnDemandVehicleStationsCentralEvent;
import com.vividsolutions.jts.geom.Coordinate;
import cz.agents.agentpolis.utils.nearestelement.NearestElementUtil;
import cz.agents.agentpolis.utils.nearestelement.NearestElementUtil.SerializableIntFunction;
import cz.agents.alite.common.event.Event;
import cz.agents.alite.common.event.EventHandlerAdapter;
import cz.agents.alite.common.event.EventProcessor;
import cz.agents.basestructures.GPSLocation;
import cz.agents.basestructures.Node;
import cz.agents.geotools.Transformer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.javatuples.Pair;

/**
 *
 * @author fido
 */
@Singleton
public class OnDemandVehicleStationsCentral extends EventHandlerAdapter{
    
    private final OnDemandvehicleStationStorage onDemandvehicleStationStorage;
    
    private final Transformer transformer;
    
    private final Map<Long,Node> nodesMappedByNodeSourceIds;
    
    private final EventProcessor eventProcessor;
    
    private NearestElementUtil<OnDemandVehicleStation> nearestElementUtil;
    
    
    
    
    @Inject
    public OnDemandVehicleStationsCentral(OnDemandvehicleStationStorage onDemandvehicleStationStorage, 
            Map<Long,Node> nodesMappedByNodeSourceIds, EventProcessor eventProcessor, @Named("mapSrid") int srid) {
        this.onDemandvehicleStationStorage = onDemandvehicleStationStorage;
        this.nodesMappedByNodeSourceIds = nodesMappedByNodeSourceIds;
        this.eventProcessor = eventProcessor;
        transformer = new Transformer(srid);
    }

    
    
    
    
    @Override
    public void handleEvent(Event event) {
        OnDemandVehicleStationsCentralEvent eventType = (OnDemandVehicleStationsCentralEvent) event.getType();
        
        switch(eventType){
            case DEMAND:
                serveDemand(event);
                break;
        }
        
    }
    
    public OnDemandVehicleStation getNearestStation(GPSLocation position){
        if(nearestElementUtil == null){
            nearestElementUtil = getNearestElementUtilForStations();
        }
        return nearestElementUtil.getNearestElement(new GPSLocation(position.latE6, position.lonE6, 0, 0, position.elevation));
    }
    
    private NearestElementUtil<OnDemandVehicleStation> getNearestElementUtilForStations() {
        List<Pair<Coordinate,OnDemandVehicleStation>> pairs = new ArrayList<>();
        
        OnDemandvehicleStationStorage.EntityIterator iterator = onDemandvehicleStationStorage.new EntityIterator();
		
        OnDemandVehicleStation station;
		while ((station = iterator.getNextEntity()) != null) {
            GPSLocation location = station.getPositionInGraph();
			pairs.add(new Pair<>(new Coordinate(location.getLongitude(), location.getLatitude(), location.getElevation()), station));
		}
		
		return new NearestElementUtil<>(pairs, transformer, new OnDemandVehicleStationArrayConstructor());
    }

    private void serveDemand(Event event) {
        DemandData demandData = (DemandData) event.getContent();
        List<Long> locations = demandData.locations;
        Node startNode = nodesMappedByNodeSourceIds.get(locations.get(0));
        OnDemandVehicleStation nearestStation = getNearestStation(startNode);        
        eventProcessor.addEvent(OnDemandVehicleStationEvent.TRIP, nearestStation, null, demandData);
    }
    
    
    
    
    
    private static class OnDemandVehicleStationArrayConstructor 
            implements SerializableIntFunction<OnDemandVehicleStation[]>{

        @Override
        public OnDemandVehicleStation[] apply(int value) {
            return new OnDemandVehicleStation[value];
        }

    }
    
    
}
