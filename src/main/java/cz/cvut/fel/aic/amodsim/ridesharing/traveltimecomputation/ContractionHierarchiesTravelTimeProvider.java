package cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.MoveUtil;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.MovingEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.utils.PositionUtil;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.contractionhierarchies.IntegerDistanceQueryManagerWithMappingAPI;

import java.math.BigInteger;

/**
 *
 * @author Michal Cvach
 */
@Singleton
public class ContractionHierarchiesTravelTimeProvider implements TravelTimeProvider{

    private final PositionUtil positionUtil;

    private final AmodsimConfig config;

    //private final double travelSpeedEstimatePerSecond;

    private long callCount = 0;

    public long getCallCount() {
        return callCount;
    }

    private IntegerDistanceQueryManagerWithMappingAPI dqmm;

    private boolean closed = false;



    @Inject
    public ContractionHierarchiesTravelTimeProvider(PositionUtil positionUtil, AmodsimConfig config) {
        // Note that you have to guarantee, that the path to the library is always included in the java.library.path.
        // If this doesn't hold, the library will not get found and this class won't function. You can set
        // java.library.path using the -Djava.library.path option. Alternatively, System.load() lets you load a library
        // using an absolute path instead of trying to find in in the java.library.path.
        System.loadLibrary("contractionHierarchies");

        this.positionUtil = positionUtil;
        this.config = config;
        //travelSpeedEstimatePerSecond = config.ridesharing.maxDirectSpeedEstimationKmh / 3.6;
        this.dqmm = new IntegerDistanceQueryManagerWithMappingAPI();
        // FIXME relative path should be probably used here instead. Maybe this should be included in the config as well?
        this.dqmm.initializeCH("/home/xenty/sum/2019/amodsim/amod-to-agentpolis/data/ch/Prague_map_int_prec10000.ch", "/home/xenty/sum/2019/amodsim/amod-to-agentpolis/data/ch/Prague_int_graph.xeni");
    }


    @Override
    public double getTravelTime(MovingEntity entity, SimulationNode positionA, SimulationNode positionB) {
        callCount++;
        return dqmm.distanceQuery(BigInteger.valueOf(positionA.sourceId), BigInteger.valueOf(positionB.sourceId));
    }

    @Override
    public double getExpectedTravelTime(SimulationNode positionA, SimulationNode positionB) {
        return getTravelTime(null, positionA, positionB);
    }

    public void close() {
        dqmm.clearStructures();
        closed = true;
    }

}
