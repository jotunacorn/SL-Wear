/**
 * Created by Jotunn on 2015-02-13.
 */
package com.example.jotunn.slwear;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemAsset;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Map;
import java.text.DecimalFormat;
import android.location.Location;

public class DataRetreiver extends WearableListenerService implements SharedPreferences.OnSharedPreferenceChangeListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private GoogleApiClient mGoogleApiClient;
    MessageEvent lastMessage = null;
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        PreferenceManager.setDefaultValues(this, R.xml.advanced_preferences, false);
        lastMessage = messageEvent;
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        if (!mGoogleApiClient.isConnected()) {
            System.out.println("Connecting to API");
            mGoogleApiClient.connect();
        }
        else{
            String path = messageEvent.getPath();
            if(path.equals("GETDATA")) {
                Toast.makeText(getApplicationContext(), "Receiving dataa!",
                        Toast.LENGTH_LONG).show();
                String requestString = new String(lastMessage.getData());
                new GetDepartures(requestString).execute();
            }
            else if(path.equals("GETLOCATIONS")){

                LocationRequest mLocationRequest = new LocationRequest();
                mLocationRequest.setInterval(10000);
                mLocationRequest.setFastestInterval(500);
                mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            }
        }


    }



    @Override
    public void onConnected(Bundle bundle) {
        System.out.println("Connected");
        if(lastMessage != null) {
            String path = lastMessage.getPath();
            if (path.equals("GETDATA")) {
                Toast.makeText(getApplicationContext(), "Receiving dataa!",
                        Toast.LENGTH_LONG).show();
                String requestString = new String(lastMessage.getData());
                new GetDepartures(requestString).execute();
            } else if (path.equals("GETLOCATIONS")) {

                LocationRequest mLocationRequest = new LocationRequest();
                mLocationRequest.setInterval(10000);
                mLocationRequest.setFastestInterval(500);
                mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            } else if (path.equals("GETSTOPID")) {

            }
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location mLastLocation) {
        if(mLastLocation != null) {
            Toast.makeText(getApplicationContext(), "Getting nearby stops from location " + mLastLocation.getLatitude() + " ; " + mLastLocation.getLongitude() + "!",
                    Toast.LENGTH_LONG).show();
            new GetLocations(mLastLocation, 800).execute();
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
        }
        else{
            Toast.makeText(getApplicationContext(), "Couldn't find a location!",
                    Toast.LENGTH_LONG).show();
        }
    }

    private class GetLocations extends  AsyncTask<String, String, String>{
        Location location;
        int radius;
        LinkedList<String> locations;
        public GetLocations(Location location, int radius){
            this.location = location;
            this.radius = radius;
            locations = new LinkedList<String>();

        }
        @Override
        protected String doInBackground(String... params) {
            String urlString = "https://api.trafiklab.se/samtrafiken/resrobot/StationsInZone.xml?apiVersion=2.1&centerX="+ String.format( "%.8f",location.getLongitude()) + "&centerY="+ String.format( "%.8f",location.getLatitude()) + "&radius="+ radius +"&coordSys=WGS84&key=<API KEY>";

            String resultToDisplay = "";
            InputStream in = null;
            System.out.println("Getting XML at " + urlString);
            // HTTP Get
            try {
                URL url = new URL(urlString);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                in = new BufferedInputStream(urlConnection.getInputStream());
            } catch (Exception e ) {
                System.out.println(e.getMessage());
                return e.getMessage();
            }
            System.out.println("Got XML. Starting parsing");

            // Parse XML
            XmlPullParserFactory pullParserFactory = null;
            String result = null;
            try {
                try {
                    pullParserFactory = XmlPullParserFactory.newInstance();
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                }
                XmlPullParser parser = pullParserFactory.newPullParser();
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                parser.setInput(in, null);
                parseXML(parser);
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Simple logic to determine if the email is dangerous, invalid, or valid
            return "";
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            String allLocations = "";
            for(String location : locations){
                allLocations = allLocations + location + "@";
            }
            byte [] rawData = allLocations.getBytes();
            sendMessageToWearable("AVAILABLE_LOCATIONS", rawData);
        }
        private void parseXML( XmlPullParser parser ) throws XmlPullParserException, IOException {
            int eventType = parser.getEventType();
            while( eventType!= XmlPullParser.END_DOCUMENT) {
                if(eventType == XmlPullParser.START_TAG) {
                    String tag = parser.getName();
                    if(tag.equals("location")){
                        String location = getNextEvent(parser);
                        System.out.println("Found location " + location);
                        locations.add(location);
                    }
                }
                eventType = parser.next();
            } // end while

        }
        private String getNextEvent(XmlPullParser parser) throws IOException, XmlPullParserException {
            int eventType = parser.next();
            String found = "NOT SET";
            while(true){
                if(eventType == XmlPullParser.END_TAG && parser.getName()!= null && parser.getName().equals("location")) {
                    break;
                }
                    if(parser.getName() != null) {
                        if (eventType == XmlPullParser.START_TAG && parser.getName().equals("name")) {
                            parser.next();
                            found = parser.getText();
                            break;
                        }
                    }
                    eventType = parser.next();
                }
            if(found.equals("NOT SET"))
                return null;
            else
                return found;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

    }

    private class GetDepartures extends AsyncTask<String, String, String> {
        SharedPreferences prefs;
        LinkedList<TravelEvent> rides = new LinkedList<TravelEvent>();
        String station;
        public GetDepartures(String station){
            this.station = Uri.encode(station);
        }
        @Override
        protected String doInBackground(String... params) {
            //Get station ID
            String urlString = "http://api.sl.se/api2/typeahead.xml?key=<API KEY>&searchstring=" + station;
            String stationID = "";
            InputStream in = null;
            System.out.println("Getting XML at " + urlString);
            // HTTP Get
            try {
                URL url = new URL(urlString);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                in = new BufferedInputStream(urlConnection.getInputStream());
            } catch (Exception e ) {
                System.out.println(e.getMessage());
                return e.getMessage();
            }
            System.out.println("Got XML. Starting parsing");

            // Parse XML
            XmlPullParserFactory pullParserFactory = null;
            String result = null;
            try {
                try {
                    pullParserFactory = XmlPullParserFactory.newInstance();
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                }
                XmlPullParser parser = pullParserFactory.newPullParser();
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                parser.setInput(in, null);
                result = parseXMLID(parser);
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            stationID = result;
            // Simple logic to determine if the email is dangerous, invalid, or valid
            if (result == null ) {
                stationID = "Exception Occured";
            }

            System.out.println("The station ID for name " + station + " is " + stationID);
            //Get realtime info
             prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            urlString="http://api.sl.se/api2/realtimedepartures.xml?key=<API KEY>&siteid="+stationID+"&timewindow="+prefs.getString("timeWindow","60");
            String resultToDisplay = "";
            in = null;
            System.out.println("Getting XML at " + urlString);
            // HTTP Get
            try {
                URL url = new URL(urlString);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                in = new BufferedInputStream(urlConnection.getInputStream());
            } catch (Exception e ) {
                System.out.println(e.getMessage());
                return e.getMessage();
            }
            System.out.println("Got XML. Starting parsing");

            // Parse XML
            pullParserFactory = null;
            result = null;
            try {
                try {
                    pullParserFactory = XmlPullParserFactory.newInstance();
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                }
                XmlPullParser parser = pullParserFactory.newPullParser();
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                parser.setInput(in, null);
                result = parseXML(parser);
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            resultToDisplay = result;
            // Simple logic to determine if the email is dangerous, invalid, or valid
            if (result == null ) {
                resultToDisplay = "Exception Occured";
            }
            return resultToDisplay;

        }

        protected void onPostExecute(String result) {
            TravelEvent [] foundRides = rides.toArray(new TravelEvent[rides.size()]);
            System.out.println("The input array has length " + rides.size());
            Arrays.sort(foundRides, new TimeComparator());
            System.out.println("The sorted array has length " + foundRides.length);
            Toast.makeText(getApplicationContext(), "Making request",
                    Toast.LENGTH_LONG).show();
            PutDataMapRequest request = PutDataMapRequest.create("/travelData");
            DataMap reply = request.getDataMap();
            for(int i = 0; i<foundRides.length; i++)
                reply.putString(i+"",foundRides[i].toString());
            byte [] rawData = reply.toByteArray();
            Toast.makeText(getApplicationContext(), "Returning request",
                    Toast.LENGTH_LONG).show();
            sendMessageToWearable("DATARETURN", rawData);
        }
        private String parseXMLID(XmlPullParser parser)throws XmlPullParserException, IOException {
            int eventType = parser.getEventType();
            String result ="";

            while( eventType!= XmlPullParser.END_DOCUMENT) {
                if(eventType == XmlPullParser.START_TAG) {
                    String tag = parser.getName();
                    if(tag.equals("SiteId")){
                        parser.next();
                        result = parser.getText();
                        break;
                    }
                }
                eventType = parser.next();
            } // end while
            return result;
        }

        private String parseXML( XmlPullParser parser ) throws XmlPullParserException, IOException {
            int eventType = parser.getEventType();
            String result ="";

            while( eventType!= XmlPullParser.END_DOCUMENT) {
                if(eventType == XmlPullParser.START_TAG) {
                    String tag = parser.getName();
                    if(tag.equals("Metros")|| tag.equals("Buses")|| (tag.equals("Trains"))
                            || (tag.equals("Trams")) || (tag.equals("Ships"))){
                        TravelEvent nextEvent = getNextEvent(parser);
                        while(nextEvent != null){
                            rides.add(nextEvent);
                            System.out.println("Added event " + nextEvent.toString());
                            nextEvent = getNextEvent(parser);
                        }
                    }
                }
                eventType = parser.next();
            } // end while

            return result;
        }
        private TravelEvent getNextEvent(XmlPullParser parser) throws IOException, XmlPullParserException {
            int eventType;
            eventType = parser.next();
            String tag = parser.getName();
            TravelEvent found = new TravelEvent();
            if(tag != null && (tag.equals("Metro") || tag.equals("Bus") || tag.equals("Train") || tag.equals("Tram") || tag.equals("Ship"))){
                eventType = parser.next();
                while(true){
                    if(eventType == XmlPullParser.END_TAG && parser.getName()!= null && (parser.getName().equals("Metro") || parser.getName().equals("Bus") || parser.getName().equals("Train") || parser.getName().equals("Tram") || parser.getName().equals("Ship")))
                        break;
                    if(parser.getName() != null) {
                        if (eventType == XmlPullParser.START_TAG && parser.getName().equals("TransportMode")) {
                            parser.next();
                            found.setType(parser.getText());
                        }
                        else if (eventType == XmlPullParser.START_TAG && parser.getName().equals("LineNumber")) {
                            parser.next();
                            found.setLineNumber(parser.getText());
                        }
                        else if (eventType == XmlPullParser.START_TAG && parser.getName().equals("Destination")) {
                            parser.next();
                            found.setDestination(parser.getText());
                        }
                        else if (eventType == XmlPullParser.START_TAG && parser.getName().equals("DisplayTime")) {
                            parser.next();
                            found.setTime(parser.getText());
                        }
                    }
                    eventType = parser.next();
                }
            }
            if(found.getType().equals("NOT SET"))
                return null;
            else
                return found;
        }
        class TimeComparator implements Comparator<TravelEvent> {
            @Override
            public int compare(TravelEvent a, TravelEvent b) {

                String timeOfA = a.getTime();
                String timeOfB = b.getTime();
                if(timeOfA.length() <3 && timeOfB.length() < 3)
                    return 0;
                else if(timeOfA.length()<3)
                    return -1;
                else if(timeOfB.length()<3)
                    return 1;
                if(timeOfA.substring(timeOfA.length() - 3).equals("min") && timeOfB.substring(timeOfB.length() - 3).equals("min") ){
                    String [] temp = timeOfA.split(" ");
                    int nrA = Integer.parseInt(temp[0]);
                    temp = timeOfB.split(" ");
                    int nrB = Integer.parseInt(temp[0]);
                    if(nrA<nrB)
                        return -1;
                    if(nrA == nrB)
                        return 0;
                    if(nrA > nrB)
                        return 1;
                }
                else if(timeOfA.substring(timeOfA.length() - 3).equals("min") && !timeOfB.substring(timeOfB.length() - 3).equals("min")){
                    return -1;
                }
                else if(!timeOfA.substring(timeOfA.length() - 3).equals("min") && timeOfB.substring(timeOfB.length() - 3).equals("min")){
                    return 1;
                }
                else{
                    timeOfA = timeOfA.replace(":","");
                    timeOfB = timeOfB.replace(":","");
                    return timeOfA.compareTo(timeOfB);
                }
                return -1;
            }
        }


    } // end CallAPI
    private void sendMessageToWearable(final String path, final byte[] data) {
        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(
                new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onResult(NodeApi.GetConnectedNodesResult nodes) {
                        for (Node node : nodes.getNodes()) {
                            Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), path, data);
                        }
                    }
                });
    }

}
