/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.ridesharing.traveltimecomputation;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import cz.cvut.fel.aic.agentpolis.system.StandardAgentPolisModule;

/**
 *
 * @author matal
 */
public class TestAgentPolisInitializer {
	private Module mainModule;

	public TestAgentPolisInitializer(TestStandardAgentPolisModule mainModule) {
		this.mainModule = mainModule;
	}
	
	public TestAgentPolisInitializer() {
		this(new TestStandardAgentPolisModule());
	}

	public void overrideModule(Module module){
		mainModule = Modules.override(mainModule).with(module);
	}
	
	public Injector initialize(){
		Injector injector = Guice.createInjector(mainModule);
		return injector;
	}
}
