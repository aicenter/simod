/* 
 * Copyright (C) 2019 Czech Technical University in Prague.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package cz.cvut.fel.aic.amodsim.ridesharing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.PhysicalVehicleDriveFactory;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.NearestElementUtils;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioPositionUtil;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.amodsim.StationsDispatcher;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.deliveryPackage.DeliveryPackage;
import cz.cvut.fel.aic.amodsim.entity.deliveryPackage.DeliveryPackageLoad;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicleFactory;
import cz.cvut.fel.aic.amodsim.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.amodsim.storage.PhysicalTransportVehicleStorage;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import org.slf4j.LoggerFactory;

/**
 *
 * @author F.I.D.O.
 */
@Singleton
public class RidesharingOnDemandVehicleFactory extends OnDemandVehicleFactory {

        private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RidesharingOnDemandVehicleFactory.class);

        private ArrayList<SimulationNode> packageDestinations;

        private final NearestElementUtils nearestElementUtils;

        private Random randomGenerator;

        @Inject
        public RidesharingOnDemandVehicleFactory(PhysicalTransportVehicleStorage vehicleStorage, TripsUtil tripsUtil,
                StationsDispatcher onDemandVehicleStationsCentral,
                PhysicalVehicleDriveFactory driveActivityFactory, VisioPositionUtil positionUtil,
                EventProcessor eventProcessor, StandardTimeProvider timeProvider, IdGenerator rebalancingIdGenerator,
                AmodsimConfig config, NearestElementUtils nearestElementUtils) {
                super(vehicleStorage, tripsUtil, onDemandVehicleStationsCentral, driveActivityFactory, positionUtil,
                        eventProcessor, timeProvider, rebalancingIdGenerator, config);
                this.nearestElementUtils = nearestElementUtils;
                this.randomGenerator = new Random();
                loadPackageDestinations(new File(config.packagePath));
        }

        @Override
        public OnDemandVehicle create(String vehicleId, SimulationNode startPosition) {
                DeliveryPackageLoad packageLoad = new DeliveryPackageLoad(assignPackages());
                return new RideSharingOnDemandVehicle(vehicleStorage, tripsUtil,
                        onDemandVehicleStationsCentral, driveActivityFactory, positionUtil, eventProcessor, timeProvider,
                        rebalancingIdGenerator, config, vehicleId, startPosition, packageLoad);
        }

        private void loadPackageDestinations(File inputFile) {
                this.packageDestinations = new ArrayList<SimulationNode>();
                try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                                String[] parts = line.split(" ");
                                GPSLocation packageDestLocation
                                        = new GPSLocation(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), 0, 0);
                                this.packageDestinations.add(nearestElementUtils.getNearestElement(packageDestLocation, EGraphType.HIGHWAY));

                        }
                } catch (IOException ex) {
                        LOGGER.error(null, ex);
                }
        }

        private List<DeliveryPackage> assignPackages() {
                List<DeliveryPackage> packages = new LinkedList<DeliveryPackage>();
                for (int i = 0; i < 10; i++) {
                        packages.add(getRandomPackage());
                }
                return packages;

        }

        private DeliveryPackage getRandomPackage() {
                int index = randomGenerator.nextInt(packageDestinations.size());
                return new DeliveryPackage(null, 0, packageDestinations.get(index));
        }
}
