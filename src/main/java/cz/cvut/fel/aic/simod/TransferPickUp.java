package cz.cvut.fel.aic.simod;

import cz.cvut.fel.aic.agentpolis.simmodel.agent.TransportEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.TransportableEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalTransportVehicle;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class TransferPickUp <T extends TransportableEntity, E extends TransportEntity> {

    public static <T extends TransportableEntity, E extends TransportEntity> void pickUp(T entity,
                                                                                         boolean isVehicleFull, E transportingEntity, List<T> transportedEntities) {
        if (isVehicleFull) {
            try {
                throw new Exception(
                        String.format("Cannot pick up entity, the vehicle is full! [%s]", entity));
            } catch (Exception ex) {
                Logger.getLogger(PhysicalTransportVehicle.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if(entity.getTransportingEntity() != null) {
            // auto se snazi zavolat pickup na demand, ktery jeste nedojel do cilove stanice
            // ale nez by toto auto dojelo na stanici, demand uz by tam byl vylozeny
            // proto podminka entity.getTransportingEntity() != null neni spravne

//            try {
//                throw new Exception(
//                        String.format("Cannot pick up entity, it's already being transported! [%s]", entity));
//            } catch (Exception ex) {
//                Logger.getLogger(PhysicalTransportVehicle.class.getName()).log(Level.SEVERE, null, ex);
//            }
        }
        else{
            transportedEntities.add(entity);
            entity.setTransportingEntity(transportingEntity);
        }
    }

}