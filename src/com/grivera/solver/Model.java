package com.grivera.solver;

public interface Model {
    void run();
    void run(int epi);
    int getTotalCost();
    int getTotalProfit();
}
