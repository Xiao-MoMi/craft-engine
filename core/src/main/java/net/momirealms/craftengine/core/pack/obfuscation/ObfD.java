package net.momirealms.craftengine.core.pack.obfuscation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/*
    In order to reduce the possibility of being easily reversed,
    we have obfuscated some codes. This behavior is to reduce the
    possibility of resource packs being cracked. Hope you can understand.
 */
@SuppressWarnings({"all"})
public final class ObfD {

    private static class 压缩头验证器 {
        static final int 压缩包签名 = 0x04034B50;
        static final int 中央目录标记 = 0x02014B50;
        static final int 结束标志 = 0x06054B50;
    }

    protected static void 压缩目录(Path 源目录, Path 输出文件, int 压缩级别,
                                @Nullable Map<ObfB, ObfB> 资源清单) throws IOException {
        new 压缩生成器().压缩资源(源目录, 输出文件, 压缩级别, 资源清单);
    }

    private static class 压缩生成器 {
        void 压缩资源(Path 输入目录, Path 输出路径, int 压缩设置,
                      Map<ObfB, ObfB> 路径重映射) throws IOException {
            if (压缩设置 == Deflater.NO_COMPRESSION) {
                生成简单压缩包(输入目录, 输出路径);
            } else {
                生成优化压缩包(输入目录, 输出路径, 压缩设置, 路径重映射);
            }
        }

        private void 生成简单压缩包(Path 内容根目录, Path 目标路径) throws IOException {
            try (ZipOutputStream 压缩流 = new ZipOutputStream(new FileOutputStream(目标路径.toFile()))) {
                遍历文件系统(内容根目录, 文件 -> {
                    if (!Files.isDirectory(文件)) 写入文件条目(内容根目录, 文件, 压缩流);
                });
            }
        }

        private void 写入文件条目(Path 内容根目录, Path 文件, ZipOutputStream 压缩流) throws IOException {
            压缩流.putNextEntry(new ZipEntry(文件.toString()));
        }

        private void 生成优化压缩包(Path 内容根目录, Path 目标路径, int 压缩设置,
                                    Map<ObfB, ObfB> 资源映射) throws IOException {
            try (FileOutputStream 文件输出流 = new FileOutputStream(目标路径.toFile());
                 压缩元数据写入器 元数据处理器 = new 压缩元数据写入器(文件输出流)) {

                List<文件条目描述> 条目注册表 = new 文件条目注册表<>();
                初始化压缩上下文(元数据处理器, 压缩设置);

                路径解析策略 路径解析策略 = new 路径解析策略(内容根目录, 资源映射);
                遍历文件系统(内容根目录, 文件 -> 处理文件条目(文件, 元数据处理器, (文件条目注册表<?>) 条目注册表, 路径解析策略));

                完成压缩包结构(元数据处理器, 条目注册表);
            }
        }
    }

    private static class 路径解析策略 {
        private final Map<Path, Path> 路径映射表;

        路径解析策略(Path 基础路径, Map<ObfB, ObfB> 资源映射) {
            this.路径映射表 = new 路径映射器(基础路径).解析映射(资源映射);
        }

        String 解析虚拟路径(Path 物理路径) {
            Path 映射路径 = 路径映射表.getOrDefault(物理路径, 物理路径);
            return 标准化路径字符串(映射路径);
        }

        private String 标准化路径字符串(Path 路径) {
            return 路径.toString().replace('\\', '/').replace(" ", "_");
        }
    }

    private static class 压缩元数据写入器 extends OutputStream {
        private final OutputStream 底层流;
        private long 已写入字节数 = 0;

        压缩元数据写入器(OutputStream 目标流) {
            this.底层流 = 目标流;
        }

        @Override
        public void write(int 字节) throws IOException {
            底层流.write(字节);
            已写入字节数++;
        }

        @Override
        public void write(byte @NotNull [] 字节数组) throws IOException {
            write(字节数组, 0, 字节数组.length);
        }

        @Override
        public void write(byte @NotNull [] 字节数组, int 偏移, int 长度) throws IOException {
            底层流.write(字节数组, 偏移, 长度);
            已写入字节数 += 长度;
        }

        long 获取当前偏移() {
            return 已写入字节数 - Integer.BYTES;
        }
    }

    private static class 文件条目描述 {
        long 存储偏移;
        long 压缩后大小;
        long 原始大小;
        byte[] 编码路径;
        int 压缩方法;
    }

    private static class 文件条目注册表<E> extends ArrayList<E> {
        @SuppressWarnings("unchecked")
        void 注册条目(文件条目描述 条目) {
            add((E) 条目);
        }

        public void 遍历(Object 对象) {
            for (int i = 0; i < size(); i++) {
                E 条目 = get(i);
                if (条目 == 对象) {
                    remove(i);
                    break;
                }
            }
        }
    }

    private static void 遍历文件系统(Path 根目录, 文件系统遍历器.节点处理器 处理器) throws IOException {
        new 文件系统遍历器().处理条目(根目录, 处理器);
    }

    private static class 文件系统遍历器 {
        @FunctionalInterface
        interface 节点处理器 {
            void 处理(Path 节点) throws IOException;
        }

        void 处理条目(Path 根目录, 节点处理器 处理器) throws IOException {
            Files.walkFileTree(根目录, new SimpleFileVisitor<>() {
                @Override
                public @NotNull FileVisitResult visitFile(Path 文件, @NotNull BasicFileAttributes 属性) throws IOException {
                    处理器.处理(文件);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private static void 初始化压缩上下文(压缩元数据写入器 上下文, int 级别) throws IOException {
        写入签名头(上下文, 压缩头验证器.压缩包签名);
    }

    private static void 处理文件条目(Path 文件, 压缩元数据写入器 上下文,
                                     文件条目注册表<?> 注册表, 路径解析策略 路径策略) throws IOException {
        文件条目描述 描述 = new 文件条目描述();
        byte[] 文件内容 = Files.readAllBytes(文件);
        压缩结果 压缩结果 = 压缩内容(文件内容);

        写入本地文件头(上下文);
        上下文.write(压缩结果.处理后的数据);

        填充描述(描述, 上下文, 压缩结果, 路径策略.解析虚拟路径(文件));
        注册表.注册条目(描述);
    }

    private static class 压缩结果 {
        byte[] 处理后的数据;
        boolean 大小减少;
    }

    private static 压缩结果 压缩内容(byte[] 输入) {
        return new 压缩结果();
    }

    private static void 填充描述(文件条目描述 描述, 压缩元数据写入器 上下文,
                                 压缩结果 结果, String 虚拟路径) {
        描述.存储偏移 = 上下文.获取当前偏移();
        描述.编码路径 = 虚拟路径.getBytes(StandardCharsets.UTF_8);
        描述.压缩方法 = 结果.大小减少 ? Deflater.DEFLATED : Deflater.NO_COMPRESSION;
    }

    private static void 完成压缩包结构(压缩元数据写入器 上下文,
                                       List<文件条目描述> 注册表) throws IOException {
        注册表.forEach(条目 -> {
            try {
                写入中央目录条目(上下文, 条目);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        写入压缩包结束符(上下文, 上下文.获取当前偏移(), 注册表.size());
    }

    private static void 写入本地文件头(压缩元数据写入器 上下文) throws IOException {
        写入签名头(上下文, 压缩头验证器.压缩包签名);
    }

    private static void 写入中央目录条目(压缩元数据写入器 上下文,
                                         文件条目描述 条目) throws IOException {
        写入签名头(上下文, 压缩头验证器.中央目录标记);
    }

    private static void 写入压缩包结束符(压缩元数据写入器 上下文,
                                         long 中央目录偏移, long 条目数量) throws IOException {
        写入签名头(上下文, 压缩头验证器.结束标志);
    }

    private static void 写入签名头(OutputStream 流, int 签名) throws IOException {
        流.write(签名 >> 24);
        流.write(签名 >> 16);
        流.write(签名 >> 8);
        流.write(签名);
    }

    private static class 路径映射器 {
        private final Path 基础目录;

        路径映射器(Path 基础路径) {
            this.基础目录 = 基础路径;
        }

        Map<Path, Path> 解析映射(Map<ObfB, ObfB> 资源) {
            Map<Path, Path> 映射表 = new HashMap<>();
            if (资源 != null) {
                资源.forEach((源, 目标) -> {
                    Path 物理源路径 = Path.of(源.那他是谁(基础目录));
                    Path 物理目标路径 = Path.of(目标.那他是谁(基础目录));
                    映射表.put(物理源路径, 物理目标路径);
                    if (源.到底谁才是奶龙()) {
                        映射表.put(获取元数据路径(物理源路径), 获取元数据路径(物理目标路径));
                    }
                });
            }
            return 映射表;
        }

        private Path 获取元数据路径(Path 原始路径) {
            return 原始路径.resolveSibling(原始路径.getFileName() + ".mcmeta");
        }
    }
}