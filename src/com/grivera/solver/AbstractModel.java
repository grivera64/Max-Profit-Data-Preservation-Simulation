package com.grivera.solver;

import com.grivera.generator.sensors.DataNode;
import com.grivera.generator.Network;
import com.grivera.generator.SensorNetwork;

public abstract class AbstractModel implements Model {
    private final Network network;
    private boolean hasRan;

    public AbstractModel(String fileName) {
        this(SensorNetwork.from(fileName));
    }

    public AbstractModel(String fileName, int overflowPackets, int storageCapacity) {
        this(SensorNetwork.from(fileName, overflowPackets, storageCapacity));
    }

    public AbstractModel(Network network) {
        this.network = network;
        this.hasRan = false;
    }

    @Override
    public void run() {
        this.hasRan = true;
        this.network.resetPackets();
    }

    public void run(int episodes) {
        if (episodes < 1) {
            throw new IllegalArgumentException("Episodes count cannot be negative!");
        }
        this.hasRan = true;
        this.network.resetPackets();
    }

    @Override
    public long getTotalCost() {
        if (!this.hasRan) {
            throw new IllegalStateException("Cannot get the total cost before running the model!");
        }
        return -1;
    }

    @Override
    public long getTotalProfit() {
        if (!this.hasRan) {
            throw new IllegalStateException("Cannot get the total profit before running the model!");
        }
        return -1;
    }

    @Override
    public long getTotalValue() {
        if (!this.hasRan) {
            throw new IllegalStateException("Cannot get the total value before running the model!");
        }
        return -1;
    }

    @Override
    public void printRoute() {
        if (!this.hasRan()) {
            throw new IllegalStateException("Cannot print the route before running the model!");
        }
        return;
    }

    public final Network getNetwork() {
        return this.network;
    }

    protected boolean overflowPacketsRemain() {
        for (DataNode dn : this.network.getDataNodes()) {
            if (!dn.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    public boolean hasRan() {
        return this.hasRan;
    }
}
