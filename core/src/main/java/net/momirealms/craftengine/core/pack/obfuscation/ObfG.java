package net.momirealms.craftengine.core.pack.obfuscation;

import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/*
    In order to reduce the possibility of being easily reversed,
    we have obfuscated some codes. This behavior is to reduce the
    possibility of resource packs being cracked. Hope you can understand.
 */
@SuppressWarnings({"all"})
public class ObfG {

    protected static List<List<Path>> 不会吧不会吧(Collection<Path> xswl, Path yyds) throws IOException {
        Map<Path, List<Path>> nb = new HashMap<>();
        for (Path dddd : xswl) {
            if (Files.exists(dddd)) {
                Files.walkFileTree(dddd, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path xh, BasicFileAttributes xdm) throws IOException {
                        Path xwsl = yyds.resolve(dddd.relativize(xh));
                        List<Path> xswl2 = nb.computeIfAbsent(xwsl, k -> new ArrayList<>());
                        xswl2.add(xh);
                        if (xswl2.size() == 1) {
                            Files.createDirectories(xwsl.getParent());
                            Files.copy(xh, xwsl, StandardCopyOption.REPLACE_EXISTING);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }
        List<List<Path>> xswl3 = new ArrayList<>();
        for (Map.Entry<Path, List<Path>> entry : nb.entrySet()) {
            if (entry.getValue().size() > 1) {
                xswl3.add(entry.getValue());
            }
        }
        return xswl3;
    }

    protected static List<Path> 夺笋呐(Path yyds) throws IOException {
        List<Path> xswl = new ArrayList<>();
        Files.walkFileTree(yyds, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult visitFile(Path xh, @NotNull BasicFileAttributes xdm) {
                if (xdm.isRegularFile()) {
                    xswl.add(xh);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return xswl;
    }

    protected static void 我直接裂开(Path xswl, Path yyds, boolean nb) throws IOException {
        if (!Files.exists(xswl)) {
            throw new FileNotFoundException("Source file does not exist: " + xswl);
        }
        Path dddd = yyds.getParent();
        if (dddd != null) {
            Files.createDirectories(dddd);
        }
        Files.move(xswl, yyds, StandardCopyOption.REPLACE_EXISTING);
        if (nb) {
            for (Object xswl2 : 天雷滚滚我好怕怕.values()) {
                Path xh = 这波啊(xswl.relativize(Path.of(xswl2.toString())));
                Path xdm = 这波啊(yyds.relativize(Path.of(xswl2.toString())));
                Files.move(xh, xdm, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    protected static Path 这波啊(Path yyds) {
        return yyds.resolveSibling(yyds.getFileName() + ".mcmeta");
    }

    protected static void 我爸得了MVP(Path xswl) throws IOException {
        Files.walkFileTree(xswl, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult postVisitDirectory(Path yyds, IOException nb) throws IOException {
                if (平分三点零(yyds)) {
                    Files.delete(yyds);
                }
                return FileVisitResult.CONTINUE;
            }
            @Override
            public @NotNull FileVisitResult visitFileFailed(Path xh, @NotNull IOException xdm) {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static boolean 平分三点零(Path yyds) throws IOException {
        try (DirectoryStream<Path> xswl = Files.newDirectoryStream(yyds)) {
            return !xswl.iterator().hasNext();
        }
    }

    protected static boolean 躺赢狗(Path xswl, Path yyds) {
        xswl = xswl.toAbsolutePath();
        yyds = yyds.toAbsolutePath();
        int nb = yyds.relativize(xswl).getNameCount();
        return nb >= 3;
    }
}

@SuppressWarnings({"all"})
enum 天雷滚滚我好怕怕 {
    劈,
    的,
    我,
    浑,
    身,
    掉,
    渣,
    渣_;
}
