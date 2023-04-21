package com.grivera.solver;

import com.grivera.generator.sensors.SensorNode;
import com.grivera.util.Pair;

import java.util.*;

public class NetworkJointAction implements Iterable<Pair<SensorNode, SensorNode>> {
    private final List<Pair<SensorNode, SensorNode>> actions;

    public NetworkJointAction(Pair<SensorNode, SensorNode> ... actions) {
        this.actions = new ArrayList<>(Arrays.asList(actions));
    }

    public int hashCode() {
        return this.actions.hashCode();
    }

    public Pair<SensorNode, SensorNode> getAgentAction(int agentId) {
        return this.actions.get(agentId - 1);
    }

    public int size() {
        return this.actions.size();
    }

    @Override
    public Iterator<Pair<SensorNode, SensorNode>> iterator() {
        return Collections.unmodifiableList(this.actions).iterator();
    }
}
