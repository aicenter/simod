package cz.agents.amodsim.visio;


import cz.agents.amodsim.entity.vehicle.OnDemandVehicle;
import cz.agents.amodsim.storage.OnDemandVehicleStorage;
import cz.agents.amodsim.visio.OnDemandVehicleLayer;
import cz.agents.agentpolis.simmodel.entity.AgentPolisEntity;
import cz.agents.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.agents.agentpolis.simulator.visualization.visio.entity.AgentPositionUtil;
import cz.agents.agentpolis.simulator.visualization.visio.entity.VehiclePositionUtil;
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
    
    
    
    public RandomColorOnDemandVehicleLayer(OnDemandVehicleStorage vehicleStorage) {
        super(vehicleStorage);
        this.random = new Random();
        agentColors = new HashMap<>();
    }

    @Override
    protected Color getEntityDrawColor(OnDemandVehicle agent) {
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
