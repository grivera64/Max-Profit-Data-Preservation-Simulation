package com.grivera.generator.sensors;

public class TransitionNode extends SensorNode {
    private static int idCounter = 1;

    public TransitionNode(double x, double y, double tr) {
        super(x, y, tr, String.format("TN%02d", idCounter++));
    }

    @Override
    public void resetPackets() { /* Do Nothing */ }

    public static void resetCounter() {
        idCounter = 1;
    }
}
