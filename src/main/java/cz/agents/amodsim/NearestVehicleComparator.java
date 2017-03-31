/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim;

import cz.agents.amodsim.entity.vehicle.OnDemandVehicle;
import cz.agents.agentpolis.simmodel.environment.model.NearestEntityComparator;
import cz.agents.agentpolis.simulator.visualization.visio.PositionUtil;
import javax.vecmath.Point2d;

/**
 *
 * @author fido
 */
public class NearestVehicleComparator extends NearestEntityComparator<OnDemandVehicle>{
    
    public NearestVehicleComparator(PositionUtil positionUtil, Point2d from) {
        super(positionUtil, from);
    }
    
    @Override
	public int compare(OnDemandVehicle e1, OnDemandVehicle e2) {
		return Double.compare(
                positionUtil.getCanvasPositionInterpolated(e1).distance(from), 
                positionUtil.getCanvasPositionInterpolated(e2).distance(from));
	}
}
