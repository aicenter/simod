/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.visio;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.agents.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.agents.amodsim.entity.DemandAgent;
import cz.agents.amodsim.entity.DemandAgentState;
import cz.agents.amodsim.storage.DemandStorage;
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
    
    private static final Color STATIONS_COLOR = Color.RED;
    
    private static final int SIZE = 3;
    
    
    
    
    protected final PositionUtil positionUtil;
    
    private final DemandStorage demandStorage;
    

    
    
    @Inject
    public DemandLayer(PositionUtil postitionUtil, DemandStorage demandStorage) {
        this.positionUtil = postitionUtil;
        this.demandStorage = demandStorage;
    }
    
    @Override
    public void paint(Graphics2D canvas) {
        Dimension dim = Vis.getDrawingDimension();

        DemandStorage.EntityIterator entityIterator = demandStorage.new EntityIterator();
        DemandAgent demandAgent;
        while((demandAgent = entityIterator.getNextEntity()) != null){
            Point2d agentPosition;
            if(demandAgent.getState() == DemandAgentState.RIDING){
                agentPosition = getDrivingAgentPosition(demandAgent);
            }
            else{
                agentPosition = getWaitingAgentPosition(demandAgent, dim);
            }
            
            if(agentPosition == null){
                continue;
            }
            
			drawDemand(agentPosition, canvas, dim);
        }
    }
    
    protected Point2d getDrivingAgentPosition(DemandAgent demandAgent){
        return positionUtil.getCanvasPositionInterpolated(demandAgent.getOnDemandVehicle());
    }
    
    protected Point2d getWaitingAgentPosition(DemandAgent demandAgent, Dimension drawingDimension){
        return positionUtil.getCanvasPosition(demandAgent.getPosition());
    }

    private void drawDemand(Point2d demandPosition, Graphics2D canvas, Dimension dim) {
        canvas.setColor(STATIONS_COLOR);
        int radius = SIZE;
		int width = radius * 2;

        int x1 = (int) (demandPosition.getX() - radius);
        int y1 = (int) (demandPosition.getY() - radius);
        int x2 = (int) (demandPosition.getX() + radius);
        int y2 = (int) (demandPosition.getY() + radius);
        if (x2 > 0 && x1 < dim.width && y2 > 0 && y1 < dim.height) {
            canvas.fillOval(x1, y1, width, width);
        }
    }
    
    
}
