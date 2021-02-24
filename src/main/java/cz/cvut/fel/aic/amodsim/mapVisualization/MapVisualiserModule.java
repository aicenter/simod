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
package cz.cvut.fel.aic.amodsim.mapVisualization;

import com.google.common.collect.Sets;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.GeojsonMapInitializer;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.MapInitializer;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioInitializer;
import cz.cvut.fel.aic.amodsim.MainModule;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicleFactory;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicleFactorySpec;
import cz.cvut.fel.aic.amodsim.visio.MapVisualizerVisioInitializer;
import cz.cvut.fel.aic.geographtools.TransportMode;
import java.io.File;
import java.util.Set;

/**
 *
 * @author F.I.D.O.
 */
public class MapVisualiserModule  extends MainModule{

	private final AmodsimConfig amodsimConfig;
	
	public MapVisualiserModule(AmodsimConfig amodsimConfig, File localConfigFile) {
		super(amodsimConfig, localConfigFile);
		this.amodsimConfig = amodsimConfig;
        this.amodsimConfig.startTime = 0;
	}
	
	@Override
	protected void bindVisioInitializer() {
		bind(VisioInitializer.class).to(MapVisualizerVisioInitializer.class);
	}
        
    @Override
	protected void configureNext() {
        bind(new TypeLiteral<Set<TransportMode>>(){}).toInstance(Sets.immutableEnumSet(TransportMode.CAR));
		bind(AmodsimConfig.class).toInstance(amodsimConfig);
		bind(MapInitializer.class).to(GeojsonMapInitializer.class);   
		bind(OnDemandVehicleFactorySpec.class).to(OnDemandVehicleFactory.class);
		install(new FactoryModuleBuilder().implement(OnDemandVehicleStation.class, OnDemandVehicleStation.class)
				.build(OnDemandVehicleStation.OnDemandVehicleStationFactory.class));
    }
}
