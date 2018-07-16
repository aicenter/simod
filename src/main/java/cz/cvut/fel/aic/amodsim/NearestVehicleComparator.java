/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim;

import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.NearestEntityComparator;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;

import javax.vecmath.Point2d;

/**
 * @author fido
 */
public class NearestVehicleComparator extends NearestEntityComparator<OnDemandVehicle> {

    private PositionUtil positionUtil;

    public NearestVehicleComparator(PositionUtil positionUtil, Point2d from) {
        super(positionUtil, from);
    }

    @Override
    public int compare(OnDemandVehicle e1, OnDemandVehicle e2) {
        return Double.compare(
                positionUtil.getCanvasPositionInterpolated(e1, EGraphType.HIGHWAY).distance(from),
                positionUtil.getCanvasPositionInterpolated(e2, EGraphType.HIGHWAY).distance(from));
    }
}
