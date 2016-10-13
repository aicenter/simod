/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim.visio;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mycompany.testsim.entity.DemandAgent;
import com.mycompany.testsim.entity.OnDemandVehicleStation;
import com.mycompany.testsim.storage.DemandStorage;
import com.mycompany.testsim.storage.OnDemandvehicleStationStorage;
import cz.agents.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.agents.agentpolis.simulator.visualization.visio.entity.AgentPositionUtil;
import cz.agents.alite.vis.Vis;
import cz.agents.alite.vis.layer.AbstractLayer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.vecmath.Point2d;

/**
 *
 * @author fido
 */
@Singleton
public class DemandLayer extends AbstractLayer{
    
    private static final Color STATIONS_COLOR = Color.YELLOW;
    
    private static final int SIZE = 3;
    
    
    
    
    private final AgentPositionUtil postitionUtil;
    
    private final DemandStorage demandStorage;

    
    
    
    @Inject
    public DemandLayer(AgentPositionUtil postitionUtil, 
            DemandStorage demandStorage) {
        this.postitionUtil = postitionUtil;
        this.demandStorage = demandStorage;
    }
    
    @Override
    public void paint(Graphics2D canvas) {
        Dimension dim = Vis.getDrawingDimension();

        DemandStorage.EntityIterator entityIterator = demandStorage.new EntityIterator();
        DemandAgent demandAgent;
        while((demandAgent = entityIterator.getNextEntity()) != null){
            Point2d agentPosition = postitionUtil.getEntityCanvasPosition(demandAgent);
            if(agentPosition == null){
                continue;
            }
			drawStation(agentPosition, canvas, dim);
        }
    }

    private void drawStation(Point2d stationPosition, Graphics2D canvas, Dimension dim) {
        canvas.setColor(STATIONS_COLOR);
        int radius = SIZE;
		int width = radius * 2;

        int x1 = (int) (stationPosition.getX() - radius);
        int y1 = (int) (stationPosition.getY() - radius);
        int x2 = (int) (stationPosition.getX() + radius);
        int y2 = (int) (stationPosition.getY() + radius);
        if (x2 > 0 && x1 < dim.width && y2 > 0 && y1 < dim.height) {
            canvas.fillOval(x1, y1, width, width);
        }
    }
    
    
}
