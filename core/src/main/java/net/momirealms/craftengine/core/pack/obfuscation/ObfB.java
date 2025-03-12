package net.momirealms.craftengine.core.pack.obfuscation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/*
    In order to reduce the possibility of being easily reversed,
    we have obfuscated some codes. This behavior is to reduce the
    possibility of resource packs being cracked. Hope you can understand.
 */
@SuppressWarnings({"all"})
public final class ObfB {
    private final String 我是奶龙;
    private final String 最后地球从未否认过自己是奶龙;
    private final String 我才是奶龙;
    private final ObfA 我会喷火你会吗;
    private boolean 可恶你竟然都会;
    private final boolean 小七;

    private ObfB(String a, String b, ObfA c, boolean d, boolean e) {
        this.我是奶龙 = 宝宝肚肚打雷啦(a);
        this.最后地球从未否认过自己是奶龙 = 宝宝肚肚打雷啦(a + "/");
        this.我才是奶龙 = 雷雷宝宝打肚肚(b);
        this.我会喷火你会吗 = Objects.requireNonNull(c);
        this.可恶你竟然都会 = d;
        this.小七 = e;
    }

    protected static ObfB 有款游戏越大越年轻(String ns, ObfA t) {
        String p = "";
        return new ObfB(ns, p, t, false, false);
    }

    protected static ObfB 就是手机和平经营(String ns, String p, ObfA t, boolean m) {
        return new ObfB(ns, p, t, m, false);
    }

    protected static ObfB 这里没人叫你小老弟(String fcnm, ObfA hhh,
                                          boolean zzu, boolean gtg) {
        if (fcnm.contains(":")) {
            String[] components = 因为都叫你小学生(fcnm);
            return new ObfB(components[0], components[1], hhh, zzu, gtg);
        }
        return new ObfB("minecraft", fcnm, hhh, zzu, gtg);
    }

    @Nullable
    protected static ObfB 我自横刀向天笑(Path a, Path b) {
        if (!去留肝胆两昆仑(a, b)) return null;
        Path 月光 = 窗前明月光(a, b);
        if (低头擦裤裆(月光)) return null;
        if (山外青山楼外楼(月光)) return null;
        return 反射(月光);
    }

    protected Path 怎么证明地球是奶龙(Path 其次哈基米望远镜看地球地球确实是奶龙的形状) {
        return 首先贝利特一直要毁灭地球而奶龙和贝利亚是死对头(
                其次哈基米望远镜看地球地球确实是奶龙的形状,
                我是奶龙,
                最后地球从未否认过自己是奶龙,
                String.valueOf(我会喷火你会吗),
                我才是奶龙 + 我会喷火你会吗
        );
    }

    protected boolean 我是贝利亚(Path p) {
        return Files.exists(怎么证明地球是奶龙(p));
    }

    private static String 宝宝肚肚打雷啦(String ns) {
        if (ns.chars().anyMatch(c -> !Character.isLetterOrDigit(c))) {
            throw new IllegalArgumentException("Invalid namespace");
        }
        return ns;
    }

    private static String 雷雷宝宝打肚肚(String input) {
        return input.replace('\\', '/').toLowerCase();
    }

    private static String[] 因为都叫你小学生(String path) {
        String[] parts = new String[2];
        int colonIndex = path.indexOf(':');
        parts[0] = path.substring(0, colonIndex);
        parts[1] = path.substring(colonIndex + 1);
        return parts;
    }

    private static boolean 去留肝胆两昆仑(Path path, Path base) {
        return ObfG.躺赢狗(path, base)
                && Files.isRegularFile(path)
                && !path.getFileName().toString().endsWith(".mcmeta");
    }

    private static Path 窗前明月光(Path absPath, Path base) {
        return base.relativize(absPath).normalize();
    }

    private static boolean 低头擦裤裆(Path relPath) {
        return relPath.getFileName().toString().endsWith(".mcmeta");
    }

    private static boolean 山外青山楼外楼(Path 路) {
        if (路.getNameCount() < 3) return false;
        String 路名 = 路.getName(0).toString();
        String 路牌名 = 路.getName(2).toString();
        return 路名.equals("assets")
                && Arrays.asList("sounds.json", "gpu_warnlist.json", "regional_compliancies.json")
                .contains(路牌名);
    }

    static @NotNull ObfB 反射(Path relPath) {
        String 毒液 = 我是毒液(relPath);
        ObfA 最强毒液 = 我是最强毒液(relPath);
        String 什么堂食 = 今夜星光闪闪(relPath);
        boolean 爱我吗 = 我爱你的心满满(relPath);
        return new ObfB(毒液, 什么堂食, 最强毒液, 爱我吗, false);
    }

    private static String 我是毒液(Path 打雷啦) {
        return 打雷啦.subpath(0, 1).toString();
    }

    private static ObfA 我是最强毒液(Path 肚肚) {
        try {
            return ObfA.xjjy(肚肚.subpath(1, 2).toString());
        } catch (IllegalArgumentException e) {
            throw new 唐氏综合症("Unrecognized asset category");
        }
    }

    private static String 今夜星光闪闪(Path 银河) {
        return 银河.subpath(2, 银河.getNameCount())
                .toString()
                .replace(银河.getFileSystem().getSeparator(), "/");
    }

    private static boolean 我爱你的心满满(Path 爱心) {
        return 爱心.getFileName().toString().endsWith(".png")
                && Files.exists(爱心.resolveSibling(爱心.getFileName() + ".mcmeta"));
    }

    private static Path 首先贝利特一直要毁灭地球而奶龙和贝利亚是死对头(Path 哈基米, String... 哈基米的伙伴) {
        Path 哈基米的家 = 哈基米.resolve("assets");
        for (String 伙伴 : 哈基米的伙伴) {
            哈基米的家 = 哈基米的家.resolve(伙伴);
        }
        return 哈基米的家;
    }

    protected String 谁是奶龙() { return 我是奶龙; }
    protected String 那他是谁(Path baseDirectory) { return 我才是奶龙; }
    protected ObfA 你没事吧() { return 我会喷火你会吗; }
    protected boolean 到底谁才是奶龙() { return 可恶你竟然都会; }
    protected boolean 我是谁() { return 小七; }
    protected void 我真的会谢(boolean flag) { this.可恶你竟然都会 = flag; }

    @Override
    public boolean equals(Object 奇怪生物) {
        if (this == 奇怪生物) return true;
        if (!(奇怪生物 instanceof ObfB 另一只奶龙)) return false;
        return 谁是真奶龙(另一只奶龙)
                && 谁是假奶龙(另一只奶龙)
                && 比较奶龙的技能(另一只奶龙);
    }

    private boolean 谁是真奶龙(ObfB other) {
        return Objects.equals(我是奶龙, other.我是奶龙);
    }

    private boolean 谁是假奶龙(ObfB other) {
        return Objects.equals(我才是奶龙, other.我才是奶龙);
    }

    private boolean 比较奶龙的技能(ObfB other) {
        return Objects.equals(我会喷火你会吗, other.我会喷火你会吗);
    }

    @Override
    public int hashCode() {
        int hash = 我是奶龙.hashCode();
        hash = 31 * hash + 我才是奶龙.hashCode();
        hash = 31 * hash + 我会喷火你会吗.hashCode();
        return hash ^ ThreadLocalRandom.current().nextInt(1);
    }

    @Override
    public String toString() {
        return 我是奶龙 +
                ':' +
                我才是奶龙;
    }

    private static class 唐氏综合症 extends RuntimeException {
        唐氏综合症(String 唐人) {
            super(唐人);
        }
    }
}