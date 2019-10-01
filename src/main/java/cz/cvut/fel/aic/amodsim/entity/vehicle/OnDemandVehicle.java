/* 
 * Copyright (C) 2019 Czech Technical University in Prague.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package cz.cvut.fel.aic.amodsim.entity.vehicle;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import cz.cvut.fel.aic.amodsim.DemandSimulationEntityType;
import cz.cvut.fel.aic.amodsim.StationsDispatcher;
import cz.cvut.fel.aic.amodsim.entity.PlanningAgent;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.Trip;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.TripItem;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.VehicleTrip;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.Activity;
import cz.cvut.fel.aic.agentpolis.simmodel.Agent;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.PhysicalVehicleDrive;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.PhysicalVehicleDriveFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.agent.Driver;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioPositionUtil;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandler;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.amodsim.DemandData;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.amodsim.event.OnDemandVehicleEvent;
import cz.cvut.fel.aic.amodsim.event.OnDemandVehicleEventContent;
import cz.cvut.fel.aic.amodsim.statistics.PickupEventContent;
import cz.cvut.fel.aic.agentpolis.simmodel.agent.DelayData;
import cz.cvut.fel.aic.geographtools.Node;
import java.util.LinkedList;
import java.util.List;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.EntityType;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalTransportVehicle;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.event.RebalancingEventContent;
import cz.cvut.fel.aic.amodsim.storage.PhysicalTransportVehicleStorage;
import java.util.ArrayList;


/**
 *
 * @author fido
 */
public class OnDemandVehicle extends Agent implements EventHandler, PlanningAgent,
		Driver<PhysicalTransportVehicle>{
	
	private static final int LENGTH = 4;
	
	
	
	
	protected PhysicalTransportVehicle vehicle;
	
	protected final TripsUtil tripsUtil;
	
	protected final StationsDispatcher onDemandVehicleStationsCentral;
	
	protected final PhysicalVehicleDriveFactory driveFactory;
	
	private final VisioPositionUtil positionUtil;
	
	protected final EventProcessor eventProcessor;
	
	protected final StandardTimeProvider timeProvider;
	
	private final IdGenerator rebalancingIdGenerator;
	
	private final AmodsimConfig config;
	
	
	private List<SimulationNode> demandNodes;
	
	protected OnDemandVehicleState state;
	
	protected OnDemandVehicleStation departureStation;
	
	protected OnDemandVehicleStation targetStation;
	
	protected VehicleTrip currentTrip;
	
	protected VehicleTrip demandTrip;
	
	protected VehicleTrip tripToStation;
	
	private VehicleTrip completeTrip;
	
	protected int metersWithPassenger;
	
	protected int metersToStartLocation;
	
	protected int metersToStation;
	
	private int metersRebalancing;
	
	private SimulationNode targetNode;
	
	private DelayData delayData;
	
	private DemandData currentlyServedDemmand;
	
	private int currentRebalancingId;
	
	protected OnDemandVehicleStation parkedIn;
	
	
	
	public VehicleTrip getCurrentTrips() {
		return currentTrip;
	}

	public VehicleTrip getDemandTrip(DemandAgent agent) {
		return demandTrip.clone();
	}

	public OnDemandVehicleState getState() {
		return state;
	}

	public int getMetersWithPassenger() {
		return metersWithPassenger;
	}

	public int getMetersToStartLocation() {
		return metersToStartLocation;
	}

	public int getMetersToStation() {
		return metersToStation;
	}

	public int getMetersRebalancing() {
		return metersRebalancing;
	}

	public OnDemandVehicleStation getParkedIn() {
		return parkedIn;
	}
	

	// remove in future to be more agent-like
	public void setDepartureStation(OnDemandVehicleStation departureStation) {
		this.departureStation = departureStation;
	}

	public void setParkedIn(OnDemandVehicleStation parkedIn) {
		this.parkedIn = parkedIn;
	}

	
	
	
	
	
	@Inject
	public OnDemandVehicle(PhysicalTransportVehicleStorage vehicleStorage, 
			TripsUtil tripsUtil, StationsDispatcher onDemandVehicleStationsCentral, 
			PhysicalVehicleDriveFactory driveFactory, VisioPositionUtil positionUtil, EventProcessor eventProcessor,
			StandardTimeProvider timeProvider, IdGenerator rebalancingIdGenerator, AmodsimConfig config, 
			@Assisted String vehicleId, @Assisted SimulationNode startPosition) {
		super(vehicleId, startPosition);
		this.tripsUtil = tripsUtil;
		this.onDemandVehicleStationsCentral = onDemandVehicleStationsCentral;
		this.driveFactory = driveFactory;
		this.positionUtil = positionUtil;
		this.eventProcessor = eventProcessor;
		this.timeProvider = timeProvider;
		this.rebalancingIdGenerator = rebalancingIdGenerator;
		this.config = config;
		
		vehicle = new PhysicalTransportVehicle(vehicleId + " - vehicle", 
				DemandSimulationEntityType.VEHICLE, LENGTH, config.ridesharing.vehicleCapacity, 
				EGraphType.HIGHWAY, startPosition, 
				config.vehicleSpeedInMeters);
		
		vehicleStorage.addEntity(vehicle);
		vehicle.setDriver(this);
		state = OnDemandVehicleState.WAITING;
		
		metersWithPassenger = 0;
		metersToStartLocation = 0;
		metersToStation = 0;
		metersRebalancing = 0;
	}

	@Override
	public EventProcessor getEventProcessor() {
		return null;
	}
	
	public String getVehicleId(){
		return vehicle.getId();
	}

	@Override
	public void handleEvent(Event event) {
		currentlyServedDemmand = (DemandData) event.getContent();
		List<SimulationNode> locations = currentlyServedDemmand.locations;
		
		demandNodes = new ArrayList<>();
		demandNodes.add(locations.get(0));
		demandNodes.add(locations.get(locations.size() - 1));
		
		leavingStationEvent();
		driveToDemandStartLocation();
	}
	
	public void finishedDriving(boolean wasStopped) {
		switch(state){
			case DRIVING_TO_START_LOCATION:
				driveToTargetLocation();
				break;
			case DRIVING_TO_TARGET_LOCATION:
				dropOffDemand();
				driveToNearestStation();
				break;
			case DRIVING_TO_STATION:
				finishDrivingToStation();
				break;
			case REBALANCING:
				finishRebalancing();
				break;
		}
	}

	protected void driveToDemandStartLocation() {
		
		if(getPosition() == demandNodes.get(0)){
			currentTrip = null;
		}
		else{
			currentTrip = tripsUtil.createTrip(getPosition().id, 
				demandNodes.get(0).getId(), vehicle);
			metersToStartLocation += positionUtil.getTripLengthInMeters(currentTrip);
		}
		demandTrip = tripsUtil.createTrip(demandNodes.get(0).getId(), demandNodes.get(1).getId(), vehicle);
		metersWithPassenger += positionUtil.getTripLengthInMeters(demandTrip);
		
		SimulationNode demandEndNode = demandNodes.get(demandNodes.size() - 1);
		
		targetStation = onDemandVehicleStationsCentral.getNearestStation(demandEndNode);
		
		if(demandEndNode.getId() == targetStation.getPosition().getId()){
			tripToStation = null;
		}
		else{
			tripToStation = tripsUtil.createTrip(demandEndNode.getId(), 
					targetStation.getPosition().getId(), vehicle);
			metersToStation += positionUtil.getTripLengthInMeters(tripToStation);
		}
		
		completeTrip = TripsUtil.mergeTripsOld(currentTrip, demandTrip, tripToStation);
		
		if(currentTrip == null){
			driveToTargetLocation();
			return;
		}
		
		state = OnDemandVehicleState.DRIVING_TO_START_LOCATION;
				
//		driveVehicleActivity.drive(getId(), vehicle, currentTrip.clone(), this);
//		driveActivityFactory.create(this, vehicle, vehicleTripToTrip(currentTrip)).run();
		driveFactory.runActivity(this, vehicle, vehicleTripToTrip(currentTrip));
	}

	

	protected void driveToTargetLocation() {
		state = OnDemandVehicleState.DRIVING_TO_TARGET_LOCATION;
		pickupDemand();
		
		departureStation.release(this);
		
		currentTrip = demandTrip;
				
//		driveVehicleActivity.drive(getId(), vehicle, currentTrip.clone(), this);
//		driveActivityFactory.create(this, vehicle, vehicleTripToTrip(currentTrip)).run();
		driveFactory.runActivity(this, vehicle, vehicleTripToTrip(currentTrip));
		
	}

	protected void driveToNearestStation() {
		if(tripToStation == null){
			waitInStation();
			return;
		}
		
		state = OnDemandVehicleState.DRIVING_TO_STATION;

		currentTrip = tripToStation;  
				
//		driveVehicleActivity.drive(getId(), vehicle, currentTrip.clone(), this);
//		driveActivityFactory.create(this, vehicle, vehicleTripToTrip(currentTrip)).run();
		driveFactory.runActivity(this, vehicle, vehicleTripToTrip(currentTrip));
	}

	protected void waitInStation() {
		targetStation.parkVehicle(this);
		park();
	}
	
	protected void park(){
		state = OnDemandVehicleState.WAITING;
		completeTrip = null;
	}

	@Override
	public VehicleTrip getCurrentTripPlan() {
		return completeTrip;
	}
	
	public Node getDemandTarget(){
		if(demandNodes != null){
			return demandNodes.get(demandNodes.size() - 1);
		}
		return null;
	}

	public void startRebalancing(OnDemandVehicleStation targetStation) {
		eventProcessor.addEvent(OnDemandVehicleEvent.START_REBALANCING, null, null, 
				new RebalancingEventContent(timeProvider.getCurrentSimTime(), currentRebalancingId, 
						getId(), getParkedIn(), targetStation));
		
		parkedIn.releaseVehicle(this);
		state = OnDemandVehicleState.REBALANCING;
		currentRebalancingId = rebalancingIdGenerator.getId();
		
		currentTrip = tripsUtil.createTrip(getPosition().id, 
				targetStation.getPosition().getId(), vehicle);
		metersRebalancing += positionUtil.getTripLengthInMeters(currentTrip);
		
		completeTrip = currentTrip.clone();
		
		this.targetStation = targetStation;
		
		driveFactory.runActivity(this, vehicle, vehicleTripToTrip(currentTrip));
	}

	@Override
	public PhysicalTransportVehicle getVehicle() {
		return vehicle;
	}

	@Override
	public int getVelocity() {
		return config.vehicleSpeedInMeters;
	}
	
	public int getCapacity(){
		return vehicle.getCapacity();
	}

//	@Override
//	public List<AgentPolisEntity> getTransportedEntities() {
//		return cargo;
//	}

	@Override
	public void setTargetNode(SimulationNode targetNode) {
		this.targetNode = targetNode;
	}

	@Override
	public SimulationNode getTargetNode() {
		return targetNode;
	}

	protected void leavingStationEvent() {
		eventProcessor.addEvent(OnDemandVehicleEvent.LEAVE_STATION, null, null, 
				new OnDemandVehicleEventContent(timeProvider.getCurrentSimTime(), 
						currentlyServedDemmand.demandAgent.getSimpleId(), getId()));
	}

	protected void pickupDemand() {
		currentlyServedDemmand.demandAgent.tripStarted(this);
		vehicle.pickUp(currentlyServedDemmand.demandAgent);
		eventProcessor.addEvent(OnDemandVehicleEvent.PICKUP, null, null, 
				new PickupEventContent(timeProvider.getCurrentSimTime(), 
						currentlyServedDemmand.demandAgent.getSimpleId(), getId(),
						positionUtil.getTripLengthInMeters(demandTrip)));
	}
	
	protected void dropOffDemand() {
		currentlyServedDemmand.demandAgent.tripEnded();
		vehicle.dropOff(currentlyServedDemmand.demandAgent);
		eventProcessor.addEvent(OnDemandVehicleEvent.DROP_OFF, null, null, 
				new OnDemandVehicleEventContent(timeProvider.getCurrentSimTime(), 
						currentlyServedDemmand.demandAgent.getSimpleId(), getId()));
	}
	
	// todo - repair path planner and remove this
	protected Trip<Node> vehicleTripToTrip(VehicleTrip<TripItem> vehicleTrip){
		LinkedList<Node>  locations = new LinkedList<>();
		for (TripItem tripItem : vehicleTrip.getLocations()) {
			locations.add(positionUtil.getNode(tripItem.tripPositionByNodeId));
		}
		Trip<Node> trip = new Trip<>(locations);
		
		return trip;
	}

	@Override
	protected void onActivityFinish(Activity activity) {
		super.onActivityFinish(activity);
		PhysicalVehicleDrive drive = (PhysicalVehicleDrive) activity;
		finishedDriving(drive.isStoped());
	}

	@Override
	public EntityType getType() {
		return DemandSimulationEntityType.ON_DEMAND_VEHICLE;
	}


	@Override
	public void startDriving(PhysicalTransportVehicle vehicle){
		this.vehicle = vehicle;
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
	public void endDriving() {
		
	}

	protected void finishRebalancing() {
		waitInStation();
		eventProcessor.addEvent(OnDemandVehicleEvent.FINISH_REBALANCING, null, null, 
					new RebalancingEventContent(timeProvider.getCurrentSimTime(), 
						currentRebalancingId, getId(), null, parkedIn));
	}

	protected void finishDrivingToStation() {
		eventProcessor.addEvent(OnDemandVehicleEvent.REACH_NEAREST_STATION, null, null, 
					new OnDemandVehicleEventContent(timeProvider.getCurrentSimTime(), 
						-1, getId()));
		waitInStation();
	}
	
	
}
