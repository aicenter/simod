package cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.DemandAgent;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanActionDropoff;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanActionPickup;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;
import org.apache.commons.lang.NotImplementedException;

import java.util.Random;


public class DefaultPFPlanCompRequest implements PFPlanCompRequest {

	public final int id;

	/**
	 * Request origin time in seconds.
	 */
	private final int originTime;

	private final TransportableEntity_2 demandEntity;

	private final PlanActionPickup pickUpAction;

	private final PlanActionDropoff dropOffAction;

	/**
	 * Min travel time in seconds
	 */
	private final int minTravelTime;

	private boolean onboard;

	private int hash;


	@Inject
	public DefaultPFPlanCompRequest(TravelTimeProvider travelTimeProvider, @Assisted int id,
									SimodConfig SimodConfig, @Assisted("origin") SimulationNode origin,
									@Assisted("destination") SimulationNode destination, @Assisted TransportableEntity_2 demandEntity){
		this.id = id;

		hash = 0;

		originTime = (int) Math.round(demandEntity.getDemandTime() / 1000.0);
		minTravelTime = (int) Math.round(
				travelTimeProvider.getExpectedTravelTime(origin, destination) / 1000.0);

		int maxProlongation;
		if(SimodConfig.ridesharing.discomfortConstraint.equals("absolute")){
			maxProlongation = SimodConfig.ridesharing.maxProlongationInSeconds;
		}
		else{
			maxProlongation = (int) Math.round(
					SimodConfig.ridesharing.maximumRelativeDiscomfort * minTravelTime);
		}

		int maxPickUpTime = originTime + maxProlongation;
		int maxDropOffTime = originTime + minTravelTime + maxProlongation;

		this.demandEntity = demandEntity;
		onboard = false;

		pickUpAction = new PlanActionPickup(this, origin, maxPickUpTime);
		dropOffAction = new PlanActionDropoff(this, destination, maxDropOffTime);
	}


	@Override
	public int getId() {
		return id;
	}

	@Override
	public PlanActionPickup getPickUpAction() {
		return pickUpAction;
	}

	@Override
	public PlanActionDropoff getDropOffAction() {
		return dropOffAction;
	}

	@Override
	public boolean isOnboard() {
		return onboard;
	}

	public void setOnboard(boolean onboard) {
		this.onboard = onboard;
	}

	@Override
	public DemandAgent getDemandAgent() {
		throw new NotImplementedException("DemandAgent not implemented in default PF plan request.");
	}

	@Override
	public TransportableEntity_2 getDemandEntity() {
		return demandEntity;
	}

	@Override
	public int getOriginTime() {
		return originTime;
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}

	@Override
	public int hashCode() {
		if(hash == 0){
			int p = 1_200_007;
			Random rand = new Random();
			int a = rand.nextInt(p) + 1;
			int b = rand.nextInt(p);
			hash = (int) (((long) a * demandEntity.getSimpleId() + b) % p) % 1_200_000 ;
		}
		return hash;
	}

	@Override
	public String toString() {
		return String.format("%s - from: %s to: %s", demandEntity, getFrom(), getTo());
	}

	@Override
	public int getMaxPickupTime() {
		return pickUpAction.getMaxTime();
	}

	@Override
	public int getMaxDropoffTime() {
		return dropOffAction.getMaxTime();
	}

	@Override
	public int getMinTravelTime() {
		return minTravelTime;
	}

	@Override
	public SimulationNode getFrom() {
		return pickUpAction.getPosition();
	}

	@Override
	public SimulationNode getTo() {
		return dropOffAction.getPosition();
	}



	public interface DefaultPFPlanComputationRequestFactory {
		public DefaultPFPlanCompRequest create(int id, @Assisted("origin") SimulationNode origin,
										@Assisted("destination") SimulationNode destination, TransportableEntity_2 demandEntity);
	}
}
