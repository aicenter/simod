/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.mapVisualization;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeEventGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.Graphs;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.AllNetworkNodes;
import cz.cvut.fel.aic.agentpolis.simulator.MapData;
import cz.cvut.fel.aic.agentpolis.simulator.SimulationProvider;
import cz.cvut.fel.aic.agentpolis.simulator.SimulationUtils;
import cz.cvut.fel.aic.agentpolis.simulator.creator.SimulationCreator;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioInitializer;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.alite.vis.VisManager;
import org.slf4j.LoggerFactory;

/**
 *
 * @author travnja5
 */
@Singleton
public class MapVisualizationCreator{
        private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SimulationCreator.class);
	
        private TypedSimulation simulation;
	private final AgentpolisConfig config;
	private final SimulationProvider simulationProvider;
	private final AllNetworkNodes allNetworkNodes;
	private final Graphs graphs;
	private final Provider<VisioInitializer> visioInitializerProvider;
	private final TimeEventGenerator timeEventGenerator;
	
	private final SimulationUtils simulationUtils;

	@Inject
	public MapVisualizationCreator(final AgentpolisConfig config, SimulationProvider simulationProvider,
							 AllNetworkNodes allNetworkNodes, Graphs graphs,
							 Provider<VisioInitializer> visioInitializerProvider,
							 TimeEventGenerator timeEventGenerator, SimulationUtils simulationUtils) {
		this.config = config;
		this.simulationProvider = simulationProvider;
		this.allNetworkNodes = allNetworkNodes;
		this.graphs = graphs;
		this.visioInitializerProvider = visioInitializerProvider;
		this.timeEventGenerator = timeEventGenerator;
		this.simulationUtils = simulationUtils;
                
	}

	public void prepareSimulation(final MapData osmDTO, final long seed) {
		LOGGER.debug("Using seed {}.", seed);

		initSimulation();

		LOGGER.info(">>> CREATING MAP");

		initEnvironment(osmDTO, seed);
	}

	public void prepareSimulation(final MapData osmDTO) {
		prepareSimulation(osmDTO, 0L);
	}

	public void startSimulation() {
		long simTimeInit = System.currentTimeMillis();

		initVisio();
                
                VisManager.setRecordingFilepath(config.pathToSaveRecordings);                        
                LOGGER.info("Map visualization initalized. ({} ms)", (System.currentTimeMillis() - simTimeInit));
//                long simulationStartTime = System.currentTimeMillis();
//                timeEventGenerator.start();
                simulation.run();
//                LOGGER.info("Simulation finished: ({} ms)", (System.currentTimeMillis() - simulationStartTime));

	}

	private void initSimulation() {
		simulation = new TypedSimulation(1);
//		simulation.setPrintouts(10000000);
		simulationProvider.setSimulation(simulation);
	}

	private void initEnvironment(MapData osmDTO, long seed) {
                LOGGER.info("Creating instance of environment...");
                allNetworkNodes.setAllNetworkNodes(osmDTO.nodesFromAllGraphs);
                graphs.setGraphs(osmDTO.graphByType);
                LOGGER.info("Done.");
	}

	private void initVisio() {
                LOGGER.info("Initializing Visio");
                visioInitializerProvider.get().initialize(simulation);
                simulation.setSimulationSpeed(1);
                LOGGER.info("Initialized Visio");
	}
}
