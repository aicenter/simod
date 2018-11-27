/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.taxify.search;

import cz.cvut.fel.aic.amodsim.ridesharing.taxify.io.TripTaxify;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.MovingEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.TransportNetworks;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.io.TimeTripWithValue;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.ConfigTaxify;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.search.AStar;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Graph;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;


@Singleton
public class TravelTimeProviderOSRM implements TravelTimeProvider{
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TravelTimeProviderTaxify.class); 
	private final ConfigTaxify config;
	private long callCount = 0;
    private final Graph<SimulationNode, SimulationEdge> graph;
    private final double speedMs;
    HttpClient httpClient;
    int n;
   
    
	public long getCallCount() {
		return callCount;
	}
	

	@Inject
	public TravelTimeProviderOSRM(ConfigTaxify config, TransportNetworks transportNetworks) {
		this.config = config;
        this.graph = transportNetworks.getGraph(EGraphType.HIGHWAY);
        speedMs = config.speed;
        n = graph.numberOfNodes();
        httpClient = HttpClients.createDefault();
	}
    
   

    @Override
    public int getTravelTimeInMillis(Integer startId, Integer targetId) {
        return distToTime(getOsrmDistance(startId, targetId));
    }

    /**
     * Returns best possible travel time for the trip in milliseconds.
     * The total trip consists of three parts: from first location to one of the assigned nodes, from last location to one of the
     * assigned nodes, and trip itself. Trip instance has data about distance btw location and nodes in meters(!).
     * Value for shortest path from time matrix is already in milliseconds.
     * @param trip
     * @return 
     */
    @Override
    public int getTravelTimeInMillis(TripTaxify<GPSLocation> trip) {
        GPSLocation start = trip.getFirstLocation();
        GPSLocation end = trip.getLocations().get(1);
        return distToTime(getOsrmDistance(start.getLatitude(), start.getLongitude(),
                                          end.getLatitude(), end.getLongitude()));

    }
    /**
     * 
     * @param startNodes
     * @param endNodes
     * @return 
     */
    @Override
    public int  getTravelTimeInMillis(int[] startNodes, int[] endNodes) {
        double bestDist = Integer.MAX_VALUE;
        int[] nodes = new int[]{1,1};
        for (int i=0; i<startNodes.length; i+=2){
            for(int j=0; j<endNodes.length; j+=2){
                double n2n = getOsrmDistance(startNodes[i], endNodes[j]);
                int s2n = startNodes[i+1];
                int e2n = endNodes[j+1];
                double dist = n2n + s2n + e2n;
                if(dist < bestDist){
                    bestDist = dist;
                    nodes[0] = i;
                    nodes[1] = j;
                }
            }
        }
        if(nodes[0] == 2){
            swapNodes(startNodes);            
        }
        if(nodes[1] == 2){
            swapNodes(endNodes);
        }
        return distToTime(bestDist);
    }
    
    private void swapNodes(int[] nodes){
        int tmpN = nodes[0];
        int tmpD = nodes[1];
        nodes[0] = nodes[2];
        nodes[1] = nodes[3];
        nodes[2] = tmpN;
        nodes[3] = tmpD;
    }
    
    private int distToTime(double dist){
        return (int) Math.round(1000*(dist/speedMs));
    }
    
    private double getOsrmDistance(int node1, int node2){
        SimulationNode start = graph.getNode(node1);
        SimulationNode end = graph.getNode(node2);
        return getOsrmDistance(start.getLatitude(), start.getLongitude(),end.getLatitude(), end.getLongitude());
    }
    private double  getOsrmDistance(double startLat, double startLon, double endLat, double endLon) {
		String url = String.format("http://127.0.0.1:5000/route/v1/%s/%f,%f;%f,%f?geometries=geojson&overview=false&steps=false",
                                    "driving", startLon, startLat, endLon, endLat);
		//HttpClient httpClient = HttpClients.createDefault();
		JSONObject result = null;
		try {
			URIBuilder builder = new URIBuilder(url);
			URI uri = builder.build();
			HttpGet request = new HttpGet(uri);
			request.addHeader("accept", "application/json");
			HttpResponse response = httpClient.execute(request);
			result = new JSONObject(IOUtils.toString(response.getEntity().getContent()));
            JSONObject route =  (JSONObject)result.getJSONArray("routes").get(0);
            return Double.parseDouble(route.get("distance").toString());
		} catch (IOException | NumberFormatException | UnsupportedOperationException | URISyntaxException | JSONException ex){
			LOGGER.error("OSRM error: "+ex);
            return -1;
		}
     }

	// unused interface methods
	@Override
	public double getTravelTime(MovingEntity entity, SimulationNode positionA, SimulationNode positionB) {
        throw new UnsupportedOperationException("Not supported yet.");
	}
    
    @Override
	public double getTravelTime(SimulationNode positionA, SimulationNode positionB) {
        throw new UnsupportedOperationException("Not supported yet.");
	}

}
