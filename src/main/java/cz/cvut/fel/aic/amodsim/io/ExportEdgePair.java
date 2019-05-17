/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.io;

/**
 *
 * @author fido
 */
public class ExportEdgePair {
	private final ExportEdge edge1;
	
	private final ExportEdge edge2;

	
	
	
	public ExportEdge getEdge1() {
		return edge1;
	}

	public ExportEdge getEdge2() {
		return edge2;
	}
	
	
   

	public ExportEdgePair(ExportEdge edge1, ExportEdge edge2) {
		this.edge1 = edge1;
		this.edge2 = edge2;
	}

}
