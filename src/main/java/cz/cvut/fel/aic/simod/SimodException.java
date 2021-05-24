/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim;

/**
 * The purpose of this class is to enable simple exception filtering.
 * @author Fido
 */
public class SimodException extends Exception{

	public SimodException(String message) {
		super(message);
	}
	
}
