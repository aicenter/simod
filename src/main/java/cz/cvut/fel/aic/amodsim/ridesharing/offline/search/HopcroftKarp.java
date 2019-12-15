/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.offline.search;

import java.util.LinkedList;
import java.util.List;
import org.slf4j.LoggerFactory;

/**
 * Map-covering. Connects single trips into the paths using Hopcroft-Carp algorithm.
 * https://en.wikipedia.org/wiki/Hopcroft%E2%80%93Karp_algorithm
 * 
 * @author Olga Kholkovskaia
 */
public class HopcroftKarp {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(HopcroftKarp.class);

    private int[][] adj;
    private final int N;
    private final int inf;
    private final int[] pair_u;
    private int[] pair_v;
    private int[] dist;
    
    /**
     *
     * @param numOfTrips total number of nodes (trips) 
     */
    public HopcroftKarp(int numOfTrips){
        N = numOfTrips;
        inf = Integer.MAX_VALUE;
        dist = new int[N+1];
        pair_u = new int[N];
        pair_v = new int[N];
        for(int i = 0; i < N; i++){
            pair_u[i] = N;
            pair_v[i] = N;
        }
    }
    
    /**
     * Creates map cover for graph represented with adjacency matrix passed as argument.
     * 
     * @param adjacency adjacency matrix
     * @return disjoint set of node pairs from two subsets. Position in the array is trip index, 
     * value at that position - index of the next trip, to which the car should proceed.
     */
    public int[] findMapCover(int[][] adjacency){
        //LOGGER.debug("HK " +adjacency.length);
        //LOGGER.debug(Arrays.toString(adjacency[0]));
        adj = adjacency;
        //int matching = 0;
        while(BFS() == true){
            for(int ind = 0; ind < N; ind++){
                if(pair_u[ind] == N){
                    if(DFS(ind) == true){
                       // matching++;
                    }
                }
            }
        }
        pair_v = null;
        dist = null;
        //LOGGER.debug("Matching "+matching);
        return pair_u;
    }
   
    private boolean BFS(){
        List<Integer> queue = new LinkedList<>();
        for(int uInd = 0; uInd < N; uInd++){
            if(pair_u[uInd] == N){
                dist[uInd] = 0;
                queue.add(uInd);
            }else{
                dist[uInd] = inf;
            }
        }
        dist[N] = inf;
        while(!queue.isEmpty()){
            int u = queue.remove(0);
            if(dist[u] < dist[N]){
                for(int v : adj[u]){
                    if(dist[pair_v[v]] == inf){
                        dist[pair_v[v]] = dist[u] + 1;
                        queue.add(pair_v[v]);
                    }
                }
            }
        }
        return dist[N] != inf;
    }
    
    private boolean DFS(int u){
        if(u == N){
            return true;
        }
        for(int v : adj[u]){
            if(dist[pair_v[v]] == dist[u] + 1){
                if(DFS(pair_v[v]) == true){
                    pair_v[v] = u;
                    pair_u[u] = v;
                    return true;
                }
            }
        }
        dist[u] = inf;
        return false;
    }
 }
