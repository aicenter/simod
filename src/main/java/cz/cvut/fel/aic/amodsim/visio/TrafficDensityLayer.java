/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.visio;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.load.AllEdgesLoad;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.HighwayNetwork;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.agents.alite.vis.Vis;
import cz.agents.alite.vis.layer.AbstractLayer;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.geographtools.Graph;
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
    
    
    
    
    
    private final Provider<AllEdgesLoad<OnDemandVehicle, OnDemandVehicleStorage>> allEdgesLoadProvider;
    
    private final Graph<SimulationNode,SimulationEdge> graph;
    
    private final ColorMap colorMap;
    
    private final PositionUtil positionUtil;
    
    
    
    

    @Inject
    public TrafficDensityLayer(HighwayNetwork highwayNetwork, PositionUtil positionUtil, 
            Provider<AllEdgesLoad<OnDemandVehicle, OnDemandVehicleStorage>> allEdgesLoadProvider) {
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
        int id = edge.getUniqueId();
        double averageLoad = allEdgesLoad.getLoadPerEdge(id);
        double loadPerLength = averageLoad / edge.getLength();
        return colorMap.getColor(loadPerLength);
    }
}
