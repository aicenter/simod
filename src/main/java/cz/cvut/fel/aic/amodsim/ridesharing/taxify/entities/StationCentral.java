/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.taxify.entities;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.io.Rtree;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.ConfigTaxify;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.Demand;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.util.GPSLocationTools;
import org.slf4j.LoggerFactory;
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Keeps information about stations.
 * Returns nearest station for simulation nodes by id.
 *
 * @author olga
 */
public class StationCentral {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(StationCentral.class);
    ConfigTaxify config;
    TravelTimeProvider travelTimeProvider;
    private Demand demand;
    Graph<SimulationNode, SimulationEdge> graph;
    private final Map<Integer, int[]> nearestStation;
    private final int N;
    Map<Integer, Integer> depoIds;
    private Rtree rtree;
    private HashMap<Integer, Integer> deposById = new HashMap();

    /**
     * Loads station coordinates from .csv, maps stations to graph nodes,
     * create rtree for search.
     *
     * @param config
     * @param travelTimeProvider
     * @param graph
     * @param demand
     */
    public StationCentral(ConfigTaxify config, TravelTimeProvider travelTimeProvider,
                          Graph<SimulationNode, SimulationEdge> graph, Demand demand) {
        this.config = config;
        this.graph = graph;
        this.travelTimeProvider = travelTimeProvider;
        this.demand = demand;
        nearestStation = new HashMap<>();
        depoIds = new HashMap<>();
        N = config.maxStations;
        if (config.optimizeDepoLocations > 0) {
            optimizeStationsLocations();
        } else {
            loadStations();
        }
    }

    private void optimizeStationsLocations() {
        LOGGER.info("Optimizing depo lacations.");
        int radius = 40 * config.pickupRadius;

        List data = new ArrayList();
        if (config.optimizeDepoLocations == 1) {
            LOGGER.info("Optimizing by demand clustering");

            for (int d = 0; d < demand.size(); d++) {
                double[] coords = demand.getGpsCoordinates(d);
                double[] coordsStart = {coords[0], coords[1]};
                double[] coordsEnd = {coords[2], coords[3]};
                data.add(coordsStart);
                data.add(coordsEnd);
            }
        } else if (config.optimizeDepoLocations == 2) {
            LOGGER.info("Optimizing by graph nodes clustering");

            for (SimulationNode node : graph.getAllNodes()) {
                double[] coords = {node.getLatitude(), node.getLongitude()};
                data.add(coords);

            }
        }
        List<double[]> centroids = kmeans(data, N);
        Rtree rtree = new Rtree(this.graph.getAllNodes(), this.graph.getAllEdges());


        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(config.dir + "centroids.csv"));
            writer.write("depo_lat,depo_lng\n");
            for (double[] c : centroids) {
                writer.write(c[0] + ", " + c[1] + "\n");
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int i = 0;
        for (double[] centroid : centroids) {
            GPSLocation loc = GPSLocationTools.createGPSLocation(centroid[0],
                    centroid[1],
                    0, config.SRID);
            Object[] result = rtree.findNode(loc, radius);

            if (result == null) {
                LOGGER.error("Node not found for station " + i);
            } else {
                depoIds.put((int) result[0], i);
                deposById.put(i, (int) result[0]);
            }
            i++;
        }
        this.rtree = new Rtree(depoIds.keySet().stream()
                .map(nodeId -> graph.getNode(nodeId))
                .collect(Collectors.toList()));
        exportStations();
    }

    private List<double[]> kmeans(List<double[]> data, int k) {
        // Create the KMeans object.
        int maxIteration = 100;
        SimpleKMeans kmeans = new SimpleKMeans();
        ArrayList<Attribute> attr_list = new ArrayList<>();
        attr_list.add(new Attribute("lat"));
        attr_list.add(new Attribute("lon"));
        Instances instances = new Instances("data", attr_list, data.size());
        for (double[] d : data) {
            Instance instance = new DenseInstance(1, d);
            instances.add(instance);
        }
        try {
            kmeans.setNumClusters(k);

            kmeans.setMaxIterations(maxIteration);
            kmeans.setPreserveInstancesOrder(true);


            kmeans.buildClusterer(instances);
        } catch (Exception ex) {
            System.err.println("Unable to build Clusterer: " + ex.getMessage());
            ex.printStackTrace();
        }

        // print out the cluster centroids
        Instances centroids = kmeans.getClusterCentroids();
        List<double[]> centroidList = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            System.out.print("Cluster " + i + " size: " + kmeans.getClusterSizes()[i]);
            System.out.println(" Centroid: " + centroids.instance(i));
            double[] c = {centroids.get(i).value(0),
                    centroids.get(i).value(1)};
            centroidList.add(c);
        }
        return centroidList;
    }

    /**
     * Finds nearest station by SimulationNode id.
     *
     * @param nodeId id of SimulationNode
     * @return id of Simulation node where the nearest station is located
     */
    public int[] findNearestStation(int nodeId) {
        //TODO the map is completely filled, destroy the rtree.
        if (nearestStation.containsKey(nodeId)) {
            return nearestStation.get(nodeId);
        }
        List<Integer> stations = rtree.findNearestStations(graph.getNode(nodeId));
        nearestStation.put(nodeId, new int[2]);
        int bestTime = Integer.MAX_VALUE;
        for (Integer station : stations) {
            int time = travelTimeProvider.getTravelTimeInMillis(nodeId, station);
            if (time < bestTime) {
                bestTime = time;
                nearestStation.get(nodeId)[0] = station;
                nearestStation.get(nodeId)[1] = bestTime;
            }
        }
        return nearestStation.get(nodeId);
    }

    /**
     * Finds nearest station for the location with one or two possible nodes.
     *
     * @param nodes array of simulationNodes ids and distances
     *              [node1, dist1, node2, dist2]
     *              array length is either 2 (one node, ie gps location was mapped to the node),
     *              or 4 (2 nodes, mapped to edge)
     * @return
     */
    public int[] findNearestStation(int[] nodes) {

//        LOGGER.debug(Arrays.toString(nodes));
        if (nodes.length == 2) { //
            return findNearestStation(nodes[0]);
        }
        int[] node1Result = findNearestStation(nodes[0]);
        int[] node2Result = findNearestStation(nodes[2]);
        if (node1Result[1] <= node2Result[1]) {
            return node1Result;
        } else {
            return node2Result;
        }
    }

    /**
     * returns depo id (from 0 to maxDepo) by SimulationNode id
     *
     * @param nodeId
     * @return
     */
    public int getDepoId(int nodeId) {
        nodeId = nodeId >= 0 ? nodeId : -nodeId;
        return depoIds.get(nodeId);
    }

    private void exportStations() {
        String[] lines = new String[N];
        for (int depoId : deposById.keySet()) {
            SimulationNode node = this.graph.getNode(deposById.get(depoId));
//            System.out.println(node.getLatitude() + "," + node.getLongitude());
            lines[depoId] = node.getLatitude() + "," + node.getLongitude() + "\n";
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(config.dir + "generated-depos.csv"));
            writer.write("depo_lat,depo_lng\n");
            for (String line : lines) {
                writer.write(line);
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void loadStations() {
        LOGGER.info("Loading default depo stations location");
        int radius = 4 * config.pickupRadius;
        //List<SimulationNode> stationNodes = new ArrayList<>();
        rtree = new Rtree(this.graph.getAllNodes(), this.graph.getAllEdges());
        try (BufferedReader br = new BufferedReader(new FileReader(config.depoFileName))) {
            String line = br.readLine();
            for (int i = 0; i < N; i++) {
                line = br.readLine();
                String[] parts = line.split(",");
                //System.out.println(count+Arrays.toString(parts));
                GPSLocation loc = GPSLocationTools.createGPSLocation(Double.parseDouble(parts[0]),
                        Double.parseDouble(parts[1]),
                        0, config.SRID);
                Object[] result = rtree.findNode(loc, radius);
                if (result == null) {
                    LOGGER.error("Node not found for station " + i);
                } else {
                    depoIds.put((int) result[0], i);
                    deposById.put(i, (int) result[0]);
                }
            }
            rtree = null;
            if (depoIds.size() != N) {
                LOGGER.error("Number of stations loaded differs from config");
            }
            rtree = new Rtree(depoIds.keySet().stream()
                    .map(nodeId -> graph.getNode(nodeId))
                    .collect(Collectors.toList()));
            exportStations();
        } catch (IOException ex) {
            LOGGER.error("Error loading stations: " + ex);
        }
    }


}


