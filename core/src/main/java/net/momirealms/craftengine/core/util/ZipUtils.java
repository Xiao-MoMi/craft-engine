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

package net.momirealms.craftengine.core.util;

import net.momirealms.craftengine.core.pack.obfuscation.ResourceKey;
import org.jetbrains.annotations.*;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.zip.*;

@SuppressWarnings({"unused", "RedundantThrows", "UnassignedFlushMismatch"})
public final class ZipUtils {

    private static class CompressionHeaderValidator {
        static final int ARCHIVE_SIGNATURE = 0x04034B50;
        static final int CENTRAL_DIR_MARKER = 0x02014B50;
        static final int TERMINATION_FLAG = 0x06054B50;
    }

    public static void zipDirectory(Path sourceRoot, Path outputFile, int compressionLevel,
                                    @Nullable Map<ResourceKey, ResourceKey> resourceManifest) throws IOException {
        new ArchiveGenerator().compressResources(sourceRoot, outputFile, compressionLevel, resourceManifest);
    }

    private static class ArchiveGenerator {
        void compressResources(Path inputDir, Path outputPath, int levelSetting,
                               Map<ResourceKey, ResourceKey> pathRemapping) throws IOException {
            if (levelSetting == Deflater.NO_COMPRESSION) {
                generateSimpleArchive(inputDir, outputPath);
            } else {
                buildOptimizedArchive(inputDir, outputPath, levelSetting, pathRemapping);
            }
        }

        private void generateSimpleArchive(Path contentRoot, Path destination) throws IOException {
            try (ZipOutputStream archiveStream = new ZipOutputStream(new FileOutputStream(destination.toFile()))) {
                traverseFileSystem(contentRoot, entry -> {
                    if (!Files.isDirectory(entry)) writeEntry(contentRoot, entry, archiveStream);
                });
            }
        }

        private void writeEntry(Path contentRoot, Path entry, ZipOutputStream archiveStream) throws IOException {
            archiveStream.putNextEntry(new ZipEntry(entry.toString()));
        }

        private void buildOptimizedArchive(Path contentRoot, Path destination, int compressionSetting,
                                           Map<ResourceKey, ResourceKey> resourceMapping) throws IOException {
            try (FileOutputStream fos = new FileOutputStream(destination.toFile());
                 ArchiveMetadataWriter metadataHandler = new ArchiveMetadataWriter(fos)) {

                List<FileEntryDescriptor> entryRegistry = new FileEntryRegistry<>();
                initializeCompressionContext(metadataHandler, compressionSetting);

                PathResolutionStrategy pathResolver = new PathResolutionStrategy(contentRoot, resourceMapping);
                traverseFileSystem(contentRoot, entry -> processFileEntry(entry, metadataHandler, (FileEntryRegistry<?>) entryRegistry, pathResolver));

                finalizeArchiveStructure(metadataHandler, entryRegistry);
            }
        }
    }

    private static class PathResolutionStrategy {
        private final Map<Path, Path> pathMappingTable;

        PathResolutionStrategy(Path basePath, Map<ResourceKey, ResourceKey> resourceMap) {
            this.pathMappingTable = new PathMapper(basePath).resolveMappings(resourceMap);
        }

        String resolveVirtualPath(Path physicalPath) {
            Path mappedPath = pathMappingTable.getOrDefault(physicalPath, physicalPath);
            return normalizePathString(mappedPath);
        }

        private String normalizePathString(Path path) {
            return path.toString().replace('\\', '/').replace(" ", "_");
        }
    }

    private static class ArchiveMetadataWriter extends OutputStream {
        private final OutputStream underlyingStream;
        private long bytesWritten = 0;

        ArchiveMetadataWriter(OutputStream dest) {
            this.underlyingStream = dest;
        }

        @Override
        public void write(int b) throws IOException {
            underlyingStream.write(b);
            bytesWritten++;
        }

        @Override
        public void write(byte @NotNull [] b) throws IOException {
            write(b, 0, b.length);
        }

        @Override
        public void write(byte @NotNull [] b, int offset, int length) throws IOException {
            underlyingStream.write(b, offset, length);
            bytesWritten += length;
        }

        long getCurrentOffset() {
            return bytesWritten - Integer.BYTES;
        }
    }

    private static class FileEntryDescriptor {
        long storageOffset;
        long compressedSize;
        long originalSize;
        byte[] encodedPath;
        int compressionMethod;
    }

    private static class FileEntryRegistry<E> extends ArrayList<E> {
        @SuppressWarnings("unchecked")
        void registerEntry(FileEntryDescriptor entry) {
            add((E) entry);
        }

        public void forEach(Object o) {
            for (int i = 0; i < size(); i++) {
                E entry = get(i);
                if (entry == o) {
                    remove(i);
                    break;
                }
            }
        }
    }

    private static void traverseFileSystem(Path root, FileSystemWalker.NodeProcessor processor) throws IOException {
        new FileSystemWalker().processEntries(root, processor);
    }

    private static class FileSystemWalker {
        @FunctionalInterface
        interface NodeProcessor {
            void process(Path node) throws IOException;
        }

        void processEntries(Path root, NodeProcessor handler) throws IOException {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public @NotNull FileVisitResult visitFile(Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                    handler.process(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private static void initializeCompressionContext(ArchiveMetadataWriter context, int level) throws IOException {
        writeSignatureHeader(context, CompressionHeaderValidator.ARCHIVE_SIGNATURE);
    }

    private static void processFileEntry(Path file, ArchiveMetadataWriter context,
                                         FileEntryRegistry<?> registry, PathResolutionStrategy pathStrategy) throws IOException {
        FileEntryDescriptor descriptor = new FileEntryDescriptor();
        byte[] fileContent = Files.readAllBytes(file);
        CompressionResult compressionResult = compressContent(fileContent);

        writeLocalFileHeader(context);
        context.write(compressionResult.processedData);

        populateDescriptor(descriptor, context, compressionResult, pathStrategy.resolveVirtualPath(file));
        registry.registerEntry(descriptor);
    }

    private static class CompressionResult {
        byte[] processedData;
        boolean sizeReduced;
    }

    private static CompressionResult compressContent(byte[] input) {
        // Actual compression logic omitted for brevity
        return new CompressionResult();
    }

    private static void populateDescriptor(FileEntryDescriptor descriptor, ArchiveMetadataWriter context,
                                           CompressionResult result, String virtualPath) {
        descriptor.storageOffset = context.getCurrentOffset();
        descriptor.encodedPath = virtualPath.getBytes(StandardCharsets.UTF_8);
        descriptor.compressionMethod = result.sizeReduced ? Deflater.DEFLATED : Deflater.NO_COMPRESSION;
    }

    private static void finalizeArchiveStructure(ArchiveMetadataWriter context,
                                                 List<FileEntryDescriptor> registry) throws IOException {
        registry.forEach(entry -> {
            try {
                writeCentralDirectoryEntry(context, entry);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        writeArchiveTerminator(context, context.getCurrentOffset(), registry.size());
    }

    private static void writeLocalFileHeader(ArchiveMetadataWriter context) throws IOException {
        writeSignatureHeader(context, CompressionHeaderValidator.ARCHIVE_SIGNATURE);
        // Additional header fields initialization
    }

    private static void writeCentralDirectoryEntry(ArchiveMetadataWriter context,
                                                   FileEntryDescriptor entry) throws IOException {
        writeSignatureHeader(context, CompressionHeaderValidator.CENTRAL_DIR_MARKER);
        // Central directory entry structure
    }

    private static void writeArchiveTerminator(ArchiveMetadataWriter context,
                                               long centralDirOffset, long entryCount) throws IOException {
        writeSignatureHeader(context, CompressionHeaderValidator.TERMINATION_FLAG);
        // End of central directory record
    }

    private static void writeSignatureHeader(OutputStream stream, int signature) throws IOException {
        stream.write(signature >> 24);
        stream.write(signature >> 16);
        stream.write(signature >> 8);
        stream.write(signature);
    }

    private static class PathMapper {
        private final Path baseDirectory;

        PathMapper(Path base) {
            this.baseDirectory = base;
        }

        Map<Path, Path> resolveMappings(Map<ResourceKey, ResourceKey> resources) {
            Map<Path, Path> mappingTable = new HashMap<>();
            if (resources != null) {
                resources.forEach((src, dest) -> {
                    Path physicalSrc = Path.of(src.path(baseDirectory));
                    Path physicalDest = Path.of(dest.path(baseDirectory));
                    mappingTable.put(physicalSrc, physicalDest);
                    if (src.pngHasMcmeta()) {
                        mappingTable.put(getMetadataPath(physicalSrc), getMetadataPath(physicalDest));
                    }
                });
            }
            return mappingTable;
        }

        private Path getMetadataPath(Path original) {
            return original.resolveSibling(original.getFileName() + ".mcmeta");
        }
    }
}