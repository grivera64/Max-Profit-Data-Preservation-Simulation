package com.grivera.solver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.grivera.generator.sensors.SensorNode;

public class Agent {
    private SensorNode currLocation;
    private SensorNode originalLocation;
    private SensorNode lastStateLocation;
    private long packetValue;
    private SensorNode nextLocation;
    private long travelCost = 0;
    private double rewardCol = 0;
    private boolean storedInStorage = false;
    private boolean storedInStorageNext = false;
    private List<SensorNode> route = new ArrayList<SensorNode>();
    Map<Integer, Double> distToOthers = new HashMap<>();

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
        rewardCol = 0;
        storedInStorage = false;
        storedInStorageNext = false;
        route = new ArrayList<>();
        route.add(0, originalLocation);
    }

    public long getPacketValue() {
        return packetValue;
    }

    public void setPacketValue(long packetValue) {
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

    public long getTravelCost() {
        return travelCost;
    }

    public void setTravelCost(long travelCost) {
        this.travelCost = travelCost;
    }
    public double getRewardCol() {
        return rewardCol;
    }
    public void setRewardCol(double rewardCol) {
        this.rewardCol = rewardCol;
    }

    public void addToRoute() {
        if (route.isEmpty()) {
            route.add(nextLocation);
            return;
        }
        if (!(route.get(route.size() - 1) == nextLocation)) {
            route.add(nextLocation);
        }
        
    }

    public List<SensorNode> getRoute() {
        return route;
    }

    public int calculateCostOfPath() {
        int totalCost = 0;
        for (int i = 1; i < route.size(); i++) {
            SensorNode prevNode = route.get(i - 1);
            SensorNode currNode = route.get(i);

            totalCost += prevNode.calculateTransmissionCost(currNode);
            totalCost += currNode.calculateReceivingCost();
        }
        return totalCost;
    }

    public void resetLocation() {
        this.setCurrentLocation(this.getOriginalLocation());
    }

    public SensorNode getLastStateLocation() {
        return lastStateLocation;
    }

    public void setLastStateLocation(SensorNode node) {
        lastStateLocation=node;
    }
}
