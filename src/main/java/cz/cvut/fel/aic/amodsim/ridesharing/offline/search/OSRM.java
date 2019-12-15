/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.offline.search;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Olga Kholkovskaia
 */
public class OSRM {
	
	/**
	 * Finds way points nearest to the given location.
     * @param lat latitude
     * @param lon longitude
	 * @return A JSON object containing the response code, and an array of waypoint objects.
	 * 
	 */
	public JSONObject getNearestWaypoint(double lat, double lon) {
		String url = String.format("http://127.0.0.1:5000/nearest/v1/%s/%f,%f", "car", lon, lat);
        System.out.println(url);
		HttpClient httpClient = HttpClients.createDefault();
		JSONObject result = null;
		try {
			URIBuilder builder = new URIBuilder(url);
			HttpGet request = new HttpGet(builder.build());
			request.addHeader("accept", "application/json");
			HttpResponse response = httpClient.execute(request);
			result = new JSONObject(IOUtils.toString(response.getEntity().getContent()));
		} catch (IOException | UnsupportedOperationException | URISyntaxException | JSONException ex){
			System.out.println(ex.getMessage());
		}
        System.out.println(result);        
		return result;
	}
    
    /**
     * Returns distance of the shortest (fastest) possible route.
     * 
     * @param startLat start location latitude
     * @param startLon start location longitude
     * @param endLat end location latitude
     * @param endLon end location longitude
     * @return distance between location, meters.
     */
    public double getRoute(double startLat, double startLon, double endLat, double endLon) {
		String url = String.format("http://127.0.0.1:5000/route/v1/%s/%f,%f;%f,%f?geometries=geojson&overview=false&steps=false",
                                    "driving", startLon, startLat, endLon, endLat);
         //System.out.println(url);
		HttpClient httpClient = HttpClients.createDefault();
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
			System.out.println(ex.getMessage());
            return -1;
		}

}

    /**
     *
     * @param args
     */
    public static void main(String[] args){
        OSRM osrm = new OSRM();
        Map<String, Object> data;
        double dist = osrm.getRoute(59.433002,	24.744429, 59.438425, 24.72181);
        System.out.println(dist);
        System.out.println((1000*(dist/13.88)));
    }
}
