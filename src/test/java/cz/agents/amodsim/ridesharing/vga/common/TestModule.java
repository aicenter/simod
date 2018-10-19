/* 
 * Copyright (C) 2017 Czech Technical University in Prague.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package cz.agents.amodsim.ridesharing.vga.common;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import cz.cvut.fel.aic.agentpolis.VisualTests;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.PhysicalVehicleDriveFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.StandardDriveFactory;
import cz.cvut.fel.aic.agentpolis.system.StandardAgentPolisModule;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioInitializer;
import cz.cvut.fel.aic.amodsim.OnDemandVehicleStationsCentral;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicleFactorySpec;
import cz.cvut.fel.aic.amodsim.ridesharing.DARPSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.EuclideanTravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.RidesharingStationsCentral;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.RidesharingOnDemandVehicleFactory;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.VehicleGroupAssignmentSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGARequest;
import java.io.File;

/**
 *
 * @author fido
 */
public class TestModule extends StandardAgentPolisModule{
	
	private final AmodsimConfig amodsimConfig;

    public TestModule(AmodsimConfig amodsimConfig, File localConfigFile) {
        super(amodsimConfig, localConfigFile, "agentpolis"); 
		this.amodsimConfig = amodsimConfig;
        agentpolisConfig.showVisio = VisualTests.SHOW_VISIO;
		amodsimConfig.amodsim.startTime = 0;
		amodsimConfig.tripsMultiplier = 1.0;
    }

	
	
	
	@Override
	protected void configureNext() {
		super.configureNext();
		bind(AmodsimConfig.class).toInstance(amodsimConfig);
		install(new FactoryModuleBuilder().implement(DemandAgent.class, DemandAgent.class)
            .build(DemandAgent.DemandAgentFactory.class));
		bind(PhysicalVehicleDriveFactory.class).to(StandardDriveFactory.class);
		bind(OnDemandVehicleFactorySpec.class).to(RidesharingOnDemandVehicleFactory.class);
		bind(OnDemandVehicleStationsCentral.class).to(RidesharingStationsCentral.class);
		bind(DARPSolver.class).to(VehicleGroupAssignmentSolver.class);
		bind(TravelTimeProvider.class).to(EuclideanTravelTimeProvider.class);
		install(new FactoryModuleBuilder().implement(VGARequest.class, VGARequest.class)
				.build(VGARequest.VGARequestFactory.class));
	}
    

    @Override
    protected void bindVisioInitializer() {
        bind(VisioInitializer.class).to(VGATestVisioInitializer.class);
    }
    
}
