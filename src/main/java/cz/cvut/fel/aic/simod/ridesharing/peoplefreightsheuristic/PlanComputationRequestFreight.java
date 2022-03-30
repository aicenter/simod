package cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.DemandAgent;
import cz.cvut.fel.aic.simod.ridesharing.model.DefaultPlanComputationRequest;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;

public class PlanComputationRequestFreight extends DefaultPlanComputationRequest
{
    private final int weight;

    @Inject
    public PlanComputationRequestFreight(TravelTimeProvider travelTimeProvider, @Assisted int id,
                                        SimodConfig SimodConfig, @Assisted("origin") SimulationNode origin,
                                        @Assisted("destination") SimulationNode destination, @Assisted DemandAgent demandAgent, int weight)
    {
        super(travelTimeProvider, id, SimodConfig, origin, destination, demandAgent);
        this.weight = weight;
    }

    public int getWeight()
    {
        return weight;
    }
}