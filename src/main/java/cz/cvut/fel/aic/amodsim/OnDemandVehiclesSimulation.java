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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.FileAppender;
import com.google.inject.Injector;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.MapInitializer;
import cz.cvut.fel.aic.agentpolis.simulator.creator.SimulationCreator;
import cz.cvut.fel.aic.agentpolis.system.AgentPolisInitializer;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.init.EventInitializer;
import cz.cvut.fel.aic.amodsim.init.StationsInitializer;
import cz.cvut.fel.aic.amodsim.init.StatisticInitializer;
import cz.cvut.fel.aic.amodsim.io.RebalancingLoader;
import cz.cvut.fel.aic.amodsim.io.TripTransform;
import cz.cvut.fel.aic.amodsim.rebalancing.ReactiveRebalancing;
import cz.cvut.fel.aic.amodsim.statistics.Statistics;
import cz.cvut.fel.aic.amodsim.tripUtil.TripsUtilCached;
import java.io.BufferedReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David Fiedler
 */
public class OnDemandVehiclesSimulation {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(OnDemandVehiclesSimulation.class);
	
	public static void main(String[] args) throws MalformedURLException {
		new OnDemandVehiclesSimulation().run(args);
	}


	public void run(String[] args) {
		AmodsimConfig config = new AmodsimConfig();
		
		File localConfigFile = null;
        String config_path = "\\src\\main\\resources\\cz\\cvut\\fel\\aic\\amodsim\\config\\config.cfg";
		if(args.length > 0){
			localConfigFile = new File(args[0]);
            config_path = args[0];
		}
        setLoggerFilePath(config_path);
		Injector injector = new AgentPolisInitializer(new MainModule(config, localConfigFile)).initialize();
        SimulationCreator creator = injector.getInstance(SimulationCreator.class);
		// prepare map, entity storages...
		creator.prepareSimulation(injector.getInstance(MapInitializer.class).getMap());

		TripTransform tripTransform = injector.getInstance(TripTransform.class);
		RebalancingLoader rebalancingLoader = injector.getInstance(RebalancingLoader.class);

		if(config.rebalancing.on){
//				rebalancingLoader.load(new File(config.rebalancing.external.policyFilePath), true);
			// load stations
			injector.getInstance(StationsInitializer.class).loadStations();

			// start rebalancing
			injector.getInstance(ReactiveRebalancing.class).start();

			// load trips
			injector.getInstance(EventInitializer.class).initialize(
				tripTransform.loadTripsFromTxt(new File(config.tripsPath)), null);
		}

		injector.getInstance(StatisticInitializer.class).initialize();
            
		// start it up
		creator.startSimulation();

		if (config.useTripCache) {
			injector.getInstance(TripsUtilCached.class).saveNewTrips();
		}
		injector.getInstance(Statistics.class).simulationFinished();
	}

    private void setLoggerFilePath(String config_path) {
        String experiments_path = "";
        try {
            String line;
            BufferedReader reader = new BufferedReader(new FileReader(config_path));
            while ((line = reader.readLine()) != null)
            {
                String[] parts = line.split(":", 2);
                if (parts.length >= 2)
                {
                    if("amodsim_experiment_dir".equals(parts[0])){
                        experiments_path = parts[1].substring(2, parts[1].length()-1);
                        break;
                    }
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(OnDemandVehiclesSimulation.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(OnDemandVehiclesSimulation.class.getName()).log(Level.SEVERE, null, ex);
        }
        File file = new File(experiments_path + "log");
        File[] files = file.listFiles();
        for (int i = 0; files != null && i < files.length; i++) {
            files[i].delete();
        }
        file.delete();
        file.mkdir();  
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        FileAppender appender =
        (FileAppender) lc.getLogger("ROOT").getAppender("FILE");
        appender.setFile(experiments_path+"\\log\\log.txt");
        appender.start();
    }
}
