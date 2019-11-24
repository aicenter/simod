/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.entity.deliveryPackage;

/**
 *
 * @author kubis
 */
public class DeliveryPackageDimensions {

        private final int length;

        private final int width;

        private final int height;

        public DeliveryPackageDimensions(int length, int width, int height) {
                this.length = length;
                this.width = width;
                this.height = height;
        }
}
