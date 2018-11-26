/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.taxify.search;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jdk.nashorn.internal.parser.JSONParser;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class OSRM {
	
	/**
	 * Finds the nearest route to the given location.
     * @param lat
     * @param lon
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
//        System.out.println(result.get("waypoints"));
//        System.out.println(result.get("waypoints").getClass());
		return result;
	}
    
    public JSONObject getRoute(double startLat, double startLon, double endLat, double endLon) {
		String url = String.format("http://127.0.0.1:5000/route/v1/%s/%f,%f;%f,%f?geometries=geojson&overview=simplified&steps=false",
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
		} catch (Exception e){
			System.out.println(e.getMessage());
		}
		return result;

}

    public static void main(String[] args){
//59.4364307856	24.556601603	59.4386433725	24.7540305453

        OSRM osrm = new OSRM();
//        JSONObject data = osrm.getNearestWaypoint(59.433105, 24.74458);
        Map<String, Object> data;
        
        //"waypoints":[{"nodes":[1104932152,1104932297],"distance":0.12478267386325627,"location":[24.744581,59.433106]}]}:

        //System.out.println("CODE "+data.get("code"));
        //15502 (59.433002,	24.744429, 59.438425, 24.72181)
        JSONObject json = osrm.getRoute(59.433002,	24.744429, 59.438425, 24.72181);
        //data = json.toMap();
        JSONArray array = json.getJSONArray("routes");
        JSONObject map = (JSONObject)array.get(0);
        double dist = (double) map.get("distance");
 
        System.out.println((1000*(dist/13.88)));
    }
	

}
//        List wp = (List) data.get("waypoints");
//        for(Object el: wp){
//            System.out.println("  waypoint element is of type "+el.getClass());
//        }
         //    "{"routes":[{"geometry":{"coordinates":[[24.744586,59.433104],[24.744581,59.433106],[24.744226,59.433136],[24.743872,59.433167],[24.743626,59.433188],[24.743626,59.433187]],"type":"LineString"},"legs":[{"summary":"","weight":51.6,"duration":11.4,"steps":[],"distance":55.3}],"weight_name":"routability","weight":51.6,"duration":11.4,"distance":55.3}]"
       //      + ","location":[24.744586,59.433104]},"location":[24.743626,59.433187]}],"code":"Ok"}"
            
             //trip0 (59.4364307856,24.556601603,59.4386433725,24.7540305453);
            //osrm.getNearestWaypoint(59.433105, 24.74458);