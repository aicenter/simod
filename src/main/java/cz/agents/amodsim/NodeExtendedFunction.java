package cz.agents.amodsim;

import com.vividsolutions.jts.geom.Coordinate;
import net.sf.javaml.core.kdtree.KDTree;

import java.util.Map;


public class NodeExtendedFunction {

	private final Map<Integer, Coordinate> projectedNodeCoordinats;
	private final KDTree<Integer> kdTreeForAllNodes;
	private WGS84Converter WGS84Converter;

	public NodeExtendedFunction(Map<Integer, Coordinate> projectedNodeCoordinats, KDTree<Integer> kdTreeForAllNodes,
								WGS84Converter WGS84Converter) {
		super();
		this.projectedNodeCoordinats = projectedNodeCoordinats;
		this.kdTreeForAllNodes = kdTreeForAllNodes;
		this.WGS84Converter = WGS84Converter;
	}

    // TODO: this is not "ByNodeId"
	public Integer getNearestNodeByNodeId(double longitude, double latitude) {
		Coordinate coordinate = WGS84Converter.convert(longitude, latitude);
		return kdTreeForAllNodes.nearest(new double[] { coordinate.x, coordinate.y });

	}

	public double computeDistanceBetweenNodes(int fromNodeId, int toNodeId) {
		Coordinate from = projectedNodeCoordinats.get(fromNodeId);
		Coordinate to = projectedNodeCoordinats.get(toNodeId);
		return from.distance(to);
	}

    protected Map<Integer, Coordinate> getProjectedNodeCoordinats() {
        return projectedNodeCoordinats;
    }

    protected KDTree<Integer> getKdTreeForAllNodes() {
        return kdTreeForAllNodes;
    }

    protected WGS84Converter getWGS84Converter() {
        return WGS84Converter;
    }
}
