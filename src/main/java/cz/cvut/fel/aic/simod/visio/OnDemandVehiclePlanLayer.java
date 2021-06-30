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
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.HighwayNetwork;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioPositionUtil;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.simod.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.geographtools.Node;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import javax.vecmath.Point2d;

/**
 *
 * @author F.I.D.O.
 */
@Singleton
public class OnDemandVehiclePlanLayer extends PlanLayer<OnDemandVehicle>{
	
	private static final Color COLOR = Color.CYAN;
	
	private static final int SIZE = 3;
	
	@Inject
	public OnDemandVehiclePlanLayer(OnDemandVehicleStorage entityStorage, VisioPositionUtil positionUtil,
			HighwayNetwork highwayNetwork) {
		super(entityStorage, positionUtil, highwayNetwork);
	}

	@Override
	protected void drawTrip(Graphics2D canvas, Dimension dim, Rectangle2D drawingRectangle, OnDemandVehicle entity) {
		super.drawTrip(canvas, dim, drawingRectangle, entity); 
		
		Node demandTarget = entity.getDemandTarget();
		if(demandTarget != null){
			Point2d demandTargetPosition = positionUtil.getCanvasPosition(demandTarget);

			canvas.setColor(COLOR);
			int radius = SIZE;
			int width = radius * 2;

			int x1 = (int) (demandTargetPosition.getX() - radius);
			int y1 = (int) (demandTargetPosition.getY() - radius);
			int x2 = (int) (demandTargetPosition.getX() + radius);
			int y2 = (int) (demandTargetPosition.getY() + radius);
			if (x2 > 0 && x1 < dim.width && y2 > 0 && y1 < dim.height) {
				canvas.fillOval(x1, y1, width, width);
			}

			canvas.setColor(PlanLayer.TRIP_COLOR);
		}
	}
	
	
	
}
