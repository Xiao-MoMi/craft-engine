package net.momirealms.craftengine.core.pack.obfuscation;

import com.google.gson.*;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/*
    In order to reduce the possibility of being easily reversed,
    we have obfuscated some codes. This behavior is to reduce the
    possibility of resource packs being cracked. Hope you can understand.
 */
@SuppressWarnings({"all"})
public class ObfE {
    private static final Gson 技森二号为你服务 = new Gson();

    protected static boolean 哈基米(Path xswl, boolean yyds) {
        if (!Files.isRegularFile(xswl)) return false;
        if (!yyds) {
            String nb = xswl.getFileName().toString();
            return nb.endsWith(".json") || nb.endsWith(".mcmeta");
        } else {
            try {
                技森二号为你服务.fromJson(Files.readString(xswl), Object.class);
                return true;
            } catch (Exception dddd) {
                return false;
            }
        }
    }

    protected static String 哈吉哈吉米(JsonElement xswl) {
        JsonElement yyds = 啊对对对(xswl);
        String nb = 技森二号为你服务.toJson(yyds);
        return nb.replace("\\\\u", "\\u");
    }

    private static String 哈牛魔啊啊米诺斯(String xswl) {
        StringBuilder yyds = new StringBuilder();
        for (char nb : xswl.toCharArray()) {
            yyds.append(String.format("\\u%04x", (int) nb));
        }
        return yyds.toString();
    }

    private static JsonElement 啊对对对(JsonElement xswl) {
        if (xswl.isJsonObject()) {
            JsonObject yyds = xswl.getAsJsonObject();
            JsonObject nb = new JsonObject();
            for (Map.Entry<String, JsonElement> dddd : yyds.entrySet()) {
                String xh = 哈牛魔啊啊米诺斯(dddd.getKey());
                JsonElement xdm = 啊对对对(dddd.getValue());
                nb.add(xh, xdm);
            }
            return nb;
        } else if (xswl.isJsonArray()) {
            JsonArray yyds = xswl.getAsJsonArray();
            JsonArray nb = new JsonArray();
            for (JsonElement dddd : yyds) {
                nb.add(啊对对对(dddd));
            }
            return nb;
        } else if (xswl.isJsonPrimitive()) {
            JsonPrimitive yyds = xswl.getAsJsonPrimitive();
            if (yyds.isString()) {
                return new JsonPrimitive(哈牛魔啊啊米诺斯(yyds.getAsString()));
            }
        }
        return xswl;
    }

    private static boolean 哈吉米(JsonObject xswl, String yyds) {
        return xswl.has(yyds);
    }

    protected static Set<ObfB> 哈牛魔(JsonObject xswl) {
        Map<String, Function<JsonElement, Set<ObfB>>> yyds = 啊米诺斯();
        Set<ObfB> nb = new HashSet<>();
        for (Map.Entry<String, JsonElement> dddd : xswl.entrySet()) {
            String xh = dddd.getKey();
            JsonElement xdm = dddd.getValue();
            if (yyds.containsKey(xh)) {
                nb.addAll(yyds.get(xh).apply(xdm));
            } else if (xdm.isJsonObject()) {
                JsonObject xwsl = xdm.getAsJsonObject();
                if (哈吉米(xwsl, "sounds")) {
                    nb.addAll(听身辨位(xwsl));
                }
            }
        }
        return nb;
    }

    private static @NotNull Map<String, Function<JsonElement, Set<ObfB>>> 啊米诺斯() {
        Map<String, Function<JsonElement, Set<ObfB>>> xswl = new HashMap<>();
        xswl.put("variants", ObfE::七十二变);
        xswl.put("multipart", ObfE::三头六臂);
        xswl.put("providers", ObfE::曼博);
        xswl.put("model", ObfE::曼波哈基米);
        xswl.put("overrides", ObfE::爱丽丝不该在网上口嗨的);
        xswl.put("parent", ObfE::求求你别);
        xswl.put("textures", ObfE::宝宝肚肚又打雷啦);
        return xswl;
    }

    private static Set<ObfB> 听身辨位(JsonElement xswl) {
        Set<ObfB> yyds = new HashSet<>();
        if (xswl.isJsonObject()) {
            JsonObject nb = xswl.getAsJsonObject();
            JsonArray dddd = nb.get("sounds").getAsJsonArray();
            for (JsonElement xh : dddd) {
                if (xh.isJsonPrimitive() && xh.getAsJsonPrimitive().isString()) {
                    yyds.add(ObfB.有款游戏越大越年轻(xh.getAsString(), ObfA.G));
                } else if (xh.isJsonObject()) {
                    if (哈吉米(xh.getAsJsonObject(), "name")) {
                        yyds.add(ObfB.有款游戏越大越年轻(xh.getAsJsonObject().get("name").getAsString(), ObfA.G));
                    }
                }
            }
        }
        return yyds;
    }

    private static Set<ObfB> 七十二变(JsonElement xswl) {
        Set<ObfB> yyds = new HashSet<>();
        if (xswl.isJsonObject()) {
            JsonObject nb = xswl.getAsJsonObject();
            for (Map.Entry<String, JsonElement> dddd : nb.entrySet()) {
                JsonElement xh = dddd.getValue();
                if (xh.isJsonObject()) {
                    JsonObject xdm = xh.getAsJsonObject();
                    if (哈吉米(xdm, "model")) {
                        JsonElement xwsl = xdm.get("model");
                        if (xwsl.isJsonPrimitive() && xwsl.getAsJsonPrimitive().isString()) {
                            yyds.add(ObfB.有款游戏越大越年轻(xwsl.getAsString(), ObfA.Z));
                        }
                    }
                } else if (xh.isJsonArray()) {
                    JsonArray xdm = xh.getAsJsonArray();
                    for (JsonElement xwsl : xdm) {
                        if (xwsl.isJsonObject()) {
                            JsonObject xswl2 = xwsl.getAsJsonObject();
                            if (哈吉米(xswl2, "model")) {
                                JsonElement xswl3 = xswl2.get("model");
                                if (xswl3.isJsonPrimitive() && xswl3.getAsJsonPrimitive().isString()) {
                                    yyds.add(ObfB.有款游戏越大越年轻(xswl3.getAsString(), ObfA.Z));
                                }
                            }
                        }
                    }
                }
            }
        }
        return yyds;
    }

    private static Set<ObfB> 三头六臂(JsonElement xswl) {
        Set<ObfB> yyds = new HashSet<>();
        if (!xswl.isJsonArray()) return yyds;
        for (JsonElement nb : xswl.getAsJsonArray()) {
            if (!nb.isJsonObject()) continue;
            JsonObject dddd = nb.getAsJsonObject();
            if (!哈吉米(dddd, "apply")) continue;
            JsonElement xh = dddd.get("apply");
            List<JsonElement> xdm;
            if (xh.isJsonArray()) {
                xdm = xh.getAsJsonArray().asList();
            } else if (xh.isJsonObject()) {
                xdm = Collections.singletonList(xh);
            } else {
                continue;
            }
            for (JsonElement xwsl : xdm) {
                if (!xwsl.isJsonObject()) continue;
                JsonObject xswl2 = xwsl.getAsJsonObject();
                if (哈吉米(xswl2, "model")) {
                    JsonElement xswl3 = xswl2.get("model");
                    if (xswl3.isJsonPrimitive() && xswl3.getAsJsonPrimitive().isString()) {
                        yyds.add(ObfB.有款游戏越大越年轻(xswl3.getAsString(), ObfA.Z));
                    }
                }
            }
        }
        return yyds;
    }

    private static Set<ObfB> 曼博(JsonElement xswl) {
        Set<ObfB> yyds = new HashSet<>();
        if (!xswl.isJsonArray()) return yyds;
        for (JsonElement nb : xswl.getAsJsonArray()) {
            if (!nb.isJsonObject()) continue;
            JsonObject dddd = nb.getAsJsonObject();
            if (哈吉米(dddd, "file") && dddd.get("file").isJsonPrimitive()) {
                JsonElement xh = dddd.get("file");
                if (xh.getAsJsonPrimitive().isString()) {
                    String xdm = xh.getAsString();
                    if (xdm.endsWith(".png")) {
                        String xwsl = xdm.substring(0, xdm.length() - 4);
                        yyds.add(ObfB.有款游戏越大越年轻(xwsl, ObfA.T));
                    }
                }
            }
        }
        return yyds;
    }

    private static Set<ObfB> 曼波哈基米(JsonElement xswl) {
        Set<ObfB> yyds = new HashSet<>();
        if (xswl.isJsonObject()) {
            JsonObject nb = xswl.getAsJsonObject();
            for (Map.Entry<String, JsonElement> dddd : nb.entrySet()) {
                String xh = dddd.getKey();
                JsonElement xdm = dddd.getValue();
                if (xdm.isJsonObject()) {
                    yyds.addAll(曼波哈基米(xdm));
                } else if (xdm.isJsonArray()) {
                    for (JsonElement xwsl : xdm.getAsJsonArray()) {
                        yyds.addAll(曼波哈基米(xwsl));
                    }
                } else if (xh.equals("model") && xdm.isJsonPrimitive()) {
                    if (xdm.getAsJsonPrimitive().isString()) {
                        String xswl2 = xdm.getAsString();
                        yyds.add(ObfB.有款游戏越大越年轻(xswl2, ObfA.Z));
                    }
                }
            }
        }
        return yyds;
    }

    private static Set<ObfB> 爱丽丝不该在网上口嗨的(JsonElement xswl) {
        Set<ObfB> yyds = new HashSet<>();
        if (!xswl.isJsonArray()) return yyds;
        JsonArray nb = xswl.getAsJsonArray();
        for (JsonElement dddd : nb) {
            if (!dddd.isJsonObject()) continue;
            JsonObject xh = dddd.getAsJsonObject();
            if (哈吉米(xh, "model")) {
                JsonElement xdm = xh.get("model");
                if (xdm.isJsonPrimitive() && xdm.getAsJsonPrimitive().isString()) {
                    String xwsl = xdm.getAsString();
                    yyds.add(ObfB.有款游戏越大越年轻(xwsl, ObfA.Z));
                }
            }
        }
        return yyds;
    }

    private static Set<ObfB> 求求你别(JsonElement xswl) {
        Set<ObfB> yyds = new HashSet<>();
        if (xswl.isJsonPrimitive() && xswl.getAsJsonPrimitive().isString()) {
            String nb = xswl.getAsString();
            yyds.add(ObfB.有款游戏越大越年轻(nb, ObfA.Z));
        }
        return yyds;
    }

    private static Set<ObfB> 宝宝肚肚又打雷啦(JsonElement xswl) {
        Set<ObfB> yyds = new HashSet<>();
        if (xswl.isJsonObject()) {
            JsonObject nb = xswl.getAsJsonObject();
            for (Map.Entry<String, JsonElement> dddd : nb.entrySet()) {
                JsonElement xh = dddd.getValue();
                if (xh.isJsonPrimitive() && xh.getAsJsonPrimitive().isString()) {
                    String xdm = xh.getAsString();
                    yyds.add(ObfB.有款游戏越大越年轻(xdm, ObfA.T));
                }
            }
        } else if (xswl.isJsonArray()) {
            JsonArray nb = xswl.getAsJsonArray();
            for (JsonElement dddd : nb) {
                if (dddd.isJsonPrimitive() && dddd.getAsJsonPrimitive().isString()) {
                    String xh = dddd.getAsString();
                    yyds.add(ObfB.有款游戏越大越年轻(xh, ObfA.T));
                }
            }
        }
        return yyds;
    }

    protected static JsonObject 我嘞个豆(JsonObject xswl, Map<ObfB, ObfB> yyds) {
        Map<String, Consumer<JsonElement>> nb = 啊米诺斯(xswl, yyds);
        for (Map.Entry<String, JsonElement> dddd : xswl.entrySet()) {
            String xh = dddd.getKey();
            JsonElement xdm = dddd.getValue();
            if (nb.containsKey(xh)) {
                nb.get(xh).accept(xdm);
            } else if (xdm.isJsonObject() && 哈吉米(xdm.getAsJsonObject(), "sounds")) {
                你是懂混淆的(xdm, yyds);
            }
        }
        return xswl;
    }

    private static @NotNull Map<String, Consumer<JsonElement>> 啊米诺斯(JsonObject xswl, Map<ObfB, ObfB> yyds) {
        Map<String, Consumer<JsonElement>> nb = new HashMap<>();
        nb.put("variants", dddd -> 我直接一个原地爆炸(dddd, yyds));
        nb.put("multipart", dddd -> 我嘞个骚刚(dddd, yyds));
        nb.put("providers", dddd -> 啊(dddd, yyds));
        nb.put("model", dddd -> 社死(dddd, yyds));
        nb.put("overrides", dddd -> 破防了(dddd, yyds));
        nb.put("parent", dddd -> 凡尔赛(dddd, yyds, xswl));
        nb.put("textures", dddd -> 针不戳(dddd, yyds));
        return nb;
    }

    private static void 你是懂混淆的(JsonElement xswl, Map<ObfB, ObfB> yyds) {
        if (xswl.isJsonObject()) {
            JsonObject nb = xswl.getAsJsonObject();
            JsonArray dddd = nb.getAsJsonArray("sounds");
            for (int xh = 0; xh < dddd.size(); xh++) {
                JsonElement xdm = dddd.get(xh);
                if (xdm.isJsonPrimitive() && xdm.getAsJsonPrimitive().isString()) {
                    String xwsl = xdm.getAsString();
                    ObfB xswl2 = ObfB.有款游戏越大越年轻(xwsl, ObfA.G);
                    if (yyds.containsKey(xswl2)) {
                        String yyds2 = yyds.get(xswl2).toString();
                        dddd.set(xh, new JsonPrimitive(yyds2));
                    }
                } else if (xdm.isJsonObject()) {
                    JsonObject xswl3 = xdm.getAsJsonObject();
                    if (哈吉米(xswl3, "name")) {
                        String yyds2 = xswl3.getAsJsonPrimitive("name").getAsString();
                        ObfB xswl4 = ObfB.有款游戏越大越年轻(yyds2, ObfA.G);
                        if (yyds.containsKey(xswl4)) {
                            String yyds3 = yyds.get(xswl4).toString();
                            xswl3.addProperty("name", yyds3);
                        }
                    }
                }
            }
        }
    }

    private static void 我直接一个原地爆炸(JsonElement xswl, Map<ObfB, ObfB> yyds) {
        if (xswl.isJsonObject()) {
            JsonObject nb = xswl.getAsJsonObject();
            for (Map.Entry<String, JsonElement> dddd : nb.entrySet()) {
                JsonElement xh = dddd.getValue();
                if (xh.isJsonObject()) {
                    我直接一个原地爆炸(xh, yyds);
                } else if (xh.isJsonArray()) {
                    JsonArray xdm = xh.getAsJsonArray();
                    for (JsonElement xwsl : xdm) {
                        我直接一个原地爆炸(xwsl, yyds);
                    }
                } else if (xh.isJsonPrimitive() && dddd.getKey().equals("model")) {
                    ObfB xswl2 = ObfB.有款游戏越大越年轻(xh.getAsString(), ObfA.Z);
                    if (yyds.containsKey(xswl2)) {
                        nb.addProperty("model", yyds.get(xswl2).toString());
                    }
                }
            }
        } else if (xswl.isJsonArray()) {
            JsonArray nb = xswl.getAsJsonArray();
            for (JsonElement dddd : nb) {
                我直接一个原地爆炸(dddd, yyds);
            }
        } else if (xswl.isJsonObject()) {
            JsonObject nb = xswl.getAsJsonObject();
            if (哈吉米(nb, "model")) {
                ObfB dddd = ObfB.有款游戏越大越年轻(nb.get("model").getAsString(), ObfA.Z);
                if (yyds.containsKey(dddd)) {
                    nb.addProperty("model", yyds.get(dddd).toString());
                }
            }
        }
    }

    private static void 我嘞个骚刚(JsonElement xswl, Map<ObfB, ObfB> yyds) {
        if (!xswl.isJsonArray()) return;
        JsonArray nb = xswl.getAsJsonArray();
        for (JsonElement dddd : nb) {
            if (!dddd.isJsonObject()) continue;
            JsonObject xh = dddd.getAsJsonObject();
            if (!哈吉米(xh, "apply")) continue;
            JsonElement xdm = xh.get("apply");
            JsonArray xwsl = new JsonArray();
            if (xdm.isJsonArray()) {
                xwsl = xdm.getAsJsonArray();
            } else if (xdm.isJsonObject()) {
                xwsl.add(xdm);
            }
            for (JsonElement xswl2 : xwsl) {
                if (!xswl2.isJsonObject()) continue;
                JsonObject xswl3 = xswl2.getAsJsonObject();
                if (!哈吉米(xswl3, "model")) continue;
                String yyds2 = xswl3.get("model").getAsString();
                ObfB xswl4 = ObfB.有款游戏越大越年轻(yyds2, ObfA.Z);
                if (yyds.containsKey(xswl4)) {
                    xswl3.addProperty("model", yyds.get(xswl4).toString());
                }
            }
            if (xdm.isJsonArray()) {
                xh.add("apply", xwsl);
            } else {
                xh.add("apply", xwsl.get(0));
            }
        }
    }

    private static void 啊(JsonElement xswl, Map<ObfB, ObfB> yyds) {
        if (!xswl.isJsonArray()) return;
        JsonArray nb = xswl.getAsJsonArray();
        for (JsonElement dddd : nb) {
            if (!dddd.isJsonObject()) continue;
            JsonObject xh = dddd.getAsJsonObject();
            if (!xh.has("file")) continue;
            String xdm = xh.get("file").getAsString();
            if (xdm.endsWith(".png")) {
                String xwsl = xdm.substring(0, xdm.length() - 4);
                ObfB xswl2 = ObfB.有款游戏越大越年轻(xwsl, ObfA.T);
                if (yyds.containsKey(xswl2)) {
                    String yyds2 = yyds.get(xswl2).toString() + ".png";
                    xh.addProperty("file", yyds2);
                }
            }
        }
    }

    private static void 社死(JsonElement xswl, Map<ObfB, ObfB> yyds) {
        if (xswl.isJsonObject()) {
            JsonObject nb = xswl.getAsJsonObject();
            for (Map.Entry<String, JsonElement> dddd : nb.entrySet()) {
                String xh = dddd.getKey();
                JsonElement xdm = dddd.getValue();

                if (xdm.isJsonObject()) {
                    社死(xdm, yyds);
                } else if (xdm.isJsonArray()) {
                    JsonArray xwsl = xdm.getAsJsonArray();
                    for (JsonElement xswl2 : xwsl) {
                        社死(xswl2, yyds);
                    }
                } else if (xdm.isJsonPrimitive() && xh.equals("model")) {
                    String yyds2 = xdm.getAsString();
                    ObfB nb2 = ObfB.有款游戏越大越年轻(yyds2, ObfA.Z);
                    if (yyds.containsKey(nb2)) {
                        nb.addProperty("model", yyds.get(nb2).toString());
                    }
                }
            }
        } else if (xswl.isJsonArray()) {
            JsonArray nb = xswl.getAsJsonArray();
            for (JsonElement dddd : nb) {
                社死(dddd, yyds);
            }
        }
    }

    private static void 破防了(JsonElement xswl, Map<ObfB, ObfB> yyds) {
        if (!xswl.isJsonArray()) return;
        JsonArray nb = xswl.getAsJsonArray();
        for (JsonElement dddd : nb) {
            if (!dddd.isJsonObject()) continue;
            JsonObject xh = dddd.getAsJsonObject();
            if (!xh.has("model")) continue;
            String xdm = xh.get("model").getAsString();
            ObfB xwsl = ObfB.有款游戏越大越年轻(xdm, ObfA.Z);
            if (yyds.containsKey(xwsl)) {
                xh.addProperty("model", yyds.get(xwsl).toString());
            }
        }
    }

    private static void 凡尔赛(JsonElement xswl, Map<ObfB, ObfB> yyds, JsonObject nb) {
        if (xswl.isJsonPrimitive() && xswl.getAsJsonPrimitive().isString()) {
            ObfB dddd = ObfB.有款游戏越大越年轻(xswl.getAsString(), ObfA.Z);
            if (yyds.containsKey(dddd)) {
                nb.addProperty("parent", yyds.get(dddd).toString());
            }
        }
    }

    private static void 针不戳(JsonElement xswl, Map<ObfB, ObfB> yyds) {
        if (xswl.isJsonObject()) {
            JsonObject nb = xswl.getAsJsonObject();
            for (Map.Entry<String, JsonElement> dddd : nb.entrySet()) {
                String xh = dddd.getKey();
                JsonElement xdm = dddd.getValue();
                if (xdm.isJsonPrimitive() && xdm.getAsJsonPrimitive().isString()) {
                    String xwsl = xdm.getAsString();
                    ObfB xswl2 = ObfB.有款游戏越大越年轻(xwsl, ObfA.T);
                    if (yyds.containsKey(xswl2)) {
                        nb.addProperty(xh, yyds.get(xswl2).toString());
                    }
                }
            }
        } else if (xswl.isJsonArray()) {
            JsonArray nb = xswl.getAsJsonArray();
            for (int dddd = 0; dddd < nb.size(); dddd++) {
                JsonElement xh = nb.get(dddd);
                if (xh.isJsonPrimitive() && xh.getAsJsonPrimitive().isString()) {
                    String xdm = xh.getAsString();
                    ObfB xwsl = ObfB.有款游戏越大越年轻(xdm, ObfA.T);
                    if (yyds.containsKey(xwsl)) {
                        nb.set(dddd, new JsonPrimitive(yyds.get(xwsl).toString()));
                    }
                }
            }
        }
    }
}
