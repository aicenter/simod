/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim.visio;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.HighwayNetwork;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.AllNodesLoad;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.SimulationEdge;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.SimulationNode;
import cz.agents.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.agents.alite.vis.Vis;
import cz.agents.alite.vis.layer.AbstractLayer;
import cz.agents.basestructures.Graph;
import edu.mines.jtk.awt.ColorMap;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import javax.vecmath.Point2d;

/**
 *
 * @author fido
 */
@Singleton
public class TrafficDensityLayer extends AbstractLayer{
    
    private static final int EDGE_WIDTH = 2;
    
    private static final int MAX_LOAD = 10;
    
    
    
    
    
    private final Provider<AllNodesLoad> allNodesLoadProvider;
    
    private final Graph<SimulationNode,SimulationEdge> graph;
    
    private final ColorMap colorMap;
    
    private final PositionUtil positionUtil;
    
    
    
    

    @Inject
    public TrafficDensityLayer(HighwayNetwork highwayNetwork, PositionUtil positionUtil, 
            Provider<AllNodesLoad> allNodesLoadProvider) {
        this.positionUtil = positionUtil;
        this.allNodesLoadProvider = allNodesLoadProvider;
        graph = highwayNetwork.getNetwork();
        colorMap = new ColorMap(0, MAX_LOAD, ColorMap.HUE_BLUE_TO_RED);
    }
    
    
    
    
   @Override
    public void paint(Graphics2D canvas) {
//        canvas.setColor(lineElements.getColor());
//        canvas.setStroke(new BasicStroke(lineElements.getStrokeWidth()));

        AllNodesLoad allNodesLoad = allNodesLoadProvider.get();

        canvas.setStroke(new BasicStroke(EDGE_WIDTH));

        Dimension dim = Vis.getDrawingDimension();
        Rectangle2D drawingRectangle = new Rectangle(dim);
        
        for (SimulationEdge edge : graph.getAllEdges()) {
            canvas.setColor(getColorForEdge(allNodesLoad, edge));
            Point2d from = positionUtil.getCanvasPosition(graph.getNode(edge.fromId));
            Point2d to = positionUtil.getCanvasPosition(graph.getNode(edge.toId));
            Line2D line2d = new Line2D.Double(from.x, from.y, to.x, to.y);
            if (line2d.intersects(drawingRectangle)) {
                canvas.draw(line2d);
            }
        }
    }

    private Color getColorForEdge(AllNodesLoad allNodesLoad, SimulationEdge edge) {
        double averageLoad = (allNodesLoad.getLoadPerNode(edge.fromId) + allNodesLoad.getLoadPerNode(edge.toId)) / 2;
        return colorMap.getColor(averageLoad);
    }
}
