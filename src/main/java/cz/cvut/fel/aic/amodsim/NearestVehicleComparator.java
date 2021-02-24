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
package cz.cvut.fel.aic.amodsim;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioPositionUtil;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import java.util.Comparator;
import javax.vecmath.Point2d;

/**
 * @author fido
 */
public class NearestVehicleComparator implements Comparator<OnDemandVehicle> {

	private final VisioPositionUtil positionUtil;
	
	private final Point2d from;

	public NearestVehicleComparator(VisioPositionUtil positionUtil, Point2d from) {
		this.positionUtil = positionUtil;
		this.from = from;
	}

	@Override
	public int compare(OnDemandVehicle e1, OnDemandVehicle e2) {
		return Double.compare(
				positionUtil.getCanvasPositionInterpolated(e1, EGraphType.HIGHWAY).distance(from),
				positionUtil.getCanvasPositionInterpolated(e2, EGraphType.HIGHWAY).distance(from));
	}
}
