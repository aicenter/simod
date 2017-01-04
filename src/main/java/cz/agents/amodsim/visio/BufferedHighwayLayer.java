/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.visio;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.SimulationEdge;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.networks.HighwayNetwork;
import cz.agents.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.agents.alite.vis.Vis;

import javax.vecmath.Point2d;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 *
 * @author fido
 */
@Singleton
public class BufferedHighwayLayer extends HighwayLayer{
    
    BufferedImage cachedHighwayNetwork;
    
    
    
    @Inject
    public BufferedHighwayLayer(HighwayNetwork highwayNetwork, PositionUtil positionUtil) {
        super(highwayNetwork, positionUtil);
    }

    @Override
    protected void paintGraph(Graphics2D canvas, Rectangle2D drawingRectangle) {
        if(cachedHighwayNetwork == null){
            int imageWidth = positionUtil.getWorldWidth();
            int imageHeight = positionUtil.getWorldHeight();
            
            cachedHighwayNetwork = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_BYTE_BINARY);
            
            Graphics2D newCanvas = cachedHighwayNetwork.createGraphics();
            
            // background
            newCanvas.setBackground(Color.WHITE);
            newCanvas.fillRect(0, 0, imageWidth, imageHeight);
            
            // graph
            newCanvas.setColor(Color.BLACK);
            newCanvas.setStroke(new BasicStroke(8));

            for (SimulationEdge edge : graph.getAllEdges()) {
                Point2d from = positionUtil.getPosition(graph.getNode(edge.fromId));
                Point2d to = positionUtil.getPosition(graph.getNode(edge.toId));
                Line2D line2d = new Line2D.Double(from.x, from.y, to.x, to.y);
                newCanvas.draw(line2d);
            }
        }
        
        canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        
        canvas.drawImage(cachedHighwayNetwork, Vis.transX(drawingRectangle.getX()), 
                Vis.transY(drawingRectangle.getY()), Vis.transW(cachedHighwayNetwork.getWidth()), 
                Vis.transH(cachedHighwayNetwork.getHeight()), null);
    }
    
}
