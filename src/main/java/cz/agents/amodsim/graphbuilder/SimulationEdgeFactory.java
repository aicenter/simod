/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.graphbuilder;

import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.SimulationEdge;
import cz.agents.gtdgraphimporter.structurebuilders.client.EdgeFactory;
import cz.agents.gtdgraphimporter.structurebuilders.internal.InternalEdge;

/**
 *
 * @author fido
 */
public class SimulationEdgeFactory extends EdgeFactory<SimulationEdge>{

    @Override
    public SimulationEdge createEdge(InternalEdge internalEdge) {
         return new SimulationEdge(internalEdge.fromId, internalEdge.toId, internalEdge.get("wayID"), 
                 internalEdge.get("uniqueWayID"), internalEdge.get("oppositeWayUniqueId"), internalEdge.getLength(), 
                 internalEdge.get("modeOfTransports"), internalEdge.get("allowedMaxSpeedInMpS"),
                 internalEdge.get("lanesCount"));
    }
    
}
