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
package cz.cvut.fel.aic.simod.system;

import org.junit.Test;

/**
 *
 * @author fido
 */
public class SimulationMinuteTest {
	
	private static final int ONE_MINUTE_IN_MILIS = 60000;
	
	// we expect trips to be no longer then 40 minutes
	private static final int TRIP_MAX_DURATION = 2400000;
	
	private static final int START_TIME_MILIS = 25200000;
	
	@Test
	public void run(){
		FullTest.runFullTest(ONE_MINUTE_IN_MILIS, START_TIME_MILIS, TRIP_MAX_DURATION);
	}
}
