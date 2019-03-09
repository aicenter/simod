package cz.cvut.fel.aic.amodsim.ridesharing.insertionheuristic;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanAction;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author F.I.D.O.
 */
public class DriverPlan implements Iterable<PlanAction>{
	public final List<PlanAction> plan;

	public final long totalTime;
	
	public final double cost;

	
	
	
	public DriverPlan(List<PlanAction> plan, long totalTime, double cost) {
		this.plan = plan;
		this.totalTime = totalTime;
		this.cost = cost;
	}

	@Override
	public Iterator<PlanAction> iterator() {
		return plan.iterator();
	}
	
	public int getLength(){
		return plan.size();
	}
	
	public void updateCurrentPosition(SimulationNode position){
		plan.set(0, new PlanActionCurrentPosition(position));
	}
	
	public PlanAction getNextTask(){
		return plan.get(1);
	}
	
	public void taskCompleted(){
		plan.remove(1);
	}
}
