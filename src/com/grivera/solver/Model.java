package com.grivera.solver;

public interface Model {
    void run();
    void run(int episodes);
    int getTotalValue();
    int getTotalCost();
    int getTotalProfit();
    int getTotalPackets();
    void printRoute();
}