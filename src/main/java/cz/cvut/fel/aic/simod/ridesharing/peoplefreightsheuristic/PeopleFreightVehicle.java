package cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic;

import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.PhysicalVehicleDriveFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.EntityStorage;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioPositionUtil;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.simod.StationsDispatcher;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.simod.storage.PeopleFreightVehicleStorage;
import cz.cvut.fel.aic.simod.storage.PhysicalTransportVehicleStorage;

public class PeopleFreightVehicle extends RideSharingOnDemandVehicle
{
    private boolean passengerOnboard;

    private final int maxParcelsCapacity;

    private int currentParcelsWeight;



    public PeopleFreightVehicle(
            PeopleFreightVehicleStorage vehicleStorage,
            TripsUtil tripsUtil,
            StationsDispatcher onDemandVehicleStationsCentral,
            PhysicalVehicleDriveFactory driveActivityFactory,
            VisioPositionUtil positionUtil,
            IdGenerator tripIdGenerator,
            EventProcessor eventProcessor,
            StandardTimeProvider timeProvider,
            IdGenerator rebalancingIdGenerator,
            SimodConfig config,
            IdGenerator idGenerator,
            AgentpolisConfig agentpolisConfig,
            String vehicleId,
            SimulationNode startPosition,
            int maxParcelsCapacity)
    {
        super(vehicleStorage, tripsUtil,
                onDemandVehicleStationsCentral, driveActivityFactory, positionUtil,
                tripIdGenerator, eventProcessor, timeProvider,
                rebalancingIdGenerator, config, idGenerator,
                agentpolisConfig, vehicleId, startPosition);
        this.maxParcelsCapacity = maxParcelsCapacity;
        this.currentParcelsWeight = 0;
        this.passengerOnboard = false;
    }

    public int getMaxParcelsCapacity()
    {
        return maxParcelsCapacity;
    }

    public int getCurrentParcelsWeight()
    {
        return currentParcelsWeight;
    }

    public boolean isPassengerOnboard()
    {
        return passengerOnboard;
    }

    public void setPassengerOnboard(boolean passengerOnboard)
    {
        this.passengerOnboard = passengerOnboard;
    }

    public void setCurrentParcelsWeight(int curWeight)
    {
        this.currentParcelsWeight = curWeight;
    }

    // potrebuji urcit vzdalenost a min/max rychlost mezi dvema uzly = TRAVELtimeProvider


//    public bool findSchedule()
//    {
    // for i in range(Trajectory.size()):
        // d_i_i+1 = shortest distance from Trajectory[i] to Trajectory[i+1]
        // speed_min, speed_max = minimum, maximum speed to travel from Trajectory[i] to Trajectory[i+1]
        // earlyTime = e[Trajectory[i]] + d_i_i+1 / speed_max
        // lateTime = l[Trajectory[i]] + d_i_i+1 / speed_min + max_wait_time_at_i

        // if lateTime <= e[Trajectory[i+1]]:
            // p_nearest = najdi nejblizsi parkoviste (to ktere ma nejmensi vzdalenost od Trajectory[i] a Trajectory[i+1])
            // if p_nearest is found:
                // insert p_nearest between Trajectory[i] and Trajectory[i+1]
                // findSchedule() for new trajectory of taxi k

        // if lateTime<= e[Trajectory[i+1]] or earlyTime >= l[Trajectory[i+1]]:
            // Terminate - not feasible
        // e[Trajectory[i+1]] = math.max(earlyTime, e[Trajectory[i+1]])
        // l[Trajectory[i+1]] = math.max(lateTime, l[Trajectory[i+1]])

//    }



}
