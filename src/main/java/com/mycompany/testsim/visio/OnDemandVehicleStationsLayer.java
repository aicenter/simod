/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim.visio;

import com.mycompany.testsim.storage.OnDemandvehicleStationStorage;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mycompany.testsim.entity.OnDemandVehicleStation;
import cz.agents.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.agents.agentpolis.simulator.visualization.visio.VisioUtils;
import cz.agents.alite.vis.Vis;
import cz.agents.alite.vis.layer.AbstractLayer;
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
    
    
    
    
    private final PositionUtil postitionUtil;
    
    private final OnDemandvehicleStationStorage onDemandvehicleStationStorage;

    
    
    
    @Inject
    public OnDemandVehicleStationsLayer(PositionUtil postitionUtil, 
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
            Point2d stationPosition = postitionUtil.getCanvasPosition(onDemandVehicleStation.getPositionInGraph());
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
        }

    }
    
    
}
