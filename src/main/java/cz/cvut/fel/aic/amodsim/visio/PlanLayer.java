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
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.TripItem;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.VehicleTrip;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.AgentPolisEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioPositionUtil;
import cz.cvut.fel.aic.alite.vis.Vis;
import cz.cvut.fel.aic.alite.vis.layer.AbstractLayer;
import cz.cvut.fel.aic.amodsim.NearestVehicleComparator;
import cz.cvut.fel.aic.amodsim.entity.PlanningAgent;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;

import javax.vecmath.Point2d;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * @param <E> entity type
 * @author fido
 */

public class PlanLayer<E extends AgentPolisEntity & PlanningAgent> extends AbstractLayer implements MouseListener {

	protected static final Color TRIP_COLOR = Color.YELLOW;

	private static final float LINE_WIDTH = 0.5f;

	private static final int CLICK_DISTANCE_IN_PX = 15;


	protected final OnDemandVehicleStorage entityStorage;

	protected final VisioPositionUtil positionUtil;

	protected final ArrayList<E> drawedEntities;


	@Inject
	public PlanLayer(OnDemandVehicleStorage entityStorage, VisioPositionUtil positionUtil) {
		this.entityStorage = entityStorage;
		this.positionUtil = positionUtil;
		drawedEntities = new ArrayList<>();
	}


	@Override
	public void paint(Graphics2D canvas) {
		canvas.setColor(TRIP_COLOR);
		canvas.setStroke(new BasicStroke(LINE_WIDTH));
		Dimension dim = Vis.getDrawingDimension();
		Rectangle2D drawingRectangle = new Rectangle(dim);
		for (E entity : drawedEntities) {
			if (entity.getCurrentTripPlan() != null) {
				drawTrip(canvas, dim, drawingRectangle, entity);
			}
		}
	}

	protected void drawTrip(Graphics2D canvas, Dimension dim, Rectangle2D drawingRectangle, E entity) {
		VehicleTrip trip = entity.getCurrentTripPlan();
		LinkedList<TripItem> locations = trip.getLocations();
		Iterator<TripItem> iterator = locations.iterator();
		int startLocationNodeId = iterator.next().tripPositionByNodeId;
		while (iterator.hasNext()) {
			int targetLocationNodeId = iterator.next().tripPositionByNodeId;
			drawLine(canvas, drawingRectangle, startLocationNodeId, targetLocationNodeId);
			startLocationNodeId = targetLocationNodeId;
		}
	}

	protected void drawLine(Graphics2D canvas, Rectangle2D drawingRectangle, int startLocationNodeId, 
			int targetLocationNodeId) {
		Point2d startPosition = positionUtil.getCanvasPosition(startLocationNodeId);
		Point2d targetPosition = positionUtil.getCanvasPosition(targetLocationNodeId);

		int x = (int) startPosition.x;
		int y = (int) startPosition.y;
		int xTo = (int) targetPosition.x;
		int yTo = (int) targetPosition.y;

		Line2D line2d = new Line2D.Double(x, y, xTo, yTo);

		if (line2d.intersects(drawingRectangle)) {
			canvas.draw(line2d);
		}
	}

//	@Override
//	public void mouseClicked(MouseEvent mouseEvent) {
//		if (mouseEvent.getButton() == MouseEvent.BUTTON1) {
//			double clickDistanceInM = Vis.transInvW(CLICK_DISTANCE_IN_PX);
//			Point2d clickInRealCoords = new Point2d(Vis.transInvX(mouseEvent.getX()), Vis.transInvY(mouseEvent.getY()));
//
//			if (entityStorage.isEmpty() == false) {
//				AgentPolisEntity closestAgent = Collections.min(entityStorage.getEntities(), 
//						new NearestEntityComparator<>(entityPositionUtil, clickInRealCoords));
//
//				if (entityPositionUtil.getEntityPosition(closestAgent).distance(clickInRealCoords) 
//						<= clickDistanceInM) {
//					switchDrawPlan((E) closestAgent);
//				}
//			}
//		}
//	}

	@Override
	public void mouseClicked(MouseEvent mouseEvent) {
		if (mouseEvent.getButton() == MouseEvent.BUTTON1) {
			Point2d click = new Point2d(mouseEvent.getX(), mouseEvent.getY());

			if (entityStorage.isEmpty() == false) {
				OnDemandVehicle closestAgent = (OnDemandVehicle) Collections.min(entityStorage.getEntities(),
						new NearestVehicleComparator(positionUtil, click));

				if (positionUtil.getCanvasPositionInterpolated(closestAgent, EGraphType.HIGHWAY).distance(click) <= CLICK_DISTANCE_IN_PX) {
					switchDrawPlan((E) closestAgent);
				}
			}
		}
	}

	@Override
	public void mouseEntered(MouseEvent me) {

	}

	@Override
	public void mouseExited(MouseEvent me) {

	}

	@Override
	public void mousePressed(MouseEvent me) {

	}

	@Override
	public void mouseReleased(MouseEvent me) {

	}


	private void switchDrawPlan(E agent) {
   		if (drawedEntities.contains(agent)) {
			drawedEntities.remove(agent);
		} else {
			drawedEntities.add(agent);
		}
	}

	@Override
	public void init(Vis vis) {
		super.init(vis);
		vis.addMouseListener((java.awt.event.MouseListener) this);
	}


}
