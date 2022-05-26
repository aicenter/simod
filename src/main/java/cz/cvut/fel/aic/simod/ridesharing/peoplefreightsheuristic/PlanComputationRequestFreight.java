package cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;

public class PlanComputationRequestFreight extends DefaultPFPlanCompRequest
{
    private final int weight;

    @Inject
    public PlanComputationRequestFreight(TravelTimeProvider travelTimeProvider, @Assisted("id") int id,
                                        SimodConfig SimodConfig, @Assisted("origin") SimulationNode origin,
                                        @Assisted("destination") SimulationNode destination, @Assisted TransportableDemandEntity demandEntity,
                                         @Assisted("weight") int weight)
    {
        super(travelTimeProvider, id, SimodConfig, origin, destination, demandEntity);
        this.weight = weight;
    }

    public int getWeight()
    {
        return weight;
    }

    public interface PlanComputationRequestFreightFactory {
        public PlanComputationRequestFreight create(@Assisted("id") int id, @Assisted("origin") SimulationNode origin,
                                                    @Assisted("destination") SimulationNode destination,
                                                    @Assisted TransportableDemandEntity demandEntity, @Assisted("weight") int weight);
    }
}