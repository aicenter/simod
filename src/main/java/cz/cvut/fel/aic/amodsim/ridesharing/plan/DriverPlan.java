package cz.cvut.fel.aic.amodsim.ridesharing.plan;

import java.util.Iterator;
import java.util.List;

/**
 *
 * @author F.I.D.O.
 */
public class DriverPlan implements Iterable<DriverPlanTask>{
	private final List<DriverPlanTask> plan;
	
	private final double plannedTraveltime;

	public double getPlannedTraveltime() {
		return plannedTraveltime;
	}
	
	

	public DriverPlan(List<DriverPlanTask> plan, int plannedTraveltime) {
		this.plan = plan;
		this.plannedTraveltime = plannedTraveltime;
	}

	@Override
	public Iterator<DriverPlanTask> iterator() {
		return plan.iterator();
	}
	
	public int getLength(){
		return plan.size();
	}
	
	
}
