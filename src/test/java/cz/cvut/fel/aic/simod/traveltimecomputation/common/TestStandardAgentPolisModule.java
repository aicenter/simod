/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of the SiMoD project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.fel.aic.simod.traveltimecomputation.common;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.AStarShortestPathPlanner;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.EuclideanTraveltimeHeuristic;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.ShortestPathPlanner;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.Graphs;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.BikewayNetwork;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.HighwayNetwork;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.MetrowayNetwork;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.PedestrianNetwork;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.RailwayNetwork;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.TramwayNetwork;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.TransportNetworks;
import cz.cvut.fel.aic.agentpolis.simulator.SimulationProvider;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.DefaultVisioInitializer;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioInitializer;
import cz.cvut.fel.aic.agentpolis.system.AgentPolisMainModule;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.alite.simulation.Simulation;
import cz.cvut.fel.aic.geographtools.GraphSpec2D;
import cz.cvut.fel.aic.geographtools.util.Transformer;
import cz.cvut.fel.aic.geographtools.util.Utils2D;
import java.io.File;
import java.time.ZonedDateTime;
import javax.annotation.Nullable;
import ninja.fido.config.Configuration;
import ninja.fido.config.GeneratedConfig;
import org.jgrapht.alg.interfaces.AStarAdmissibleHeuristic;

/**
 *
 * @author matal
 */
public class TestStandardAgentPolisModule extends AbstractModule implements AgentPolisMainModule{
	
	protected final AgentpolisConfig agentpolisConfig;
	
	public AgentpolisConfig getAgentpolisConfig() {
		return agentpolisConfig;
	}
	
	public TestStandardAgentPolisModule() {
		this(null, null, null);
	}
	
	public TestStandardAgentPolisModule(GeneratedConfig generatedConfig, String keyinClient) {
		this(generatedConfig, null, keyinClient);
	}

	public TestStandardAgentPolisModule(GeneratedConfig generatedConfig, File clientLocalConfigFile, String keyinClient) {
		agentpolisConfig = new AgentpolisConfig();
		Configuration.load(agentpolisConfig, generatedConfig, clientLocalConfigFile, keyinClient);
	}

	@Override
	protected void configure() {
		
		bindConstant().annotatedWith(Names.named("mapSrid")).to(agentpolisConfig.srid);

		bind(Transformer.class).toInstance(new Transformer(agentpolisConfig.srid));

		bind(AgentpolisConfig.class).toInstance(agentpolisConfig);
		
		bind(TimeProvider.class).to(StandardTimeProvider.class);
		
		bind(ShortestPathPlanner.class).to(AStarShortestPathPlanner.class);
		
		bind(AStarAdmissibleHeuristic.class).to(EuclideanTraveltimeHeuristic.class);
		
		bindVisioInitializer();
		configureNext();
	}

	protected void configureNext() {
	}
	
	
	@Provides 
	@Singleton
	public GraphSpec2D getMapSpecification(HighwayNetwork highwayNetwork){
		return Utils2D.getGraphSpec(highwayNetwork.getNetwork());
	}
	
	@Provides 
	@Singleton
	public Simulation getSimulation(SimulationProvider simulationProvider){
		return simulationProvider.getSimulation();
	}
	
	@Provides 
	@Singleton
	public EventProcessor getEventProcessor(SimulationProvider simulationProvider){
		return simulationProvider.getSimulation();
	}
	
	@Provides 
	@Singleton
	public TypedSimulation getTypedSimulation(SimulationProvider simulationProvider){
		return simulationProvider.getSimulation();
	}
	
	@Provides
	@Singleton
	MetrowayNetwork provideMetrowayNetwork(Graphs graphs) {
		return new MetrowayNetwork(graphs.getGraphs().get(EGraphType.METROWAY));
	}

	@Provides
	@Singleton
	BikewayNetwork provideBikewayNetwork(Graphs graphs) {
		return new BikewayNetwork(graphs.getGraphs().get(EGraphType.BIKEWAY));
	}

	@Provides
	@Singleton
	TramwayNetwork provideTramwayNetwork(Graphs graphs) {
		return new TramwayNetwork(graphs.getGraphs().get(EGraphType.TRAMWAY));
	}

	@Provides
	@Singleton
	RailwayNetwork provideRailwayNetwork(Graphs graphs) {
		return new RailwayNetwork(graphs.getGraphs().get(EGraphType.RAILWAY));
	}

	@Provides
	@Singleton
	HighwayNetwork provideHighwayNetwork(Graphs graphs) {
		return new HighwayNetwork(graphs.getGraphs().get(EGraphType.HIGHWAY));
	}

	@Provides
	@Singleton
	PedestrianNetwork providePedestrianNetwork(Graphs graphs) {
		return new PedestrianNetwork(graphs.getGraphs().get(EGraphType.PEDESTRIAN));
	}

	@Provides
	@Singleton
	TransportNetworks provideTransportNetworks(Graphs graphs) {
		return new TransportNetworks(graphs.getGraphs());
	}

	@Provides
	@Singleton
	StandardTimeProvider provideTimeProvider(@Nullable EventProcessor eventProcessor) {
		return new StandardTimeProvider(eventProcessor, ZonedDateTime.now());
	}

	protected void bindVisioInitializer() {
		bind(VisioInitializer.class).to(DefaultVisioInitializer.class);
	}
}
