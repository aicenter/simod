/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of Amodsim project.
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
package cz.cvut.fel.aic.amodsim.visio;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.HighwayNetwork;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.HighwayLayer;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioPositionUtil;
import cz.cvut.fel.aic.alite.vis.Vis;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.GraphSpec2D;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import javax.vecmath.Point2d;

/**
 *
 * @author fido
 */
@Singleton
public class BufferedHighwayLayer extends HighwayLayer{
	
	private static final int DEFAULT_SCALE = 5;
	
	BufferedImage cachedHighwayNetwork;
	
	private final int scale;
	
	private final GraphSpec2D mapSpecification;
	
	@Inject
	public BufferedHighwayLayer(HighwayNetwork highwayNetwork, VisioPositionUtil positionUtil, GraphSpec2D mapSpecification) {
		super(highwayNetwork, positionUtil);
		scale = DEFAULT_SCALE;
		this.mapSpecification = mapSpecification;
	}

	@Override
	protected void paintGraph(Graphics2D canvas, Rectangle2D drawingRectangle) {
		if(cachedHighwayNetwork == null){
			int imageWidth = mapSpecification.getWidth() / scale;
			int imageHeight = mapSpecification.getHeight() / scale;
			
			cachedHighwayNetwork = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_BYTE_BINARY);
//			cachedHighwayNetwork = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
			
			Graphics2D newCanvas = cachedHighwayNetwork.createGraphics();
			
			// background
			newCanvas.setColor(Color.WHITE);
			newCanvas.fillRect(0, 0, imageWidth, imageHeight);
			
			// graph
			newCanvas.setColor(Color.BLACK);
			newCanvas.setStroke(new BasicStroke(8));

			for (SimulationEdge edge : graph.getAllEdges()) {
				Point2d from = getPositionOnImage(edge.fromNode);
				Point2d to = getPositionOnImage(edge.toNode);
				Line2D line2d = new Line2D.Double(from.x, from.y, to.x, to.y);
				newCanvas.draw(line2d);
			}
		}
		
		canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		
		canvas.drawImage(cachedHighwayNetwork, Vis.transX(mapSpecification.minLon), 
				Vis.transY(mapSpecification.maxLat), Vis.transW(cachedHighwayNetwork.getWidth() * scale), 
				Vis.transH(cachedHighwayNetwork.getHeight() * scale), null);
		


//		//TEST
//		int imageWidth = positionUtil.getWorldWidth() / scale;
//		int imageHeight = positionUtil.getWorldHeight() / scale;
//		BufferedImage testImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
//
//		Graphics2D newCanvas = testImage.createGraphics();
//			
//		// background
//		newCanvas.setColor(Color.YELLOW);
//		newCanvas.fillRect(0, 0, imageWidth, imageHeight);
//		
////		newCanvas.setColor(Color.BLACK);
////		newCanvas.setStroke(new BasicStroke(20));
////		newCanvas.draw(new Line2D.Double(0, 0, imageWidth, imageHeight));
//		
//		canvas.drawImage(testImage, Vis.transX(mapSpecification.minLon), Vis.transY(mapSpecification.maxLat), 
//				Vis.transW(imageWidth * scale), Vis.transH(imageHeight * scale), null);
	}
	
	private Point2d getPositionOnImage(GPSLocation location){
		double x = (location.getLongitudeProjected() - mapSpecification.minLon) / scale;
		double y = (mapSpecification.maxLat - location.getLatitudeProjected()) / scale;
		
		return new Point2d(x, y);
	}
	
}
