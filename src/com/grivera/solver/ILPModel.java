package com.grivera.solver;

import com.grivera.generator.Network;
import com.grivera.generator.sensors.DataNode;
import com.grivera.generator.sensors.SensorNode;
import com.grivera.generator.sensors.StorageNode;

import java.util.List;

//imports for google OR-tools
import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

public class ILPModel extends AbstractModel {
    private MPVariable[][] cachedX;
    private MPObjective cachedObjective;

    public ILPModel(Network network) {
        super(network);
    }

    public ILPModel(String fileName) {
        super(fileName);
    }

    public ILPModel(String fileName, int overflowPackets, int storageCapacity) {
        super(fileName, overflowPackets, storageCapacity);
    }

    @Override
    public void run() {
        super.run();
        this.solveIlp();
    }

    @Override
    public int getTotalCost() {
        super.getTotalCost();

        Network network = this.getNetwork();
        List<DataNode> dNodes = network.getDataNodes();
        List<StorageNode> sNodes = network.getStorageNodes();
        final int n = dNodes.size() + sNodes.size();
        int totalCost = 0;
        for (DataNode dn : dNodes) {
            for (StorageNode sn : sNodes) {
                totalCost += this.cachedX[dn.getUuid() + n][sn.getUuid()].solutionValue() * network.calculateMinCost(dn, sn);
            }
        }
        return totalCost;
    }

    @Override
    public int getTotalProfit() {
        super.getTotalProfit();
        final int sourceIndex = 0;
        int totalValue = 0;
        for (DataNode dn : this.getNetwork().getDataNodes()) {
            totalValue += this.cachedX[sourceIndex][dn.getUuid()].solutionValue() * dn.getOverflowPacketValue();
        }
        return totalValue - this.getTotalCost();
    }

    @Override
    public int getTotalPackets() {
        super.getTotalPackets();
        return (int) Math.floor(this.cachedObjective.value());
    }

    /* TODO(grivera64@) Verify! */
    private void solveIlp() {
        Network network = this.getNetwork();

        List<DataNode> dNodes = network.getDataNodes();
        List<StorageNode> sNodes = network.getStorageNodes();
        List<SensorNode> nodes = network.getSensorNodes();

        int infinity = Integer.MAX_VALUE;
        int n = network.getSensorNodes().size();
        int sourceIndex = 0;
        int sinkIndex = 2 * n + 1;
        Loader.loadNativeLibraries();
        MPSolver solver = MPSolver.createSolver("GLOP");
        MPVariable[][] x = new MPVariable[2 * n + 2][2 * n + 2];

        // create 2d array of decision variable x
        // x_i_j value represents number of flows from i to j

        for (int fromIndex = 0; fromIndex < x.length; fromIndex++) {
            for (int toIndex = 0; toIndex < x[fromIndex].length; toIndex++) {
                x[fromIndex][toIndex] = solver.makeIntVar(0, infinity, String.format("x_%d_%d", fromIndex, toIndex));
            }
        }
        // if i has no edge to j, x_i_j=0
        makeMaxFlowEdges(x, solver);

        // constraint (4):
        // indicates the maximum number of packets data node i can offload, di.
        // the initial number of data packets data node i has
        MPConstraint[] four = new MPConstraint[dNodes.size()];
        int constraintIndex = 0;
        for (DataNode dn : dNodes) {
            four[constraintIndex] = solver.makeConstraint(-infinity, dn.getOverflowPackets(), String.format("%s_in", dn.getName()));
            four[constraintIndex].setCoefficient(x[sourceIndex][dn.getUuid()], 1);
            constraintIndex++;
        }

        // constraint (5):
        // indicates the maximum number of packets storage node i can store is mi, the
        // storage capacity of storage node i
        MPConstraint[] five = new MPConstraint[sNodes.size()];
        constraintIndex = 0;
        for (StorageNode sn : sNodes) {
            five[constraintIndex] = solver.makeConstraint(-infinity, sn.getCapacity(), String.format("%s_out", sn.getName()));
            five[constraintIndex].setCoefficient(x[sn.getUuid() + n][sinkIndex], 1);
            constraintIndex++;
        }

        // constraint (6):
        // the flow conservation for data nodes, where the number of its own data
        // packets offloaded plus the number of data packets it relays for other data
        // nodes equals the number of data packets it transmits.
        //
        // x_si' + sum(x_j"i') - sum(x_i"j') == 0
        MPConstraint[] six = new MPConstraint[dNodes.size()];
        constraintIndex = 0;
        for (DataNode dn : dNodes) {
            six[constraintIndex] = solver.makeConstraint(0, 0);
            six[constraintIndex].setCoefficient(x[sourceIndex][dn.getUuid()], 1);
            for (SensorNode sn : nodes) {
                six[constraintIndex].setCoefficient(x[sn.getUuid() + n][dn.getUuid()], 1);
                six[constraintIndex].setCoefficient(x[dn.getUuid() + n][sn.getUuid()], -1);
            }
            constraintIndex++;
        }

        // constraint (7):
        // the flow conservation for storage nodes, which says that data packets a
        // storage node receives are either relayed to other nodes or stored by this
        // storage node
        //
        // sum(x_j"i') - sum(x_i"j') - x_i"t == 0
        MPConstraint[] seven = new MPConstraint[sNodes.size()];
        constraintIndex = 0;
        for (StorageNode sn : sNodes) {
            seven[constraintIndex] = solver.makeConstraint(0, 0);
            seven[constraintIndex].setCoefficient(x[sn.getUuid() + n][sinkIndex], -1);
            for (SensorNode snp : nodes) {
                seven[constraintIndex].setCoefficient(x[snp.getUuid() + n][sn.getUuid()], 1);

                seven[constraintIndex].setCoefficient(x[sn.getUuid() + n][snp.getUuid()], -1);
            }
            constraintIndex++;
        }

        // constraint(8):
        // (8) and (9) represents the energy constraints for data nodes and storage
        // nodes respectively
        // in our work we don't consider the storage cost, so its just one constraint
        //
        // Er_i + sum(x_j"i') + Et_i * sum(x_i"j') <= E_i
        MPConstraint[] eight = new MPConstraint[nodes.size()];
        constraintIndex = 0;
        for (SensorNode sn : nodes) {
            eight[constraintIndex] = solver.makeConstraint(-infinity, sn.getEnergy());
            // for (SensorNode snp : this.nodes) {
            for (SensorNode snp : network.getNeighbors(sn)) {
                // if (snp.equals(sn)) {
                    // continue;
                // }
                
                eight[constraintIndex].setCoefficient(x[snp.getUuid() + n][sn.getUuid()], sn.calculateReceivingCost());
                eight[constraintIndex].setCoefficient(x[sn.getUuid() + n][snp.getUuid()], sn.calculateTransmissionCost(snp));

            }
            constraintIndex++;
        }

        // set Objective, (maximize flow from source to data in nodes)
        MPObjective objective = solver.objective();
        for (DataNode dn : dNodes) {
            objective.setCoefficient(x[sourceIndex][dn.getUuid()], 1);
        }
        objective.setMaximization();

        // solve
        final MPSolver.ResultStatus resultStatus = solver.solve();

        // Cache the variables used
        this.cachedX = x;
        this.cachedObjective = objective;
    }

    private void makeMaxFlowEdges(MPVariable[][] x, MPSolver solver) {
        Network network = this.getNetwork();

        List<DataNode> dNodes = network.getDataNodes();
        List<StorageNode> sNodes = network.getStorageNodes();
        List<SensorNode> nodes = network.getSensorNodes();

        // make all edges from the source node to every node that is NOT a data in-node
        // capacity 0
        int n = nodes.size();
        int sourceIndex = 0;
        int sinkIndex = 2 * n + 1;
        MPConstraint c = solver.makeConstraint(0.0, 0.0, "no Edge from i to j");
        for (SensorNode sn : nodes) {
            if (!(sn instanceof DataNode)) {
                // storage in-node
                c.setCoefficient(x[sourceIndex][sn.getUuid()], 1);
                // storage out-node
                c.setCoefficient(x[sourceIndex][sn.getUuid() + n], 1);
            } else {
                // data out-node
                c.setCoefficient(x[sourceIndex][sn.getUuid() + n], 1);

            }
        }
        // sink node
        c.setCoefficient(x[sourceIndex][sinkIndex], 1);

        // make all edges from the data in-nodes to every node that is NOT its
        // respective out-node capacity 0

        for (DataNode dn : dNodes) {

            // source node
            c.setCoefficient(x[dn.getUuid()][0], 1);

            // other data in-nodes
            for (DataNode dnp : dNodes) {
                if (dnp.equals(dn)) {
                    continue;
                }
                c.setCoefficient(x[dn.getUuid()][dnp.getUuid()], 1);
                // all other data out-nodes
                c.setCoefficient(x[dn.getUuid()][dnp.getUuid() + n], 1);
            }

            // storage nodes
            for (StorageNode sn : sNodes) {
                // in-node
                c.setCoefficient(x[dn.getUuid()][sn.getUuid()], 1);
                // out-node
                c.setCoefficient(x[dn.getUuid()][sn.getUuid() + n], 1);
            }
            // sink node
            c.setCoefficient(x[dn.getUuid()][sinkIndex], 1);
        }

        // make all edges from data out-node to:
        // source capacity 0 (1)
        // its respective in-node capacity 0 (2)
        // any data in-node it is NOT directly connected to capacity 0 (3)
        // any other data out-node capacity 0 (4)
        // any storage in- node it is not directly connected to capacity 0 (5)
        // every storage out-node capacity 0 (6)
        // sink capacity 0 (7)
        for (DataNode dn : dNodes) {
            // (1)
            c.setCoefficient(x[dn.getUuid() + n][0], 1);
            // (2)
            c.setCoefficient(x[dn.getUuid() + n][dn.getUuid()], 1);
            for (DataNode dnp : dNodes) {
                if (dn.getUuid() == dnp.getUuid()) {
                    continue;
                }
                if (!network.isConnected(dnp, dn)) {
                    // (3)
                    c.setCoefficient(x[dn.getUuid() + n][dnp.getUuid()], 1);
                }
                // (4)
                c.setCoefficient(x[dn.getUuid() + n][dnp.getUuid() + n], 1);
            }
            for (StorageNode sn : sNodes) {
                if (!network.isConnected(sn, dn)) {
                    // (5)
                    c.setCoefficient(x[dn.getUuid() + n][sn.getUuid()], 1);
                }
                // (6)
                c.setCoefficient(x[dn.getUuid() + n][sn.getUuid() + n], 1);
            }
            // (7)
            c.setCoefficient(x[dn.getUuid() + n][sinkIndex], 1);
        }

        // make all edges from storage in-nodes to any other node that is not its
        // respective out-node capacity 0
        for (StorageNode sn : sNodes) {
            // source node
            c.setCoefficient(x[sn.getUuid()][0], 1);

            for (DataNode dn : dNodes) {
                // data nodes
                // in-nodes
                c.setCoefficient(x[sn.getUuid()][dn.getUuid()], 1);
                // out-nodes
                c.setCoefficient(x[sn.getUuid()][dn.getUuid() + n], 1);
            }

            for (StorageNode snp : sNodes) {
                if (snp.getUuid() == sn.getUuid()) {
                    continue;
                }
                // other storage in-nodes
                c.setCoefficient(x[sn.getUuid()][snp.getUuid()], 1);
                // all other storage out-nodes
                c.setCoefficient(x[sn.getUuid()][snp.getUuid() + n], 1);
            }
            // sink
            c.setCoefficient(x[sn.getUuid()][sinkIndex], 1);
        }
        // make all edges from storage out-nodes to
        // source node capacity 0
        // data in-nodes that it is not directly connected to capacity 0
        // data out-nodes capacity 0
        // its respective storage in-node capcity 0
        // all other storage in-nodes it is not connected to capacity 0
        // all other other storage out-nodes capacity 0
        for (StorageNode sn : sNodes) {
            // source
            c.setCoefficient(x[sn.getUuid() + n][0], 1);
            for (DataNode dn : dNodes) {
                if (!network.isConnected(dn, sn)) {
                    // data in-nodes it is not connected to directly
                    c.setCoefficient(x[sn.getUuid() + n][dn.getUuid()], 1);
                }
                // data out-nodes
                c.setCoefficient(x[sn.getUuid() + n][dn.getUuid() + n], 1);
            }
            // its respective in-node
            c.setCoefficient(x[sn.getUuid() + n][sn.getUuid()], 1);

            for (StorageNode snp : sNodes) {
                if (snp.getUuid() == sn.getUuid()) {
                    continue;
                }
                if (!network.isConnected(sn, snp)) {
                    // storage in-node it is not directly connected to
                    c.setCoefficient(x[sn.getUuid() + n][snp.getUuid()], 1);
                }
                // all other storage out-nodes
                c.setCoefficient(x[sn.getUuid() + n][snp.getUuid() + n], 1);
            }
        }
        // make all edges from the sink node to any other node capacity 0
        // source
        c.setCoefficient(x[sinkIndex][0], 1);
        // sensor in and out-nodes
        for (SensorNode sn : nodes) {
            c.setCoefficient(x[sinkIndex][sn.getUuid()], 1);
            c.setCoefficient(x[sinkIndex][sn.getUuid() + n], 1);
        }
        // finally if i==j, x[i][j]=0
        for (int i = 0; i < x.length; i++) {
            c.setCoefficient(x[i][i], 1);
        }

    }

    @Override
    public void printRoute() {
        super.printRoute();

        Network network = this.getNetwork();
        List<DataNode> dNodes = network.getDataNodes();
        List<StorageNode> sNodes = network.getStorageNodes();
        final int n = dNodes.size() + sNodes.size();

        double flow;
        for (DataNode dn : dNodes) {
            for (StorageNode sn : sNodes) {
                flow = this.cachedX[dn.getUuid() + n][sn.getUuid()].solutionValue();
                if (flow > 1e-3) {
                    System.out.printf("%s -> %s (flow = %.0f)\n", dn.getName(), sn.getName(), flow);
                    System.out.printf("\tCost: %f\n", this.cachedX[dn.getUuid() + n][sn.getUuid()].solutionValue() * network.calculateMinCost(dn, sn));
                    System.out.printf("\tValue: %f\n", this.cachedX[dn.getUuid() + n][sn.getUuid()].solutionValue() * dn.getOverflowPacketValue());
                }
            }
        }
    }
}