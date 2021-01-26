/* 
 * Copyright (C) 2019 Czech Technical University in Prague.
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
package cz.cvut.fel.aic.amodsim.system;

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
