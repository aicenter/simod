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
package cz.cvut.fel.aic.simod.visio;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.ClickableEntityLayer;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioUtils;
import cz.cvut.fel.aic.alite.vis.Vis;
import cz.cvut.fel.aic.simod.entity.DemandAgentState;
import cz.cvut.fel.aic.simod.entity.ParcelAgent;
import cz.cvut.fel.aic.simod.entity.SimulationAgent;
import cz.cvut.fel.aic.simod.storage.ParcelStorage;

import javax.vecmath.Point2d;
import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author viskuond
 */
@Singleton
public class ParcelLayer extends ClickableEntityLayer<ParcelAgent> {
    private static final Color DEMAND_COLOR = new Color(194, 184, 0);

    private static final Color DROPPED_COLOR = Color.ORANGE;

    private static final int SIZE = 1;

    private static final int TRANSFORMABLE_SIZE = 4;

    private static final Double TEXT_MARGIN_BOTTOM = 5.0;

    private static final Color TEXT_BACKGROUND_COLOR = Color.WHITE;

    protected final Set<ParcelAgent> demandsWithPrintedInfo;

    @Inject
    public ParcelLayer(ParcelStorage parcelStorage, AgentpolisConfig agentpolisConfig) {
        super(parcelStorage, agentpolisConfig);
        demandsWithPrintedInfo = new HashSet<>();
    }

    @Override
    public void init(Vis vis) {
        super.init(vis);
        vis.addMouseListener(this);
    }


    protected Point2d getDrivingAgentPosition(ParcelAgent parcelAgent) {
        return positionUtil.getCanvasPositionInterpolatedForVehicle(parcelAgent.getTransportingEntity());
    }

    protected Point2d getDrivingAgentPositionInTime(ParcelAgent parcelAgent, long time) {
        return positionUtil.getCanvasPositionInterpolatedForVehicleInTime(parcelAgent.getTransportingEntity(), time);
    }

    protected Point2d getWaitingAgentPosition(ParcelAgent parcelAgent) {
        return positionUtil.getCanvasPosition(parcelAgent.getPosition());
    }


    @Override
    protected Point2d getEntityPosition(ParcelAgent parcelAgent) {
        if (parcelAgent.getState() == DemandAgentState.DRIVING) {
            return getDrivingAgentPosition(parcelAgent);
        } else {
            return getWaitingAgentPosition(parcelAgent);
        }
    }

    @Override
    protected Point2d getEntityPositionInTime(ParcelAgent parcelAgent, long time) {
        if (parcelAgent.getState() == DemandAgentState.DRIVING) {
            return getDrivingAgentPositionInTime(parcelAgent, time);
        } else {
            return getWaitingAgentPosition(parcelAgent);
        }
    }

    @Override
    protected Color getEntityDrawColor(ParcelAgent parcelAgent) {
        if (parcelAgent.isDropped()) {
            return DROPPED_COLOR;
        }
        return DEMAND_COLOR;
    }

    @Override
    protected int getEntityTransformableRadius(ParcelAgent parcelAgent) {
        return SIZE;
    }

    @Override
    protected double getEntityStaticRadius(ParcelAgent parcelAgent) {
        return SIZE;
    }

    @Override
    protected void processClick(ParcelAgent parcelAgent) {
    }

    @Override
    protected void drawEntities(List<ParcelAgent> parcelAgents, Point2d entityPosition, Graphics2D canvas,
                                Dimension dim) {
        super.drawEntities(parcelAgents, entityPosition, canvas, dim);
        if (demandsWithPrintedInfo.contains(parcelAgents.get(0))) {
            double radius = getRadius(parcelAgents.get(0));
            int x1 = (int) (entityPosition.getX() - radius);
            int y1 = (int) (entityPosition.getY() - radius);
            int x2 = (int) (entityPosition.getX() + radius);
            int y2 = (int) (entityPosition.getY() + radius);
            if (x2 > 0 && x1 < dim.width && y2 > 0 && y1 < dim.height) {
                VisioUtils.printTextWithBackgroud(canvas, parcelAgents.get(0).getId(),
                        new Point((int) (x1 + TEXT_MARGIN_BOTTOM), y1 + (y2 - y1) / 2), Color.BLACK,
                        TEXT_BACKGROUND_COLOR);
            }
        }
    }
}
