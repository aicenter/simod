/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.storage;

import com.google.inject.Singleton;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.EntityStorage;
import java.util.HashMap;

/**
 *
 * @author fido
 */
@Singleton
public class DemandStorage extends EntityStorage<DemandAgent>{
    
    public DemandStorage() {
        super();
    }
    
}