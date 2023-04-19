package com.grivera.solver;


import com.grivera.generator.Network;
import com.grivera.generator.sensors.DataNode;
import com.grivera.generator.sensors.SensorNode;
import com.grivera.generator.sensors.StorageNode;
import com.grivera.util.Pair;

import java.util.List;

public class NetworkState {
    private int[] state;
    public NetworkState(Network network) {
        List<DataNode> dataNodes = network.getDataNodes();
        this.state = new int[dataNodes.size()];

        for (int index = 0; index < dataNodes.size(); index++) {
            this.state[index] = dataNodes.get(index).getUuid();
        }
    }

    private NetworkState(int[] state) {
        this.state = state;
    }

    public NetworkState update(NetworkJointAction actions) {
        int[] newState = new int[this.state.length];

        int index = 0;
        for (Pair<SensorNode, SensorNode> action : actions) {
            newState[index] = action.second().getUuid();
            index++;
        }
        return new NetworkState(newState);
    }
}
