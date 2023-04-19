package com.grivera.solver;

import com.grivera.generator.Network;
import com.grivera.generator.sensors.DataNode;
import com.grivera.generator.sensors.SensorNode;
import com.grivera.generator.sensors.StorageNode;

import java.util.ArrayList;
import java.util.List;

public class NashQModel extends AbstractModel {

    private final int EPISODES = 1;

    private int totalCost;
    private int totalProfit;

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

        this.totalCost = 0;
        this.totalProfit = 0;

        List<NashAgent> agents = new ArrayList<>(dns.size());
        for (int i = 0; i < dns.size(); i++) {
            agents.add(new NashAgent(i + 1, dns.get(i)));
            System.out.printf("DEBUG: %s\n", agents.get(i));
        }

        NetworkState currState = new NetworkState(network);

        for (int episode = 0; episode < EPISODES; episode++) {
            for (NashAgent agent : agents) {
                agent.reset();
            }

            // TODO

        }


    }

    @Override
    public int getTotalCost() {
        super.getTotalCost();
        return this.totalCost;
    }

    @Override
    public int getTotalProfit() {
        super.getTotalProfit();
        return this.totalProfit;
    }
}
