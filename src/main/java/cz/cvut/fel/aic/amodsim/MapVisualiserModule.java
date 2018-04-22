package cz.cvut.fel.aic.amodsim;

import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioInitializer;
import cz.cvut.fel.aic.agentpolis.system.StandardAgentPolisModule;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.visio.MapVisualizerVisioInitializer;
import java.io.File;

/**
 *
 * @author F.I.D.O.
 */
public class MapVisualiserModule  extends MainModule{

	private final AmodsimConfig amodsimConfig;
	
	public MapVisualiserModule(AmodsimConfig amodsimConfig, File localConfigFile) {
        super(amodsimConfig, localConfigFile);
        this.amodsimConfig = amodsimConfig;
    }
	
	@Override
    protected void bindVisioInitializer() {
        bind(VisioInitializer.class).to(MapVisualizerVisioInitializer.class);
    }
}
