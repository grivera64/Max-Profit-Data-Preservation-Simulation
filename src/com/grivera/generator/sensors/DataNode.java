package com.grivera.generator.sensors;

/**
 * Represents a Sensor Node in a com.grivera.generator.Network that has overflow data packets to store.
 *
 * @see SensorNode
 */
public class DataNode extends SensorNode {

    private static long idCounter = 1;
    private long id;
    private long overflowPackets;
    private long overflowPacketsValue;
    private long packetsLeft;

    public DataNode(double x, double y, double tr, long overflowPackets, long overflowPacketsValue) {
        super(x, y, tr, String.format("DN%02d", idCounter));
        this.id = idCounter++;
        this.setOverflowPackets(overflowPackets);
        this.overflowPacketsValue = overflowPacketsValue;
    }

    public void setOverflowPackets(long overflowPackets) {
        this.overflowPackets = overflowPackets;
        this.packetsLeft = overflowPackets;
    }

    public long getOverflowPackets() {
        return this.overflowPackets;
    }

    public boolean isEmpty() {
        return this.packetsLeft < 1;
    }

    public boolean canRemovePackets(long deltaPackets) {
        return this.packetsLeft - deltaPackets >= 0;
    }

    public void removePackets(long packets) {
        if (!this.canRemovePackets(packets)) {
            throw new IllegalArgumentException(
                    String.format("%s cannot remove %d packets (%d/%d left)",
                            this.getName(), packets, this.packetsLeft, this.overflowPackets
                    )
            );
        }
        this.packetsLeft -= packets;
    }

    @Override
    public void resetPackets() {
        this.packetsLeft = this.overflowPackets;
    }

    @Override
    public long getId() {
        return this.id;
    }

    public long getPacketsLeft() {
        return this.packetsLeft;
    }

    public long getOverflowPacketValue() {
        return this.overflowPacketsValue;
    }

    public static void resetCounter() {
        idCounter = 1;
    }

}
