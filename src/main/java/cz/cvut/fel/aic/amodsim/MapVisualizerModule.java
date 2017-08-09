/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim;

import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioInitializer;
import cz.cvut.fel.aic.amodsim.config.Config;

/**
 *
 * @author fido
 */
public class MapVisualizerModule extends MainModule{
    
    public MapVisualizerModule(Config config) {
        super(config);
    }

    @Override
    protected void bindVisioInitializer() {
        bind(VisioInitializer.class).to(MapVisualizerVisioInItializer.class);
    }
    
    
    
}
