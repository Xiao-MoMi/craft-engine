package net.momirealms.craftengine.core.plugin.config.blockbench;

import com.google.gson.annotations.SerializedName;

public class Faces {
    @SerializedName("north")
    private Face north;
    @SerializedName("east")
    private Face east;
    @SerializedName("south")
    private Face south;
    @SerializedName("west")
    private Face west;
    @SerializedName("up")
    private Face up;
    @SerializedName("down")
    private Face down;

    public Face north() {
        return north;
    }

    public Face east() {
        return east;
    }

    public Face south() {
        return south;
    }

    public Face west() {
        return west;
    }

    public Face up() {
        return up;
    }

    public Face down() {
        return down;
    }

    public static class Face {
        @SerializedName("uv")
        private double[] uv;
        @SerializedName("texture")
        private int texture;

        public double[] uv() {
            return uv;
        }

        public int texture() {
            return texture;
        }
    }
}
