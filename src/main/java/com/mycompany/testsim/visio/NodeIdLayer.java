/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim.visio;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.AllNetworkNodes;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.HighwayNetwork;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.SimulationNode;
import cz.agents.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.agents.alite.vis.layer.AbstractLayer;
import java.awt.Color;
import java.awt.Graphics2D;
import javax.vecmath.Point2d;

/**
 *
 * @author fido
 */
@Singleton
public class NodeIdLayer extends AbstractLayer{
    
    private final HighwayNetwork highwayNetwork;
    
    private final PositionUtil positionUtil;

    
    
    @Inject
    public NodeIdLayer(HighwayNetwork highwayNetwork, PositionUtil positionUtil) {
        this.highwayNetwork = highwayNetwork;
        this.positionUtil = positionUtil;
    }

    
    
    
    @Override
    public void paint(Graphics2D canvas) {
        canvas.setColor(Color.BLACK);
        for (SimulationNode node : highwayNetwork.getNetwork().getAllNodes()) {
            Point2d nodePoint = positionUtil.getCanvasPosition(node);
            canvas.drawString(Integer.toString(node.getId()), (int) nodePoint.x, (int) nodePoint.y);
        }
    }
    
    
    
    
}
