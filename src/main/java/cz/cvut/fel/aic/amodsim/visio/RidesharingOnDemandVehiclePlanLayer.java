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
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioPositionUtil;
import cz.cvut.fel.aic.amodsim.ridesharing.RideSharingOnDemandVehicle;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author F.I.D.O.
 */
@Singleton
public class RidesharingOnDemandVehiclePlanLayer extends PlanLayer<RideSharingOnDemandVehicle>{
	
	private static final Color COLOR = Color.CYAN;
	
	private static final int SIZE = 3;
	
	@Inject
	public RidesharingOnDemandVehiclePlanLayer(OnDemandVehicleStorage entityStorage, VisioPositionUtil positionUtil) {
		super(entityStorage, positionUtil);
	}

	@Override
	protected void drawTrip(Graphics2D canvas, Dimension dim, Rectangle2D drawingRectangle, 
			RideSharingOnDemandVehicle entity) {
		List<PlanLayerTrip> trips = entity.getPlanForRendering();
		for(PlanLayerTrip trip: trips){
			drawTrip(canvas, drawingRectangle, trip);
		}
	}

	private void drawTrip(Graphics2D canvas, Rectangle2D drawingRectangle, PlanLayerTrip trip) {
		LinkedList<SimulationNode> locations = trip.getLocations();
		Iterator<SimulationNode> iterator = locations.iterator();
		int startLocationNodeId = iterator.next().getId();
		while (iterator.hasNext()) {
			int targetLocationNodeId = iterator.next().getId();
			drawLine(canvas, drawingRectangle, startLocationNodeId, targetLocationNodeId);
			startLocationNodeId = targetLocationNodeId;
		}
	}
	
	
	
}
