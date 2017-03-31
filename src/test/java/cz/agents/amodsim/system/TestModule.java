/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.system;

import com.google.inject.AbstractModule;
import cz.agents.amodsim.statistics.EdgesLoadByState;

/**
 *
 * @author fido
 */
public class TestModule extends AbstractModule{


    @Override
    protected void configure() {
        bind(EdgesLoadByState.class).to(OnDemandVehicleLoad.class);
    }
    
    
}
