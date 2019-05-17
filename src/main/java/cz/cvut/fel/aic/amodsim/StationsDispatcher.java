/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim;

import cz.cvut.fel.aic.amodsim.storage.OnDemandvehicleStationStorage;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.amodsim.event.OnDemandVehicleStationsCentralEvent;
import cz.cvut.fel.aic.amodsim.io.TimeTrip;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandlerAdapter;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Node;
import java.util.List;

/**
 *
 * @author fido
 */
@Singleton
public class StationsDispatcher extends EventHandlerAdapter{
	
	private final OnDemandvehicleStationStorage onDemandvehicleStationStorage;
	
	
	
	private final EventProcessor eventProcessor;
	
	protected final AmodsimConfig config;
	
	
	
	
	protected int numberOfDemandsDropped;
	
	private int demandsCount;
	
	private int rebalancingDropped;

	
	
	
	public int getNumberOfDemandsNotServedFromNearestStation() {
		return onDemandvehicleStationStorage.getNumberOfDemandsNotServedFromNearestStation();
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
	
	public boolean stationsOn(){
		return !onDemandvehicleStationStorage.isEmpty();
	}
	
	
	
	
	
	@Inject
	public StationsDispatcher(OnDemandvehicleStationStorage onDemandvehicleStationStorage,
			EventProcessor eventProcessor, AmodsimConfig config) {
		this.onDemandvehicleStationStorage = onDemandvehicleStationStorage;
		this.eventProcessor = eventProcessor;
		this.config = config;
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

	private void processDemand(Event event) {
		demandsCount++;
		DemandData demandData = (DemandData) event.getContent();
		List<SimulationNode> locations = demandData.locations;
		Node startNode = locations.get(0);
		
		serveDemand(startNode, demandData);
	}

	private void serveRebalancing(Event event) {
		TimeTrip<OnDemandVehicleStation> rebalancingTrip = (TimeTrip<OnDemandVehicleStation>) event.getContent();
		OnDemandVehicleStation sourceStation = rebalancingTrip.getLocations().peek();
		OnDemandVehicleStation targetStation = rebalancingTrip.getLocations().peekLast();
		rebalance(sourceStation, targetStation);
	}
	
	public void rebalance(OnDemandVehicleStation from, OnDemandVehicleStation to){
		boolean success = from.rebalance(to);
		if(!success){
			rebalancingDropped++;
		}
	}
	
	public void createBulkDelaydRebalancing(OnDemandVehicleStation from, OnDemandVehicleStation to, int amount, 
			int rebalancingIterval){
		createBulkDelaydRebalancing(from, to, amount, rebalancingIterval, 0);
	}
	
	public void createBulkDelaydRebalancing(OnDemandVehicleStation from, OnDemandVehicleStation to, int amount, 
			int rebalancingIterval, long inititalDelay){
		
		// event delay can't be zero
		if(inititalDelay == 0){
			inititalDelay = 1;
		}
		
		int intervalBetweenCars = rebalancingIterval / amount;
		long finalStartDelay = inititalDelay;

		for (int l = 0; l < amount; l++) {
			TimeTrip<OnDemandVehicleStation> rebalancingTrip = new TimeTrip<>(from, to, finalStartDelay);
			eventProcessor.addEvent(OnDemandVehicleStationsCentralEvent.REBALANCING, this, 
						null, rebalancingTrip, finalStartDelay);
			finalStartDelay += intervalBetweenCars;
		}
	}

	private int getNumberOfstations() {
		return onDemandvehicleStationStorage.getEntityIds().size();
	}

	protected void serveDemand(Node startNode, DemandData demandData) {
		OnDemandVehicleStation nearestStation = onDemandvehicleStationStorage.getNearestReadyStation(startNode); 
		if(nearestStation != null){
			nearestStation.handleTripRequest(demandData);
		}
		else{
			numberOfDemandsDropped++;
		}
	}

	public OnDemandVehicleStation getNearestStation(GPSLocation position) {
		return onDemandvehicleStationStorage.getNearestStation(position);
	}
	
	
	
	
	

	
	
}
