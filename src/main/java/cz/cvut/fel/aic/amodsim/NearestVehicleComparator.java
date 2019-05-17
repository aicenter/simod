/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim;

import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.EntityLayer;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.NearestEntityComparator;
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
