package com.grivera.solver;

import com.grivera.generator.Network;
import com.grivera.generator.sensors.DataNode;
import com.grivera.generator.sensors.SensorNode;
import com.grivera.generator.sensors.StorageNode;
import com.grivera.util.Tuple;

import java.util.*;

public class PMPGreedyModel extends AbstractModel {

    private final Map<SensorNode, List<Tuple<StorageNode, Long, List<SensorNode>>>> routes = new HashMap<>();
    private long totalCost;
    private long totalProfit;
    private long totalValue;
    private long totalPackets;

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
        this.totalCost = 0;
        this.totalProfit = 0;
        this.totalValue = 0;
        this.totalPackets = 0;

        Network network = this.getNetwork();
        StorageNode chosenSn;
        long chosenProfit = Long.MIN_VALUE;
        long packetsToSend;
        long cost;

        network.resetPackets();
        long currProfit;
        long currPacketsToSend;
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
                    this.totalValue += (chosenProfit * packetsToSend) + (cost * packetsToSend);
                    this.totalPackets += packetsToSend;

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
    public long getTotalCost() {
        super.getTotalCost();

        return this.totalCost;
    }

    @Override
    public long getTotalProfit() {
        super.getTotalProfit();
        return this.totalProfit;
    }

    @Override
    public long getTotalValue() {
        super.getTotalValue();
        return this.totalValue;
    }

    @Override
    public long getTotalPackets() {
        super.getTotalPackets();

        return this.totalPackets;
    }

    @Override
    public void printRoute() {
        super.printRoute();

        StringJoiner str;

        for (Map.Entry<SensorNode, List<Tuple<StorageNode, Long, List<SensorNode>>>> entry : this.routes.entrySet()) {
            for (Tuple<StorageNode, Long, List<SensorNode>> route : entry.getValue()) {
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
