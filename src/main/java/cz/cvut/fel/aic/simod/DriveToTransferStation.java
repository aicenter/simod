package cz.cvut.fel.aic.simod;

import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.Trip;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.Activity;
import cz.cvut.fel.aic.agentpolis.simmodel.ActivityInitializer;
import cz.cvut.fel.aic.agentpolis.simmodel.Agent;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.PhysicalVehicleDrive;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.VehicleMoveActivityFactory;
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
import cz.cvut.fel.aic.simod.ridesharing.RideSharingOnDemandVehicle;


/**
 * @param <A>
 * @author fido
 */
public class DriveToTransferStation<A extends Agent & Driver> extends PhysicalVehicleDrive<A> {

    private final Vehicle vehicle;

    private final Trip<SimulationNode> trip;

    private final VehicleMoveActivityFactory moveActivityFactory;

    private final Graph<SimulationNode, SimulationEdge> graph;

    private final EventProcessor eventProcessor;

    private final StandardTimeProvider timeProvider;


    private SimulationNode from;

    private SimulationNode to;




    public DriveToTransferStation(ActivityInitializer activityInitializer, TransportNetworks transportNetworks,
                 VehicleMoveActivityFactory moveActivityFactory, TypedSimulation eventProcessor,
                 StandardTimeProvider timeProvider,
                 A agent, Vehicle vehicle, Trip<SimulationNode> trip) {
        super(activityInitializer, agent);
        this.vehicle = vehicle;
        this.trip = trip;
        this.moveActivityFactory = moveActivityFactory;
        this.eventProcessor = eventProcessor;
        this.timeProvider = timeProvider;
        graph = transportNetworks.getGraph(EGraphType.HIGHWAY);
    }

    @Override
    protected void performAction() {
        agent.startDriving(vehicle);
        from = trip.removeFirstLocation();
        move();
    }

    @Override
    protected void onChildActivityFinish(Activity activity) {
        if (trip.isEmpty() || stoped) {
            agent.endDriving();
            // todo: nastavit tripalreadyplanned na false
            if (agent instanceof RideSharingOnDemandVehicle) {
                ((RideSharingOnDemandVehicle) agent).tripAlreadyPlanned = false;
            }
            vehicle.setLastFromPosition(from);
            finish();
        } else {
            from = to;
            move();
        }
    }


    private void move() {
        to = trip.removeFirstLocation();
        SimulationEdge edge = graph.getEdge(from, to);

        runChildActivity(moveActivityFactory.create(agent, edge, from, to));
        triggerVehicleEnteredEdgeEvent();
    }

    private void triggerVehicleEnteredEdgeEvent() {
        SimulationEdge edge = graph.getEdge(from, to);
        Transit transit = new Transit(timeProvider.getCurrentSimTime(), edge.getStaticId(),trip.getTripId(), agent);
        eventProcessor.addEvent(DriveEvent.VEHICLE_ENTERED_EDGE, null, null, transit);
    }

    public Trip<SimulationNode> getTrip() {
        return trip;
    }
}
