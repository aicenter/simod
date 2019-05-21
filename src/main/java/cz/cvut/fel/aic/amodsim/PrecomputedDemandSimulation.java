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
package cz.cvut.fel.aic.amodsim;


import cz.cvut.fel.aic.amodsim.io.TimeTrip;
import cz.cvut.fel.aic.amodsim.io.TripTransform;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import org.slf4j.LoggerFactory;

/**
 *
 * @author F-I-D-O
 */
public class PrecomputedDemandSimulation {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PrecomputedDemandSimulation.class);
	
	private static File EXPERIMENT_DIR = new File("data/Prague");

	private static final String INPUT_FILE_PATH = "trips.json";

	private static final int SRID = 2065;
	
	private static final int START_TIME = 25200000; // 7h
	
	public static void main(String[] args) throws MalformedURLException{
		if (args.length >= 1) {
			EXPERIMENT_DIR = new File(args[0]);
		}
		new PrecomputedDemandSimulation().run();
	}
	
	public void run(){
		try {
			List<TimeTrip<Long>> osmNodesList = TripTransform.jsonToTrips(new File(EXPERIMENT_DIR, INPUT_FILE_PATH), Long.class);

//			SimpleEnvinromentFactory envinromentFactory = new SimpleEnvinromentFactory(new InfinityDelayingSegmentCapacityDeterminer());

			
//			Injector injector = Guice.createInjector(new StandardAgentPolisModule(envinromentFactory, parameters));
			

//			SimulationCreator creator = new SimulationCreator(
//					new SimpleEnvinromentFactory(new InfinityDelayingSegmentCapacityDeterminer()), parameters);
//			
//			SimulationCreator creator = injector.getInstance(SimulationCreator.class);
//			
//			creator.setMainEnvironment(injector);

	//		creator.addInitModuleFactory(new VehicleDataModelFactory(parameters.vehicleDataModelFile));

	//		creator.addAgentInit(new MyAgentInitFactory());
	
//			creator.addInitFactory(new DemendsInitFactory(osmNodesList, creator));

			// set up visual appearance of agents
//			creator.addEntityStyleVis(DemandSimulationEntityType.DEMAND, Color.GREEN, 8);
//
//			// start it up
//			creator.prepareSimulation(new MyMapInitFactory(SRID));
//			
//			creator.startSimulation();

			System.exit(0);
			
		} catch (IOException ex) {
						LOGGER.error(null, ex);
		}
	}
	
	
}
