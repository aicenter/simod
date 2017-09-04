package cz.cvut.fel.aic.amodsim.visio;


import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.AgentPolisEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalTransportVehicle;
import cz.cvut.fel.aic.amodsim.storage.PhysicalTransportVehicleStorage;
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
    
    
    
    public RandomColorOnDemandVehicleLayer(PhysicalTransportVehicleStorage physicalTransportVehicleStorage) {
        super(physicalTransportVehicleStorage);
        this.random = new Random();
        agentColors = new HashMap<>();
    }

    @Override
    protected Color getEntityDrawColor(PhysicalTransportVehicle agent) {
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
