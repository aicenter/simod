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
package cz.cvut.fel.aic.simod.entity;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.Trip;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.Agent;
import cz.cvut.fel.aic.agentpolis.simmodel.agent.TransportEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.EntityType;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.TransportableEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandler;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.simod.DemandData;
import cz.cvut.fel.aic.simod.DemandSimulationEntityType;
import cz.cvut.fel.aic.simod.StationsDispatcher;
import cz.cvut.fel.aic.simod.entity.transportable.SeatTransportableEntity;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleStationsCentralEvent;
import cz.cvut.fel.aic.simod.io.TimeTrip;
import cz.cvut.fel.aic.simod.statistics.DemandServiceStatistic;
import cz.cvut.fel.aic.simod.statistics.StatisticEvent;
import cz.cvut.fel.aic.simod.storage.DemandStorage;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author F-I-D-O
 */
public class DemandAgent extends Agent implements EventHandler, SeatTransportableEntity, SimulationAgent {
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DemandAgent.class);
	
	private final int simpleId;
	
	private final TimeTrip<SimulationNode> trip;
	
	private final StationsDispatcher onDemandVehicleStationsCentral;
	
	private final EventProcessor eventProcessor;
	
	private final DemandStorage demandStorage;
	
	private final StandardTimeProvider timeProvider;
	
	private final TripsUtil tripsUtil;
	
	/**
	 * Request announcement time in milliseconds
	 */
	private final long demandTime;
	
	
	private DemandAgentState state;
	
	private OnDemandVehicle onDemandVehicle;
	
	private TransportEntity transportEntity;
	
	private SimulationNode lastFromPosition;
	
//	private long scheduledPickupDelay;
	
	private long realPickupTime = 0;
	
	private long minDemandServiceDuration;
	
	// only to save compuatational time it|s 
//	private long currentServiceDuration;
	
	private boolean dropped;
	@Override
	public int getSimpleId() {
		return simpleId;
	}

//	public void setScheduledPickupDelay(long scheduledPickupDelay) {
//		this.scheduledPickupDelay = scheduledPickupDelay;
//	}
//
//	public long getScheduledPickupDelay() {
//		return scheduledPickupDelay;
//	}

	public long getRealPickupTime() {
		return realPickupTime;
	}

	@Override
	public long getDemandTime() {
		return demandTime;
	}

	public DemandAgentState getState() {
		return state;
	}

	public OnDemandVehicle getVehicle() {
		return onDemandVehicle;
	}

	public OnDemandVehicle getOnDemandVehicle() {
		return onDemandVehicle;
	}

	public long getMinDemandServiceDuration() {
		return minDemandServiceDuration;
	}

	@Override
	public void setDropped(boolean dropped) {
		this.dropped = dropped;
	}

	@Override
	public boolean isDropped() {
		return dropped;
	}
	
	

	
	
	
	@Inject
	public DemandAgent(StationsDispatcher onDemandVehicleStationsCentral, EventProcessor eventProcessor, 
			DemandStorage demandStorage, StandardTimeProvider timeProvider, TripsUtil tripsUtil,
			@Assisted String agentId, @Assisted int id, @Assisted TimeTrip<SimulationNode> trip) {
		super(agentId, trip.getLocations()[0]);
		this.simpleId = id;
		this.trip = trip;
		this.onDemandVehicleStationsCentral = onDemandVehicleStationsCentral;
		this.eventProcessor = eventProcessor;
		this.demandStorage = demandStorage;
		this.timeProvider = timeProvider;
		this.tripsUtil = tripsUtil;
		state = DemandAgentState.WAITING;
		dropped = false;
		demandTime = timeProvider.getCurrentSimTime();
		computeMinServiceDuration();
	}

	
	

	@Override
	public void born() {
		demandStorage.addEntity(this);
		eventProcessor.addEvent(OnDemandVehicleStationsCentralEvent.DEMAND, onDemandVehicleStationsCentral, null, 
				new DemandData(trip.getLocations(), this));
	}

	@Override
	public void die() {
		demandStorage.removeEntity(this);
	}

	@Override
	public EventProcessor getEventProcessor() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void handleEvent(Event event) {
		onDemandVehicle = (OnDemandVehicle) event.getContent();
//		vehicle = onDemandVehicle.getVehicle();
//		rideAsPassengerActivity.usingVehicleAsPassenger(this.getId(), onDemandVehicle.getVehicleId(), 
//				onDemandVehicle.getDemandTrip(this), this);
	}

	@Override
	public void tripEnded() {
		if(!getPosition().equals(trip.getLastLocation())){
			try {
				throw new Exception("Demand not served properly");
			} catch (Exception ex) {
				LOGGER.error(null, ex);
			}
		}
		eventProcessor.addEvent(StatisticEvent.DEMAND_DROPPED_OFF, null, null, 
				new DemandServiceStatistic(demandTime, realPickupTime, timeProvider.getCurrentSimTime(), 
						minDemandServiceDuration, getId(), onDemandVehicle.getId()));
		
		die();
	}

	@Override
	public void tripStarted(OnDemandVehicle vehicle) {
		if(state == DemandAgentState.DRIVING){
			try {
				throw new Exception(String.format("Demand Agent %s already driving in vehicle %s, it cannot be picked up by"
						+ "another vehicle %s", this, onDemandVehicle, vehicle));
			} catch (Exception ex) {
				Logger.getLogger(DemandAgent.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		else{
			state = DemandAgentState.DRIVING;
			realPickupTime = timeProvider.getCurrentSimTime();
			this.onDemandVehicle = vehicle;
		}
	}

	@Override
	public EntityType getType() {
		return DemandSimulationEntityType.DEMAND;
	}

	@Override
	public <T extends TransportEntity> T getTransportingEntity() {
		return (T) transportEntity;
	}

	@Override
	public <T extends TransportEntity> void setTransportingEntity(T transportingEntity) {
		this.transportEntity = transportingEntity;
	}

	@Override
	public void setLastFromPosition(SimulationNode lastFromPosition) {
		this.lastFromPosition = lastFromPosition;
	}

	private void computeMinServiceDuration() {
		Trip<SimulationNode> minTrip = tripsUtil.createTrip(getPosition(), trip.getLastLocation());
		minDemandServiceDuration = tripsUtil.getTripDuration(minTrip);
	}

	
	
	
	public interface DemandAgentFactory {
		public DemandAgent create(String agentId, int id, TimeTrip<SimulationNode> osmNodeTrip);
	}

	@Override
	public String getId() {
		return super.getId();
	}
}
