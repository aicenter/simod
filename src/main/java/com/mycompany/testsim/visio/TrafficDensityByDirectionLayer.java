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
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.AllEdgesLoad;
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
import java.util.HashMap;
import javax.vecmath.Point2d;

/**
 * @author Zdenek Bousa
 */
@Singleton
public class TrafficDensityByDirectionLayer extends AbstractLayer {

    private static final int EDGE_WIDTH = 3;

    private static final double MAX_LOAD = 0.05;


    private final Provider<AllEdgesLoad> allEdgesLoadProvider;

    private final Graph<SimulationNode, SimulationEdge> graph;

    private final ColorMap colorMap;

    private final PositionUtil positionUtil;

    private Dimension dimension;

    private HashMap<SimulationEdge, Line2D> tableOfVisibleEdges;

    private Point2d lastPoint = new Point2d(0, 0);


    /**
     * On/Off {@link DemandsVisioInitializer}
     */
    @Inject
    public TrafficDensityByDirectionLayer(HighwayNetwork highwayNetwork, PositionUtil positionUtil,
                                          Provider<AllEdgesLoad> allEdgesLoadProvider) {
        this.positionUtil = positionUtil;
        this.allEdgesLoadProvider = allEdgesLoadProvider;
        graph = highwayNetwork.getNetwork();
        colorMap = new ColorMap(0, MAX_LOAD, ColorMap.HUE_BLUE_TO_RED);

        this.setHelpOverrideString("Traffic density layer by direction");
    }

    @Override
    public void paint(Graphics2D canvas) {
        Dimension dimTemp = Vis.getDrawingDimension();

        // TODO: correctly check for zoom and visio changes
        // Hacked via change of the calculated position for nodeId
        Point2d point = positionUtil.getCanvasPosition(0);

        if (!point.equals(lastPoint)) {
            System.out.println(lastPoint);
            lastPoint = point;
            dimension = dimTemp;
            refreshListOfVisibleEdges();
        }

        canvas.setStroke(new BasicStroke(EDGE_WIDTH));
        canvas.getClipBounds();

        AllEdgesLoad allEdgesLoad = allEdgesLoadProvider.get();

        for (SimulationEdge edge : tableOfVisibleEdges.keySet()) {
            canvas.setColor(getColorForEdge(allEdgesLoad, edge));
            canvas.draw(tableOfVisibleEdges.get(edge));
        }
    }

    private Color getColorForEdge(AllEdgesLoad allEdgesLoad, SimulationEdge edge) {
        double averageLoad = allEdgesLoad.getLoadPerEdge(edge.wayID);
        double loadPerLength = averageLoad / edge.getLength();
        return colorMap.getColor(loadPerLength);
    }

    private void refreshListOfVisibleEdges() {
        tableOfVisibleEdges = new HashMap<>();
        Rectangle2D drawingRectangle = new Rectangle(dimension);

        for (SimulationEdge edge : graph.getAllEdges()) {
            Point2d from = positionUtil.getCanvasPosition(graph.getNode(edge.fromId));
            Point2d to = positionUtil.getCanvasPosition(graph.getNode(edge.toId));

            Line2D line2d = new Line2D.Double(from.x, from.y, to.x, to.y);
            if (line2d.intersects(drawingRectangle)) {
                tableOfVisibleEdges.put(edge, line2d);
            }
        }
    }

}
