package com.grivera.solver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.grivera.generator.sensors.SensorNode;
import com.grivera.generator.sensors.StorageNode;

public class NetState {
    private int numNodes;
    private int numAgents;
    private List<Agent> agents;
    private Map<String, Double> Q;
    public Map<SensorNode, Map<SensorNode, Double>> edgeReward = new HashMap<>();
    public Map<String, Double> stateTransitionReward = new HashMap<>();
    public List<String> stateTransitions = new ArrayList<>();
    public final int delta = 1;
    public final int beta = 2;
    public Map<StorageNode, Integer> packetsStoredInNode = new HashMap<>();
    public Map<StorageNode, Integer> packetsStoredInNodeNext = new HashMap<>();
    public Map<String,Double> maxQNextTransition = new HashMap<>();

    public NetState(int numNodes, int numAgents) {
        this.numNodes = numNodes;
        this.numAgents = numAgents;
        agents = new ArrayList<Agent>();
        for (int i = 0; i < numAgents; i++) {
            agents.add(new Agent());
        }
    }

    public void resetForEpisode(){
        //for episode, reset stateTransitions, packetsStoredinNode, packetsStoredinNodeNext, maxQnextTransition
        stateTransitionReward = new HashMap<>();
        packetsStoredInNode = new HashMap<>();
        packetsStoredInNodeNext = new HashMap<>();
        maxQNextTransition = new HashMap<>();
    }

    public List<Agent> getAgents() {
        return agents;
    }

    public void setAgents(List<Agent> agents) {
        this.agents = agents;
    }

    public String encodeState() {
        // represent state as a string
        StringBuilder state = new StringBuilder("");
        for (Agent agent : agents) {
            // state[agent.getCurrentLocation()] = 1.0;
            state.append(agent.getCurrentLocation().getUuid());
        }
        return state.toString();
    }

    public void setQTable() {
        this.Q = new HashMap<>();
    }

    public Map<String, Double> getQTable() {
        return Q;
    }

    public boolean allAgentsAtStorage() {
        // returns true if all agents (packets) are stored in a Storage node
        // returns false otherwise
        boolean flag = true;
        for (Agent agent : agents) {
            if (agent.getStoredInStorage() == false) {
                flag = false;
                break;
            }
        }
        return flag;
    }

    public String encodeNextState() {
        // represent state as a string
        StringBuilder state = new StringBuilder("");
        for (Agent agent : agents) {
            // state[agent.getCurrentLocation()] = 1.0;
            state.append(agent.getNextLocation().getUuid());
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
        stateTransitions.add(bestStateTransition);
    }
}
