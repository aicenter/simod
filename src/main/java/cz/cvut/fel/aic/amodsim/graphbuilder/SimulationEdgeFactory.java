/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.graphbuilder;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.EdgeShape;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.graphimporter.structurebuilders.client.EdgeFactory;
import cz.cvut.fel.aic.graphimporter.structurebuilders.internal.InternalEdge;

import java.util.List;

/**
 * @author fido
 */
public class SimulationEdgeFactory implements EdgeFactory<SimulationEdge> {

    @Override
    public SimulationEdge createEdge(InternalEdge internalEdge) {
        List<GPSLocation> coordinatesList = internalEdge.get("coordinateList");
        EdgeShape edgeShape = new EdgeShape(coordinatesList);
        return new SimulationEdge(internalEdge.fromId, internalEdge.toId, internalEdge.get("wayID"),
                internalEdge.get("uniqueWayID"), internalEdge.get("oppositeWayUniqueId"), internalEdge.getLength(),
                internalEdge.get("allowedMaxSpeedInMpS"),
                internalEdge.get("lanesCount"), edgeShape);
    }

}
