package cz.cvut.fel.aic.simod.entity;

import cz.cvut.fel.aic.agentpolis.simmodel.entity.EntityType;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.TransportableEntity;
import cz.cvut.fel.aic.alite.common.event.EventHandler;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicle;

public interface SimulationAgent extends TransportableEntity, EventHandler {
    public void setDropped(boolean dropped);

    public boolean isDropped();

    public void tripStarted(OnDemandVehicle vehicle);

    public void tripEnded();

    public long getDemandTime();

    public int getSimpleId();

    public String getId();

    public EntityType getType();

    public OnDemandVehicle getOnDemandVehicle();

    public DemandAgentState getState();
}
