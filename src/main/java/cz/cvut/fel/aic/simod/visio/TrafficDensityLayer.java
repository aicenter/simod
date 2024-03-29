/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of the SiMoD project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.fel.aic.simod.visio;

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
import cz.cvut.fel.aic.simod.entity.agent.OnDemandVehicle;
import cz.cvut.fel.aic.simod.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.geographtools.Graph;
import edu.mines.jtk.awt.ColorMap;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.math.BigInteger;
import javax.vecmath.Point2d;

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
	
	private final VisioPositionUtil positionUtil;
	
	
	
	

	@Inject
	public TrafficDensityLayer(HighwayNetwork highwayNetwork, VisioPositionUtil positionUtil, 
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
			Point2d from = positionUtil.getCanvasPosition(edge.getFromNode());
			Point2d to = positionUtil.getCanvasPosition(edge.getToNode());
			Line2D line2d = new Line2D.Double(from.x, from.y, to.x, to.y);
			if (line2d.intersects(drawingRectangle)) {
				canvas.draw(line2d);
			}
		}
	}

	private Color getColorForEdge(AllEdgesLoad allEdgesLoad, SimulationEdge edge) {
		BigInteger id = edge.getStaticId();
		double averageLoad = allEdgesLoad.getLoadPerEdge(id);
		double loadPerLength = averageLoad / edge.getLengthCm();
		return colorMap.getColor(loadPerLength);
	}
}
