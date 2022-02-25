package cz.cvut.fel.aic.simod.entity;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.Trip;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.agent.TransportEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.AgentPolisEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.EntityType;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.TransportableEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandler;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.simod.DemandData;
import cz.cvut.fel.aic.simod.DemandSimulationEntityType;
import cz.cvut.fel.aic.simod.StationsDispatcher;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleStationsCentralEvent;
import cz.cvut.fel.aic.simod.io.TimeTrip;
import cz.cvut.fel.aic.simod.statistics.DemandServiceStatistic;
import cz.cvut.fel.aic.simod.statistics.StatisticEvent;
import cz.cvut.fel.aic.simod.storage.DemandPackageStorage;
import cz.cvut.fel.aic.simod.storage.DemandStorage;
import org.slf4j.LoggerFactory;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DemandPackage extends AgentPolisEntity implements TransportableEntity
{
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DemandAgent.class);

	private final int simpleId;

	private final TimeTrip<SimulationNode> trip;

	private final StationsDispatcher onDemandVehicleStationsCentral;

	private final EventProcessor eventProcessor;

	private final DemandPackageStorage demandStorage;

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

	private final int weight;


	@Inject
	public DemandPackage(StationsDispatcher onDemandVehicleStationsCentral,
						 EventProcessor eventProcessor,
						 DemandPackageStorage demandStorage,
						 StandardTimeProvider timeProvider,
						 TripsUtil tripsUtil,
						 int packageWeight,
						 @Assisted String packageId,
						 @Assisted int id,
						 @Assisted TimeTrip<SimulationNode> trip) {
		super(packageId, trip.getLocations()[0]);
		this.simpleId = id;
		this.trip = trip;
		this.onDemandVehicleStationsCentral = onDemandVehicleStationsCentral;
		this.eventProcessor = eventProcessor;
		this.demandStorage = demandStorage;
		this.timeProvider = timeProvider;
		this.tripsUtil = tripsUtil;
		this.weight = packageWeight;
		state = DemandAgentState.WAITING;
		dropped = false;
		demandTime = timeProvider.getCurrentSimTime();
		computeMinServiceDuration();
	}


	public void create() {
		demandStorage.addEntity(this);

		eventProcessor.addEvent(OnDemandVehicleStationsCentralEvent.DEMAND, onDemandVehicleStationsCentral, null,
				new DemandData(trip.getLocations(), this));	// TODO: upravit v eventProcessoru
	}

	public void destroy() {
		demandStorage.removeEntity(this);
	}


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

	public void setDropped(boolean dropped) {
		this.dropped = dropped;
	}

	public boolean isDropped() {
		return dropped;
	}


	public void tripEnded()
	{
		if(!getPosition().equals(trip.getLastLocation())){
			try {
				throw new Exception("Demand package not served properly");
			} catch (Exception ex) {
				LOGGER.error(null, ex);
			}
		}
		eventProcessor.addEvent(StatisticEvent.DEMAND_DROPPED_OFF, null, null,
				new DemandServiceStatistic(demandTime, realPickupTime, timeProvider.getCurrentSimTime(),
						minDemandServiceDuration, getId(), onDemandVehicle.getId()));

		destroy();
	}

	public void tripStarted(OnDemandVehicle vehicle)
	{
		if (state == DemandAgentState.DRIVING)
		{
			try
			{
				throw new Exception(String.format("Demand Package %s already driving in vehicle %s, it cannot be picked up by"
						+ "another vehicle %s", this, onDemandVehicle, vehicle));
			}
			catch (Exception ex)
			{
				Logger.getLogger(DemandPackage.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		else
		{
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



	public interface DemandPackageFactory {
		DemandPackage create(String packageId, int id, TimeTrip<SimulationNode> osmNodeTrip);
	}
}
