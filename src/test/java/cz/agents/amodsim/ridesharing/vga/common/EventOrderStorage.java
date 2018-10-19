/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.ridesharing.vga.common;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.typed.AliteEntity;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.amodsim.statistics.OnDemandVehicleEvent;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author David Fiedler
 */
@Singleton
public class EventOrderStorage extends AliteEntity{
	
	private final List<Event> onDemandVehicleEvents;

	public List<Event> getOnDemandVehicleEvents() {
		return onDemandVehicleEvents;
	}
	
	

	@Inject
	public EventOrderStorage(TypedSimulation eventProcessor) {
		onDemandVehicleEvents = new LinkedList<>();
		eventProcessor.addEventHandler(this);
		init(eventProcessor);
	}
	
	
	
	@Override
	protected List<Enum> getEventTypesToHandle() {
		List<Enum> typesToHandle = new LinkedList<>();
		typesToHandle.add(OnDemandVehicleEvent.PICKUP);
		typesToHandle.add(OnDemandVehicleEvent.DROP_OFF);
		return typesToHandle;
	}
	
	@Override
    public void handleEvent(Event event) {
		onDemandVehicleEvents.add(event);
	}
	
}
