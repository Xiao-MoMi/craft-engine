package net.momirealms.craftengine.core.entity.furniture;

public class FurnitureHitData {
    private int times;
    private long lastHitTime;
    private int lastHitFurniture;

    public int hit(int hitFurniture) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastHitTime > 2000 || hitFurniture != this.lastHitFurniture) {
            this.times = 0;
        }

        this.times++;
        this.lastHitFurniture = hitFurniture;
        this.lastHitTime = currentTime;
        return this.times;
    }

    public int times(int hitFurniture) {
        if (hitFurniture != this.lastHitFurniture) return 0;
        return this.times;
    }

    public void setTimes(int times) {
        this.times = times;
    }

    public void reset() {
        this.times = 0;
        this.lastHitTime = 0;
        this.lastHitFurniture = -1;
    }
}
