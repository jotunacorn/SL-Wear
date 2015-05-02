package com.example.jotunn.slwear;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.support.wearable.view.DismissOverlayView;
import android.support.wearable.view.WatchViewStub;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends Activity implements DataApi.DataListener, MessageApi.MessageListener {
    public static String lastLocation = "";
    private static final int LOCATION_LENGTH = 15;
    private static final int HEADER_LENGTH = 13;
    private RelativeLayout mRectBackground;
    private RelativeLayout mRoundBackground;
    private final int textColor = Color.BLACK;
    private GestureDetectorCompat mGestureDetector;
    private DismissOverlayView mDismissOverlayView;
    StableArrayAdapter adapter = null;
    UpdateDataService listener = new UpdateDataService();
    GoogleApiClient mGoogleApiClient;
    @Override
    protected void onStart() {
        super.onStart();
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }
    @Override
    protected void onStop() {
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        // Tell the wearable to end the quiz (counting unanswered questions as skipped), and then
        // disconnect mGoogleApiClient.
        super.onStop();
    }
    private void sendMessageToHandHeld(final String path, final byte[] data) {
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

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mRectBackground = (RelativeLayout) findViewById(R.id.rect_layout);
                mRoundBackground = (RelativeLayout) findViewById(R.id.round_layout);
            }
        });
        mGestureDetector = new GestureDetectorCompat(this, new LongPressListener());
        final ArrayList<TravelEvent> list = new ArrayList<TravelEvent>();
        adapter = new StableArrayAdapter(this,
                android.R.layout.simple_list_item_1, list);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(listener)
                .addOnConnectionFailedListener(listener)
                .build();

        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);

        /*
        String message = "Ripstigen";
        sendMessageToHandHeld("GETDATA", message.getBytes());
        if (mGoogleApiClient.isConnected()) {
            System.out.println("Connected to wear API");
        }
        */

    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        String path = messageEvent.getPath();
        System.out.println("Received messag with path " + path);
        if(path.equals("DATARETURN")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Button b = (Button)findViewById(R.id.button);
                    b.setText(lastLocation);
                }
            });

            final ArrayList<TravelEvent> list = new ArrayList<TravelEvent>();
            adapter = new StableArrayAdapter(this,
                    android.R.layout.simple_list_item_1, list);
            DataMap values = DataMap.fromByteArray(messageEvent.getData());
            LinkedList<TravelEvent> rides = new LinkedList<>();
            int i = 0;
            String aRide = values.getString(i + "");
            while(aRide != null){
                rides.add(new TravelEvent(aRide));
                System.out.println("adding ride " + aRide);
                i++;
                aRide = values.getString(i + "");
            }
            rides.add(new TravelEvent("","","",""));
            final LinkedList<TravelEvent> ridesCopy = rides;

            adapter.addAll(ridesCopy);
            if(mRectBackground != null) {
                MainActivity.this.mRectBackground.post(new Runnable() {
                    public void run() {
                        adapter.notifyDataSetChanged();
                        ListView listview = null;
                        listview = (ListView) mRectBackground.findViewById(R.id.rides);
                        mRectBackground.findViewById(R.id.progressBar).setVisibility(View.GONE);
                        listview.setAdapter(adapter);
                    }
                });
            }
            if(mRoundBackground != null){
                MainActivity.this.mRoundBackground.post(new Runnable() {
                    public void run() {
                        adapter.notifyDataSetChanged();
                        ListView listview = null;
                        listview = (ListView) mRoundBackground.findViewById(R.id.rides);
                        mRoundBackground.findViewById(R.id.progressBar).setVisibility(View.GONE);
                        listview.setAdapter(adapter);
                    }
                });
            }

        }
        else if(path.equals("AVAILABLE_LOCATIONS")){
            String locationsData = new String(messageEvent.getData());
            System.out.println("Received the available locations:" + locationsData);
            String [] locations = locationsData.split("@");
            LinkedList<TravelEvent> rides = new LinkedList<TravelEvent>();
            for(int i = 0; i<locations.length; i++){
                TravelEvent anEvent = new TravelEvent("", locations[i], "", "");
                rides.add(anEvent);
                System.out.println("Added the event " + anEvent.toString());
            }
            rides.add(new TravelEvent("","","",""));
            adapter.addAll(rides);
            if(mRectBackground != null) {
                MainActivity.this.mRectBackground.post(new Runnable() {
                    public void run() {
                        adapter.notifyDataSetChanged();
                        ListView listview = null;
                        listview = (ListView) mRectBackground.findViewById(R.id.rides);
                        mRectBackground.findViewById(R.id.progressBar).setVisibility(View.GONE);
                        listview.setAdapter(adapter);
                    }
                });
            }
            if(mRoundBackground != null){
                MainActivity.this.mRoundBackground.post(new Runnable() {
                    public void run() {
                        adapter.notifyDataSetChanged();
                        ListView listview = null;
                        listview = (ListView) mRoundBackground.findViewById(R.id.rides);
                        mRoundBackground.findViewById(R.id.progressBar).setVisibility(View.GONE);
                        listview.setAdapter(adapter);
                    }
                });
            }
        }
    }

    private class StableArrayAdapter extends ArrayAdapter<TravelEvent> {

        int lastIndex = 0;
        LinkedList<TravelEvent> rides = new LinkedList<TravelEvent>();
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.travel_item, parent, false);
            final TextView destination = (TextView) view.findViewById(R.id.Destination);
            destination.setTextColor(textColor);
            final TextView time = (TextView) view.findViewById(R.id.TimeLeft);
            time.setTextColor(textColor);
            if(rides.get(position) != null && rides.get(position).getLineNumber().equals("") && rides.get(position).getType().equals("")){ // It's just a location
                destination.setText(rides.get(position).getDestination());
                time.setText("");
                view.setOnClickListener(new ListClick());
            }
            else if(rides.get(position) != null && position<lastIndex) { //It's a complete travel event
                String destinationString = rides.get(position).getDestination();
                String lineNumber = rides.get(position).getLineNumber();
                if(destinationString.length() > LOCATION_LENGTH){
                    destinationString = destinationString.substring(0, LOCATION_LENGTH-3);
                    destinationString = destinationString + "...";
                }

                String lineToSet = lineNumber + " | " + destinationString;
                if(lineToSet.length()<4)
                    lineToSet=" ";
                destination.setText(lineToSet);
                time.setText(rides.get(position).getTime());

            }
            return view;
        }
        HashMap<TravelEvent, Integer> mIdMap = new HashMap<TravelEvent, Integer>();

        public StableArrayAdapter(Context context, int textViewResourceId,
                                  List<TravelEvent> objects) {
            super(context, textViewResourceId, objects);
            for (int i = 0; i < objects.size(); ++i) {
                rides.add(new TravelEvent("","","",""));
                mIdMap.put(objects.get(i), i);
                lastIndex++;
            }
        }


        @Override
        public void clear() {
            super.clear();
            lastIndex = 0;
            rides.clear();
            mIdMap.clear();
        }

        @Override
        public void addAll(Collection<? extends TravelEvent> collection) {
            super.addAll(collection);
            Iterator<? extends TravelEvent> iterator= collection.iterator();
            while(iterator.hasNext()){
                TravelEvent event = iterator.next();
                mIdMap.put(event, lastIndex);
                rides.add(event);
                System.out.println("added :" + event.toString() + " to i " + lastIndex);
                lastIndex++;
            }
        }

/*        @Override
        public long getItemId(int position) {
            TravelEvent item = getItem(position);
            return mIdMap.get(item);
        }*/

        @Override
        public boolean hasStableIds() {
            return false;
        }
    }

    /**
     * Animates the layout when clicked. The animation used depends on whether the
     * device is round or rectangular.
     */
    public void onLayoutClicked(View view) {/*
        if (mRectBackground != null) {
            ScaleAnimation scaleAnimation = new ScaleAnimation(1.0f, 0.7f, 1.0f, 0.7f,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            scaleAnimation.setDuration(300);
            scaleAnimation.setRepeatCount(1);
            scaleAnimation.setRepeatMode(Animation.REVERSE);
            mRectBackground.startAnimation(scaleAnimation);
        }
        if (mRoundBackground != null) {
            mRoundBackground.animate().rotationBy(360).setDuration(300).start();
        }*/
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event) || super.dispatchTouchEvent(event);
    }

    private class LongPressListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public void onLongPress(MotionEvent event) {
            mDismissOverlayView.show();
        }
    }
    private class ListClick implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            final TextView destination = (TextView) v.findViewById(R.id.Destination);
            System.out.println("The destination " + destination.getText() + " was pressed.");
            String message = destination.getText().toString();
            if(message.length()>HEADER_LENGTH){
                MainActivity.lastLocation = message.substring(0, HEADER_LENGTH-3) + "...";
            }
            else {
                MainActivity.lastLocation = message;
            }
            sendMessageToHandHeld("GETDATA", message.getBytes());
        }
    }

    private class UpdateDataService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnected(Bundle bundle) {
            String message = "_";
            sendMessageToHandHeld("GETLOCATIONS", message.getBytes());
        }

        @Override
        public void onConnectionSuspended(int i) {

        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {

        }
    }
}