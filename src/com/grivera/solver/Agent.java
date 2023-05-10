package com.grivera.solver;

import java.util.ArrayList;
import java.util.List;

import com.grivera.generator.sensors.SensorNode;

public class Agent {
    private SensorNode currLocation;
    private SensorNode originalLocation;
    private int packetValue;
    private SensorNode nextLocation;
    private int travelCost = 0;
    private boolean storedInStorage = false;
    private boolean storedInStorageNext = false;
    private List<SensorNode> route = new ArrayList<SensorNode>();

    public Agent() {
        // this.currLocation = currLocation;
    }

    public SensorNode getCurrentLocation() {
        return currLocation;
    }

    public SensorNode getOriginalLocation() {
        return originalLocation;
    }

    public SensorNode getNextLocation() {
        return nextLocation;
    }

    public void setNextLocation(SensorNode node) {
        this.nextLocation = node;
    }

    public void setOriginalLocation(SensorNode originalLocation) {
        this.originalLocation = originalLocation;
        //route.add(null);
        route.add(0, originalLocation);
    }

    public void setCurrentLocation(SensorNode currLocation) {
        this.currLocation = currLocation;
    }

    public void resetTravel() {
        travelCost = 0;
        storedInStorage = false;
        storedInStorageNext = false;
        route = new ArrayList<>();
        route.add(0, originalLocation);
    }

    public int getPacketValue() {
        return packetValue;
    }

    public void setPacketValue(int packetValue) {
        this.packetValue = packetValue;
    }

    public boolean getStoredInStorage() {
        return storedInStorage;
    }

    public void setStoredInStorage(boolean flag) {
        this.storedInStorage = flag;
    }

    public boolean getStoredInStorageNext() {
        return storedInStorageNext;
    }

    public void setStoredInStorageNext(){
        this.storedInStorageNext = true;
    }

    public int getTravelCost() {
        return travelCost;
    }

    public void setTravelCost(int travelCost) {
        this.travelCost = travelCost;
    }

    public void addToRoute() {
        if(route.isEmpty()){
            route.add(nextLocation);
            return;
        }
        if(!(route.get(route.size()-1)==nextLocation)){
            route.add(nextLocation);
        }
        
    }

    public List<SensorNode> getRoute() {
        return route;
    }

    public int calculateCostOfPath() {
        int totalCost=0;
        for (int i=1; i<route.size(); i++) {
            SensorNode prevNode = route.get(i-1);
            SensorNode currNode = route.get(i);

            totalCost+=prevNode.calculateTransmissionCost(currNode);
            totalCost+=currNode.calculateReceivingCost();
        }
        return totalCost;
    }
}
