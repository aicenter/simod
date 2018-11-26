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
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.ConfigTaxify;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Graph;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;


@Singleton
public class TravelTimeProviderOSRM implements TravelTimeProvider{
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TravelTimeProviderOSRM.class); 
	private final ConfigTaxify config;
    private final Graph<SimulationNode, SimulationEdge> graph;
    private final double speedMs;
    int n;
   
	

	@Inject
	public TravelTimeProviderOSRM(ConfigTaxify config, TransportNetworks transportNetworks) {
		this.config = config;
        this.graph = transportNetworks.getGraph(EGraphType.HIGHWAY);
        speedMs = config.speed;
        n = graph.numberOfNodes();
	}
    
     

    @Override
    /**
     * Distance by SimulationNode ids.
     */
    public int getTravelTimeInMillis(Integer startId, Integer targetId) {
        double dist = getOsrmDistance(startId, targetId); 
        return distToTimeInMs(dist);
    }

    /**
     * Returns travel time for the trip from coordinates.
     * (without control of distance between real coordinates and assigned points on the map)
     * @param trip
     * @return 
     */
    @Override
    public int getTravelTimeInMillis(TripTaxify<GPSLocation> trip) {
        List<GPSLocation> locations = trip.getLocations();
        GPSLocation start = trip.getFirstLocation();
        GPSLocation end = locations.get(locations.size()-1);
        double dist = getOsrmDistance(start.getLatitude(),start.getLongitude(),end.getLatitude(),end.getLongitude());
        return distToTimeInMs(dist);
    }
       

    /**
     * Return best distance from combination of nodes.
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
        return distToTimeInMs(bestDist);
    }

    
    private void swapNodes(int[] nodes){
        int tmpN = nodes[0];
        int tmpD = nodes[1];
        nodes[0] = nodes[2];
        nodes[1] = nodes[3];
        nodes[2] = tmpN;
        nodes[3] = tmpD;
    }
    
    private double getOsrmDistance(double start_lat, double start_lon, double end_lat, double end_lon){
        String url = String.format("http://127.0.0.1:5000/route/v1/%s/%f,%f;%f,%f?geometries=geojson&overview=simplified&steps=false",
                                    "driving", start_lon, start_lat, end_lon, end_lat);
        HttpClient httpClient = HttpClients.createDefault();
		JSONObject result = null;
		try {
			URIBuilder builder = new URIBuilder(url);
			HttpGet request = new HttpGet(builder.build());
			request.addHeader("accept", "application/json");
			HttpResponse response = httpClient.execute(request);
			result = new JSONObject(IOUtils.toString(response.getEntity().getContent()));
		} catch (IOException | UnsupportedOperationException | URISyntaxException | JSONException ex){
			LOGGER.error("osrm request error: "+ex);
            return -1;
		}
        JSONArray array = result.getJSONArray("routes");
        JSONObject map = (JSONObject)array.get(0);
        double dist = (double) map.get("distance");
		return dist;
    }
    
    private double getOsrmDistance(Integer startId, Integer targetId) {
        SimulationNode start = graph.getNode(startId);
        SimulationNode target = graph.getNode(targetId);
     	return getOsrmDistance(start.getLatitude(), start.getLongitude(),   target.getLatitude(),  target.getLongitude()); 
    }

    private int distToTimeInMs(double dist){
        return (int) Math.round(1000*(dist/speedMs));
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
