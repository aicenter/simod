/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim.visio;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.SimulationEdge;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.SimulationNode;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.load.AllEdgesLoad;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.networks.HighwayNetwork;
import cz.agents.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.agents.alite.vis.Vis;
import cz.agents.alite.vis.layer.AbstractLayer;
import cz.agents.basestructures.Graph;
import edu.mines.jtk.awt.ColorMap;

import javax.vecmath.Point2d;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author fido
 */
@Singleton
public class TrafficDensityLayer extends AbstractLayer{
    
    private static final int EDGE_WIDTH = 2;
    
    private static final double MAX_LOAD = 0.05;
    
    
    
    
    
    private final Provider<AllEdgesLoad> allEdgesLoadProvider;
    
    private final Graph<SimulationNode,SimulationEdge> graph;
    
    private final ColorMap colorMap;
    
    private final PositionUtil positionUtil;
    
    
    
    

    @Inject
    public TrafficDensityLayer(HighwayNetwork highwayNetwork, PositionUtil positionUtil, 
            Provider<AllEdgesLoad> allEdgesLoadProvider) {
        this.positionUtil = positionUtil;
        this.allEdgesLoadProvider = allEdgesLoadProvider;
        graph = highwayNetwork.getNetwork();
        colorMap = new ColorMap(0, MAX_LOAD, ColorMap.HUE_BLUE_TO_RED);
    }
    
    
    
    
   @Override
    public void paint(Graphics2D canvas) {
        AllEdgesLoad allEdgesLoad = allEdgesLoadProvider.get();

        canvas.setStroke(new BasicStroke(EDGE_WIDTH));

        Dimension dim = Vis.getDrawingDimension();
        Rectangle2D drawingRectangle = new Rectangle(dim);
        
        for (SimulationEdge edge : graph.getAllEdges()) {
            canvas.setColor(getColorForEdge(allEdgesLoad, edge));
            Point2d from = positionUtil.getCanvasPosition(graph.getNode(edge.fromId));
            Point2d to = positionUtil.getCanvasPosition(graph.getNode(edge.toId));
            Line2D line2d = new Line2D.Double(from.x, from.y, to.x, to.y);
            if (line2d.intersects(drawingRectangle)) {
                canvas.draw(line2d);
            }
        }
    }

    private Color getColorForEdge(AllEdgesLoad allEdgesLoad, SimulationEdge edge) {
        // TODO - proper edge id mechanism
        String id = null;
//        String id = Long.toString(network.getNode(currentNodeId).getSourceId()) + "-"
//                        + Long.toString(network.getNode(targetNodeId).getSourceId());
        double averageLoad = allEdgesLoad.getLoadPerEdge(id);
        double loadPerLength = averageLoad / edge.getLength();
        return colorMap.getColor(loadPerLength);
    }
}
