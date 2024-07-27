package com.grivera.generator.sensors;

/**
 * Represents a Sensor Node in a com.grivera.generator.Network that has storage space for overflow data packets.
 *
 * @see SensorNode
 */
public class StorageNode extends SensorNode {

    private static final double E_store = 100e-9;

    private static long idCounter = 1;
    private long id;
    private long capacity;
    private long usedSpace;

    public StorageNode(double x, double y, double tr, long capacity) {
        super(x, y, tr, String.format("SN%02d", idCounter));
        this.id = idCounter++;
        this.setCapacity(capacity);
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
        this.usedSpace = 0;
    }

    public long getCapacity() {
        return this.capacity;
    }

    public long getUsedSpace() {
        return this.usedSpace;
    }

    public boolean isFull() {
        return this.usedSpace >= this.capacity;
    }

    public boolean canStore(long deltaPackets) {
        return this.usedSpace + deltaPackets <= this.capacity;
    }

    public void storePackets(long packets) {
        if (!this.canStore(packets)) {
            throw new IllegalArgumentException(
                    String.format("%s cannot store %d packets (%d/%d full)",
                            this.getName(), packets, this.usedSpace, this.capacity
                    )
            );
        }
        this.usedSpace += packets;
    }

    @Override
    public void resetPackets() {
        this.usedSpace = 0;
    }

    @Override
    public long getId() {
        return this.id;
    }

    public long getSpaceLeft() {
        return this.capacity - this.usedSpace;
    }

    public int calculateStorageCost() {
        double cost = this.usedSpace * BITS_PER_PACKET * E_store;
        return (int) Math.round(cost * Math.pow(10, 6));
    }

    public static void resetCounter() {
        idCounter = 1;
    }
}
