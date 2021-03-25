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
package cz.cvut.fel.aic.simod.visual.ridesharing.vga.mock;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;

/**
 *
 * @author david
 */
@Singleton
public class TestTimeProvider implements TimeProvider{
	
	private long simulationTime;

	@Inject
	public TestTimeProvider() {
		this.simulationTime = 0;
	}
	
	

	@Override
	public long getCurrentSimTime() {
		return simulationTime;
	}

	public void setSimulationTime(long simulationTime) {
		this.simulationTime = simulationTime;
	}
	
	
	
}
