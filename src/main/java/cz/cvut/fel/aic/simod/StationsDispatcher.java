/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of the SiMoD project.
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
package cz.cvut.fel.aic.simod;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandlerAdapter;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleStationsCentralEvent;
import cz.cvut.fel.aic.simod.io.TimeTrip;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;

/**
 *
 * @author fido
 */
@Singleton
public class StationsDispatcher extends EventHandlerAdapter{
	
	private final OnDemandvehicleStationStorage onDemandvehicleStationStorage;
	
	
	
	protected final TypedSimulation eventProcessor;
	
	protected final SimodConfig config;

	protected final TimeProvider timeProvider;
	
	
	
	protected int numberOfDemandsDropped;
	

	
	private int rebalancingDropped;

	private IdGenerator tripIdGenerator;

	
	
	public int getNumberOfDemandsNotServedFromNearestStation() {
		return onDemandvehicleStationStorage.getNumberOfDemandsNotServedFromNearestStation();
	}

	public int getNumberOfDemandsDropped() {
		return numberOfDemandsDropped;
	}



	public int getNumberOfRebalancingDropped() {
		return rebalancingDropped;
	}
	
	public boolean stationsOn(){
		return !onDemandvehicleStationStorage.isEmpty();
	}
	
	
	
	
	
	@Inject
	public StationsDispatcher(
		OnDemandvehicleStationStorage onDemandvehicleStationStorage,
		TypedSimulation eventProcessor,
		SimodConfig config,
		IdGenerator tripIdGenerator,
		TimeProvider timeProvider
	) {
		this.onDemandvehicleStationStorage = onDemandvehicleStationStorage;
		this.eventProcessor = eventProcessor;
		this.config = config;
		this.tripIdGenerator = tripIdGenerator;
		this.timeProvider = timeProvider;
		numberOfDemandsDropped = 0;
		rebalancingDropped = 0;
	}



	@Override
	public void handleEvent(Event event) {
		OnDemandVehicleStationsCentralEvent eventType = (OnDemandVehicleStationsCentralEvent) event.getType();
		
		switch(eventType){
			case DEMAND:
				throw new UnsupportedOperationException("Demand event is not supported anymore");
			case REBALANCING:
				serveRebalancing(event);
				break;
		}
		
	}

	private void serveRebalancing(Event event) {
		TimeTrip<OnDemandVehicleStation> rebalancingTrip = (TimeTrip<OnDemandVehicleStation>) event.getContent();
		OnDemandVehicleStation sourceStation = rebalancingTrip.getFirstLocation();
		OnDemandVehicleStation targetStation = rebalancingTrip.getLastLocation();
		rebalance(sourceStation, targetStation);
	}
	
	public void rebalance(OnDemandVehicleStation from, OnDemandVehicleStation to){
		boolean success = from.rebalance(to);
		if(!success){
			rebalancingDropped++;
		}
	}

	public OnDemandVehicleStation getNearestStation(SimulationNode position) {
		return onDemandvehicleStationStorage.getNearestStation(position);
	}


	public int getDemandsCount() {
		return 0;
	}
}
