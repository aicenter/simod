/* 
 * Copyright (C) 2019 Czech Technical University in Prague.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations;

import com.google.common.io.LittleEndianDataInputStream;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.ojalgo.function.BinaryFunction;
import org.ojalgo.function.UnaryFunction;
import org.ojalgo.matrix.store.MatrixStore;
import org.ojalgo.matrix.store.PhysicalStore;
import org.ojalgo.matrix.store.PrimitiveDenseStore;

/**
 *
 * @author matal
 */
public class MatrixMultiplyNN implements NN {   
    private final MatrixStore W[][], b[][];
    private final double mu[][];
    private final double sigma[][];
    private final PhysicalStore.Factory<Double, PrimitiveDenseStore> storeFactory;
    public MatrixMultiplyNN() {
        W = new MatrixStore[5][4];
        b = new MatrixStore[5][4];
        mu = new double[5][];
        sigma = new double[5][];
        storeFactory = PrimitiveDenseStore.FACTORY;
        //groups of range 3-7
        for (int i = 3; i < 8; i++) {
            loadJSON(i); //read mean and standard deviation for each attribute
            loadNPZ(i); //read NN model
        }
        
    }
    
    @Override
    public void setProbability(Set gd, IOptimalPlanVehicle vehicle, int groupSize) {
        MatrixStore matrixX = fillWithStandardizedData(gd,vehicle,groupSize+1);
        matrixX = compute(matrixX, groupSize+1);
        int i = 0;
        for (GroupData newGroupToCheck : (Set<GroupData>) gd) {
            newGroupToCheck.setFeasible((Double)matrixX.get(i));
            i++;
        }
    }
    private MatrixStore fillWithStandardizedData(Set gd, IOptimalPlanVehicle vehicle, int groupSize){
        Double[][] data = new Double[gd.size()][3+6*(groupSize)];
        DoubleIterator curr_mu = new DoubleIterator(mu[groupSize-3]);
        DoubleIterator curr_sigma = new DoubleIterator(sigma[groupSize-3]);
        double car_onboard = (vehicle.getRequestsOnBoard().size() - curr_mu.next())/curr_sigma.next();
        double car_lat = (vehicle.getPosition().latE6 - curr_mu.next())/curr_sigma.next();
        double car_lon = (vehicle.getPosition().lonE6- curr_mu.next())/curr_sigma.next();
        int k = 0;
        for (GroupData newGroupToCheck : (Set<GroupData>) gd) {
            int j = 0;
            curr_mu.reset();
            curr_sigma.reset();
            data[k][j++] = car_onboard;
            data[k][j++] = car_lat;
            data[k][j++] = car_lon;                       
            for (PlanComputationRequest rq : newGroupToCheck.getRequests()) {
                data[k][j++] = (rq.getFrom().latE6- curr_mu.next())/curr_sigma.next();
                data[k][j++] = (rq.getFrom().lonE6- curr_mu.next())/curr_sigma.next();
                data[k][j++] = (rq.getMaxPickupTime()- curr_mu.next())/curr_sigma.next();
                data[k][j++] = (rq.getTo().latE6- curr_mu.next())/curr_sigma.next();
                data[k][j++] = (rq.getTo().lonE6- curr_mu.next())/curr_sigma.next();
                data[k][j++] = (rq.getMaxDropoffTime()- curr_mu.next())/curr_sigma.next();
            }
            k++;
        }
        return storeFactory.rows(data);
    }
    private void loadNPZ(int groupSize){
        System.out.print("Loading model for group size "+groupSize+"....");
        String fileName = "data//NN_models//model_"+ groupSize +"//model.npz";
        File file = new File(fileName);
        try {        
            LittleEndianDataInputStream  fileInputStream = new LittleEndianDataInputStream(new FileInputStream(file));
            int singleCharInt;
            int counter = 0;
            fileInputStream.skipBytes(30);
            while ((singleCharInt = fileInputStream.read()) != -1) {               
                if((char) singleCharInt == 'W'){
                    char value = (char) fileInputStream.read();
                    for (int j = 0; j < 11; j++) {
                        fileInputStream.read();
                    }
                    int skip = fileInputStream.readUnsignedShort();
                    fileInputStream.skipBytes(Integer.rotateLeft(skip, 24)+1);
                    switch(value){
                        case '1':
                            W[groupSize-3][0] = readMatrix(fileInputStream, 3+6*(groupSize), 1024);
                            
                            counter++;
                            break;
                        case '2':
                            W[groupSize-3][1] = readMatrix(fileInputStream, 1024, 512);
                            counter++;
                            break;
                        case '3':
                            W[groupSize-3][2] = readMatrix(fileInputStream, 512, 256);
                            counter++;
                            break;
                        case '4':
                            W[groupSize-3][3] = readMatrix(fileInputStream, 256, 1);
                            counter++;
                            break;
                    }
                }
                else if((char) singleCharInt == 'b'){
                    char value = (char) fileInputStream.read();
                    for (int j = 0; j < 11; j++) {
                        fileInputStream.read();
                    }
                    int skip = fileInputStream.readUnsignedShort();
                    fileInputStream.skipBytes(Integer.rotateLeft(skip, 24)+1);
                    switch(value){
                        case '1':
                            b[groupSize-3][0] = readMatrix(fileInputStream, 1, 1024);
                            counter++;
                            break;
                        case '2':
                            b[groupSize-3][1] = readMatrix(fileInputStream, 1, 512);
                            counter++;
                            break;
                        case '3':
                            b[groupSize-3][2] = readMatrix(fileInputStream, 1, 256);
                            counter++;
                            break;
                        case '4':
                            b[groupSize-3][3] = readMatrix(fileInputStream, 1, 1);
                            counter++;
                            break;
                    }
                }

                if(counter == 8){
                    break;
                }
                fileInputStream.skipBytes(30);
            }
            fileInputStream.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(MatrixMultiplyNN.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MatrixMultiplyNN.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("model loaded.");
    }
    private MatrixStore readMatrix(LittleEndianDataInputStream  fileInputStream, int rows, int columns){
        //load transpose matrix, couse of fortran order implementation
        double[][] data = new double[rows][columns];
        for (int i = 0; i < columns; i++) {
            for (int j = 0; j < rows; j++) {
                try {
                    data[j][i] = fileInputStream.readFloat();                  
                } catch (IOException ex) {
                    Logger.getLogger(MatrixMultiplyNN.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return storeFactory.rows(data);
    }

        private void loadJSON(int groupSize) {
        String fileName = "data//NN_models//model_"+ groupSize +"//model_pre.json";
        File file = new File(fileName);        
        try (FileReader fr = new FileReader(file)) {
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(fr);
            double[] mu_data = new double[3+6*(groupSize)];
            double[] sigma_data = new double[3+6*(groupSize)];
            load_value(mu_data, sigma_data, 0, "onboard_count", jsonObject);
            load_value(mu_data, sigma_data, 1, "lat", jsonObject);
            load_value(mu_data, sigma_data, 2, "lon", jsonObject);
            for (int i = 1; i <= groupSize; i++) {
                load_value(mu_data, sigma_data, 6*(i-1)+3, "pickup_"+i+"_lat", jsonObject);
                load_value(mu_data, sigma_data, 6*(i-1)+4, "pickup_"+i+"_lon", jsonObject);
                load_value(mu_data, sigma_data, 6*(i-1)+5, "pickup_"+i+"_maxtime", jsonObject);
                load_value(mu_data, sigma_data, 6*(i-1)+6, "dropoff_"+i+"_lat", jsonObject);
                load_value(mu_data, sigma_data, 6*(i-1)+7, "dropoff_"+i+"_lon", jsonObject);
                load_value(mu_data, sigma_data, 6*(i-1)+8, "dropoff_"+i+"_maxtime", jsonObject);
            }
            mu[groupSize-3] = mu_data;
            sigma[groupSize-3] = sigma_data;
		} catch (IOException | ParseException e) {
			throw new IllegalStateException("JSON file can't be parsed.", e);
		}
    }        
    private void load_value(double[] mu_data, double[] sigma_data, int pos, String where, JSONObject jsonObject){
                JSONObject current = (JSONObject) jsonObject.get(where);
                mu_data[pos] = (Double) current.get("mu");
                sigma_data[pos] = (Double) current.get("sigma");       
    }

    private MatrixStore compute(MatrixStore X, int groupSize) {
        BinaryFunction<Double> modifier = storeFactory.function().add();
        UnaryFunction<Double> modifier2 = storeFactory.function().max().second(0);
        UnaryFunction<Double> exp = storeFactory.function().exp();
        UnaryFunction<Double> divide_one = storeFactory.function().divide().first(1);
        UnaryFunction<Double> modifier3 = 
                storeFactory.function().negate()
                        .andThen(exp)
                        .andThen(modifier.second(1))                      
                        .andThen(divide_one);
        for (int i = 0; i < 4; i++) {
            PrimitiveDenseStore Y = storeFactory.makeZero(X.countRows(), W[groupSize-3][i].countColumns());
            //ElementsSupplier<Double> Y = W[groupSize-3][i].premultiply(X);
            X.multiply(W[groupSize-3][i],Y);
            //Y = Y.operateOnColumns(modifier, b[groupSize-3][i]);
            Y.modifyMatchingInRows(modifier, b[groupSize-3][i]);

            if(i != 3){
                //Y = Y.operateOnAll(modifier2);
                Y.modifyAll(modifier2);
            }else{
                //Y = Y.operateOnAll(modifier3);
                Y.modifyAll(modifier3);                
            }
            //X = Y.get();
            X = Y;
        }
        return X;
    }
    private class DoubleIterator implements Iterator{
        private double[] values;
        private int pos;
        public DoubleIterator(double[] values) {
            this.values = values;
            this.pos = 0;
        }
        
        @Override
        public boolean hasNext() {
            return pos != values.length;
        }

        @Override
        public Double next() {
            return values[pos++];
        }
        public void reset(){
            this.pos = 3;
        }
        
    }
}
