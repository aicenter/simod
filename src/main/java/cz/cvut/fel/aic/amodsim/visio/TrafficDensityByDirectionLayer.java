/* 
 * Copyright (C) 2019 Czech Technical University in Prague.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package cz.cvut.fel.aic.amodsim.visio;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.load.AllEdgesLoad;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.HighwayNetwork;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioPositionUtil;
import cz.cvut.fel.aic.alite.vis.Vis;
import cz.cvut.fel.aic.alite.vis.layer.AbstractLayer;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.geographtools.Edge;
import cz.cvut.fel.aic.geographtools.Graph;
import edu.mines.jtk.awt.ColorMap;

import javax.vecmath.Point2d;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Layer that shows traffic on edges. Two-way edges are split for each direction. Start of each edge is with light blue
 * dot.
 * Refreshing of computed positions in a canvas is only done when something has changed.
 *
 * @author Zdenek Bousa
 */
@Singleton
public class TrafficDensityByDirectionLayer extends AbstractLayer {

	private static final int EDGE_WIDTH = 3;

	private static final double MAX_LOAD = 0.05;


	private final Provider<AllEdgesLoad<OnDemandVehicle, OnDemandVehicleStorage>> allEdgesLoadProvider;

	private final Graph<SimulationNode, SimulationEdge> graph;

	private final ColorMap colorMap;

	private final VisioPositionUtil positionUtil;

	private Dimension dimension;

	private Point2d lastPoint = new Point2d(0, 0);

	private Map<Edge, Edge> twoWayEdges;

	private Map<Edge, SimulationEdge> edgeMapping;

	private Map<Edge, Line2D> edgePosition;


	/**
	 * On/Off {@link DemandsVisioInitializer}
	 * @param highwayNetwork
	 * @param positionUtil
	 * @param allEdgesLoadProvider
	 */
	@Inject
	public TrafficDensityByDirectionLayer(HighwayNetwork highwayNetwork, VisioPositionUtil positionUtil,
								Provider<AllEdgesLoad<OnDemandVehicle, OnDemandVehicleStorage>> allEdgesLoadProvider) {
		this.positionUtil = positionUtil;
		this.allEdgesLoadProvider = allEdgesLoadProvider;
		graph = highwayNetwork.getNetwork();
		colorMap = new ColorMap(0, MAX_LOAD, ColorMap.HUE_BLUE_TO_RED);

		this.setHelpOverrideString("Traffic density layer by direction");
	}

	@Override
	public void paint(Graphics2D canvas) {
		Dimension dimTemp = Vis.getDrawingDimension();

		// TODO: correctly check for zoom and other visio changes
		// Hacked via change of the calculated position for nodeId
		Point2d point = positionUtil.getCanvasPosition(0);

		if (!point.equals(lastPoint)) {

			// Debug
			// System.out.println(lastPoint);

			lastPoint = point;
			dimension = dimTemp;

			// refresh list of all visible edges
			refreshListOfVisibleEdgesAndMapping();

			// generate list of one-way and two-way edges
			refreshTwoWayEdgesList();

			// regenerate new position for each edge
			refreshEdgesPosition();

		}

		canvas.setStroke(new BasicStroke(EDGE_WIDTH));
		canvas.getClipBounds();

		// refresh load provider
		AllEdgesLoad allEdgesLoad = allEdgesLoadProvider.get();

		for (Edge edge : edgePosition.keySet()) {
			canvas.setColor(getColorForEdge(allEdgesLoad, edgeMapping.get(edge)));
			canvas.draw(edgePosition.get(edge));

			//Debug - show begin of the line
			Line2D line = edgePosition.get(edge);
			canvas.setColor(Color.CYAN);
			canvas.fillRect((int) line.getX1(), (int) line.getY1(), 1, 1);
		}
	}

	/**
	 * Refresh or create list of edges that intersects with Rectangle(dimension)
	 * Also maintenance mapping between Edge and SimulationEdge
	 */
	private void refreshListOfVisibleEdgesAndMapping() {
		edgeMapping = new HashMap<>();
		Rectangle2D drawingRectangle = new Rectangle(dimension);

		for (SimulationEdge edge : graph.getAllEdges()) {
			Point2d from = positionUtil.getCanvasPosition(edge.getFromNode());
			Point2d to = positionUtil.getCanvasPosition(edge.getToNode());
			Line2D line2d = new Line2D.Double(from.x, from.y, to.x, to.y);

			if (line2d.intersects(drawingRectangle)) {
				edgeMapping.put(new Edge(edge.getFromNode(), edge.getToNode(), edge.getLengthCm()), edge);
			}
		}
	}

	/**
	 * Create Map of edges, where one-way edges have null value, two-eay edges have its partner edge in opposite
	 * direction as an value.
	 */
	private void refreshTwoWayEdgesList() {
		twoWayEdges = new HashMap<>();

		for (Edge edge : edgeMapping.keySet()) {
			Edge edgeOpposite = new Edge(edge.getToNode(), edge.getFromNode(), edge.getLengthCm());
			if (twoWayEdges.containsKey(edgeOpposite)) {
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
			if (edge2 != null && (!edgePosition.containsKey(edge) || !edgePosition.containsKey(edge2))) {
				calculateTwoWayEdgesPosition(edge, edge2);
			} else if (edge2 == null) {
				Point2d from = positionUtil.getCanvasPosition(edge.getFromNode());
				Point2d to = positionUtil.getCanvasPosition(edge.getToNode());

				//Debug - do not show one-way edges
				//Line2D line2d = new Line2D.Double(0, 0, 0, 0);
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
		// move
		double move = 0.5 * EDGE_WIDTH + 1;

		// get canvas positions
		Point2d A = positionUtil.getCanvasPosition(edge1.getFromNode());
		Point2d B = positionUtil.getCanvasPosition(edge1.getToNode());

		// calculate move of one of the edge
		double vectorX = B.y - A.y;
		double vectorY = -(B.x - A.x);
		double scaleToUnit = Math.sqrt(Math.pow(vectorX, 2) + Math.pow(vectorY, 2));
		vectorX = (vectorX / scaleToUnit) * move;
		vectorY = (vectorY / scaleToUnit) * move;

		// new positions in canvas
		Line2D line2DE1 = new Line2D.Double(B.x + vectorX, B.y + vectorY, A.x + vectorX, A.y + vectorY); // lane B(from)-A(to)
		Line2D line2DE2 = new Line2D.Double(A.x - vectorX, A.y - vectorY, B.x - vectorX, B.y - vectorY); // lane A(from)-B(to)

		// connect edge with its line
		edgePosition.put(edge1, line2DE2);
		edgePosition.put(edge2, line2DE1);
	}

	/**
	 * Edge color depends on number of cars per length.
	 *
	 * @param allEdgesLoad provides data about edge load
	 * @param edge		 examined edge
	 * @return Color based on load per length(m) or by default gray
	 */
	private Color getColorForEdge(AllEdgesLoad allEdgesLoad, SimulationEdge edge) {
		BigInteger id;
		try {
			id = edge.getStaticId();
		} catch (Exception e) {
			id = BigInteger.valueOf(-1);
		}
		if (id == BigInteger.valueOf(-1)) {
			double averageLoad = allEdgesLoad.getLoadPerEdge(id);
			double loadPerLength = averageLoad / edge.getLengthCm();
			return colorMap.getColor(loadPerLength);
		} else {
			return Color.gray;
		}
	}
}
