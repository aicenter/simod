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
package cz.cvut.fel.aic.simod.system;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import cz.cvut.fel.aic.simod.entity.agent.OnDemandVehicle;
import cz.cvut.fel.aic.simod.entity.agent.OnDemandVehicleFactorySpec;
import cz.cvut.fel.aic.simod.statistics.EdgesLoadByState;

/**
 *
 * @author fido
 */
public class TestModule extends AbstractModule{


	@Override
	protected void configure() {
		bind(EdgesLoadByState.class).to(OnDemandVehicleLoad.class);
		
		install(new FactoryModuleBuilder().implement(OnDemandVehicle.class, TestOnDemandVehicle.class)
				.build(OnDemandVehicleFactorySpec.class));
	}
	
	
}
