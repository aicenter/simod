/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;
import java.util.List;

/**
 *
 * @author fido
 */
public class DemandData {
    public List<SimulationNode> locations;
    public DemandAgent demandAgent;


    public DemandData(List<SimulationNode> locList, DemandAgent demandAgent) {
        this.locations = locList;
        this.demandAgent = demandAgent;
    }
}
