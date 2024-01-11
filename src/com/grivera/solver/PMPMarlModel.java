package com.grivera.solver;

import java.util.*;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import com.grivera.generator.Network;
import com.grivera.generator.SensorNetwork;
import com.grivera.generator.sensors.StorageNode;
import com.grivera.util.ProgressBar;
import com.grivera.generator.sensors.SensorNode;

public class PMPMarlModel extends AbstractModel {
    private double alpha;
    private double alphaStart = .1;
    private double alphaEnd = .1;
    private double gamma;
    private double gammaStart = .1;
    private double gammaEnd = .1;
    private double epsilon;
    private double epsilonStart = .2;
    private double epsilonEnd = .2;
    private int episodes;
    private static final int delta = 1;
    private static final int beta = 2;
    private static final int w = 10000;
    private static final double storageReward = 1000;
    private static final double nonStorageReward = 1;
    private int storageCapacity;
    private int totalCost;
    private int totalProfit;
    private NetState finalState;

    public PMPMarlModel(Network network) {
        super(network);
        setField();
    }

    public PMPMarlModel(String fileName) {
        super(fileName);
        setField();
    }

    public PMPMarlModel(String fileName, int overflowPackets, int storageCapacity) {
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
        int max = Integer.MIN_VALUE; // line 0.
        // create state object with nodes and an agent for each packet
        NetState state = new NetState(network);

        // initialize empty Q-table
        state.initQTable();

        ProgressBar bar = new ProgressBar(epi, 50, "Training");
        String lastStateTransition = "";
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

                moveAgentsToState(state);
                // update state-reward value
                double newVal = 0.0;
                String stateTransition = state.encodeST();
                for (Agent agent : state.getAgents()) {
                    if (!state.edgeHasReward(agent.getCurrentLocation(), agent.getNextLocation())) {
                        updateEdgeReward(state, agent);
                    }
                    // after this add to sum
                    newVal += state.getEdgeReward(agent.getCurrentLocation(), agent.getNextLocation());
                }
                // commenting out this line, state transition reward not being updated here

                // update Q value
                String sStateTState = state.encodeST();

                double newQValue = (1 - alpha) * state.getQ(sStateTState) + alpha * gamma * maxQNextState(state);// check if
                                                                                                            // null? no,
                                                                                                            // already
                                                                                                            // checked
                                                                                                            // when next
                                                                                                            // state was
                                                                                                            // being
                                                                                                            // picked
                state.setQ(sStateTState, newQValue);
                lastStateTransition = sStateTState;
                // s=t move to next state
                // for each agent:
                // so currentlocation=nextLocation
                // storedinStorage = storedInStorageNext
                // state.packetsStoredInNode(agent.nextLocation) =
                // state.packetsStoredInNodeNext(agent.nextLocation)
                updateState(state);
            } // end while
              // all agents have arrived at storage node
            for (Agent agent : state.getAgents()) {
                cost += agent.getTravelCost();
                reward += (int) agent.getRewardCol();
            }
            profit = reward - cost;
            if (profit > max) {// 28.
                max = profit;
                // update edge rewards for each agent's route
                for (Agent agent : state.getAgents()) {
                    // double netTransitionReward=0.0;
                    for (int c = 1; c < agent.getRoute().size(); c++) {
                        SensorNode prevNode = agent.getRoute().get(c - 1);
                        SensorNode currNode = agent.getRoute().get(c);
                        if (prevNode == null || currNode == null) {
                            System.out.println("node in route is null");
                        }

                        double newReward = state.getEdgeReward(prevNode, currNode) + (double) (w / cost);
                        state.setEdgeReward(prevNode, currNode, newReward); // line 30.

                    }
                }
                for (int j = 0; j < state.stateTransitions.size(); j++) {
                    String transition = state.stateTransitions.get(j); // (s,t)
                    double totalReward = 0.0;// r(s,t)
                    double totalCost = 0.0; // c(s,t)
                    double stateTransitProfit; // p(s,t)
                    for (Agent agent : state.getAgents()) {
                        if (j + 1 < agent.getRoute().size()) {
                            SensorNode currNode = agent.getRoute().get(j);
                            SensorNode nextNode = agent.getRoute().get(j + 1);
                            // if j+1 is less than the route size then node changed in state transition
                            // first transition is route.j to route j+1
                            totalReward += state.getEdgeReward(currNode, nextNode);
                            totalCost += currNode.calculateTransmissionCost(nextNode);
                            totalCost += nextNode.calculateReceivingCost();
                        }
                    }
                    state.setTransitionReward(transition, totalReward); // line 32
                    stateTransitProfit = totalReward - totalCost;
                    state.setTransitionProfit(transition, stateTransitProfit); // line 33.
                    double newQValue = (1 - alpha) * state.getQ(transition)
                            + alpha * (state.getTransitionProfit(transition)
                                    + gamma * state.maxQNextTransition.get(transition));
                    state.setQ(transition, newQValue); // line 34.
                }

            }

            // reset for new episode
        } // end of each episode in learning stage
        for (Agent agent : state.getAgents()) {
            agent.resetLocation();
            agent.resetTravel();
        }
        state.reset();
        // execution stage
        while (!state.allAgentsAtStorage()) {
            findNextStateExecution(state);
            // add route, add cost
            moveAgentsToStateExecution(state);
            // s=t move to next state
            // for each agent:
            // so currentlocation=nextLocation
            // storedinStorage = storedInStorageNext
            // state.packetsStoredInNode(agent.nextLocation) =
            // state.packetsStoredInNodeNext(agent.nextLocation)
            updateState(state);
        }
        // return route, return Cost of Route, return profit of route
        // or just sys.out
        int costOfRoute = 0;
        int profitOfRoute = 0;
        for (Agent agent : state.getAgents()) {
            costOfRoute += agent.calculateCostOfPath();
            profitOfRoute += agent.getPacketValue();
        }
        profitOfRoute -= costOfRoute;

        this.totalCost = costOfRoute;
        this.totalProfit = profitOfRoute;
        this.finalState = state;
    }

    private void updateGlobalProfit(NetState state, String lastStateTransition) {
        double globalProfit;
        globalProfit = state.getTransitionReward(lastStateTransition);
        int transmitCost = 0;
        int receiveCost = 0;
        for (Agent agent : state.getAgents()) {
            transmitCost += agent.getLastStateLocation().calculateTransmissionCost(agent.getCurrentLocation());
            receiveCost += agent.getCurrentLocation().calculateReceivingCost();
        }
        globalProfit -= transmitCost + receiveCost;
        state.setTransitionProfit(lastStateTransition, globalProfit);
    }

    private void updateGlobalReward(NetState state, String lastStateTransition) {
        double globalReward = 0.0;
        for (Agent agent : state.getAgents()) {
            globalReward += state.getEdgeReward(agent.getLastStateLocation(), agent.getCurrentLocation());
        }
        state.setTransitionReward(lastStateTransition, globalReward);
    }

    private void moveAgentsToState(NetState state) {
        for (Agent agent : state.getAgents()) {
            int travelCost = agent.getTravelCost();
            double reward = agent.getRewardCol();
            int transmitCost = agent.getCurrentLocation().calculateTransmissionCost(agent.getNextLocation());
            int receiveCost = agent.getNextLocation().calculateReceivingCost();
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

    private void moveAgentsToStateExecution(NetState state) {
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
        state.maxQNextTransition.put(sStateTState, maxQValue);
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
            state.packetsStoredInNodeNext.putIfAbsent((StorageNode) nextLocation, 0);
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

    private void findNextState(NetState state) {
        Random rand = new Random();
        double randomQ = rand.nextDouble();

        String nextState;
        String sStateTState;
        List<List<SensorNode>> allJointActions = generateAllJointActions(state);
        if (randomQ <= 1 - epsilon) {

            // exploitation
            // pick the joint action with the highest value of its Qvalue^delta
            // divided by the total cost of the state change^beta

            // Compare all possible joint actions and pick the joint action
            // according to the transition rule
            double max = Double.NEGATIVE_INFINITY;
            List<SensorNode> bestJointAction = allJointActions.get(0);

            String bestStateTransition = "";
            for (List<SensorNode> jointAction : allJointActions) {
                nextState = encodeStateList(jointAction);
                sStateTState = state.encodeST(nextState);
                updateQTable(state, jointAction);
                ProbabilityResult pr = getProbabilityResult(state, sStateTState, jointAction);

                // If greater than max is reached, then update the best join action
                if (pr.numerator() / pr.denominator() > max) {
                    max = pr.numerator() / pr.denominator();
                    bestJointAction = jointAction;
                    bestStateTransition = sStateTState;
                }
            }
            // next state has been found using transition rule
            updatePacketCount(state, bestJointAction);
            state.addStateTransition(bestStateTransition);
        } else {

            // exploration
            // first find weights of all actions
            Map<Integer, Double> actionWeights = new HashMap<>();

            double probDenominator = 0.0;
            for (List<SensorNode> jointAction : allJointActions) {
                nextState = encodeStateList(jointAction);
                sStateTState = state.encodeST(nextState);
                updateQTable(state, jointAction);
                ProbabilityResult pr = getProbabilityResult(state, sStateTState, jointAction);
                probDenominator += pr.numerator() / pr.denominator();
            }
            for (List<SensorNode> jointAction : allJointActions) {
                double probNumerator;

                nextState = encodeStateList(jointAction);
                sStateTState = state.encodeST(nextState);

                ProbabilityResult pr = getProbabilityResult(state, sStateTState, jointAction);

                probNumerator = pr.numerator() / pr.denominator();
                double actionWeight = probNumerator / probDenominator;
                actionWeights.put(Objects.hashCode(jointAction), actionWeight);
            }
            List<SensorNode> randomJointAction = getRandomAction(allJointActions, actionWeights);
            // next (random) action has been found

            updatePacketCount(state, randomJointAction);
            nextState = encodeStateList(randomJointAction);
            sStateTState = state.encodeST(nextState);
            state.addStateTransition(sStateTState);
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

    private String encodeStateList(List<SensorNode> list) {
        StringJoiner sj = new StringJoiner("-");
        for (SensorNode sensorNode : list) {
            sj.add(Integer.toString(sensorNode.getUuid()));
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

    public int getTotalCost() {
        super.getTotalCost();
        return this.totalCost;
    }

    public int getTotalProfit() {
        super.getTotalProfit();
        return this.totalProfit;
    }
}
