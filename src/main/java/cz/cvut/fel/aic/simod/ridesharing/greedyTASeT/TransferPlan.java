package cz.cvut.fel.aic.simod.ridesharing.greedyTASeT;

import cz.cvut.fel.aic.simod.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanAction;
import org.jgrapht.alg.util.Pair;

import java.util.List;

public class TransferPlan {

    public final long trasferTime;
    public final long delay;
    public final Pair<List<List<PlanAction>>, List<RideSharingOnDemandVehicle>> pair;

    public TransferPlan(long trasferTime, long delay, Pair<List<List<PlanAction>>, List<RideSharingOnDemandVehicle>> pair) {
        this.trasferTime = trasferTime;
        this.delay = delay;
        this.pair = pair;
    }

    public int compareByDelay(TransferPlan o2) {
        return Long.compare(this.delay, o2.delay);
    }

    public int compareByTransferTime(TransferPlan o2) {
        return Long.compare(this.trasferTime, o2.trasferTime);
    }
}


