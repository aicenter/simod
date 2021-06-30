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
package cz.cvut.fel.aic.simod.traveltimecomputation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.MovingEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.TransportNetworks;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.shortestpaths.TNRAFDistanceQueryManagerAPI;
import java.math.BigInteger;
import org.slf4j.LoggerFactory;


/**
 * This class implements an API that allows us to call the Transit Node Routing with Arc Flags query algorithm present
 * in the C++ Shortest Paths library to answer queries.
 *
 * @author Michal Cvach
 */
@Singleton
public class TNRAFTravelTimeProvider extends TravelTimeProvider{
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TNRAFTravelTimeProvider.class);

    private final TripsUtil tripsUtil;

    private final SimodConfig config;

    private final Graph<SimulationNode, SimulationEdge> graph;

    private boolean closed = false;

    private int queryManagersCount = Runtime.getRuntime().availableProcessors();

    private int freeQueryManagers;

    private boolean[] queryManagersOccupied;
    private TNRAFDistanceQueryManagerAPI[] queryManagers;

    @Inject
    public TNRAFTravelTimeProvider(TimeProvider timeProvider, TripsUtil tripsUtil, TransportNetworks transportNetworks, SimodConfig config) {
        super(timeProvider);
        this.tripsUtil = tripsUtil;
        this.config = config;
        this.graph = transportNetworks.getGraph(EGraphType.HIGHWAY);

        // Note that you have to guarantee, that the path to the library is always included in the java.library.path.
        // If this doesn't hold, the library will not get found and this class won't function. You can set
        // java.library.path using the -Djava.library.path option. Alternatively, System.load() lets you load a library
        // using an absolute path instead of trying to find in in the java.library.path.
        System.loadLibrary("shortestPaths");
        this.freeQueryManagers = this.queryManagersCount;
        LOGGER.info("Initializing {} TNRAF query managers.", this.queryManagersCount);
        this.queryManagers = new TNRAFDistanceQueryManagerAPI[this.queryManagersCount];
        this.queryManagersOccupied = new boolean[this.queryManagersCount];
        for(int i = 0; i < this.queryManagersCount; i++) {
            this.queryManagers[i] = new TNRAFDistanceQueryManagerAPI();
            this.queryManagers[i].initializeTNRAF(config.shortestpaths.tnrafFilePath, config.shortestpaths.mappingFilePath);
            this.queryManagersOccupied[i] = false;
        }
    }

    @Override
    public long getTravelTime(MovingEntity entity, SimulationNode positionA, SimulationNode positionB) {
        int managerForQuery = 0;
        synchronized (this) {
            while(this.freeQueryManagers == 0) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            this.freeQueryManagers--;
            for(int i = 0; i < this.queryManagersCount; i++) {
                if(this.queryManagersOccupied[i] == false) {
                    managerForQuery = i;
                    this.queryManagersOccupied[i] = true;
                    break;
                }
            }
        }

        long result = this.queryManagers[managerForQuery].distanceQuery(BigInteger.valueOf(positionA.sourceId), BigInteger.valueOf(positionB.sourceId));

        synchronized (this) {
            freeQueryManagers++;
            this.queryManagersOccupied[managerForQuery] = false;
            this.notify();
        }

        return result;
    }

    public void close() {
        if(! closed) {
            for(int i = 0; i < this.queryManagersCount; i++) {
                this.queryManagers[i].clearStructures();
            }
            closed = true;
        }
    }

}
