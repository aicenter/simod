package cz.cvut.fel.aic.simod.init;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalTransportVehicle;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.SimpleTransportVehicle;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.vehicle.SpecializedTransportVehicle;

import java.io.File;
import java.io.IOException;

public class VehicleInitializer {

    private final SimodConfig config;

    @Inject
    public VehicleInitializer(SimodConfig config) {
        this.config = config;
    }

    public void run() {
        ObjectMapper objectMapper = new ObjectMapper();

        File vehicleFile = new File(config.vehiclesFilePath);

        PhysicalTransportVehicle[] vehicles = null;
        try {
            if(config.heterogeneousVehicles){
                vehicles = objectMapper.readValue(vehicleFile, SpecializedTransportVehicle[].class);
            }
            else{
                vehicles = objectMapper.readValue(vehicleFile, SimpleTransportVehicle[].class);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
