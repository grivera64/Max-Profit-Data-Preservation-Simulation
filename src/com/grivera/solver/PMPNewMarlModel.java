package com.grivera.solver;

import java.util.*;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import com.grivera.generator.Network;
import com.grivera.generator.SensorNetwork;
import com.grivera.generator.sensors.StorageNode;
import com.grivera.util.ProgressBar;
import com.grivera.generator.sensors.SensorNode;
import com.grivera.generator.sensors.DataNode;

public class PMPNewMarlModel extends AbstractModel {
    private double alpha;
    private double alphaStart = .9;
    private double alphaEnd = .1;
    private double gamma;
    private double gammaStart = .1;
    private double gammaEnd = .1;
    private double epsilon;
    private double epsilonStart = 1.0;
    private double epsilonEnd = 1.0;
    private int episodes;
    private static final int delta = 1;
    private static final int beta = 2;
    private static final int w = 10000;
    private static final double storageReward = 200;
    private static final double nonStorageReward = 1;
    private long storageCapacity;
    private long totalCost;
    private long totalProfit;
    private long totalValue;
    private NetState finalState;
    private NetState state;

    public PMPNewMarlModel(Network network) {
        super(network);
        setField();
    }

    public PMPNewMarlModel(String fileName) {
        super(fileName);
        setField();
    }

    public PMPNewMarlModel(String fileName, int overflowPackets, int storageCapacity) {
        super(fileName, overflowPackets, storageCapacity);
        setField();
    }

    private void setField() {
        Network network = this.getNetwork();
        this.storageCapacity = network.getStorageCapacity();
    }

    public void run() {
        this.run(1);
    }

    public void updateHyperParams(int currEp) {
        epsilon = linearUpdateByEpisode(currEp, epsilonStart, epsilonEnd);
        alpha = linearUpdateByEpisode(currEp, alphaStart, alphaEnd);
        gamma = linearUpdateByEpisode(currEp, gammaStart, gammaEnd);
    }

    public double linearUpdateByEpisode(int currEp, double start, double end) {
        double valueRange = end - start;
        double valuePerEpisode = valueRange / episodes;
        return start + (valuePerEpisode * currEp);
    }

    public void run(int epi) {
        super.run(epi);
        episodes = epi;
        Network network = this.getNetwork();

        int cost, reward, profit;
        long max = Long.MIN_VALUE; // line 0.
        long min = Long.MAX_VALUE;
        // create state object with nodes and an agent for each packet
        state = new NetState(network);

        // initialize empty Q-table
        state.initQTable();

        ProgressBar bar = new ProgressBar(epi, 50, "Training");
        for (int i = 0; i < epi; i++) { // learning Stage //line 1
            bar.step();
            updateHyperParams(i);
            state.reset(); // handles resetting for lines 3. to 8.

            cost = 0;
            reward = 0;
            profit = 0;
            // at least one agent has not arrived at a storage node
            while (!state.allAgentsAtStorage()) { // line 10.
                // find next state t using st. trans. rule
                findNextState(state);

                moveAgentsToStateNew(state);
                // update state-reward value
                for (Agent agent : state.getAgents()) {
                    if (!state.edgeHasReward(agent.getCurrentLocation(), agent.getNextLocation())) {
                        updateEdgeReward(state, agent);
                    }
                }
                // commenting out this line, state transition reward not being updated here

                // update Q value for each action take (no joint action anymore)
                for (Agent agent : state.getAgents()) {
                    if (agent.getCurrentLocation().getUuid() == agent.getNextLocation().getUuid()) {
                        continue;
                    }
                    String sStateTState;
                    sStateTState = state.encodeST(agent.getNextLocation().getUuid() + "", agent);
                    double newQValue = (1 - alpha) * state.getQ(sStateTState)
                            + alpha * gamma * maxQNextState(state, agent);// check
                    // if
                    // null? no,
                    // already
                    // checked
                    // when next
                    // state was
                    // being
                    // picked
                    //state.setQ(sStateTState, newQValue);
                    // s=t move to next state
                    // for each agent:
                    // so currentlocation=nextLocation
                    // storedinStorage = storedInStorageNext
                    // state.packetsStoredInNode(agent.nextLocation) =
                    // state.packetsStoredInNodeNext(agent.nextLocation)
                    updateState(state, agent);
                }

            } // end while
              // all agents have arrived at storage node
            for (Agent agent : state.getAgents()) {

                if (agent.getStoredInStorage()) {
                    reward += (int) agent.getRewardCol();
                    cost += agent.getTravelCost();
                } else {
                    //
                }

            }
            profit = reward - cost;
            // if (profit > max) {// 28.
            if (cost < min) {
                // System.out.println("minfound");
                // max = profit;
                min = cost;
                // update edge rewards for each agent's route
                for (Agent agent : state.getAgents()) {
                    // double netTransitionReward=0.0;
                    for (int c = 1; c < agent.getRoute().size(); c++) {
                        SensorNode prevNode = agent.getRoute().get(c - 1);
                        SensorNode currNode = agent.getRoute().get(c);
                        if (prevNode == null || currNode == null) {
                            System.out.println("node in route is null");
                        }
                        double newReward = 0;
                        // cost++;// Temporary; this is incase cost is 0, to avoid divide by 0
                        newReward = state.getEdgeReward(prevNode, currNode) + (double) (w / cost);

                        state.setEdgeReward(prevNode, currNode, newReward ); // line 30.

                    }
                }
                for (int j = 0; j < state.getAgents().size(); j++) {
                    for (int c = 0; c < state.getAgents().get(j).stateTransitions.size(); c++) {
                        Agent agent = state.getAgents().get(j);
                        String transition = agent.stateTransitions.get(c); // (s,t)
                        double totalReward = 0.0; // r(s,t)
                        double totalCost = 0.0; // c(s,t)
                        double stateTransitProfit; // p(s,t)
                        // for (Agent agent : state.getAgents()) {

                        if (j + 1 < agent.getRoute().size()) {
                            SensorNode currNode = agent.getRoute().get(0);
                            SensorNode nextNode = agent.getRoute().get(1);
                            // if j+1 is less than the route size then node changed in state transition
                            // first transition is route.j to route j+1
                            totalReward += state.getEdgeReward(currNode, nextNode);
                            totalCost += currNode.calculateTransmissionCost(nextNode);
                            totalCost += nextNode.calculateReceivingCost();
                        }
                        // }
                        state.setTransitionReward(transition, totalReward); // line 32
                        stateTransitProfit = totalReward - totalCost;
                        state.setTransitionProfit(transition, stateTransitProfit); // line 33.
                        // System.out.println(transition+" "+i+" " +j+" "+ c);
                        double newQValue = (1 - alpha) * state.getQ(transition)
                                + alpha * (state.getTransitionProfit(transition)
                                        + gamma * state.getMaxQ(transition));
                        state.setQ(transition, newQValue*(i+1)); // line 34.
                    }
                }

            }

            // reset for new episode
        } // end of each episode in learning stage
        for (Agent agent : state.getAgents()) {
            agent.resetLocation();
            agent.stateTransitions = new ArrayList<>();
            agent.resetTravel();
        }
        state.reset();
        // execution stage
        while (!state.allAgentsAtStorage()) {
            findNextStateExecutionNew(state);
            // add route, add cost
            moveAgentsToStateExecution(state);
            // s=t move to next state
            // for each agent:
            // so currentlocation=nextLocation
            // storedinStorage = storedInStorageNext
            // state.packetsStoredInNode(agent.nextLocation) =
            // state.packetsStoredInNodeNext(agent.nextLocation)
            for (Agent agent : state.getAgents()) {
                updateState(state);
            }

        }
        // return route, return Cost of Route, return profit of route
        // or just sys.out
        int costOfRoute = 0;
        int profitOfRoute = 0;
        for (Agent agent : state.getAgents()) {

            if (agent.getStoredInStorage()) {
                profitOfRoute += agent.getPacketValue();
                costOfRoute += agent.calculateCostOfPathNew(this.getNetwork());
            }

        }
        System.out.println("min cost in learning: "+ min);
        this.totalValue = profitOfRoute;
        profitOfRoute -= costOfRoute;

        this.totalCost = costOfRoute;
        this.totalProfit = profitOfRoute;

        this.finalState = state;
    }

    private void moveAgentsToState(NetState state) {
        for (Agent agent : state.getAgents()) {
            long travelCost = agent.getTravelCost();
            double reward = agent.getRewardCol();
            long transmitCost = agent.getCurrentLocation().calculateTransmissionCost(agent.getNextLocation());
            long receiveCost = agent.getNextLocation().calculateReceivingCost();
            if (!(agent.getCurrentLocation() == agent.getNextLocation())) {
                agent.setTravelCost(travelCost + transmitCost + receiveCost);
                if (!state.edgeHasReward(agent.getCurrentLocation(), agent.getNextLocation())) {
                    updateEdgeReward(state, agent);
                }
                agent.setRewardCol(
                        reward + state.getEdgeReward(agent.getCurrentLocation(), agent.getNextLocation()));
            }

            agent.addToRoute();
        }
    }

    private void moveAgentsToStateNew(NetState state) {
        for (Agent agent : state.getAgents()) {
            long travelCost = agent.getTravelCost();
            double reward = agent.getRewardCol();
            // int transmitCost =
            // agent.getCurrentLocation().calculateTransmissionCost(agent.getNextLocation());
            long transmitCost = this.getNetwork().calculateCostOfPath(
                    this.getNetwork().getMinCostPath(agent.getCurrentLocation(), agent.getNextLocation()));
            // int receiveCost = agent.getNextLocation().calculateReceivingCost();
            if (!(agent.getCurrentLocation() == agent.getNextLocation())) {
                agent.setTravelCost(travelCost + transmitCost);
                if (!state.edgeHasReward(agent.getCurrentLocation(), agent.getNextLocation())) {
                    updateEdgeReward(state, agent);
                }
                agent.setRewardCol(
                        reward + state.getEdgeReward(agent.getCurrentLocation(), agent.getNextLocation()));
            }

            agent.addToRoute();
        }
    }

    private void moveAgentsToStateExecutionNew(NetState state) {
        for (Agent agent : state.getAgents()) {
            long travelCost = agent.getTravelCost();
            // int transmitCost =
            // agent.getCurrentLocation().calculateTransmissionCost(agent.getNextLocation());
            // int receiveCost = agent.getNextLocation().calculateReceivingCost();
            long transmitCost = this.getNetwork().calculateCostOfPath(
                    this.getNetwork().getMinCostPath(agent.getCurrentLocation(), agent.getNextLocation()));
            if (!(agent.getCurrentLocation() == agent.getNextLocation())) {
                agent.setTravelCost(travelCost + transmitCost);
            }

            agent.addToRoute();
        }
    }

    private void moveAgentsToStateExecution(NetState state) {
        for (Agent agent : state.getAgents()) {
            long travelCost = agent.getTravelCost();
            long transmitCost = agent.getCurrentLocation().calculateTransmissionCost(agent.getNextLocation());
            long receiveCost = agent.getNextLocation().calculateReceivingCost();
            if (!(agent.getCurrentLocation() == agent.getNextLocation())) {
                agent.setTravelCost(travelCost + transmitCost + receiveCost);
            }

            agent.addToRoute();
        }
    }

    private void updateState(NetState state) {
        for (Agent agent : state.getAgents()) {
            // save "current" location before setting it to "next" state's location
            agent.setLastStateLocation(agent.getCurrentLocation());
            agent.setCurrentLocation(agent.getNextLocation());
            agent.setStoredInStorage(agent.getStoredInStorageNext());
            if (agent.getCurrentLocation() instanceof StorageNode) {
                state.packetsStoredInNode.put((StorageNode) agent.getCurrentLocation(),
                        state.packetsStoredInNodeNext.get((StorageNode) agent.getCurrentLocation()));
            }
        }
    }

    private void updateState(NetState state, Agent agent) {

        // save "current" location before setting it to "next" state's location
        agent.setLastStateLocation(agent.getCurrentLocation());
        agent.setCurrentLocation(agent.getNextLocation());
        agent.setStoredInStorage(agent.getStoredInStorageNext());
        if (agent.getCurrentLocation() instanceof StorageNode) {
            state.packetsStoredInNode.put((StorageNode) agent.getCurrentLocation(),
                    state.packetsStoredInNodeNext.get((StorageNode) agent.getCurrentLocation()));
        }

    }

    private void updateEdgeReward(NetState state, Agent agent, SensorNode action) {
        if (action instanceof StorageNode) {
            state.setEdgeReward(agent.getCurrentLocation(), action,
                    storageReward);
        } else {
            state.setEdgeReward(agent.getCurrentLocation(), action,
                    nonStorageReward);
        }
    }

    private void updateEdgeReward(NetState state, Agent agent) {
        if (agent.getNextLocation() instanceof StorageNode) {
            state.setEdgeReward(agent.getCurrentLocation(), agent.getNextLocation(),
                    storageReward);
        } else {
            state.setEdgeReward(agent.getCurrentLocation(), agent.getNextLocation(),
                    nonStorageReward);
        }
    }

    private double maxQNextState(NetState state) {
        // get the highest Q value for an action for the next state
        // involves getting the cartesian product of the actions of the next state
        // return the Q value of the action with the highest Q value

        List<List<SensorNode>> allJointActions = generateAllNextJointActions(state);
        List<Agent> agents = state.getAgents();

        double maxQValue = Double.NEGATIVE_INFINITY;
        double currQValue;
        double weight;
        for (List<SensorNode> jointAction : allJointActions) {
            String nextNextState = encodeStateList(jointAction);
            String tStateZState = state.encodeTZ(nextNextState);
            if (!state.containsQ(tStateZState)) {
                // if q value is null, set new initial Q value
                //
                currQValue = 0.0;
                for (int j = 0; j < agents.size(); j++) {
                    Agent agent = agents.get(j);
                    weight = 0.0;
                    // transmissionCost
                    weight += agent.getNextLocation().calculateTransmissionCost(jointAction.get(j));
                    // receivingCost
                    weight += jointAction.get(j).calculateReceivingCost();
                    if (jointAction.get(j) instanceof StorageNode) {
                        currQValue += storageReward / weight;
                    } else {
                        currQValue += nonStorageReward / weight;
                    }
                }
                state.setQ(tStateZState, currQValue);
            }
            if (state.getQ(tStateZState) > maxQValue) {
                maxQValue = state.getQ(tStateZState);
            }
        }
        String sStateTState = state.encodeST();
        state.setMaxQ(sStateTState, maxQValue);
        return maxQValue;
    }

    private double maxQNextState(NetState state, Agent agent) {
        // get the highest Q value for an action for the next state
        // involves getting the cartesian product of the actions of the next state
        // return the Q value of the action with the highest Q value

        // List<List<SensorNode>> allJointActions = generateAllNextJointActions(state);

        List<Agent> agents = state.getAgents();

        double maxQValue = Double.NEGATIVE_INFINITY;
        double currQValue;
        double weight;
        for (SensorNode sn : this.getNetwork().getSensorNodes()) {
            if (agent.getCurrentLocation().getUuid() == sn.getUuid()) {
                continue;
            }
            List<SensorNode> nextNodeAsList = new ArrayList<>();
            nextNodeAsList.add(sn);
            String nextNextState = encodeStateList(nextNodeAsList);
            String tStateZState = state.encodeTZ(nextNextState, agent);
            if (!state.containsQ(tStateZState)) {
                // if q value is null, set new initial Q value
                //
                currQValue = 0.0;

                weight = 0.0;
                // transmissionCost
                weight += agent.getNextLocation().calculateTransmissionCost(sn);
                // receivingCost
                weight += sn.calculateReceivingCost();
                if (sn instanceof StorageNode) {
                    currQValue += storageReward / weight;
                } else {
                    currQValue += nonStorageReward / weight;
                }

                state.setQ(tStateZState, currQValue);
            }
            if (state.getQ(tStateZState) > maxQValue) {
                maxQValue = state.getQ(tStateZState);
            }
        }
        String sStateTState = state.encodeST(agent);
        state.setMaxQ(sStateTState, maxQValue);
        return maxQValue;
    }

    private void findNextStateExecution(NetState state) {
        List<List<SensorNode>> allJointActions = generateAllJointActions(state);

        double max = Double.MIN_VALUE;
        List<SensorNode> bestJointAction = allJointActions.get(0);

        String nextState;
        String sStateTState;
        for (List<SensorNode> jointAction : allJointActions) {
            nextState = encodeStateList(jointAction);
            sStateTState = state.encodeST(nextState);
            updateQTable(state, jointAction);
            if (state.getQ(sStateTState) > max) {
                max = state.getQ(sStateTState);
                bestJointAction = jointAction;
            }
        }
        // next state has been found
        updatePacketCount(state, bestJointAction);
    }

    private void findNextStateExecutionNew(NetState state) {
        // List<List<SensorNode>> allJointActions = generateAllJointActions(state);

        // List<SensorNode> bestJointAction = allJointActions.get(0);

        for (Agent agent : state.getAgents()) {
            if(agent.getStoredInStorage()){
                continue;
            }
            double max = Double.NEGATIVE_INFINITY;
            String nextState;
            String sStateTState;
            List<SensorNode> bestNodeAsList = new ArrayList<>();
            for (StorageNode sn : this.getNetwork().getStorageNodes()) {
                if(state.packetsStoredInNodeNext.get(sn)!=null&&state.packetsStoredInNodeNext.get(sn)>=storageCapacity){
                    continue;
                }
                if (agent.getCurrentLocation().getUuid() == sn.getUuid()) {
                    continue;
                }
                List<SensorNode> nextNodeAsList = new ArrayList<>();
                nextNodeAsList.add(sn);

                nextState = encodeStateList(nextNodeAsList);
                sStateTState = state.encodeST(nextState, agent);
                updateQTable(state, nextNodeAsList, agent);
                double forDebug = state.getQ(sStateTState);
                if (state.getQ(sStateTState) > max) {
                    max = state.getQ(sStateTState);
                    bestNodeAsList = nextNodeAsList;
                }
            }
            // next state has been found
            updatePacketCount(state, bestNodeAsList, agent);
        }

    }

    private void updatePacketCount(NetState state, List<SensorNode> bestJointAction) {
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
            state.packetsStoredInNodeNext.putIfAbsent((StorageNode) nextLocation, (long) 0);
            if (state.packetsStoredInNodeNext.get(nextLocation) == storageCapacity) {
                continue;
            }
            // if this point is reached, then packet hasn't been stored yet, its next
            // location is a storage node
            // and the storage node is not at capacity
            // so packet(agent) will be placed at its next location
            state.packetsStoredInNodeNext.put((StorageNode) nextLocation,
                    state.packetsStoredInNodeNext.get(nextLocation) + 1);
            agent.setStoredInStorageNext();

        }
    }

    private void updatePacketCount(NetState state, List<SensorNode> bestJointAction, Agent agent) {

        // Agent agent = state.getAgents().get(i);
        SensorNode nextLocation = bestJointAction.get(0);
        agent.setNextLocation(nextLocation);
        // now determine which (if any) agents will be placed in their next state

        if (agent.getStoredInStorage()) {
            return;
        }
        if (!(nextLocation instanceof StorageNode)) {
            return;
        }
        state.packetsStoredInNodeNext.putIfAbsent((StorageNode) nextLocation, (long) 0);
        if (state.packetsStoredInNodeNext.get(nextLocation) == storageCapacity) {
            return;
        }
        // if this point is reached, then packet hasn't been stored yet, its next
        // location is a storage node
        // and the storage node is not at capacity
        // so packet(agent) will be placed at its next location
        /*
         * state.packetsStoredInNodeNext.put((StorageNode) nextLocation,
         * state.packetsStoredInNodeNext.get(nextLocation) + 1);
         */
        state.packetsStoredInNodeNext.put((StorageNode) nextLocation,
                this.getNetwork().getDataPacketCount());
        agent.setStoredInStorageNext();

    }

    private void updateQTable(NetState state, List<SensorNode> jointAction) {
        String nextState = encodeStateList(jointAction);
        String sStateTState = state.encodeST(nextState);
        if (state.containsQ(sStateTState)) {
            return;
        }

        // if q value is null, set new initial Q value
        //
        double sum = 0.0;
        double weight = 0.0;
        List<Agent> agents = state.getAgents();
        for (int j = 0; j < agents.size(); j++) {
            Agent agent = agents.get(j);

            // transmissionCost
            weight += agent.getCurrentLocation().calculateTransmissionCost(jointAction.get(j));
            // receivingCost
            weight += jointAction.get(j).calculateReceivingCost();
            if (!state.edgeHasReward(agent.getCurrentLocation(), jointAction.get(j))) {
                updateEdgeReward(state, agent, jointAction.get(j));
            }
            sum += state.getEdgeReward(agent.getCurrentLocation(), jointAction.get(j));
        }
        sum = sum / weight;
        state.setQ(sStateTState, sum);
    }

    private void updateQTable(NetState state, List<SensorNode> jointAction, Agent agent) {
        // String nextState = encodeStateList(jointAction);
        String nextState = jointAction.get(0).getUuid() + "";
        String sStateTState = state.encodeST(nextState, agent);
        if (state.containsQ(sStateTState)) {
            return;
        }

        // if q value is null, set new initial Q value
        //
        double sum = 0.0;
        double weight = 0.0;

        // transmissionCost
        // weight +=
        // agent.getCurrentLocation().calculateTransmissionCost(jointAction.get(0));
        weight += this.getNetwork()
                .calculateCostOfPath(this.getNetwork().getMinCostPath(agent.getCurrentLocation(), jointAction.get(0)));
        // receivingCost
        weight += jointAction.get(0).calculateReceivingCost();
        if (!state.edgeHasReward(agent.getCurrentLocation(), jointAction.get(0))) {
            updateEdgeReward(state, agent, jointAction.get(0));
        }
        sum += state.getEdgeReward(agent.getCurrentLocation(), jointAction.get(0));

        sum = sum / weight;
        state.setQ(sStateTState, sum);
    }

    private void findNextState(NetState state) {
        Random rand = new Random();
        double randomQ = rand.nextDouble();

        String nextState;
        String sStateTState;
        // List<List<SensorNode>> allJointActions = generateAllJointActions(state);

        for (Agent agent : state.getAgents()) {
            if (agent.getStoredInStorage()) {
                continue;
            }
            if (randomQ <= 1 - epsilon) {
                // exploitation
                // for each agent, pick the action as per the action rule
                // no "joint" action picked
                double max = Double.NEGATIVE_INFINITY;
                List<SensorNode> bestNode = new ArrayList<>();
                String bestStateTransition = "";

                for (StorageNode sn : this.getNetwork().getStorageNodes()) {
                    if (sn.getUuid() == agent.getCurrentLocation().getUuid()) {
                        continue;
                    }
                    if(state.packetsStoredInNodeNext.get(sn)!=null&&state.packetsStoredInNodeNext.get(sn)<storageCapacity){
                        continue;
                    }
                    SensorNode nextNode = sn;
                    sStateTState = state.encodeST(nextNode.getUuid() + "", agent);
                    List<SensorNode> nextNodeAsList = new ArrayList<>();
                    nextNodeAsList.add(nextNode);
                    updateQTable(state, nextNodeAsList, agent);
                    ProbabilityResult pr = getProbabilityResult(state, sStateTState, nextNodeAsList, agent);

                    // If greater than max is reached, then update the best join action
                    if (pr.numerator() / pr.denominator() > max) {
                        max = pr.numerator() / pr.denominator();
                        bestNode = nextNodeAsList;
                        bestStateTransition = sStateTState;
                    }
                }
                /*for (DataNode dn : this.getNetwork().getDataNodes()) {
                    if (dn.getUuid() == agent.getCurrentLocation().getUuid()) {
                        continue;
                    }
                    SensorNode nextNode = dn;
                    sStateTState = state.encodeST(nextNode.getUuid() + "", agent);
                    List<SensorNode> nextNodeAsList = new ArrayList<>();
                    nextNodeAsList.add(nextNode);
                    updateQTable(state, nextNodeAsList, agent);
                    ProbabilityResult pr = getProbabilityResult(state, sStateTState, nextNodeAsList, agent);

                    // If greater than max is reached, then update the best join action
                    if (pr.numerator() / pr.denominator() > max) {
                        max = pr.numerator() / pr.denominator();
                        bestNode = nextNodeAsList;
                        bestStateTransition = sStateTState;
                    }
                }*/
                updatePacketCount(state, bestNode, agent);
                // state.addStateTransition(bestStateTransition);
                agent.addStateTransition(bestStateTransition);
            } else {
                // exploration
                // first find weights of all actions
                Map<Integer, Double> actionWeights = new HashMap<>();
                List<SensorNode> allNextNodes = new ArrayList<>();

                double probDenominator = 0.0;
                for (StorageNode sn : this.getNetwork().getStorageNodes()) {
                    if (sn.getUuid() == agent.getCurrentLocation().getUuid()) {
                        continue;
                    }
                    if(state.packetsStoredInNodeNext.get(sn)!=null&&state.packetsStoredInNodeNext.get(sn)>=storageCapacity){
                        continue;
                    }
                    allNextNodes.add(sn);
                    SensorNode nextNode = sn;
                    sStateTState = state.encodeST(nextNode.getUuid() + "", agent);
                    List<SensorNode> nextNodeAsList = new ArrayList<>();
                    nextNodeAsList.add(nextNode);
                    updateQTable(state, nextNodeAsList, agent);
                    ProbabilityResult pr = getProbabilityResult(state, sStateTState, nextNodeAsList, agent);
                    probDenominator += pr.numerator() / pr.denominator();
                }
                /*for (DataNode dn : this.getNetwork().getDataNodes()) {
                    if (dn.getUuid() == agent.getCurrentLocation().getUuid()) {
                        continue;
                    }
                    allNextNodes.add(dn);
                    SensorNode nextNode = dn;
                    sStateTState = state.encodeST(nextNode.getUuid() + "", agent);
                    List<SensorNode> nextNodeAsList = new ArrayList<>();
                    nextNodeAsList.add(nextNode);
                    updateQTable(state, nextNodeAsList, agent);
                    ProbabilityResult pr = getProbabilityResult(state, sStateTState, nextNodeAsList, agent);
                    probDenominator += pr.numerator() / pr.denominator();
                }*/
                for (StorageNode sn : this.getNetwork().getStorageNodes()) {
                    if (sn.getUuid() == agent.getCurrentLocation().getUuid()) {
                        continue;
                    }
                    if(state.packetsStoredInNodeNext.get(sn)!=null&&state.packetsStoredInNodeNext.get(sn)>=storageCapacity){
                        continue;
                    }
                    double probNumerator;

                    SensorNode nextNode = sn;
                    List<SensorNode> nextNodeAsList = new ArrayList<>();
                    nextNodeAsList.add(nextNode);

                    nextState = encodeStateList(nextNodeAsList);
                    sStateTState = state.encodeST(nextState, agent);

                    ProbabilityResult pr = getProbabilityResult(state, sStateTState, nextNodeAsList, agent);

                    probNumerator = pr.numerator() / pr.denominator();
                    double actionWeight = probNumerator / probDenominator;
                    actionWeights.put(Objects.hashCode(nextNode.getUuid()), actionWeight);
                }
                /*for (DataNode dn : this.getNetwork().getDataNodes()) {
                    if (dn.getUuid() == agent.getCurrentLocation().getUuid()) {
                        continue;
                    }
                    double probNumerator;

                    SensorNode nextNode = dn;
                    List<SensorNode> nextNodeAsList = new ArrayList<>();
                    nextNodeAsList.add(nextNode);

                    nextState = encodeStateList(nextNodeAsList);
                    sStateTState = state.encodeST(nextState, agent);

                    ProbabilityResult pr = getProbabilityResult(state, sStateTState, nextNodeAsList, agent);

                    probNumerator = pr.numerator() / pr.denominator();
                    double actionWeight = probNumerator / probDenominator;
                    actionWeights.put(Objects.hashCode(nextNode.getUuid()), actionWeight);
                }*/
                SensorNode randomAction = getRandomActionNew(allNextNodes, actionWeights);
                // next (random) action has been found

                // updatePacketCount(state, bestNode, agent);
                // state.addStateTransition(bestStateTransition);

                List<SensorNode> nextNodeAsList = new ArrayList<>();
                nextNodeAsList.add(randomAction);

                updatePacketCount(state, nextNodeAsList, agent);
                nextState = encodeStateList(nextNodeAsList);
                sStateTState = state.encodeST(nextState, agent);
                // state.addStateTransition(sStateTState);
                agent.addStateTransition(sStateTState);
            }
        }

    }

    private static ProbabilityResult getProbabilityResult(NetState state, String sStateTState,
            List<SensorNode> jointAction) {
        double numerator = Math.pow(state.getQ(sStateTState), delta);
        double denominator = 0.0;

        for (int j = 0; j < jointAction.size(); j++) {
            denominator += state.getAgents().get(j).getCurrentLocation()
                    .calculateTransmissionCost(jointAction.get(j));
            denominator += jointAction.get(j).calculateReceivingCost();
        }
        denominator = Math.pow(denominator, beta);
        return new ProbabilityResult(numerator, denominator);
    }

    private static ProbabilityResult getProbabilityResult(NetState state, String sStateTState,
            List<SensorNode> jointAction, Agent agent) {
        double pu = nonStorageReward;
        if (jointAction.get(0) instanceof StorageNode) {
            if (state.packetsStoredInNodeNext.get(jointAction.get(0)) == null
                    || state.packetsStoredInNodeNext.get(jointAction.get(0)) > 0) {
                pu = storageReward;
            }

        }
        double numerator = Math.pow(state.getQ(sStateTState), delta) * pu;
        double denominator = 0.0;

        denominator += agent.getCurrentLocation()
                .calculateTransmissionCost(jointAction.get(0));
        denominator += jointAction.get(0).calculateReceivingCost();

        denominator = Math.pow(denominator, beta);
        return new ProbabilityResult(numerator, denominator);
    }

    private record ProbabilityResult(double numerator, double denominator) {
    }

    private List<SensorNode> getRandomAction(List<List<SensorNode>> allJointActions,
            Map<Integer, Double> actionWeights) {
        // calculate total weight
        double totalWeight = 0.0;
        List<SensorNode> randomJointAction = allJointActions.get(0);
        for (List<SensorNode> jointAction : allJointActions) {
            totalWeight += actionWeights.get(Objects.hashCode(jointAction));
        }
        double randomNum = new Random().nextDouble() * totalWeight;

        for (List<SensorNode> jointAction : allJointActions) {
            randomNum -= actionWeights.get(Objects.hashCode(jointAction));

            if (randomNum <= 0) {
                randomJointAction = jointAction;
                break;
            }
        }
        return randomJointAction;

    }

    private SensorNode getRandomActionNew(List<SensorNode> allNextNodes,
            Map<Integer, Double> actionWeights) {
        // calculate total weight
        double totalWeight = 0.0;
        SensorNode randomAction = allNextNodes.get(0);
        for (SensorNode nodeAction : allNextNodes) {
            totalWeight += actionWeights.get(Objects.hashCode(nodeAction.getUuid()));
        }
        double randomNum = new Random().nextDouble() * totalWeight;

        for (SensorNode nodeAction : allNextNodes) {
            randomNum -= actionWeights.get(Objects.hashCode(nodeAction.getUuid()));

            if (randomNum <= 0) {
                randomAction = nodeAction;
                break;
            }
        }
        return randomAction;

    }

    private String encodeStateList(List<SensorNode> list) {
        StringJoiner sj = new StringJoiner("-");
        for (SensorNode sensorNode : list) {
            sj.add(Long.toString(sensorNode.getUuid()));
        }
        return sj.toString();
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
                continue;
            }
            neighbors = ((SensorNetwork) this.getNetwork()).getNeighbors(agent.getCurrentLocation());
            possibleHops = neighbors.stream().filter(node -> !agent.getRoute().contains(node))
                    .collect(Collectors.toList());

            // If dead-end, then retrace steps
            if (possibleHops.isEmpty()) {
                possibleHops.add(agent.getRoute().get(agent.getRoute().indexOf(agent.getCurrentLocation()) - 1));
            }
            possibleTravels.add(possibleHops);
        }
        // then make cartesian product of each agent's set
        jointActions = Lists.cartesianProduct(possibleTravels);
        return jointActions;
    }

    private List<List<SensorNode>> generateAllNextJointActions(NetState state) {
        List<List<SensorNode>> jointActions;
        List<List<SensorNode>> possibleTravels = new ArrayList<>();

        SensorNetwork sensorNetwork = (SensorNetwork) this.getNetwork();
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
            neighbors = sensorNetwork.getNeighbors(agent.getNextLocation());
            possibleHops = new ArrayList<>(neighbors);
            possibleTravels.add(possibleHops);
        }
        // then make cartesian product of each agents set
        jointActions = Lists.cartesianProduct(possibleTravels);
        return jointActions;

    }

    public void printRoute() {
        if (!this.hasRan()) {
            throw new IllegalStateException("Model hasn't been run yet!");
        }

        NetState state = this.finalState;
        StringJoiner str;
        List<SensorNode> route;
        for (Agent agent : state.getAgents()) {
            route = agent.getRoute();
            System.out.printf("%s -> %s\n", route.get(0).getName(), route.get(agent.getRoute().size() - 1).getName());
            str = new StringJoiner(" -> ", "[", "]");
            for (SensorNode node : route) {
                str.add(node.getName());
            }
            System.out.printf("\t%s\n", str);
        }
    }

    public long getTotalCost() {
        super.getTotalCost();
        return this.totalCost;
    }

    public long getTotalValue() {
        super.getTotalValue();
        return this.totalValue;
    }

    public long getTotalProfit() {
        super.getTotalProfit();
        return this.totalProfit;
    }
}
