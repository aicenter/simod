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
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.alite.vis.Vis;
import cz.cvut.fel.aic.simod.entity.agent.DemandAgent;
import cz.cvut.fel.aic.simod.storage.DemandStorage;
import java.util.HashMap;
import java.util.Random;
import javax.vecmath.Point2d;

/**
 *
 * @author fido
 */
@Singleton
public class DemandLayerWithJitter extends DemandLayer{
	
	private static final double JITTER_DEVIATION = 8;
	
	
	
	
	private final HashMap<DemandAgent,Point2d> jitterCache;
	
	private final Random random;
	
	
	
	
	@Inject
	public DemandLayerWithJitter(DemandStorage demandStorage, AgentpolisConfig agentpolisConfig) {
		super(demandStorage, agentpolisConfig);
		jitterCache = new HashMap<>();
		random = new Random();
	}

	@Override
	protected Point2d getWaitingAgentPosition(DemandAgent demandAgent) {
//		Point2d agentPosition =  positionUtil.getPosition(demandAgent.getPosition()); 
		Point2d agentJitter;
		if(jitterCache.containsKey(demandAgent)){
			agentJitter = jitterCache.get(demandAgent);
		}
		else{
			agentJitter = getJitter();
			jitterCache.put(demandAgent, agentJitter);
		}
		
		Point2d canvasPosition = positionUtil.getCanvasPosition(demandAgent.getPosition());
		
		jitte(canvasPosition, agentJitter);
		
		return canvasPosition;
	}

	@Override
	protected Point2d getDrivingAgentPosition(DemandAgent demandAgent) {
		if(jitterCache.containsKey(demandAgent)){
			jitterCache.remove(demandAgent);
		}
		return super.getDrivingAgentPosition(demandAgent); 
	}

	private Point2d getJitter() {
                double jittX = random.nextGaussian();                    
                double jittY = random.nextGaussian();                    
		
		return new Point2d(jittX, jittY);               
	}

	private void jitte(Point2d agentPosition, Point2d agentJitter) {
                double zoom_deviation = Vis.getZoomFactor();
		agentPosition.x = agentPosition.x + JITTER_DEVIATION * agentJitter.x * zoom_deviation;
		agentPosition.y = agentPosition.y + JITTER_DEVIATION * agentJitter.y * zoom_deviation;
	}
	
	
}
