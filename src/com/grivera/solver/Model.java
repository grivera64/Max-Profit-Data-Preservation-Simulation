package com.grivera.solver;

public interface Model {
    void run();
    void run(int episodes);
    long getTotalCost();
    long getTotalProfit();
    long getTotalValue();
    long getTotalPackets();
    void printRoute();
}
