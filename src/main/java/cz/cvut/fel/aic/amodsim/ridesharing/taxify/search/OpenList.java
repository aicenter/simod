/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.taxify.search;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author olga
 */
public class OpenList {
    private int size;
    private final Map<Integer, Integer> costs;
    private final Map<Integer, Integer> heapPositions;
    private final int[] minHeap;

    /**
     * Creates minimum heap with array of length maxSize.
     * @param maxSize size of array to keep node ids
     * @param costs map with fScore
     */
    public OpenList(int maxSize, Map<Integer, Integer> costs) {
        minHeap = new int[maxSize];
        size = 0;
        this.costs = costs;
        heapPositions = new HashMap<>();
    }

    public boolean add(Object item) {
        int nodeId = (int) item;
        heapPositions.put(nodeId, size);
        int currentInd = size;
        minHeap[size++] = nodeId;
        if(size > 1) bubbleUp(currentInd);
        return true;
        
    }

    /**
     * Retrieves the head of the non-empty heap
     * @return long, id of the node with lowest fScore currently in the heap
     */
    public int pop(){
        int head = minHeap[0];
        size--;
        costs.remove(head);
        heapPositions.remove(head);
        if(size > 0)     swap(0, size);
        if(size > 1)     bubbleDown(0);
        return head;
    }
    
    /**
     * updates the position of the node, that is in the queue after its fScore was
     * updated in BFS.
     * fScore if updated only in case the its smaller than the old than,
     * so node can be only move up the heap if needed.
     * 
     * @param nodeId long, id of the node to be updated.
     */
    public void update(int nodeId){
        int currentInd = heapPositions.get(nodeId);
        if(size > 1) bubbleUp(currentInd);       
    }
    
    /**
     * Checks if node with this id is currently in the heap
     * @param nodeId, long, id of the node
     * @return true if node is in heap, false otherwise
     */
    public boolean containsNode(int nodeId) {
        return heapPositions.containsKey(nodeId);
    }
    
    /**
     * Check if the heap is empty.
     * 
     * @return true if the heap is empty, false otherwise
     */
    public boolean isEmpty(){
        return size == 0;
    }
      
    /**
     * Prints the whole heap.
     */
    public void print() {
        print(size);
    }
  
    
    /**
     * Prints out first n  nodes of the heap. 
     * If n is bigger than the heap size, prints the whole heap.
     * @param n, integer, number of nodes to be printed.
     */
    public void print(int n) {
        if(n > size) n = size;
        System.out.println("OpenList, min heap, size = " + size);
        System.out.println("Printing costs of first " + n + " nodes");
        int level = 1;
        int i = 0;
        while(i < n){
            for(int j = 0; j < level; j++){
                System.out.printf(" %d: [ %d ] ;", i,  costs.get(minHeap[i]));
                i++;
            }
            System.out.println();
            level *= 2;
        }
    }
    
    //Binary Heap helper methods
    private int getParent(int ind) {
	return (++ind / 2) - 1;
    }
    
    private int getLeftChild(int ind) {
	return (2 * ++ind) - 1;
    }

    private int getRightChild(int ind) {
	return (2 * ++ind);
    }
    
    private boolean isLeaf(int ind) {
        return (ind >= (size / 2) && ind < size);
    }

    private void swap(int ind1, int ind2) {
        int tmp;
        tmp = minHeap[ind1];
        minHeap[ind1] = minHeap[ind2];
        minHeap[ind2] = tmp;
        heapPositions.put(minHeap[ind1], ind1);
        heapPositions.put(minHeap[ind2], ind2);
    }
    
    private boolean isSmaller(int ind1, int ind2){
        return costs.get(minHeap[ind1]) < costs.get(minHeap[ind2]);
    }
    
    private void bubbleUp(int ind){
        int parentInd = getParent(ind);
        while (ind > 0 && isSmaller(ind, parentInd)) {
            swap(ind, parentInd);
            ind = parentInd;
            parentInd = getParent(ind);
        }
        
    }
    
    private void bubbleDown(int ind){
        if (!isLeaf(ind)) {
            int lci = getLeftChild(ind);
            int rci = getRightChild(ind);
           
            //both children
            if( rci < size && isSmaller(rci, ind)){
                if(isSmaller(lci, rci)){ //left is smaller of two 
                    swap(ind, lci);
                    bubbleDown(lci);
                }
                else{
                   swap(ind, rci);
                   bubbleDown(rci);
                }
            }
            else if (isSmaller(lci, ind)) { // only left child
                swap(ind, lci);
                bubbleDown(lci);
            }
        }
    }

}

