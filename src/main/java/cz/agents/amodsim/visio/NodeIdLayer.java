/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.visio;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.SimulationNode;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.networks.HighwayNetwork;
import cz.agents.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.agents.alite.vis.layer.AbstractLayer;

import javax.vecmath.Point2d;
import java.awt.*;
import java.util.LinkedList;

/**
 *
 * @author fido
 */
@Singleton
public class NodeIdLayer extends AbstractLayer{
    
    private final HighwayNetwork highwayNetwork;
    
    private final PositionUtil positionUtil;
    
    private final LinkedList<Integer> highLightedNodes;

    
    
    @Inject
    public NodeIdLayer(HighwayNetwork highwayNetwork, PositionUtil positionUtil) {
        this.highwayNetwork = highwayNetwork;
        this.positionUtil = positionUtil;
        highLightedNodes = new LinkedList<>();
        
        highLightedNodes.add(8721);
        highLightedNodes.add(39675);
        
        highLightedNodes.add(37832);
        highLightedNodes.add(19420);
    }

    
    
    
    @Override
    public void paint(Graphics2D canvas) {
        canvas.setColor(Color.BLUE);
        for (SimulationNode node : highwayNetwork.getNetwork().getAllNodes()) {
            Font f = null;
            if(highLightedNodes.contains(node.getId())){
                canvas.setColor(Color.GREEN);
                f = canvas.getFont();
                canvas.setFont(new Font("TimesRoman", Font.BOLD, 25)); 
            }
            
            Point2d nodePoint = positionUtil.getCanvasPosition(node);
            canvas.drawString(Integer.toString(node.getId()), (int) nodePoint.x, (int) nodePoint.y);
            
            if(highLightedNodes.contains(node.getId())){
                canvas.setColor(Color.BLUE);
                canvas.setFont(f);
            }
        }
    }
    
    
    
    
}
