package cz.cvut.fel.aic.amodsim.ridesharing.plan;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author F.I.D.O.
 */
public class DriverPlan implements Iterable<DriverPlanTask>{
	private final List<DriverPlanTask> plan;
	
	private double plannedTraveltime;

	public double getPlannedTraveltime() {
		return plannedTraveltime;
	}

	public void setPlannedTraveltime(double plannedTraveltime) {
		this.plannedTraveltime = plannedTraveltime;
	}
	
	
	
	

	public DriverPlan(List<DriverPlanTask> plan) {
		this.plan = plan;
	}

	@Override
	public Iterator<DriverPlanTask> iterator() {
		return plan.iterator();
	}
	
	public int getLength(){
		return plan.size();
	}
	
	public void updateCurrentPosition(SimulationNode position){
		plan.set(0, new DriverPlanTask(DriverPlanTaskType.CURRENT_POSITION, null, position));
	}
	
	public DriverPlanTask getNextTask(){
		return plan.get(1);
	}
	
	public void taskCompleted(){
		plan.remove(1);
	}
	
}
