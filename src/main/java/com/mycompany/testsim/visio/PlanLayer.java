/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim.visio;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.agents.agentpolis.simmodel.entity.AgentPolisEntity;
import cz.agents.agentpolis.simmodel.environment.model.EntityStorage;
import cz.agents.alite.vis.layer.AbstractLayer;
import java.awt.Graphics2D;

/**
 *
 * @author fido
 */
@Singleton
public class PlanLayer extends AbstractLayer{
    
    private final EntityStorage<AgentPolisEntity> entityStorage;
    
    private 

    
    
    
    @Inject
    public PlanLayer(EntityStorage entityStorage) {
        this.entityStorage = entityStorage;
    }
    
    
    

    @Override
    public void paint(Graphics2D canvas) {
        for (AgentPolisEntity entity : entityStorage) {
            
        }
    }
    
    
    
    
}
