package com.example.jotunn.slwear;

/**
 * Created by Jotunn on 2015-05-02.
 */
public class TravelEvent {
    String lineNumber;
    String location;
    String departureTime;
    String type;

    public TravelEvent() {
    }

    @Override
    public String toString() {
        return lineNumber + "@" + location + "@" + departureTime + "@" + type;
    }

    public TravelEvent(String lineNumber, String location, String departureTime, String type) {
    }

    public TravelEvent(String serializedString) {
        String [] values = serializedString.split("@");
        this.lineNumber = values[0];
        this.location = values[1];
        this.departureTime = values[2];
        this.type = values[3];
    }
    public String getLineNumber() {
        return lineNumber;
    }

    public String getLocation() {
        return location;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public String getType() {
        return type;
    }

    public String getDestination() {
        return location;
    }

    public String getTime() {
        return departureTime;
    }
}
