package cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic;

import com.google.inject.Provider;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.simod.StationsDispatcher;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.ridesharing.DARPSolver;
import cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.InsertionHeuristicSolver;
import cz.cvut.fel.aic.simod.statistics.EdgesLoadByState;
import cz.cvut.fel.aic.simod.statistics.Statistics;
import cz.cvut.fel.aic.simod.storage.OnDemandVehicleStorage;

import java.io.IOException;

public class PFStatistics extends Statistics {

	private final DARPSolverPFShared dARPSolverPF;

	public PFStatistics(TypedSimulation eventProcessor,
						Provider<EdgesLoadByState> allEdgesLoadProvider,
						OnDemandVehicleStorage onDemandVehicleStorage,
						StationsDispatcher onDemandVehicleStationsCentral,
						SimodConfig config,
						DARPSolverPFShared dARPSolverPF) throws IOException {
//		InsertionHeuristicSolver insertionHeuristicSolver = new InsertionHeuristicSolver();

		super(eventProcessor,
				allEdgesLoadProvider,
				onDemandVehicleStorage,
				onDemandVehicleStationsCentral,
				config,
				null);
		this.dARPSolverPF = dARPSolverPF;
	}
}
