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
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.HighwayNetwork;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioPositionUtil;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioUtils;
import cz.cvut.fel.aic.alite.vis.Vis;
import cz.cvut.fel.aic.amodsim.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanActionPickup;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanRequestAction;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import static cz.cvut.fel.aic.amodsim.visio.OnDemandVehicleLayer.NORMAL_COLOR;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.vecmath.Point2d;

/**
 *
 * @author F.I.D.O.
 */
@Singleton
public class RidesharingOnDemandVehiclePlanLayer extends PlanLayer<RideSharingOnDemandVehicle>{
	
	private static final Color TRIP_COLOR = Color.YELLOW;
	
	private static final Color PICKUP_COLOR = Color.CYAN;
	
	private static final Color DROPOFF_COLOR = Color.MAGENTA;
	
	private static final int ACTION_MARKER_SIZE = 20;
	
	private static final int TRIP_LINE_THIKNESS = 4;
	
	private static final int TRIP_BORDER_THIKNESS = 1;
	
	private static final Color TRIP_BORDER_COLOR = Color.BLACK;
	
	
	
	private final OnDemandVehicleLayer onDemandVehicleLayer;
	
	private final TimeProvider timeProvider;
	
	
	@Inject
	public RidesharingOnDemandVehiclePlanLayer(OnDemandVehicleStorage entityStorage, VisioPositionUtil positionUtil,
			HighwayNetwork highwayNetwork, OnDemandVehicleLayer onDemandVehicleLayer, TimeProvider timeProvider) {
		super(entityStorage, positionUtil, highwayNetwork);
		this.onDemandVehicleLayer = onDemandVehicleLayer;
		this.timeProvider = timeProvider;
	}

	@Override
	protected void drawTrip(Graphics2D canvas, Dimension dim, Rectangle2D drawingRectangle, 
			RideSharingOnDemandVehicle entity) {
		List<PlanLayerTrip> trips = entity.getPlanForRendering();
		boolean firstTrip = true;
		for(PlanLayerTrip trip: trips){
			drawTrip(canvas, drawingRectangle, trip, entity, firstTrip);
			firstTrip = false;
		}
		
		Font currentFont = canvas.getFont();
		canvas.setFont(currentFont.deriveFont(25f)); 
		for(PlanLayerTrip trip: trips){
			drawAction(canvas, drawingRectangle, trip.getTask());
		}
		canvas.setFont(currentFont);
		
		// draw enlarged vehicle
		canvas.setColor(OnDemandVehicleLayer.HIGHLIGHTED_COLOR);
		long time = timeProvider.getCurrentSimTime();
		Point2d entityPosition = onDemandVehicleLayer.getEntityPositionInTime(entity.getVehicle(), time);
		onDemandVehicleLayer.drawEntityShape(entity.getVehicle(), entityPosition, canvas, false);
		
		// onboard count
		int x = (int) (entityPosition.getX());
		int y = (int) (entityPosition.getY());
		currentFont = canvas.getFont();
		canvas.setFont(currentFont.deriveFont(25f)); 
		VisioUtils.printTextWithBackgroud(canvas, Integer.toString(entity.getVehicle().getTransportedEntities().size()),
				new Point(x + 8, y - 8), NORMAL_COLOR, Color.WHITE);
		canvas.setFont(currentFont); 
		
		// draw border
		
	}

	private void drawTrip(Graphics2D canvas, Rectangle2D drawingRectangle, PlanLayerTrip trip, 
			RideSharingOnDemandVehicle entity, boolean firstTrip) {
		
		Stroke stroke = canvas.getStroke();
		SimulationNode[] locations = trip.getLocations();
		
		// print border
		canvas.setColor(TRIP_BORDER_COLOR);
		canvas.setStroke(new BasicStroke(TRIP_BORDER_THIKNESS * 2 + TRIP_LINE_THIKNESS));
		Iterator<SimulationNode> iterator = Arrays.asList(locations).iterator();
		
                long time = timeProvider.getCurrentSimTime();
                Point2d entityPosition = onDemandVehicleLayer.getEntityPositionInTime(entity.getVehicle(), time);
                SimulationNode startLocation = iterator.next();
		SimulationNode targetLocation = iterator.next();
                
                //first edge is special -> start is where the car is not on the station
                if(firstTrip){drawOnEdgeWithAgent(canvas, drawingRectangle,startLocation,targetLocation,entityPosition);}
                else{drawOnEdge(canvas, drawingRectangle, startLocation, targetLocation);}
		
		while (iterator.hasNext()) {
			startLocation = targetLocation;
			targetLocation = iterator.next();
			drawOnEdge(canvas, drawingRectangle, startLocation, targetLocation);
		}
		
		canvas.setColor(TRIP_COLOR);
		canvas.setStroke(new BasicStroke(TRIP_LINE_THIKNESS));
		iterator = Arrays.asList(locations).iterator();
                
                startLocation = iterator.next();
                targetLocation = iterator.next();
                if(firstTrip){drawOnEdgeWithAgent(canvas, drawingRectangle,startLocation,targetLocation,entityPosition);}
                else{drawOnEdge(canvas, drawingRectangle, startLocation, targetLocation);}
		
		while (iterator.hasNext()) {
                        startLocation = targetLocation;
			targetLocation = iterator.next();
			drawOnEdge(canvas, drawingRectangle, startLocation, targetLocation);
		}
		
		canvas.setStroke(stroke);
	}

	private void drawAction(Graphics2D canvas, Rectangle2D drawingRectangle, PlanRequestAction task) {
		Color color = task instanceof PlanActionPickup ? PICKUP_COLOR : DROPOFF_COLOR;
		canvas.setColor(color);
		
		Point2d taskPosition = positionUtil.getCanvasPosition(task.getPosition());
		float radius = ACTION_MARKER_SIZE / 2;

		int x1 = (int) (taskPosition.getX() - radius);
		int y1 = (int) (taskPosition.getY() - radius);
		int x2 = (int) (taskPosition.getX() + radius);
		int y2 = (int) (taskPosition.getY() + radius);
		if (VisioUtils.rectangleOverlaps(x1, y1, x2, y2, Vis.getDrawingDimension())) {
			canvas.fillOval(x1, y1, ACTION_MARKER_SIZE, ACTION_MARKER_SIZE);

			String textIn = task instanceof PlanActionPickup ? "P" : "D";
			String title = String.format("%s %s", textIn, task.getRequest().getDemandAgent().getSimpleId());
			
			VisioUtils.printTextWithBackgroud(canvas, title,
						new Point(x1 + 15, y1 - 5), Color.BLACK, Color.WHITE);
			
		}
	}
	
	
	
}
