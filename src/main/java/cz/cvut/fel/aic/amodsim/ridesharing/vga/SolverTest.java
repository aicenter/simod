/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.vga;

import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

/**
 *
 * @author LocalAdmin
 */
public class SolverTest {
	
	static {
        System.loadLibrary("jniortools");
    }
	
	public static void main(String[] args) {
		
		MPSolver solver = new MPSolver("my solver", MPSolver.OptimizationProblemType.CBC_MIXED_INTEGER_PROGRAMMING);

		double infinity = MPSolver.infinity();
		// x1 and x2 are integer non-negative variables.
		MPVariable x1 = solver.makeIntVar(0.0, infinity, "x1");
		MPVariable x2 = solver.makeIntVar(0.0, infinity, "x2");

		// Minimize x1 + 2 * x2.
		MPObjective objective = solver.objective();
		objective.setCoefficient(x1, 1);
		objective.setCoefficient(x2, 2);

		// 2 * x2 + 3 * x1 >= 17.
		MPConstraint ct = solver.makeConstraint(17, infinity);
		ct.setCoefficient(x1, 3);
		ct.setCoefficient(x2, 2);
		solver.enableOutput();
		System.out.println("solving START");
		final MPSolver.ResultStatus resultStatus = solver.solve();
		System.out.println("solving END");

		// Check that the problem has an optimal solution.
		if (resultStatus != MPSolver.ResultStatus.OPTIMAL) {
		  System.err.println("The problem does not have an optimal solution!");
		  return;
		}

		// Verify that the solution satisfies all constraints (when using solvers
		// others than GLOP_LINEAR_PROGRAMMING, this is highly recommended!).
		if (!solver.verifySolution(/*tolerance=*/1e-7, /*logErrors=*/true)) {
		  System.err.println("The solution returned by the solver violated the"
			  + " problem constraints by at least 1e-7");
		  return;
		}

		System.out.println("Problem solved in " + solver.wallTime() + " milliseconds");

		// The objective value of the solution.
		System.out.println("Optimal objective value = " + solver.objective().value());

		// The value of each variable in the solution.
		System.out.println("x1 = " + x1.solutionValue());
		System.out.println("x2 = " + x2.solutionValue());

		System.out.println("Advanced usage:");
		System.out.println("Problem solved in " + solver.nodes() + " branch-and-bound nodes");
	}
}
