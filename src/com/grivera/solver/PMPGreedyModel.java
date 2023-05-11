package com.grivera.solver;

import com.grivera.generator.Network;
import com.grivera.generator.sensors.DataNode;
import com.grivera.generator.sensors.StorageNode;

public class PMPGreedyModel extends AbstractModel {

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

    @Override
    public void run() {
        super.run();
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
