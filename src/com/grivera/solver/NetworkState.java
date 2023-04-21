package com.grivera.solver;


import com.grivera.generator.Network;
import com.grivera.generator.sensors.DataNode;
import com.grivera.generator.sensors.SensorNode;
import com.grivera.generator.sensors.StorageNode;
import com.grivera.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class NetworkState {
    private List<Integer> state;
    public NetworkState(Network network) {
        List<DataNode> dataNodes = network.getDataNodes();
        this.state = new ArrayList<>();

        for (int index = 0; index < dataNodes.size(); index++) {
            this.state.add(dataNodes.get(index).getUuid());
        }
    }

    private NetworkState(List<Integer> state) {
        this.state = state;
    }

    public NetworkState update(NetworkJointAction actions) {
        List<Integer> newState = new ArrayList<>();
        for (int i = 0; i < actions.size(); i++) {
            newState.add(-1);
        }

        for (Pair<SensorNode, SensorNode> action : actions) {
            newState.add(action.second().getUuid());
        }
        return new NetworkState(newState);
    }
}
