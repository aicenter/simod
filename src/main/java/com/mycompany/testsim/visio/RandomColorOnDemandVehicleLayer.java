package com.mycompany.testsim.visio;


import com.mycompany.testsim.entity.OnDemandVehicle;
import com.mycompany.testsim.storage.OnDemandVehicleStorage;
import com.mycompany.testsim.visio.OnDemandVehicleLayer;
import cz.agents.agentpolis.simmodel.entity.AgentPolisEntity;
import cz.agents.agentpolis.simulator.visualization.visio.entity.AgentPositionUtil;
import java.awt.Color;
import java.util.HashMap;
import java.util.Random;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author fido
 */
public class RandomColorOnDemandVehicleLayer extends OnDemandVehicleLayer{
    
    private final Random random;
    
    private final HashMap<AgentPolisEntity,Color> agentColors;
    
    
    
    public RandomColorOnDemandVehicleLayer(AgentPositionUtil entityPostitionUtil, 
            OnDemandVehicleStorage vehicleStorage) {
        super(entityPostitionUtil, vehicleStorage);
        this.random = new Random();
        agentColors = new HashMap<>();
    }

    @Override
    protected Color getColor(OnDemandVehicle agent) {
        if(agentColors.containsKey(agent)){
            return agentColors.get(agent);
        }
        else{
            Color color = getRandomColor();
            agentColors.put(agent, color);
            return color;
        }
    }
    
    
    
    
    
    private Color getRandomColor(){
		float r = random.nextFloat();
		float g = random.nextFloat();
		float b = random.nextFloat();
		
		return new Color(r, g, b);
	}
    
}
