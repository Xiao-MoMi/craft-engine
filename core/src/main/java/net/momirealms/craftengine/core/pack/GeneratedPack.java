package net.momirealms.craftengine.core.pack;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.mcmeta.Overlays;
import net.momirealms.craftengine.core.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

record GeneratedPack(FileSystem fileSystem,
                     Path path,
                     JsonObject metadata,
                     Overlays overlays,
                     @Nullable PackGenerationOptions options) implements AutoCloseable {

    static GeneratedPack loadZip(Path source) throws IOException {
        if (!Files.isRegularFile(source)) throw new IOException("Resource pack does not exist: " + source);
        FileSystem fs = Jimfs.newFileSystem(Configuration.forCurrentPlatform());
        try {
            Path root = fs.getPath("resource_pack").toAbsolutePath();
            Files.createDirectories(root);
            // 按中央目录读取，保留 PackSquash 去重后指向同一段压缩数据的不同文件名。
            // ZipInputStream 只遍历本地文件头，会漏掉这类 ZIP 中的别名条目。
            try (ZipFile zip = new ZipFile(source.toFile())) {
                var entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    Path target = root.resolve(entry.getName()).normalize();
                    if (!target.startsWith(root)) throw new IOException("Bad zip entry: " + entry.getName());
                    if (entry.isDirectory()) {
                        Files.createDirectories(target);
                    } else {
                        Files.createDirectories(target.getParent());
                        try (var input = zip.getInputStream(entry)) {
                            Files.copy(input, target);
                        }
                    }
                }
            }
            JsonObject metadata = GsonHelper.readJsonObjectFromFile(root.resolve("pack.mcmeta"));
            if (metadata == null || !(metadata.get("pack") instanceof JsonObject)) {
                throw new IOException("Resource pack is missing a valid pack.mcmeta: " + source);
            }
            // 外部 ZIP 没有 generate 步骤的选项。保留其原始元数据，不能用服务器默认描述和版本覆盖。
            return new GeneratedPack(fs, root, metadata, new Overlays(metadata), null);
        } catch (IOException | RuntimeException | Error e) {
            fs.close();
            throw e;
        }
    }

    void export(Path output) throws IOException {
        // 每次导出都移除旧文件，避免已删除的资源继续被 PackSquash 打进 ZIP。
        // 不跟随符号链接，清理仅作用于导出目录中的条目。
        if (Files.exists(output, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            Files.walkFileTree(output, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exception) throws IOException {
                    if (exception != null) throw exception;
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        Files.walkFileTree(this.path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(output.resolve(path.relativize(dir).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, output.resolve(path.relativize(file).toString()));
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Override
    public void close() throws IOException {
        this.fileSystem.close();
    }
}
