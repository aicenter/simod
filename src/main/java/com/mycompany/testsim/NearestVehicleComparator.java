/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim;

import com.mycompany.testsim.entity.OnDemandVehicle;
import cz.agents.agentpolis.simmodel.environment.model.NearestEntityComparator;
import cz.agents.agentpolis.simulator.visualization.visio.entity.VehiclePositionUtil;
import javax.vecmath.Point2d;

/**
 *
 * @author fido
 */
public class NearestVehicleComparator extends NearestEntityComparator<OnDemandVehicle,VehiclePositionUtil>{
    
    public NearestVehicleComparator(VehiclePositionUtil entityPositionUtil, Point2d from) {
        super(entityPositionUtil, from);
    }
    
    @Override
	public int compare(OnDemandVehicle e1, OnDemandVehicle e2) {
		return Double.compare(
                entityPositionUtil.getVehicleCanvasPositionInterpolated(e1.getVehicle(), e1).distance(from), 
                entityPositionUtil.getVehicleCanvasPositionInterpolated(e2.getVehicle(), e2).distance(from));
	}
}
