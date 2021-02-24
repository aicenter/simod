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
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioPositionUtil;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioUtils;
import cz.cvut.fel.aic.alite.vis.Vis;
import cz.cvut.fel.aic.alite.vis.layer.AbstractLayer;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.amodsim.storage.OnDemandvehicleStationStorage;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import javax.vecmath.Point2d;

/**
 *
 * @author fido
 */
@Singleton
public class OnDemandVehicleStationsLayer extends AbstractLayer{
	
	private static final Double TEXT_MARGIN_BOTTOM = 5.0;
	
	private static final Color TEXT_BACKGROUND_COLOR = Color.WHITE;
	
	private static final Color STATIONS_COLOR = Color.PINK;
	
	private static final int SIZE = 6;
	
	
	
	
	private final VisioPositionUtil postitionUtil;
	
	private final OnDemandvehicleStationStorage onDemandvehicleStationStorage;

	
	
	
	@Inject
	public OnDemandVehicleStationsLayer(VisioPositionUtil postitionUtil, 
			OnDemandvehicleStationStorage onDemandvehicleStationStorage) {
		this.postitionUtil = postitionUtil;
		this.onDemandvehicleStationStorage = onDemandvehicleStationStorage;
	}
	
	@Override
	public void paint(Graphics2D canvas) {
		Dimension dim = Vis.getDrawingDimension();

		OnDemandvehicleStationStorage.EntityIterator entityIterator = onDemandvehicleStationStorage.new EntityIterator();
		OnDemandVehicleStation onDemandVehicleStation;
		while((onDemandVehicleStation = entityIterator.getNextEntity()) != null){
			Point2d stationPosition = postitionUtil.getCanvasPosition(onDemandVehicleStation.getPosition());
			if(stationPosition == null){
				continue;
			}
			drawStation(stationPosition, canvas, dim, onDemandVehicleStation);
		}
	}

	private void drawStation(Point2d stationPosition, Graphics2D canvas, Dimension dim, OnDemandVehicleStation station) {
		canvas.setColor(STATIONS_COLOR);
		int radius = SIZE;
		int width = radius * 2;

		int x1 = (int) (stationPosition.getX() - radius);
		int y1 = (int) (stationPosition.getY() - radius);
		int x2 = (int) (stationPosition.getX() + radius);
		int y2 = (int) (stationPosition.getY() + radius);
		if (x2 > 0 && x1 < dim.width && y2 > 0 && y1 < dim.height) {
			canvas.fillRect(x1, y1, width, width);
			
			VisioUtils.printTextWithBackgroud(canvas, Integer.toString(station.getParkedVehiclesCount()), 
					new Point((int) (x1 - TEXT_MARGIN_BOTTOM), y1 - (y2 - y1) / 2), STATIONS_COLOR, 
					TEXT_BACKGROUND_COLOR);
			
			VisioUtils.printTextWithBackgroud(canvas, station.getId(), 
					new Point((int) (x1 + TEXT_MARGIN_BOTTOM), y1 + (y2 - y1) / 2), Color.BLACK, 
					TEXT_BACKGROUND_COLOR);
		}

	}
	
	
}
