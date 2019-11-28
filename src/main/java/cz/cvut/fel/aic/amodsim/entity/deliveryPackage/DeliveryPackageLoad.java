/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.entity.deliveryPackage;

import java.util.Iterator;
import java.util.List;

/**
 *
 * @author kubis
 */
public class DeliveryPackageLoad implements Iterable<DeliveryPackage> {

        public final List<DeliveryPackage> load;

        public DeliveryPackageLoad(List<DeliveryPackage> load) {
                this.load = load;
        }

        @Override
        public Iterator<DeliveryPackage> iterator() {
                return load.iterator();
        }

        public int getSize() {
                return load.size();
        }

        public void clear() {
                load.clear();
        }
        
        public void remove(DeliveryPackage toRemove){
                load.remove(toRemove);
        }

}
