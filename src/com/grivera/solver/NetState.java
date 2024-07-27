package com.grivera.solver;

import java.util.*;

import com.grivera.generator.Network;
import com.grivera.generator.sensors.DataNode;
import com.grivera.generator.sensors.SensorNode;
import com.grivera.generator.sensors.StorageNode;

public class NetState {
    private List<Agent> agents;
    private Map<String, Double> Q;
    private Map<SensorNode, Map<SensorNode, Double>> edgeReward = new HashMap<>();
    private Map<String, Double> stateTransitionReward = new HashMap<>();
    private Map<String, Double> stateTransitionProfit = new HashMap<>();

    public List<String> stateTransitions = new ArrayList<>();
    public Map<StorageNode, Integer> packetsStoredInNode = new HashMap<>();
    public Map<StorageNode, Integer> packetsStoredInNodeNext = new HashMap<>();
    public Map<String, Double> maxQNextTransition = new HashMap<>();

    public NetState(Network network) {
        this.agents = new ArrayList<>();
        
        // for each agent/packet set current/original location and value
        Agent currAgent;
        for (DataNode dn : network.getDataNodes()) {
            for (int j = 0; j < dn.getOverflowPackets(); j++) {
                currAgent = new Agent();
                currAgent.setCurrentLocation(dn);
                currAgent.setOriginalLocation(dn);
                currAgent.setPacketValue(dn.getOverflowPacketValue());
                
                this.agents.add(currAgent);
            }
        }
    }

    public void reset() {
        //for episode, reset stateTransitions, packetsStoredinNode, packetsStoredinNodeNext, maxQnextTransition
        //stateTransitionReward = new HashMap<>();
        this.packetsStoredInNode = new HashMap<>();
        this.packetsStoredInNodeNext = new HashMap<>();
        this.maxQNextTransition = new HashMap<>();
        this.stateTransitions = new ArrayList<>();
        
        // agent j is at source node Sj
        for (Agent agent : this.agents) {
            agent.resetLocation();
            // travel cost by agent j
            agent.resetTravel();
        }
    }

    public List<Agent> getAgents() {
        return this.agents;
    }

    public void setAgents(List<Agent> agents) {
        this.agents = agents;
    }

    public String encodeState() {
        // represent state as a string
        StringJoiner state = new StringJoiner("-");
        for (Agent agent : this.agents) {
            state.add(Long.toString(agent.getCurrentLocation().getUuid()));
        }
        return state.toString();
    }

    public void initQTable() {
        this.Q = new HashMap<>();
    }

    public boolean allAgentsAtStorage() {
        // returns true if all agents (packets) are stored in a Storage node
        // returns false otherwise
        for (Agent agent : this.agents) {
            if (!agent.getStoredInStorage()) {
                return false;
            }
        }
        return true;
    }

    public String encodeNextState() {
        // represent state as a string
        StringJoiner state = new StringJoiner("-");
        for (Agent agent : this.agents) {
            state.add(Long.toString(agent.getNextLocation().getUuid()));
        }
        return state.toString();
    }

    public String encodeST(String nextState) {
        return encodeState() + "-" + nextState;
    }

    public String encodeST() {
        return encodeState() + "-" + encodeNextState();
    }

    public String encodeTZ(String nextNextState) {
        return encodeNextState() + "-" + nextNextState;
    }

    public void addStateTransition(String bestStateTransition) {
        this.stateTransitions.add(bestStateTransition);
    }
    public Map<String, Double> getStateTransitionProfit() {
        return this.stateTransitionProfit;
    }

    public void setStateTransitionProfit(Map<String, Double> stateTransitionProfit) {
        this.stateTransitionProfit = stateTransitionProfit;
    }

    public double getEdgeReward(SensorNode from, SensorNode to) {
        if (!this.edgeReward.containsKey(from)) {
            throw new IllegalArgumentException(String.format("edge from %s does not exist", from.getName()));
        }
        if (!this.edgeReward.get(from).containsKey(to)) {
            throw new IllegalArgumentException(String.format("edge from %s to %s does not exist", from.getName(), to.getName()));
        }
        return this.edgeReward.get(from).get(to);
    }

    public void setEdgeReward(SensorNode from, SensorNode to, double reward) {
        this.edgeReward.putIfAbsent(from, new HashMap<>());
        this.edgeReward.get(from).put(to, reward);
    }

    public boolean edgeHasReward(SensorNode from, SensorNode to) {
        return this.edgeReward.containsKey(from) && this.edgeReward.get(from).containsKey(to);
    }

    public double getTransitionReward(String encodedTransition) {
        if (!this.stateTransitionReward.containsKey(encodedTransition)) {
            throw new IllegalArgumentException(String.format("State %s does not have a reward", encodedTransition));
        }
        return this.stateTransitionReward.get(encodedTransition);
    }

    public void setTransitionReward(String encodedTransition, double reward) {
        this.stateTransitionReward.put(encodedTransition, reward);
    }

    public double getTransitionProfit(String encodedTransition) {
        if (!this.stateTransitionProfit.containsKey(encodedTransition)) {
            throw new IllegalArgumentException(String.format("State %s does not have a reward", encodedTransition));
        }
        return this.stateTransitionProfit.get(encodedTransition);
    }

    public void setTransitionProfit(String encodedTransition, double profit) {
        this.stateTransitionProfit.put(encodedTransition, profit);
    }

    public double getQ(String encodedTransition) {
        return this.Q.get(encodedTransition);
    }

    public void setQ(String encodedTransition, double qValue) {
        this.Q.put(encodedTransition, qValue);
    }

    public boolean containsQ(String encodedTransition) {
        return this.Q.containsKey(encodedTransition) && Q.get(encodedTransition) != null;
    }

    public double getMaxQ(String encodedTransition) {
        return this.maxQNextTransition.get(encodedTransition);
    }
    
    public void setMaxQ(String encodedTransition, double maxQ) {
        this.maxQNextTransition.put(encodedTransition, maxQ);
    }
}
