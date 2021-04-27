package cz.cvut.fel.aic.simod;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.simod.entity.SimulationAgent;

public class ParcelData extends SimulationEntityData {
    public ParcelData(SimulationNode[] locList, SimulationAgent parcelAgent) {
        this.locations = locList;
        this.simulationAgent = parcelAgent;
    }
}
