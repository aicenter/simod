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
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.HighwayNetwork;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioPositionUtil;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioUtils;
import cz.cvut.fel.aic.alite.vis.Vis;
import cz.cvut.fel.aic.amodsim.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanActionPickup;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanRequestAction;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
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
	
	private static final int ACTION_MARKER_SIZE = 10;
	
	private static final int TRIP_LINE_THIKNESS = 4;
	
	
	
	
	@Inject
	public RidesharingOnDemandVehiclePlanLayer(OnDemandVehicleStorage entityStorage, VisioPositionUtil positionUtil,
			HighwayNetwork highwayNetwork) {
		super(entityStorage, positionUtil, highwayNetwork);
	}

	@Override
	protected void drawTrip(Graphics2D canvas, Dimension dim, Rectangle2D drawingRectangle, 
			RideSharingOnDemandVehicle entity) {
		List<PlanLayerTrip> trips = entity.getPlanForRendering();
		for(PlanLayerTrip trip: trips){
			drawTrip(canvas, drawingRectangle, trip);
			drawAction(canvas, drawingRectangle, trip.getTask());
		}
	}

	private void drawTrip(Graphics2D canvas, Rectangle2D drawingRectangle, PlanLayerTrip trip) {
		canvas.setColor(TRIP_COLOR);
		Stroke stroke = canvas.getStroke();
		canvas.setStroke(new BasicStroke(TRIP_LINE_THIKNESS));
		
		SimulationNode[] locations = trip.getLocations();
		Iterator<SimulationNode> iterator = Arrays.asList(locations).iterator();
		SimulationNode startLocation = iterator.next();
		while (iterator.hasNext()) {
			SimulationNode targetLocation = iterator.next();
			drawOnEdge(canvas, drawingRectangle, startLocation, targetLocation);
			startLocation = targetLocation;
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

			String textIn = task instanceof PlanActionPickup ? "Pickup" : "Dropoff";
			String title = String.format("%s demand %s", textIn, task.getRequest().getDemandAgent().getSimpleId());
			
			VisioUtils.printTextWithBackgroud(canvas, title,
						new Point((int) (x1 - 5), y1 - (y2 - y1) / 2), color, Color.WHITE);
			
		}
	}
	
	
	
}
