package cz.agents.amodsim.io;

import cz.agents.agentpolis.utils.nearestelement.NearestElementUtil;
import cz.agents.multimodalstructures.nodes.RoadNode;

/**
 *
 * @author F-I-D-O
 */
public class RoadNodeArrayConstructor implements NearestElementUtil.SerializableIntFunction<RoadNode[]>{

	@Override
	public RoadNode[] apply(int value) {
		return new RoadNode[value];
	}
	
}
