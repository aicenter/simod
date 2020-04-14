package cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.Trip;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.MoveUtil;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.MovingEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.TransportNetworks;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.Node;
import cz.cvut.fel.aic.shortestpaths.CHDistanceQueryManagerAPI;
import cz.cvut.fel.aic.shortestpaths.TNRAFDistanceQueryManagerAPI;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;

/**
 *
 * @author Michal Cvach
 */
@Singleton
public class TNRAFTravelTimeProvider extends TravelTimeProvider{

    private final TripsUtil tripsUtil;

    private final Graph<SimulationNode, SimulationEdge> graph;

    private boolean closed = false;

    private int queryManagersCount = java.lang.Thread.activeCount();

    private int freeQueryManagers;

    private boolean[] queryManagersOccupied;
    private TNRAFDistanceQueryManagerAPI[] queryManagers;

    //private long callCount = 0;

    @Inject
    public TNRAFTravelTimeProvider(TimeProvider timeProvider, TripsUtil tripsUtil, TransportNetworks transportNetworks) {
        super(timeProvider);
        this.tripsUtil = tripsUtil;
        this.graph = transportNetworks.getGraph(EGraphType.HIGHWAY);

        System.loadLibrary("shortestPaths");
        this.freeQueryManagers = this.queryManagersCount;
        this.queryManagers = new TNRAFDistanceQueryManagerAPI[this.queryManagersCount];
        this.queryManagersOccupied = new boolean[this.queryManagersCount];
        for(int i = 0; i < this.queryManagersCount; i++) {
            this.queryManagers[i] = new TNRAFDistanceQueryManagerAPI();
            this.queryManagers[i].initializeTNRAF("/home/xenty/sum/2019/ContractionHierarchies/amod-to-agentpolis/data/shortestpathslib/Prague2000tnodes.tgaf", "/home/xenty/sum/2019/ContractionHierarchies/amod-to-agentpolis/data/shortestpathslib/PragueMapping.xeni");
            this.queryManagersOccupied[i] = false;
        }
        // Note that you have to guarantee, that the path to the library is always included in the java.library.path.
        // If this doesn't hold, the library will not get found and this class won't function. You can set
        // java.library.path using the -Djava.library.path option. Alternatively, System.load() lets you load a library
        // using an absolute path instead of trying to find in in the java.library.path.
        /*
        this.dqm = new TNRAFDistanceQueryManagerAPI("/home/xenty/sum/2019/ContractionHierarchies/amod-to-agentpolis/data/shortestpathslib/Prague2000tnodes.tgaf", "/home/xenty/sum/2019/ContractionHierarchies/amod-to-agentpolis/data/shortestpathslib/PragueMapping.xeni");
        // FIXME relative path should be probably used here instead. Maybe this should be included in the config as well?
        this.dqm.initializeTNRAF();*/
    }

    @Override
    public long getTravelTime(MovingEntity entity, SimulationNode positionA, SimulationNode positionB) {
        int managerForQuery = 0;
        synchronized (this) {
            while(this.freeQueryManagers == 0) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            this.freeQueryManagers--;
            for(int i = 0; i < this.queryManagersCount; i++) {
                if(this.queryManagersOccupied[i] == false) {
                    managerForQuery = i;
                    this.queryManagersOccupied[i] = true;
                    break;
                }
            }
        }

        long result = this.queryManagers[managerForQuery].distanceQuery(BigInteger.valueOf(positionA.sourceId), BigInteger.valueOf(positionB.sourceId));

        synchronized (this) {
            freeQueryManagers++;
            this.queryManagersOccupied[managerForQuery] = false;
            this.notify();
        }

        return result;
    }

    public void close() {
        if(! closed) {
            for(int i = 0; i < this.queryManagersCount; i++) {
                this.queryManagers[i].clearStructures();
            }
            closed = true;
        }
    }

}
