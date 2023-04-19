package com.grivera.solver;

import com.grivera.generator.sensors.SensorNode;
import com.grivera.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class NashAgent {
    private static final Map<Pair<NetworkState, NetworkJointAction>, Integer> qMap = new HashMap<>();
    private final List<SensorNode> path = new ArrayList<>();
    private int id;
    private SensorNode startingNode;
    private SensorNode currentNode;
    private int travelCost;

    public NashAgent(int id, SensorNode startingNode) {
        this.id = id;
        this.startingNode = startingNode;
        this.reset();
    }

    public String toString() {
        String pathString = this.path.stream()
                .map(SensorNode::getName)
                .collect(Collectors.joining(", ", "[", "]"));

        return String.format("Agent %02d is at %s (Path: %s)", this.id, this.currentNode.getName(), pathString);
    }

    public void reset() {
        this.currentNode = this.startingNode;
        this.path.clear();
        this.path.add(startingNode);

        this.travelCost = 0;
    }

    public List<SensorNode> getPath() {
        return Collections.unmodifiableList(this.path);
    }

    public int getTravelCost() {
        return this.travelCost;
    }

    public SensorNode getCurrentNode() {
        return this.currentNode;
    }
}
