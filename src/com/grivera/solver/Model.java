package com.grivera.solver;

public interface Model {
    void run();
    void run(int episodes);
    int getTotalCost();
    int getTotalProfit();
    void printRoute();
}
