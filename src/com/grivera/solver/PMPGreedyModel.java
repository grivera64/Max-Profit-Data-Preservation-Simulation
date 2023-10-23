package com.grivera.solver;

import com.grivera.generator.Network;
import com.grivera.generator.sensors.DataNode;
import com.grivera.generator.sensors.SensorNode;
import com.grivera.generator.sensors.StorageNode;
import com.grivera.util.Tuple;

import java.util.*;

public class PMPGreedyModel extends AbstractModel {

    private final Map<SensorNode, List<Tuple<StorageNode, Integer, List<SensorNode>>>> routes = new HashMap<>();
    private int totalValue;
    private int totalCost;
    private int totalProfit;

    public PMPGreedyModel(Network network) {
        super(network);
    }

    public PMPGreedyModel(String fileName) {
        super(fileName);
    }

    public PMPGreedyModel(String fileName, int overflowPackets, int storageCapacity) {
        super(fileName, overflowPackets, storageCapacity);
    }

    public void run(int episodes) {
        System.out.println("Warning: Ignoring episodes count; defaulting to 1...");
        this.run();
    }

    @Override
    public void run() {
        super.run();
        this.totalValue = 0;
        this.totalCost = 0;
        this.totalProfit = 0;

        Network network = this.getNetwork();
        StorageNode chosenSn;
        int chosenProfit = Integer.MIN_VALUE;
        int packetsToSend;
        int cost;

        network.resetPackets();
        int currProfit;
        int currPacketsToSend;
        boolean foundBetterProfit;
        for (DataNode dn : network.getDataNodes()) {
            while (!dn.isEmpty()) {
                chosenSn = null;
                packetsToSend = -1;
                for (StorageNode sn : network.getStorageNodes()) {
                    if (sn.isFull()) {
                        continue;
                    }

                    currProfit = network.calculateProfitOf(dn, sn);
                    currPacketsToSend = Math.min(dn.getPacketsLeft(), sn.getSpaceLeft());

                    /* Check if we can choose a better SN */
                    foundBetterProfit = chosenSn == null || currProfit > chosenProfit;
                    if (network.canSendPackets(dn, sn, currPacketsToSend) && foundBetterProfit) {
                        chosenSn = sn;
                        chosenProfit = currProfit;
                        packetsToSend = currPacketsToSend;
                    }
                }

                /* Preserve all the packets that the node can */
                if (chosenProfit > 0 && chosenSn != null) {
                    cost = network.calculateMinCost(dn, chosenSn);
//                    System.out.printf("%s [%d] -> %s [%d] (%d packets, each with %d cost, %d profit)\n",
//                            dn.getName(), dn.getUuid(), chosenSn.getName(), chosenSn.getUuid(),
//                            packetsToSend, cost, chosenProfit
//                    );
                    network.sendPackets(dn, chosenSn, packetsToSend);

                    this.totalCost += cost * packetsToSend;
                    this.totalProfit += chosenProfit * packetsToSend;
                    this.totalValue += dn.getOverflowPacketValue() * packetsToSend;

                    routes.putIfAbsent(dn, new ArrayList<>());
                    routes.get(dn).add(Tuple.of(chosenSn, packetsToSend, network.getMinCostPath(dn, chosenSn)));
                /* Discard all packets */
                } else {
//                    System.out.printf("%s [%d] -> Dummy [%d] (%d packets discarded)\n",
//                            dn.getName(), dn.getUuid(), network.getSensorNodes().size() + 1, dn.getOverflowPackets()
//                    );
                    dn.removePackets(dn.getPacketsLeft());
                }
            }
        }
    }

    @Override
    public int getTotalValue() {
        super.getTotalValue();

        return this.totalValue;
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

    @Override
    public int getTotalPackets() {
        super.getTotalPackets();

        int totalPackets = 0;
        for (StorageNode sn : this.getNetwork().getStorageNodes()) {
            totalPackets += sn.getUsedSpace();
        }
        return totalPackets;
    }

    @Override
    public void printRoute() {
        super.printRoute();
        StringJoiner str;

        for (Map.Entry<SensorNode, List<Tuple<StorageNode, Integer, List<SensorNode>>>> entry : this.routes.entrySet()) {
            for (Tuple<StorageNode, Integer, List<SensorNode>> route : entry.getValue()) {
                str = new StringJoiner(" -> ", "[", "]");
                System.out.printf("%s -> %s (flow = %d)\n", entry.getKey().getName(), route.first().getName(), route.second());
                for (SensorNode node : route.third()) {
                    str.add(node.getName());
                }
                System.out.printf("\t%s\n", str);
            }
        }

    }
}