/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim.storage;

import com.google.inject.Singleton;
import com.mycompany.testsim.entity.DemandAgent;
import cz.agents.agentpolis.simmodel.environment.model.EntityStorage;
import java.util.HashMap;

/**
 *
 * @author fido
 */
@Singleton
public class DemandStorage extends EntityStorage<DemandAgent>{
    
    public DemandStorage() {
        super(new HashMap<>(), new HashMap<>());
    }
    
}
