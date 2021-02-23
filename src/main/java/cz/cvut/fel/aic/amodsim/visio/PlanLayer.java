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
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.VehicleTrip;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.AgentPolisEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.HighwayNetwork;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioPositionUtil;
import cz.cvut.fel.aic.alite.vis.Vis;
import cz.cvut.fel.aic.alite.vis.layer.AbstractLayer;
import cz.cvut.fel.aic.amodsim.NearestVehicleComparator;
import cz.cvut.fel.aic.amodsim.entity.PlanningAgent;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Graph;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import javax.vecmath.Point2d;

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
	
	private final Graph<SimulationNode, SimulationEdge> network;


	@Inject
	public PlanLayer(OnDemandVehicleStorage entityStorage, VisioPositionUtil positionUtil, HighwayNetwork highwayNetwork) {
		this.entityStorage = entityStorage;
		this.positionUtil = positionUtil;
		network = highwayNetwork.getNetwork();
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
		VehicleTrip<SimulationNode> trip = entity.getCurrentTripPlan();
		SimulationNode[] locations = trip.getLocations();
		Iterator<SimulationNode> iterator = Arrays.asList(locations).iterator();
		SimulationNode startLocation = trip.getFirstLocation();
		while (iterator.hasNext()) {
			SimulationNode targetLocation = iterator.next();
			drawOnEdge(canvas, drawingRectangle, startLocation, targetLocation);
			startLocation = targetLocation;
		}
	}

	protected void drawOnEdge(Graphics2D canvas, Rectangle2D drawingRectangle, SimulationNode startLocation, 
			SimulationNode targetLocation) {
		SimulationEdge edge = network.getEdge(startLocation, targetLocation);
		Iterator<GPSLocation> iterator = edge.shape.iterator();
		
		Point2d startPosition = positionUtil.getCanvasPosition(iterator.next());
		while(iterator.hasNext()){
			Point2d targetPosition = positionUtil.getCanvasPosition(iterator.next());
			
			int x = (int) startPosition.x;
			int y = (int) startPosition.y;
			int xTo = (int) targetPosition.x;
			int yTo = (int) targetPosition.y;

			Line2D line2d = new Line2D.Double(x, y, xTo, yTo);

			if (line2d.intersects(drawingRectangle)) {
				canvas.draw(line2d);
			}
			startPosition = targetPosition;
		}
	}
        
        protected void drawOnEdgeWithAgent(Graphics2D canvas, Rectangle2D drawingRectangle,SimulationNode startLocation,
                SimulationNode targetLocation,Point2d entityPosition) {

                SimulationEdge edge = network.getEdge(startLocation, targetLocation);
		Iterator<GPSLocation> iterator = edge.shape.iterator();
                boolean readyToDraw = false;
                
                int entX = (int) entityPosition.x;
                int entY = (int) entityPosition.y;
                
                Point2d startPosition = positionUtil.getCanvasPosition(iterator.next());
		while(iterator.hasNext()){
			Point2d targetPosition = positionUtil.getCanvasPosition(iterator.next());
                        
                        int x = (int) startPosition.x;
                        int y = (int) startPosition.y;
                        int xTo = (int) targetPosition.x;
                        int yTo = (int) targetPosition.y;
                        
                        //entity spotted
                        if(isBetweenX(x, xTo, entX) && isBetweenY(y, yTo, entY)){
                                x = entX;
                                y = entY;
                                readyToDraw = true;
                        }
                      
                        
                        Line2D line2d = new Line2D.Double(x, y, xTo, yTo);

                        if (line2d.intersects(drawingRectangle) && readyToDraw) {
                                canvas.draw(line2d);
                        }
                        
			startPosition = targetPosition;
		}
		
	}
        
        private boolean isBetweenX(int x, int xTo, int entX){
            return (xTo >= x && entX <= xTo && entX >= x) || (xTo <= x && entX <= x && entX >= xTo);
        }
        
        private boolean isBetweenY(int y, int yTo, int entY){
            return (yTo >= y && entY <= yTo && entY >= y) ||  (yTo <= y && entY <= y && entY >= yTo);
        }


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
			((OnDemandVehicle) agent).getVehicle().setHighlited(false);
		} else {
			drawedEntities.add(agent);
			((OnDemandVehicle) agent).getVehicle().setHighlited(true);
		}
	}

	@Override
	public void init(Vis vis) {
		super.init(vis);
		vis.addMouseListener((java.awt.event.MouseListener) this);
	}


}
