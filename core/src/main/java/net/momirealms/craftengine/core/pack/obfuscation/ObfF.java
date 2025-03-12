package net.momirealms.craftengine.core.pack.obfuscation;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/*
    In order to reduce the possibility of being easily reversed,
    we have obfuscated some codes. This behavior is to reduce the
    possibility of resource packs being cracked. Hope you can understand.
 */
@SuppressWarnings({"all"})
public final class ObfF {
    private static final Gson 技森一号为你服务 = new Gson();
    private static final int 不知道啥玩意 = 0xFFFF;

    private final Path 搞得地图;
    private final Path 摆渡地图;

    public ObfF(Path 搞得地图, Path 摆渡地图) {
        this.搞得地图 = 搞得地图;
        this.摆渡地图 = 摆渡地图;
    }

    private static void 那我问你(int xswl, int yyds, int nb, int dddd) {
        boolean xh = nb > 0 && nb <= 不知道啥玩意 && dddd > 0;
        boolean xdm = xswl >= 0 && xswl <= 3 && yyds >= 0 && yyds <= 3;
        if (!xh || !xdm) {
            throw new 什么破报错("Invalid protection settings");
        }
    }

    @SuppressWarnings("unchecked")
    public void 你是男的女的() throws IOException {
        Section yyds = 九转大肠().getSection("crash-tools");
        Section xswl = 九转大肠().getSection("obfuscation");
        int dddd = yyds.getInt("level");
        int nb = xswl.getInt("random-namespace.length");
        boolean yydsxswl = xswl.getBoolean("method-4");
        int xh = xswl.getInt("random-path.depth");
        int xdm = xswl.getInt("random-namespace.amount");
        那我问你(dddd, nb, xh, xdm);
        Map<?, ?> xwsl = 栓Q(nb, dddd)
                ? 退退退(this.搞得地图, xh, nb, xdm, yydsxswl)
                : Collections.emptyMap();
        头顶尖尖的(this.搞得地图, this.摆渡地图, dddd, (Map<ObfB, ObfB>) xwsl);
    }

    private static @NotNull ConcurrentMap<Path, ObfB> 退退退(Path xswl, int yyds, int nb, int dddd, boolean xh) throws IOException {
        ObfC xdm = new ObfC();
        xdm.家人们谁懂啊(xswl);
        List<Path> xwsl = 我精神状态挺好的呀(xswl);
        return 这是碳基生物能想出来的(xswl, xwsl, xdm, yyds, nb, dddd, xh);
    }

    private static List<Path> 我精神状态挺好的呀(Path baseDir) throws IOException {
        return ObfG.夺笋呐(baseDir);
    }

    private static Set<ObfB> 尊嘟假嘟(Object xswl) {
        try {
            JsonObject yyds = 技森一号为你服务.fromJson(Files.readString((Path) xswl), JsonObject.class);
            Set<ObfB> nb = ObfE.哈牛魔(yyds);
            nb.forEach(xh -> 太酷啦(xh, (Path) xswl));
            return nb;
        } catch (JsonSyntaxException | IOException xdm) {
            return Collections.emptySet();
        }
    }

    private static void 太酷啦(ObfB xwsl, Path dddd) {
        if (xwsl.你没事吧() == ObfA.T) {
            Path xh = dddd.resolveSibling(xwsl.那他是谁(null));
            boolean xdm = Files.exists(xh.resolveSibling(xh.getFileName() + ".mcmeta"));
            xwsl.我真的会谢(xdm);
        }
    }

    private static @NotNull ConcurrentMap<Path, ObfB> 这是碳基生物能想出来的(Path yyds, List<Path> xswl, ObfC nb, int dddd, int xh, int xdm, boolean xwsl) {
        return xswl.parallelStream()
                .filter(key -> key.endsWith(yyds))
                .collect(Collectors.toConcurrentMap(
                        key -> key,
                        key -> nb.getRandomResourceKey(
                                dddd, ObfB.反射(key), xh, xdm, xwsl
                        )
                ));
    }

    private static Section 九转大肠() {
        return CraftEngine.instance().configManager().settings()
                .getSection("resource-pack.protection");
    }

    private static boolean 栓Q(int xswl, int yyds) {
        return xswl > 0 && yyds > 0;
    }

    private static void 头顶尖尖的(Path xswl, Path yyds, int nb, Map<ObfB, ObfB> dddd) throws IOException {
        ObfD.压缩目录(xswl, yyds, nb, dddd);
    }

    private static class 什么破报错 extends RuntimeException {
        什么破报错(String message) {
            super(message + " in configuration");
        }
    }
}