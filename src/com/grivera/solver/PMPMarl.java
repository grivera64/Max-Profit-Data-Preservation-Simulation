package com.grivera.solver;

// From Google's Guava Library
import com.google.common.collect.Lists;

import com.grivera.generator.Network;
import com.grivera.generator.SensorNetwork;
import com.grivera.generator.sensors.DataNode;
import com.grivera.generator.sensors.SensorNode;
import com.grivera.generator.sensors.StorageNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public class PMPMarl extends AbstractModel {
    private int dataPacketCount;
    private int storageCapacity;
    private List<DataNode> dNodes;
    private int totalCost;
    private int totalProfit;

    public PMPMarl(Network network) {
        super(network);
        setField();
    }

    public PMPMarl(String fileName) {
        super(fileName);
        setField();
    }

    public PMPMarl(String fileName, int overflowPackets, int storageCapacity) {
        super(fileName, overflowPackets, storageCapacity);
        setField();
    }

    private void setField() {
        SensorNetwork network = (SensorNetwork) this.getNetwork();
        this.dataPacketCount = network.getDataPacketCount();
        this.dNodes = network.getDataNodes();
        this.storageCapacity = network.getStorageCapacity();
    }

    public void run(int epi) {
        super.run();

        double alpha = .1;
        double gamma = .3;
        double epsilon = .2;
//        int delta = 1;
//        int beta = 2;
        int w = 10;
        int cost;
        int min = Integer.MAX_VALUE;

        SensorNetwork network = (SensorNetwork) this.getNetwork();
        // create state object with nodes and an agent for each packet
        NetState state = new NetState(network.getSensorNodes().size(), network.getDataNodes().size() * dataPacketCount);

        // for each agent/packet set current/original location and value
        int packetCount = 0;
        for (DataNode dn : this.dNodes) {
            for (int j = 0; j < this.dataPacketCount; j++) {
                state.getAgents().get(packetCount).setCurrentLocation(dn);
                state.getAgents().get(packetCount).setOriginalLocation(dn);
                state.getAgents().get(packetCount).setPacketValue(dn.getOverflowPacketValue());
                packetCount++;
            }
        }
        // initialize empty Q-table

        state.setQTable();

        for (int i = 0; i < epi; i++) {// learning Stage
            // agent j is at source node Sj
            for (Agent agent : state.getAgents()) {
                SensorNode source = agent.getOriginalLocation();
                agent.setCurrentLocation(source);
                // travel cost by agent j
                agent.resetTravel();
                state.resetForEpisode();
            }
            cost = 0;
            // at least one agent has not arrived at a storage node
            while (!state.allAgentsAtStorage()) {
                //System.out.println(state.encodeState());
                // find next state t using st. trans. rule
                findNextState(state, epsilon);
                updateRoute(state);

                // update Q value
                Map<String, Double> Q = state.getQTable();
                String sStateTState = state.encodeST();

                // check if null?
                // no, already checked when next state was being picked
                double newQValue = (1 - alpha) * Q.get(sStateTState) + alpha * gamma * maxQNextState(state);
                Q.put(sStateTState, newQValue);

                // s=t move to next state
                // for each agent:
                // so currentlocation=nextLocation
                // storedinStorage = storedInStorageNext
                // state.packetsStoredInNode(agent.nextLocation) =
                // state.packetsStoredInNodeNext(agent.nextLocation)
                updateState(state);
            } // end while
            for (int j = 0; j < state.getAgents().size(); j++) {
                Agent agent = state.getAgents().get(j);
                cost += cost + agent.getTravelCost();
            }
            if (cost < min) {
                min = cost;
                // update edge rewards for each agent's route
                for (int j = 0; j < state.getAgents().size(); j++) {
                    // double netTransitionReward=0.0;
                    Agent agent = state.getAgents().get(j);
                    for (int c = 1; c < agent.getRoute().size(); c++) {
                        SensorNode prevNode = agent.getRoute().get(c - 1);
                        SensorNode currNode = agent.getRoute().get(c);
                        if (state.edgeReward.get(prevNode) == null) {
                            Map<SensorNode, Double> edge = new HashMap<>();
                            state.edgeReward.put(prevNode, edge);
                        }
                        if (state.edgeReward.get(prevNode).get(currNode) == null) {
                            // if edge destination is StorageNode, initial reward is 100
                            // else it is -10
                            if (currNode instanceof StorageNode) {
                                state.edgeReward.get(prevNode).put(currNode, 100.0);
                            } else {
                                state.edgeReward.get(prevNode).put(currNode, -10.0);
                            }

                        }
                        double newReward = state.edgeReward.get(prevNode).get(currNode) + ((double) w) / cost;
                        // netTransitionReward = newReward
                        state.edgeReward.get(prevNode).put(currNode, newReward);

                    }
                }
            }
            // update state transition reward value
            // for each state transition update transition reward
            //
            for (int j = 0; j < state.stateTransitions.size(); j++) {
                String transition = state.stateTransitions.get(j);

                double totalReward = 0.0;
                for (Agent agent : state.getAgents()) {
                    // if j+1 is less than the route size, then node changed in
                    // state transition
                    if (j + 1 < agent.getRoute().size()) {
                        // first transition is route.j to route j+1
                        totalReward += state.edgeReward.get(agent.getRoute().get(j)).get(agent.getRoute().get(j + 1));
                    }
                    // int index = Math.min(j,agent.getRoute().size()-1);
                }
                state.stateTransitionReward.put(transition, totalReward);
            }
            // update Q value
            Map<String, Double> Q = state.getQTable();
            for (int j = 0; j < state.stateTransitions.size(); j++) {
                String transition = state.stateTransitions.get(j);
                double newQvalue = (1 - alpha) * Q.get(transition) + alpha
                        * (state.stateTransitionReward.get(transition) + gamma * state.maxQNextTransition.get(transition));
                Q.put(transition, newQvalue);
            }
            // reset for new episode
        } // end of each episode in learning stage
        for (Agent agent : state.getAgents()) {
            agent.setCurrentLocation(agent.getOriginalLocation());
            agent.resetTravel();

            state.resetForEpisode();
        }
        // execution stage
        while (!state.allAgentsAtStorage()) {
            findNextStateExecution(state);
            // add route, add cost
            updateRoute(state);
            // s=t move to next state
            // for each agent:
            // so currentlocation=nextLocation
            // storedinStorage = storedInStorageNext
            // state.packetsStoredInNode(agent.nextLocation) =
            // state.packetsStoredInNodeNext(agent.nextLocation)
            updateState(state);
        }
        //return route, return Cost of Route, return profit of route
        //or just sys.out 
        int costOfRoute = 0;
        int profitOfRoute = 0;
        for (int i = 0; i < state.getAgents().size(); i++) {
            Agent agent = state.getAgents().get(i);
            costOfRoute += agent.calculateCostOfPath();
            profitOfRoute += agent.getPacketValue() - costOfRoute;
        }
        System.out.println("The total cost of MARL packet preservation: " + costOfRoute);
        System.out.println("The total profit of MARL packet preservation: " + profitOfRoute);

        this.totalCost = costOfRoute;
        this.totalProfit = profitOfRoute;
    }

    private void updateState(NetState state) {
        for (Agent agent : state.getAgents()) {
            agent.setCurrentLocation(agent.getNextLocation());
            agent.setStoredInStorage(agent.getStoredInStorageNext());
            if (agent.getCurrentLocation() instanceof StorageNode) {
                state.packetsStoredInNode.putIfAbsent((StorageNode) agent.getCurrentLocation(), 0);
                state.packetsStoredInNode.put((StorageNode) agent.getCurrentLocation(),
                        state.packetsStoredInNodeNext.get(agent.getCurrentLocation()));
            }
        }
    }

    private void updateRoute(NetState state) {
        for (Agent agent : state.getAgents()) {
            int travelCost = agent.getTravelCost();
            int transmitCost = agent.getCurrentLocation().calculateTransmissionCost(agent.getNextLocation());
            int receiveCost = agent.getNextLocation().calculateReceivingCost();
            if (!(agent.getCurrentLocation() == agent.getNextLocation())) {
                agent.setTravelCost(travelCost + transmitCost + receiveCost);
            }

            agent.addToRoute();
        }
    }

    private double maxQNextState(NetState state) {
        // get the highest Q value for an action for the next state
        // involves getting the cartesian product of the actions of the next state
        // return the Q value of the action with the highest Q value

        List<List<SensorNode>> allJointActions = generateAllNextJointActions(state);

        double max = java.lang.Double.NEGATIVE_INFINITY;
        List<SensorNode> bestJointAction;
        String bestJointActionString = "";
        for (List<SensorNode> jointAction : allJointActions) {
            String nextNextState = encodeStateList(jointAction);
            String tStateZState = state.encodeTZ(nextNextState);
            state.getQTable().putIfAbsent(tStateZState, 0.0);
            if (state.getQTable().get(tStateZState) > max) {
                max = state.getQTable().get(tStateZState);
                bestJointAction = jointAction;
                bestJointActionString = tStateZState;
            }
        }
        state.maxQNextTransition.put(bestJointActionString, max);
        return max;
    }

    private void findNextStateExecution(NetState state) {
        List<List<SensorNode>> allJointActions = generateAllJointActions(state);

        double max = java.lang.Double.NEGATIVE_INFINITY;
        List<SensorNode> bestJointAction = allJointActions.get(0);
        String bestStateTransition = "";
        for (List<SensorNode> jointAction : allJointActions) {
            String nextState = encodeStateList(jointAction);
            String sStateTState = state.encodeST(nextState);

            state.getQTable().putIfAbsent(sStateTState, 0.0);
            if (state.getQTable().get(sStateTState) > max) {
                max = state.getQTable().get(sStateTState);
                bestJointAction = jointAction;
                bestStateTransition = sStateTState;
            }
        }
        // next state has been found
        changeState(state, bestJointAction);
    }

    private void changeState(NetState state, List<SensorNode> bestJointAction) {
        for (int i = 0; i < state.getAgents().size(); i++) {
            Agent agent = state.getAgents().get(i);
            SensorNode nextLocation = bestJointAction.get(i);
            agent.setNextLocation(nextLocation);
            // now determine which (if any) agents will be placed in their next state

            if (agent.getStoredInStorage()) {
                continue;
            }
            if (!(nextLocation instanceof StorageNode)) {
                continue;
            }

            state.packetsStoredInNodeNext.putIfAbsent((StorageNode) nextLocation, 0);
            if (state.packetsStoredInNodeNext.get(nextLocation) == storageCapacity) {
                continue;
            }

            // if this point is reached, then packet hasnt been stored yet, its next
            // location is a storage node
            // and the storage node is not at capacity
            // so packet(agent) will be placed at its next location
            state.packetsStoredInNodeNext.put((StorageNode) nextLocation,
                    state.packetsStoredInNodeNext.get(nextLocation) + 1);
            agent.setStoredInStorageNext();
        }
    }

    private void findNextState(NetState state, double qZero) {
        Random rand = new Random();
        double randomQ = rand.nextDouble();

        if (randomQ <= 1 - qZero) {

            // exploitation
            // pick the joint action with the highest value of its Qvalue^delta
            // divided by the total cost of the state change^beta
            List<List<SensorNode>> allJointActions = generateAllJointActions(state);
            // after all possible joint actions are generated, compare them
            // and pick the joint action according to the transition rule
            double max = java.lang.Double.NEGATIVE_INFINITY;
            List<SensorNode> bestJointAction = allJointActions.get(0);
            //System.out.println("joint action 0: "+bestJointAction);
            //System.out.println("allJoint action list size: "+allJointActions.size());
            String bestStateTransition = "";
            for (List<SensorNode> jointAction : allJointActions) {
                String nextState = encodeStateList(jointAction);
                String sStateTState = state.encodeST(nextState);
                state.getQTable().putIfAbsent(sStateTState, 0.0);
                double numerator = Math.pow(state.getQTable().get(sStateTState), state.delta);
                double denominator = 0.0;
                for (int j = 0; j < jointAction.size(); j++) {
                    denominator += state.getAgents().get(j).getCurrentLocation()
                            .calculateTransmissionCost(jointAction.get(j));
                    denominator += jointAction.get(j).calculateReceivingCost();
                }
                denominator = Math.pow(denominator, state.beta);
                //System.out.println("max calc: "+(numerator/denominator));
                if (numerator / denominator > max) {
                    //System.out.println("greater than max reached");
                    max = numerator / denominator;
                    bestJointAction = jointAction;
                    bestStateTransition = sStateTState;
                }
            }
            // next state has been found using transition rule
            //System.out.println(bestStateTransition);
            changeState(state, bestJointAction);
            state.addStateTransition(bestStateTransition);
        } else {

            // exploration
            //first find weights of all actions
            Map<Integer, Double> actionWeights = new HashMap<>();
            List<List<SensorNode>> allJointActions = generateAllJointActions(state);
            double probDenominator = 0.0;

            for (List<SensorNode> jointAction : allJointActions) {
                String nextState = encodeStateList(jointAction);
                String sStateTState = state.encodeST(nextState);
                state.getQTable().putIfAbsent(sStateTState, 0.0);
                double numerator = Math.pow(state.getQTable().get(sStateTState), state.delta);
                double denominator = 0.0;
                for (int j = 0; j < jointAction.size(); j++) {
                    denominator += state.getAgents().get(j).getCurrentLocation()
                            .calculateTransmissionCost(jointAction.get(j));
                    denominator += jointAction.get(j).calculateReceivingCost();
                }
                denominator = Math.pow(denominator, state.beta);
                probDenominator += numerator / denominator;
            }
            for (List<SensorNode> jointAction : allJointActions) {
                double probNumerator;

                String nextState = encodeStateList(jointAction);
                String sStateTState = state.encodeST(nextState);

                double numerator = Math.pow(state.getQTable().get(sStateTState), state.delta);
                double denominator = 0.0;

                for (int j = 0; j < jointAction.size(); j++) {
                    denominator += state.getAgents().get(j).getCurrentLocation()
                            .calculateTransmissionCost(jointAction.get(j));
                    denominator += jointAction.get(j).calculateReceivingCost();
                }
                denominator = Math.pow(denominator, state.beta);

                probNumerator = numerator / denominator;
                double actionWeight = probNumerator / probDenominator;
                actionWeights.put(Objects.hashCode(jointAction), actionWeight);
            }
            List<SensorNode> randomJointAction = getRandomAction(allJointActions, actionWeights);
            //next (random) action has been found
            //System.out.println(randomJointAction.toString());

            changeState(state, randomJointAction);
            String nextState = encodeStateList(randomJointAction);
            String sStateTState = state.encodeST(nextState);
            state.addStateTransition(sStateTState);


        }
    }

    private List<SensorNode> getRandomAction(List<List<SensorNode>> allJointActions, Map<Integer, Double> actionWeights) {
        //calculate total weight
        double totalWeight = 0.0;
        List<SensorNode> randomJointAction = allJointActions.get(0);
        //System.out.println("random joint action 0: "+randomJointAction);
        //System.out.println("joint action list size: "+allJointActions.size());
        for (List<SensorNode> jointAction : allJointActions) {
            totalWeight += actionWeights.get(Objects.hashCode(jointAction));
        }
        double randomNum = new Random().nextDouble() * totalWeight;

        for (List<SensorNode> jointAction : allJointActions) {
            randomNum -= actionWeights.get(Objects.hashCode(jointAction));

            if (randomNum <= 0) {
                //return allJointActions.get(i);
                randomJointAction = jointAction;
                break;
            }
        }
        return randomJointAction;

    }

    private String encodeStateList(List<SensorNode> list) {
        StringBuilder sb = new StringBuilder();
        for (SensorNode sensorNode : list) {
            sb.append(sensorNode.getUuid());
        }
        return sb.toString();
    }

    private List<List<SensorNode>> generateAllJointActions(NetState state) {
        List<List<SensorNode>> jointActions;
        // first for each agent in state, make a list of the set of Nodes it can
        // feasibly travel to (if any)
        List<List<SensorNode>> possibleTravels = new ArrayList<>();
        for (Agent agent : state.getAgents()) {
            Set<SensorNode> neighbors;
            List<SensorNode> possibleHops = new ArrayList<>();
            // if agent packet is stored in a storage node already, its only possible next
            // state is the node it is currently at
            if (agent.getStoredInStorage()) {
                possibleHops.add(agent.getCurrentLocation());
                possibleTravels.add(possibleHops);
                //System.out.println("agent at node: "+ agent.getCurrentLocation()+" possible hops: "+possibleHops.toString());
                continue;
            }
            neighbors = ((SensorNetwork) this.getNetwork()).getNeighbors(agent.getCurrentLocation());
            possibleHops = new ArrayList<>(neighbors);
            possibleTravels.add(possibleHops);
            //System.out.println("agent at node: "+ agent.getCurrentLocation()+" possible hops: "+possibleHops.toString());
        }
        // then make cartesian product of each agents set
        jointActions = Lists.cartesianProduct(possibleTravels);
        return jointActions;
    }

    private List<List<SensorNode>> generateAllNextJointActions(NetState state) {
        List<List<SensorNode>> jointActions;
        List<List<SensorNode>> possibleTravels = new ArrayList<>();

        for (Agent agent : state.getAgents()) {
            Set<SensorNode> neighbors;
            List<SensorNode> possibleHops = new ArrayList<>();
            // if agent packet will be stored in a storage node in its next state, its only
            // possible next state is the node its at
            if (agent.getStoredInStorageNext()) {
                possibleHops.add(agent.getNextLocation());
                possibleTravels.add(possibleHops);
                continue;
            }
            neighbors = ((SensorNetwork) this.getNetwork()).getNeighbors(agent.getNextLocation());
            possibleHops = new ArrayList<>(neighbors);
            possibleTravels.add(possibleHops);
        }
        // then make cartesian product of each agents set
        jointActions = Lists.cartesianProduct(possibleTravels);
        return jointActions;
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
