package cz.cvut.fel.aic.simod;

import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.Trip;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.Activity;
import cz.cvut.fel.aic.agentpolis.simmodel.ActivityInitializer;
import cz.cvut.fel.aic.agentpolis.simmodel.Agent;
import cz.cvut.fel.aic.agentpolis.simmodel.TimeConsumingActivity;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.VehicleMoveActivityFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.WaitActivityFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.agent.Driver;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.Vehicle;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.TransportNetworks;
import cz.cvut.fel.aic.agentpolis.simmodel.eventType.DriveEvent;
import cz.cvut.fel.aic.agentpolis.simmodel.eventType.Transit;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.geographtools.Graph;

public class WaitTransfer<A extends Agent> extends TimeConsumingActivity<A> {

    private final long waitTime;

    private final WaitActivityFactory waitActivityFactory;

//    private final Trip<SimulationNode> trip;
//
//    private final VehicleMoveActivityFactory moveActivityFactory;
//
//    private final Vehicle vehicle;
//
//    private final Graph<SimulationNode, SimulationEdge> graph;
//
//    private final EventProcessor eventProcessor;
//
//    private final StandardTimeProvider timeProvider;


//    private SimulationNode from;
//
//    private SimulationNode to;

    public WaitTransfer(ActivityInitializer activityInitializer, A agent, long waitTime, WaitActivityFactory waitActivityFactory
//                        , VehicleMoveActivityFactory moveActivityFactory,
//                        TypedSimulation eventProcessor, StandardTimeProvider timeProvider, Trip<SimulationNode> trip, Vehicle vehicle, TransportNetworks transportNetworks
    ) {
        super(activityInitializer, agent);
        this.waitTime = waitTime;
        this.waitActivityFactory = waitActivityFactory;
//        this.moveActivityFactory = moveActivityFactory;
//        this.timeProvider = timeProvider;
//        this.eventProcessor = eventProcessor;
//        this.trip = trip;
//        this.vehicle = vehicle;
//        graph = transportNetworks.getGraph(EGraphType.HIGHWAY);

    }

    @Override
    protected long performPreDelayActions() {
        return waitTime;
    }

    @Override
    protected void performAction() {
        waitTransfer();

//        finish();
    }

//    @Override
//    protected void onChildActivityFinish(Activity activity) {
////        if (trip.isEmpty()) {
////            vehicle.setLastFromPosition(from);
////            finish();
////        } else {
//            from = to;
//            move();
////        }
//    }

//    private void move() {
//
//    }

    private void waitTransfer() {
        runChildActivity(waitActivityFactory.create(agent, waitTime));
    }


}