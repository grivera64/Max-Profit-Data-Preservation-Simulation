package com.grivera.solver;

//import com.google.common.collect.Sets;
import com.grivera.generator.Network;
import com.grivera.generator.SensorNetwork;
import com.grivera.generator.sensors.DataNode;
import com.grivera.generator.sensors.SensorNode;
import com.grivera.generator.sensors.StorageNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class NashQModel extends AbstractModel {

    private final int EPISODES = 1;

    public NashQModel(Network network) {
        super(network);
    }

    public NashQModel(String fileName) {
        super(fileName);
    }

    public NashQModel(String fileName, int overflowPackets, int storageCapacity) {
        super(fileName, overflowPackets, storageCapacity);
    }

    @Override
    public void run() {
        super.run();
        Network network = this.getNetwork();
        List<DataNode> dns = network.getDataNodes();

        int cost = Integer.MAX_VALUE;
        int minCost = Integer.MAX_VALUE;

        List<NashAgent> agents = new ArrayList<>(dns.size());
        for (int i = 0; i < dns.size(); i++) {
            agents.add(new NashAgent(i + 1, dns.get(i)));
            System.out.printf("DEBUG: %s\n", agents.get(i));
        }

        /* Initial State of the network (Agents at Data Nodes) */
        NetworkState currState = new NetworkState(network);

        for (int episode = 0; episode < EPISODES; episode++) {
            for (NashAgent agent : agents) {
                agent.reset();
            }

            /* Until all agents reach an empty storage node */
            while (!agents.stream().allMatch(NashAgent::isDone)) {
                // TODO: Training process tp reach an unoccupied SNs
            }

            // TODO: Update the reward if there is a new minimum cost

        }


    }

    private Set<SensorNode> getAgentActions(SensorNode curr) {
        return ((SensorNetwork) this.getNetwork()).getNeighbors(curr);
    }

    private NetworkJointAction getExploitationActions(List<NashAgent> agents) {
        // TODO: Useful function Sets.cartesianProduct() from Google's Guava
        return null;
    }

    @Override
    public int getTotalCost() {
        super.getTotalCost();
        // TODO: Traverse the Q-table and calculate the cost of the best path
        return -1;
    }

    @Override
    public int getTotalProfit() {
        super.getTotalProfit();
        // TODO: Traverse the Q-table and calculate the profit of the best path
        return -1;
    }
}
