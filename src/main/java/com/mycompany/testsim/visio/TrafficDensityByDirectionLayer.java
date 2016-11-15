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
import cz.agents.basestructures.Edge;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.vecmath.Point2d;

/**
 * Layer that shows traffic on edges. Two-way edges are split for each direction.
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

    private List<SimulationEdge> tableOfVisibleEdges;

    private Point2d lastPoint = new Point2d(0, 0);

    private Map<Edge, Edge> twoWayEdges;

    private Map<Edge, SimulationEdge> edgeMapping;

    private Map<Edge, Line2D> edgePosition;


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

            if (twoWayEdges == null) {
                createTwoWayEdgesList();
            }

            refreshEdgesPosition();
        }

        canvas.setStroke(new BasicStroke(EDGE_WIDTH));
        canvas.getClipBounds();

        AllEdgesLoad allEdgesLoad = allEdgesLoadProvider.get();

        for (Edge edge : edgePosition.keySet()) {
            canvas.setColor(getColorForEdge(allEdgesLoad, edgeMapping.get(edge)));
            canvas.draw(edgePosition.get(edge));
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

    /**
     * Refresh or create list of edges that intersects with Rectangle(dimension)
     */
    private void refreshListOfVisibleEdges() {
        //TODO: change table of visible edges to list
        tableOfVisibleEdges = new LinkedList<>();
        Rectangle2D drawingRectangle = new Rectangle(dimension);

        // will refresh twoWayEdges
        twoWayEdges = null;

        for (SimulationEdge edge : graph.getAllEdges()) {
            Point2d from = positionUtil.getCanvasPosition(graph.getNode(edge.fromId));
            Point2d to = positionUtil.getCanvasPosition(graph.getNode(edge.toId));
            Line2D line2d = new Line2D.Double(from.x, from.y, to.x, to.y);

            if (line2d.intersects(drawingRectangle)) {
                tableOfVisibleEdges.add(edge);
            }
        }
    }

    /**
     * Provides easy mapping between Edge and SimulationEdge
     * Contains only visible edges because of the listOfVisibleEdges
     */
    private void refreshEdgeMapping() {
        edgeMapping = new HashMap<>();

        for (SimulationEdge edge : tableOfVisibleEdges) {
            edgeMapping.put(edge, edge);
        }
    }

    /**
     * Create Map of edges, where one-way edges have null value, two-eay edges have its partner edge in opposite
     * direction as an value.
     */
    private void createTwoWayEdgesList() {
        refreshEdgeMapping();
        twoWayEdges = new HashMap<>();

        for (Edge edge : edgeMapping.keySet()) {
            Edge edgeOpposite = new Edge(edge.getToId(), edge.fromId, edge.length);
            if (twoWayEdges.containsKey(edgeOpposite)) {
                twoWayEdges.replace(edgeOpposite, edge);
                twoWayEdges.put(edge, edgeOpposite);
            } else {
                twoWayEdges.put(edge, null);
            }
        }
    }

    /**
     * Refresh all pairs Edge-Line2D in edgePosition. It uses twoWayEdges map.
     */
    private void refreshEdgesPosition() {
        edgePosition = new HashMap<>();

        for (Edge edge : twoWayEdges.keySet()) {
            Edge edge2 = twoWayEdges.get(edge);
            if (twoWayEdges.get(edge) != null && (!edgePosition.containsKey(edge) || !edgePosition.containsKey(edge2))) {
                calculateTwoWayEdgesPosition(edge, edge2);
            } else if (twoWayEdges.get(edge) == null) {
                Point2d from = positionUtil.getCanvasPosition(graph.getNode(edge.fromId));
                Point2d to = positionUtil.getCanvasPosition(graph.getNode(edge.toId));

                Line2D line2d = new Line2D.Double(from.x, from.y, to.x, to.y);
                edgePosition.put(edge, line2d);
            }
        }
    }

    /**
     * Calculate position for two way edge.
     *
     * @param edge1 one direction
     * @param edge2 opposite direction
     */
    private void calculateTwoWayEdgesPosition(Edge edge1, Edge edge2) {
        //TODO: put in edgePosition
        Point2d from = positionUtil.getCanvasPosition(graph.getNode(edge1.fromId));
        Point2d to = positionUtil.getCanvasPosition(graph.getNode(edge1.toId));
        Line2D line2DE1 = new Line2D.Double(from.x, from.y, to.x, to.y);

        from = positionUtil.getCanvasPosition(graph.getNode(edge2.fromId));
        to = positionUtil.getCanvasPosition(graph.getNode(edge2.toId));
        Line2D line2DE2 = new Line2D.Double(from.x, from.y, to.x, to.y);
        edgePosition.put(edge1, line2DE1);
        edgePosition.put(edge2, line2DE2);
    }

}
