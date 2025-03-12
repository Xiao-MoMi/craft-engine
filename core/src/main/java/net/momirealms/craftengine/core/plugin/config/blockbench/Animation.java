/*
 * Copyright (C) <2025> <XiaoMoMi>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.momirealms.craftengine.core.plugin.config.blockbench;

import com.google.gson.annotations.SerializedName;

import java.util.Map;
import java.util.UUID;

public class Animation {
    @SerializedName("uuid")
    private UUID uuid;
    @SerializedName("name")
    private String name;
    @SerializedName("loop")
    private String loop;
    @SerializedName("override")
    private boolean override;
    @SerializedName("length")
    private int length;
    @SerializedName("snapping")
    private int snapping;
    @SerializedName("selected")
    private boolean selected;
    @SerializedName("anim_time_update")
    private String anim_time_update;
    @SerializedName("blend_weight")
    private String blend_weight;
    @SerializedName("start_delay")
    private String start_delay;
    @SerializedName("loop_delay")
    private String loop_delay;
    @SerializedName("animators")
    private Map<UUID, Animator> animators;

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public String loop() {
        return loop;
    }

    public boolean override() {
        return override;
    }

    public int length() {
        return length;
    }

    public int snapping() {
        return snapping;
    }

    public boolean selected() {
        return selected;
    }

    public String animTimeUpdate() {
        return anim_time_update;
    }

    public String blendWeight() {
        return blend_weight;
    }

    public String startDelay() {
        return start_delay;
    }

    public String loopDelay() {
        return loop_delay;
    }

    public Map<UUID, Animator> animators() {
        return animators;
    }

    public static class Animator {
        @SerializedName("name")
        private String name;
        @SerializedName("type")
        private String type;
        @SerializedName("keyframes")
        private KeyFrame[] keyframes;

        public String name() {
            return name;
        }

        public String type() {
            return type;
        }

        public KeyFrame[] keyframes() {
            return keyframes;
        }

        public static class KeyFrame {
            @SerializedName("channel")
            private String channel;
            @SerializedName("data_points")
            private DataPoint[] data_points;
            @SerializedName("uuid")
            private UUID uuid;
            @SerializedName("time")
            private double time;
            @SerializedName("color")
            private int color;
            @SerializedName("interpolation")
            private String interpolation;

            public String channel() {
                return channel;
            }

            public DataPoint[] dataPoints() {
                return data_points;
            }

            public UUID uuid() {
                return uuid;
            }

            public double time() {
                return time;
            }

            public int color() {
                return color;
            }

            public String interpolation() {
                return interpolation;
            }

            public static class DataPoint {
                @SerializedName("x")
                private String x;
                @SerializedName("y")
                private String y;
                @SerializedName("z")
                private String z;

                public String x() {
                    return x;
                }

                public String y() {
                    return y;
                }

                public String z() {
                    return z;
                }
            }
        }
    }
}
