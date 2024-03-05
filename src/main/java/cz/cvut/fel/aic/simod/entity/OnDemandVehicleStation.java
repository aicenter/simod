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
package cz.cvut.fel.aic.simod.entity;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.AgentPolisEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.EntityType;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.SimpleTransportVehicle;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.NearestElementUtils;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioPositionUtil;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandler;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.simod.DemandData;
import cz.cvut.fel.aic.simod.DemandSimulationEntityType;
import cz.cvut.fel.aic.simod.StationsDispatcher;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.agent.OnDemandVehicle;
import cz.cvut.fel.aic.simod.entity.vehicle.MoDVehicle;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicleFactorySpec;
import cz.cvut.fel.aic.simod.entity.vehicle.SimpleMoDVehicle;
import cz.cvut.fel.aic.simod.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Node;
import cz.cvut.fel.aic.geographtools.WKTPrintableCoord;
import cz.cvut.fel.aic.geographtools.util.NearestElementUtil;
import cz.cvut.fel.aic.geographtools.util.NearestElementUtilPair;
import cz.cvut.fel.aic.geographtools.util.Transformer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.LoggerFactory;

/**
 *
 * @author fido
 */
public class OnDemandVehicleStation extends AgentPolisEntity implements EventHandler, WKTPrintableCoord{

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(OnDemandVehicleStation.class);

	private static final int LENGTH = 4;

	
	private final ArrayList<OnDemandVehicle> parkedVehicles;
	
	private final EventProcessor eventProcessor;

	private final LinkedList<DepartureCard> departureCards;
	
	private final Transformer transformer;
	
	private final VisioPositionUtil positionUtil;
	
	private final StationsDispatcher onDemandVehicleStationsCentral;
	
	private final SimodConfig config;

	private final AgentpolisConfig agentpolisConfig;

	
	
	@Inject
	public OnDemandVehicleStation(
		SimodConfig config,
		AgentpolisConfig agentpolisConfig,
		EventProcessor eventProcessor,
		OnDemandVehicleFactorySpec onDemandVehicleFactory,
		NearestElementUtils nearestElementUtils,
		OnDemandvehicleStationStorage onDemandVehicleStationStorage,
		OnDemandVehicleStorage onDemandVehicleStorage,
		@Assisted String id,
		@Assisted SimulationNode node,
		@Assisted int initialVehicleCount,
		Transformer transformer,
		VisioPositionUtil positionUtil,
		StationsDispatcher onDemandVehicleStationsCentral
	) {
		super(id, node);
		this.eventProcessor = eventProcessor;
		this.agentpolisConfig = agentpolisConfig;
		parkedVehicles = new ArrayList<>();
//		initialVehicleCount = 10;

		for (int i = 0; i < initialVehicleCount; i++) {
			String onDemandVehicelId = String.format("%s-%d", id, i);

			// physical vehicle creation
			MoDVehicle vehicle = new SimpleMoDVehicle(
				onDemandVehicelId + " - vehicle",
				DemandSimulationEntityType.VEHICLE,
				LENGTH,
				EGraphType.HIGHWAY,
				getPosition(),
				agentpolisConfig.maxVehicleSpeedInMeters,
				config.ridesharing.vehicleCapacity
			);

			OnDemandVehicle newVehicle = onDemandVehicleFactory.create(onDemandVehicelId, getPosition(), vehicle);
			parkedVehicles.add(newVehicle);
			newVehicle.setParkedIn(this);
			onDemandVehicleStorage.addEntity(newVehicle);
		}
		onDemandVehicleStationStorage.addEntity(this);
		this.departureCards = new LinkedList<>();
		this.transformer = transformer;
		this.positionUtil = positionUtil;
		this.onDemandVehicleStationsCentral = onDemandVehicleStationsCentral;
		this.config = config;
	}

	@Override
	public String toString() {
		return getId();
	}
	
	@Override
	public EntityType getType() {
		return DemandSimulationEntityType.ON_DEMAND_VEHICLE_STATION;
	}
	
//	public void setNearestNode(final Node node){
//		positionInGraph = node;
//		
//	}

	@Override
	public EventProcessor getEventProcessor() {
		return null;
	}

	@Override
	public void handleEvent(Event event) {
//		OnDemandVehicleStationEvent eventType = (OnDemandVehicleStationEvent) event.getType();
//		switch(eventType){
//			case TRIP:
//				handleTripRequest((DemandData) event.getContent());
//				break;
//		}
	}
	
	public void parkVehicle(OnDemandVehicle onDemandVehicle){
		if(onDemandVehicle.getPosition() != getPosition()){
			try {
				throw new Exception("Vehicle cannot be parked in station, beacause it's not present in the station!");
			} catch (Exception ex) {
				LOGGER.error(null, ex);
			}
		}
		parkedVehicles.add(onDemandVehicle);
		onDemandVehicle.setParkedIn(this);
	}
	
	public void releaseVehicle(OnDemandVehicle vehicle){
		parkedVehicles.remove(vehicle);
		vehicle.setParkedIn(null);
		if(getParkedVehiclesCount() == 0){
                        LOGGER.info("Station is empty! " +  getId());
		}
	}
	
	public int getParkedVehiclesCount(){
		return parkedVehicles.size();
	}
	

	public void handleTripRequest(DemandData demandData) {
		Node startLocation = demandData.locations[0];
		Node targetLocation = demandData.locations[demandData.locations.length - 1];
		
		// hack for demands that starts and ends in the same position
		if(startLocation.equals(targetLocation)){
			try {
				throw new Exception("Start and target location cannot be the same!");
			} catch (Exception ex) {
				LOGGER.error(null, ex);
			}
			return;
		}
		
		OnDemandVehicle vehicle = getAndRemoveVehicle();
		
		if(vehicle != null){
			vehicle.setDepartureStation(this);
			eventProcessor.addEvent(null, vehicle, null, demandData);
			eventProcessor.addEvent(null, demandData.demandAgent, null, vehicle);
		}
		else{
			try {
				throw new Exception("Request cannot be handeled - station has not any vehicles available!");
			} catch (Exception ex) {
				LOGGER.error(null, ex);
			}
		}
	}

	public boolean rebalance(OnDemandVehicleStation targetStation) {
		OnDemandVehicle vehicle = getAndRemoveVehicle();
		if(vehicle != null){
			vehicle.startRebalancing(targetStation);
			return true;
		}
		else{
			return false;
		}
	}
	
	public boolean isEmpty(){
		return parkedVehicles.isEmpty();
	}
	
	public int getIndex(){
		return Integer.parseInt(getId());
	}

	private OnDemandVehicle getAndRemoveVehicle() {
		OnDemandVehicle nearestVehicle;

		nearestVehicle = parkedVehicles.remove(0);
		
		return nearestVehicle;
	}
	
	public OnDemandVehicle getVehicle(int index){
		return parkedVehicles.get(index);
	}
	
	private NearestElementUtil<OnDemandVehicle> getNearestElementUtilForVehiclesBeforeDeparture(
			OnDemandVehicleStation targetStation) {
		List<NearestElementUtilPair<Coordinate,OnDemandVehicle>> pairs = new ArrayList<>();
		
		for(DepartureCard departureCard : departureCards) {
			if(departureCard.getTargetStation() == targetStation){
				GPSLocation location = departureCard.getDemandVehicle().getPosition();

				pairs.add(new NearestElementUtilPair<>(
					new Coordinate(location.getLongitude(), location.getLatitude(), location.elevation),
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
	
	public void release(OnDemandVehicle onDemandVehicle){
		Iterator<DepartureCard> iterator = departureCards.iterator();
		while(iterator.hasNext()){
			DepartureCard departureCard = iterator.next();
			if(departureCard.demandVehicle == onDemandVehicle){
				iterator.remove();
				break;
			}
		}
//		departureCards.remove(onDemandVehicle);
	}
	
	@Override
	public String toWKTCoordinate() {
		return getPosition().toWKTCoordinate();
	}
	
	
	
	public interface OnDemandVehicleStationFactory {
		OnDemandVehicleStation create(String id, SimulationNode node, int initialVehicleCount);
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
