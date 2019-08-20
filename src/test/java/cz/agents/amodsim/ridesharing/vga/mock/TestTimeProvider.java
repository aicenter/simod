/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.ridesharing.vga.mock;

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
