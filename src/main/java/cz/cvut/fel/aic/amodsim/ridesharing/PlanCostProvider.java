package cz.cvut.fel.aic.amodsim.ridesharing;

/**
 *
 * @author F.I.D.O.
 */
public interface PlanCostProvider {
	public int calculatePlanCost(int planDiscomfort, int planDuration);
}
