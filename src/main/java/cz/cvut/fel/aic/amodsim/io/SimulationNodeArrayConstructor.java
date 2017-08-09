package cz.cvut.fel.aic.amodsim.io;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.geographtools.util.NearestElementUtil;

/**
 *
 * @author F-I-D-O
 */
public class SimulationNodeArrayConstructor implements NearestElementUtil.SerializableIntFunction<SimulationNode[]>{

	@Override
	public SimulationNode[] apply(int value) {
		return new SimulationNode[value];
	}
	
}
