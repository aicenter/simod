/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.graphbuilder;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.gtdgraphimporter.structurebuilders.client.NodeFactory;
import cz.cvut.fel.aic.gtdgraphimporter.structurebuilders.internal.InternalNode;

/**
 *
 * @author fido
 */
public class SimulationNodeFactory extends NodeFactory<SimulationNode>{

    @Override
    public SimulationNode createNode(InternalNode internalNode) {
        
        return new SimulationNode(internalNode.id, internalNode.sourceId, internalNode.latE6, internalNode.lonE6, 
                internalNode.latProjected, internalNode.lonProjected, internalNode.elevation);
    }
    
}
