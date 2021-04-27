/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of Amodsim project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.fel.aic.simod.visual.ridesharing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.typed.AliteEntity;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEvent;
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
		typesToHandle.add(OnDemandVehicleEvent.DEMAND_PICKUP);
		typesToHandle.add(OnDemandVehicleEvent.DEMAND_DROP_OFF);
		return typesToHandle;
	}
	
	@Override
	public void handleEvent(Event event) {
		onDemandVehicleEvents.add(event);
	}
	
}
