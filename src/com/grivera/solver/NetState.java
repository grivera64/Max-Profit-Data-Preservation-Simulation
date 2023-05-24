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
    public Map<StorageNode, Integer> packetsStoredInNode = new HashMap<>();
    public Map<StorageNode, Integer> packetsStoredInNodeNext = new HashMap<>();
    public Map<String,Double> maxQNextTransition = new HashMap<>();

    public NetState(Network network) {
        this.numNodes = network.getSensorNodeCount();
        this.numAgents = network.getDataNodeCount() * dataPacketCount;
        
        agents = new ArrayList<Agent>();
        
        // for each agent/packet set current/original location and value
        int packetCount = 0;
        Agent currAgent;
        for (DataNode dn : network.getDataNodes()) {
            for (int j = 0; j < dn.getOverflowPackets(); j++) {
                currAgent = new Agent();
                currAgent.setCurrentLocation(dn);
                currAgent.setOriginalLocation(dn);
                currAgent.setPacketValue(dn.getOverflowPacketValue());
                
                agents.add(currAgent);
                packetCount++;
            }
        }
    }

    public void resetForEpisode(){
        //for episode, reset stateTransitions, packetsStoredinNode, packetsStoredinNodeNext, maxQnextTransition
        //stateTransitionReward = new HashMap<>();
        packetsStoredInNode = new HashMap<>();
        packetsStoredInNodeNext = new HashMap<>();
        maxQNextTransition = new HashMap<>();
        stateTransitions = new ArrayList<>();
        
        // agent j is at source node Sj
        for (Agent agent : this.agents) {
            agent.resetLocation();
            // travel cost by agent j
            agent.resetTravel();
        }
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
        for (Agent agent : agents) {
            if (!agent.getStoredInStorage()) {
                return false;
            }
        }
        return true;
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
