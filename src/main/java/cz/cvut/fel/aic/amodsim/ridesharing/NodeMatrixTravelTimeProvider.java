package cz.cvut.fel.aic.amodsim.ridesharing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.Trip;
import cz.cvut.fel.aic.agentpolis.simmodel.MoveUtil;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.MovingEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.TransportNetworks;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.Node;
import java.util.HashMap;
import java.util.Iterator;
import me.tongfei.progressbar.ProgressBar;

/**
 *
 * @author praveale
 */
@Singleton
public class NodeMatrixTravelTimeProvider implements TravelTimeProvider{
    private final TripsUtil tripsUtil;
    private final Graph<SimulationNode, SimulationEdge> graph;
    private final long[][] nodeMatrix;
    private final HashMap<Integer, Integer> nodeIndexToId;
    private final AmodsimConfig config;


    @Inject
    public NodeMatrixTravelTimeProvider(TripsUtil tripsUtil, TransportNetworks transportNetworks, AmodsimConfig config) {
        this.config = config;
        this.tripsUtil = tripsUtil;
        this.graph = transportNetworks.getGraph(EGraphType.HIGHWAY);
        nodeMatrix = new long[graph.numberOfNodes()][graph.numberOfNodes()];
        nodeIndexToId = new HashMap<>();
        Iterator<SimulationNode> iteratorA = graph.getAllNodes().iterator();

        for (int i = 0; i < nodeMatrix.length; i++) {
            Iterator<SimulationNode> iteratorB = graph.getAllNodes().iterator();
            SimulationNode positionA = iteratorA.next();
            nodeIndexToId.put(positionA.id, i);

            for (int j = 0; j < nodeMatrix.length; j++) {

                SimulationNode positionB = iteratorB.next();

                //A* distance
                if(positionA == positionB){
                    nodeMatrix[i][j] = 0;
                } else {                      
                    Trip<SimulationNode> trip = tripsUtil.createTrip(positionA.id, positionB.id);
                    long totalDuration = 0;

                    Iterator<SimulationNode> nodeIterator = trip.getLocations().iterator();
                    Node fromNode = nodeIterator.next();
                    while (nodeIterator.hasNext()) {
                            Node toNode = nodeIterator.next();
                            SimulationEdge edge = graph.getEdge(fromNode, toNode);
                            totalDuration += MoveUtil.computeDuration(config.vehicleSpeedInMeters, edge.shape.getShapeLength());
                            fromNode = toNode;
                    }
                    nodeMatrix[i][j] = totalDuration;
                }
            }
        }
    }


    @Override
    public double getTravelTime(MovingEntity entity, SimulationNode positionA, SimulationNode positionB){
        int indexA, indexB; 
        long totalDuration = -1;
        
        if ((nodeIndexToId.get(positionA.id) == null) || (nodeIndexToId.get(positionB.id) == null)) {
            throw new RuntimeException("Cannot find requested node");
        } else {
            indexA = nodeIndexToId.get(positionA.id);
            indexB = nodeIndexToId.get(positionB.id);
            totalDuration = nodeMatrix[indexA][indexB];
        }
        
        return totalDuration;
    }

    @Override
    public double getTravelTime(SimulationNode positionA, SimulationNode positionB) {
        int indexA, indexB; 
        long totalDuration = -1;
        
        if ((nodeIndexToId.get(positionA.id) == null) || (nodeIndexToId.get(positionB.id) == null)) {
            throw new RuntimeException("Cannot find requested node");
        } else {
            indexA = nodeIndexToId.get(positionA.id);
            indexB = nodeIndexToId.get(positionB.id);
            totalDuration = nodeMatrix[indexA][indexB];
        }
        
        return totalDuration;
    }
}
