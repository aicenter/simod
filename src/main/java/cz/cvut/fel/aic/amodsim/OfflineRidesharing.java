package cz.cvut.fel.aic.amodsim;

import com.google.inject.Injector;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.MapInitializer;
import cz.cvut.fel.aic.agentpolis.simulator.creator.SimulationCreator;
import cz.cvut.fel.aic.agentpolis.system.AgentPolisInitializer;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.DARPSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.insertionheuristic.DriverPlan;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Map;
import org.slf4j.LoggerFactory;

/**
 * @author David Fiedler
 */
public class OfflineRidesharing {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(OfflineRidesharing.class);
    
    public static void main(String[] args) throws MalformedURLException {
        new OfflineRidesharing().run(args);
    }


    public void run(String[] args) {
        AmodsimConfig config = new AmodsimConfig();
        
        File localConfigFile = null;
        
        if(args.length > 0){
            localConfigFile = new File(args[0]);
        }
        Injector injector = new AgentPolisInitializer(new MainModule(config, localConfigFile)).initialize();
        SimulationCreator creator = injector.getInstance(SimulationCreator.class);
        // prepare map, entity storages...
        creator.prepareSimulation(injector.getInstance(MapInitializer.class).getMap());

        DARPSolver solver = injector.getInstance(DARPSolver.class);
        Map<RideSharingOnDemandVehicle, DriverPlan> planMap = solver.solve(new ArrayList<>(),  new ArrayList<>());  
    }
}
