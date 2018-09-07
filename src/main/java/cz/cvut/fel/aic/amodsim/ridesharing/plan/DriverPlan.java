package cz.cvut.fel.aic.amodsim.ridesharing.plan;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *
 * @author F.I.D.O.
 */
public class DriverPlan implements Iterable<DriverPlanTask>{
	public final List<DriverPlanTask> plan;
	
	private double plannedTraveltime;
	
	protected Set<DemandAgent> demands;

	public final long totalTime;

//	public double getPlannedTraveltime() {
//		return plannedTraveltime;
//	}
//
//	public void setPlannedTraveltime(double plannedTraveltime) {
//		this.plannedTraveltime = plannedTraveltime;
//	}

	public DriverPlan(List<DriverPlanTask> plan, long totalTime) {
		this.plan = plan;
		this.totalTime = totalTime;
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
		DriverPlanTask removedTask = plan.remove(1);
		if(removedTask.getTaskType() == DriverPlanTaskType.DROPOFF && demands != null){
			demands.remove(removedTask.demandAgent);
		}
	}
	
	public Set<DemandAgent> getDemands(){
		if(demands == null){
			demands = new HashSet<>();
			for(DriverPlanTask task: plan){
				if(task.demandAgent != null){
					demands.add(task.demandAgent);
				}
			}
		}
		return demands;
	}
}
