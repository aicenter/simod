/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.visio;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.ClickableEntityLayer;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioUtils;
import cz.cvut.fel.aic.alite.vis.Vis;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;
import cz.cvut.fel.aic.amodsim.entity.DemandAgentState;
import cz.cvut.fel.aic.amodsim.storage.DemandStorage;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import javax.vecmath.Point2d;

/**
 *
 * @author fido
 */
@Singleton
public class DemandLayer extends ClickableEntityLayer<DemandAgent>  {

	private static final Color DEMAND_COLOR = Color.RED;

	private static final Color DROPPED_COLOR = Color.MAGENTA;

	private static final int SIZE = 1;

	private static final int TRANSFORMABLE_SIZE = 4;

	private static final Double TEXT_MARGIN_BOTTOM = 5.0;

	private static final Color TEXT_BACKGROUND_COLOR = Color.WHITE;




	protected final Set<DemandAgent> demandsWithPrintedInfo;





	@Inject
	public DemandLayer(DemandStorage demandStorage, AgentpolisConfig agentpolisConfig) {
		super(demandStorage, agentpolisConfig);
		demandsWithPrintedInfo = new HashSet<>();
	}


	@Override
	public void init(Vis vis) {
		super.init(vis);
		vis.addMouseListener(this);
	}


	protected Point2d getDrivingAgentPosition(DemandAgent demandAgent){
		return positionUtil.getCanvasPositionInterpolatedForVehicle(demandAgent.getTransportingEntity());
	}

	protected Point2d getDrivingAgentPositionInTime(DemandAgent demandAgent, long time){
		return positionUtil.getCanvasPositionInterpolatedForVehicleInTime(demandAgent.getTransportingEntity(), time);
	}

	protected Point2d getWaitingAgentPosition(DemandAgent demandAgent, Dimension drawingDimension){
		return positionUtil.getCanvasPosition(demandAgent.getPosition());
	}


	@Override
	protected Point2d getEntityPosition(DemandAgent demandAgent) {
		if(demandAgent.getState() == DemandAgentState.DRIVING){
			return getDrivingAgentPosition(demandAgent);
        }
        else{
            return getWaitingAgentPosition(demandAgent, dim);
        }
    }

	@Override
	protected Point2d getEntityPositionInTime(DemandAgent demandAgent, long time) {
		if(demandAgent.getState() == DemandAgentState.DRIVING){
			return getDrivingAgentPositionInTime(demandAgent, time);
		}
		else{
			return getWaitingAgentPosition(demandAgent, dim);
		}
	}

	@Override
	protected Color getEntityDrawColor(DemandAgent demandAgent) {
		if(demandAgent.isDropped()){
			return DROPPED_COLOR;
		}

		return DEMAND_COLOR;
	}

	@Override
	protected int getEntityTransformableRadius(DemandAgent demandAgent) {
		return SIZE;
	}

	@Override
	protected double getEntityStaticRadius(DemandAgent demandAgent) {
		return (double) SIZE;
	}

	@Override
	protected void processClick(DemandAgent nearestEntity) {
		if(demandsWithPrintedInfo.contains(nearestEntity)){
			demandsWithPrintedInfo.remove(nearestEntity);
		}
		else{
			demandsWithPrintedInfo.add(nearestEntity);
		}
	}

	@Override
	protected void drawEntity(DemandAgent demandAgent, Point2d entityPosition, Graphics2D canvas, Dimension dim) {
		super.drawEntity(demandAgent, entityPosition, canvas, dim);
		if(demandsWithPrintedInfo.contains(demandAgent)){
			double radius = getRadius(demandAgent);

			int x1 = (int) (entityPosition.getX() - radius);
			int y1 = (int) (entityPosition.getY() - radius);
			int x2 = (int) (entityPosition.getX() + radius);
			int y2 = (int) (entityPosition.getY() + radius);
			if (x2 > 0 && x1 < dim.width && y2 > 0 && y1 < dim.height) {
				VisioUtils.printTextWithBackgroud(canvas, demandAgent.getId(),
						new Point((int) (x1 + TEXT_MARGIN_BOTTOM), y1 + (y2 - y1) / 2), Color.BLACK,
						TEXT_BACKGROUND_COLOR);
			}
		}
	}

	@Override
	protected void drawEntities(ArrayList<DemandAgent> demandAgents, Point2d entityPosition, Graphics2D canvas,
								Dimension dim) {
		super.drawEntities(demandAgents, entityPosition, canvas, dim);
		if(demandsWithPrintedInfo.contains(demandAgents.get(0))){
			double radius = getRadius(demandAgents.get(0));

			int x1 = (int) (entityPosition.getX() - radius);
			int y1 = (int) (entityPosition.getY() - radius);
			int x2 = (int) (entityPosition.getX() + radius);
			int y2 = (int) (entityPosition.getY() + radius);
			if (x2 > 0 && x1 < dim.width && y2 > 0 && y1 < dim.height) {

				VisioUtils.printTextWithBackgroud(canvas, demandAgents.get(0).getId(),
						new Point((int) (x1 + TEXT_MARGIN_BOTTOM), y1 + (y2 - y1) / 2), Color.BLACK,
						TEXT_BACKGROUND_COLOR);
			}
		}
	}

}
