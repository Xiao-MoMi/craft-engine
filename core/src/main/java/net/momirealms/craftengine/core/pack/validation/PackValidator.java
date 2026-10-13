package net.momirealms.craftengine.core.pack.validation;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.ResourcePackView;
import net.momirealms.craftengine.core.pack.mcmeta.Overlay;
import net.momirealms.craftengine.core.pack.mcmeta.Overlays;
import net.momirealms.craftengine.core.pack.mcmeta.PackVersion;
import net.momirealms.craftengine.core.pack.mcmeta.overlay.OverlayCombination;
import net.momirealms.craftengine.core.pack.revision.Revision;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.plugin.logger.PluginLogger;
import net.momirealms.craftengine.core.util.MinecraftVersion;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class PackValidator {
    public static final List<PackVersion> VALIDATION_BOUNDARIES = List.of(PackVersion.ITEM_ATLAS_VERSION);
    private final PluginLogger logger;
    private final ValidationResources resources;

    public PackValidator(PluginLogger logger, ValidationResources resources) {
        this.logger = logger;
        this.resources = resources;
    }

    public void validate(Path path, Overlays overlays, boolean obfuscation) {
        validate(path, overlays, obfuscation, ValidationSettings.capture());
    }

    public void validate(Path path, Overlays overlays, boolean obfuscation, ValidationSettings settings) {
        ValidationContext context = new ValidationContext(this.logger, this.resources, settings);
        new Session(context).validate(path, overlays, obfuscation);
    }

    private record Session(ValidationContext context) {

        private void validate(Path path, Overlays packOverlays, boolean obfuscation) {
            OverlayCombination combination = new OverlayCombination(
                    packOverlays.overlays(),
                    this.context.settings().packMinVersion().packFormat(),
                    this.context.settings().packMaxVersion().packFormat(),
                    VALIDATION_BOUNDARIES
            );
            List<OverlayCombination.Segment> segments = new ArrayList<>();
            while (combination.hasNext()) {
                OverlayCombination.Segment segment = combination.nextSegment();
                if (segment == null) {
                    break;
                }
                segments.add(segment);
            }

            Map<Path, Integer> lastRootUses = new HashMap<>();
            Set<Path> reusableRoots = new HashSet<>();
            lastRootUses.put(path, segments.size() - 1);
            if (segments.size() > 1) {
                reusableRoots.add(path);
            }
            for (int i = 0; i < segments.size(); i++) {
                for (Overlay overlay : segments.get(i).overlays()) {
                    Path root = path.resolve(overlay.directory());
                    Integer previous = lastRootUses.put(root, i);
                    if (previous != null && previous != i) {
                        reusableRoots.add(root);
                    }
                }
            }
            ModelReferenceCollector references = new ModelReferenceCollector(this.context, reusableRoots);
            AtlasFixer itemFixer = new AtlasFixer();
            AtlasFixer blockFixer = new AtlasFixer();

            boolean hasNonOverlaySupport = false;
            if (!segments.isEmpty()) {
                // 第一个segment一定是最小的
                hasNonOverlaySupport = segments.getFirst().min() <= MinecraftVersion.V1_20_1.packFormat().major();
            }

            boolean fixAtlasOnValidation = this.context.settings().fixTextureAtlas() && !obfuscation;
            Set<Revision> revisions = new TreeSet<>();
            for (int i = 0, size = segments.size(); i < size; i++) {
                OverlayCombination.Segment segment = segments.get(i);
                List<Path> rootPathList = new ArrayList<>();
                rootPathList.add(path);
                List<Overlay> overlayInOrder = new ArrayList<>(segment.overlays().size());
                for (Overlay overlay : packOverlays.overlays()) {
                    if (segment.overlays().contains(overlay)) {
                        Path overlayDir = path.resolve(overlay.directory());
                        if (Files.isDirectory(overlayDir)) {
                            overlayInOrder.add(overlay);
                            rootPathList.add(overlayDir);
                        }
                    }
                }

                this.context.logger().info(TranslationManager.instance().plainTranslation(
                        "resource_pack.validation_started",
                        String.valueOf(i + 1), String.valueOf(size), segment.minVersion().asString(), segment.maxVersion().asString(), overlayInOrder.stream().map(Overlay::directory).toList().toString()
                ));

                Path[] rootPaths = rootPathList.toArray(new Path[0]);
                AtlasValidation.ValidationResult result = validateResourcePackWithOverlays(
                        path,
                        rootPaths,
                        references,
                        revisions,
                        segment.min() >= MinecraftVersion.V1_21_6.packFormat().major(),
                        segment.max() >= MinecraftVersion.V1_21_11.packFormat().major(),
                        segment.min() >= MinecraftVersion.V26_1.packFormat().major(),
                        obfuscation
                );
                for (Path root : rootPaths) {
                    if (lastRootUses.get(root) == i) {
                        references.release(root);
                    }
                }
                if (fixAtlasOnValidation) {
                    // 有修复物品
                    if (result.fixedItemAtlas() != null) {
                        itemFixer.addEntry(segment.minVersion(), segment.maxVersion(), result.fixedItemAtlas());
                    }
                    // 有修复方块
                    if (result.fixedBlockAtlas() != null) {
                        blockFixer.addEntry(segment.minVersion(), segment.maxVersion(), result.fixedBlockAtlas());
                    } else if (hasNonOverlaySupport) {
                        // 如果有低版本的支持，那么要通过overlay复原atlas
                        blockFixer.addEntry(segment.minVersion(), segment.maxVersion(), Objects.requireNonNullElseGet(result.originalBlockAtlas(), JsonObject::new));
                    }
                }
            }

            // 加入新的 overlay
            for (Revision revision : revisions) {
                packOverlays.addOverlay(new Overlay(revision.minPackVersion(), revision.maxPackVersion(),
                        this.context.settings().createOverlayFolderName(revision.versionString())));
            }
            // 尝试修复atlas
            if (fixAtlasOnValidation) {
                // 物品
                for (AtlasFixer.Entry entry : itemFixer.entries()) {
                    PackVersion min = entry.min();
                    PackVersion max = entry.max();
                    String directoryName = this.context.settings().createOverlayFolderName(min.asString() + "-" + max.asString());
                    Path atlasPath = path.resolve(directoryName)
                            .resolve("assets")
                            .resolve("minecraft")
                            .resolve("atlases")
                            .resolve("items.json");
                    this.context.writeJson(entry.atlas(), atlasPath);
                    packOverlays.addOverlay(new Overlay(min, max, directoryName));
                }
                // 方块
                for (AtlasFixer.Entry entry : blockFixer.entries()) {
                    PackVersion min = entry.min();
                    PackVersion max = entry.max();
                    String directoryName = this.context.settings().createOverlayFolderName(min.asString() + "-" + max.asString());
                    // 这个版本不认可overlay，得把atlas直接写进主包内
                    if (min.major() <= MinecraftVersion.V1_20_1.packFormat().major()) {
                        Path atlasPath = path.resolve("assets")
                                .resolve("minecraft")
                                .resolve("atlases")
                                .resolve("blocks.json");
                        this.context.writeJson(entry.atlas(), atlasPath);
                    } else {
                        Path atlasPath = path.resolve(directoryName)
                                .resolve("assets")
                                .resolve("minecraft")
                                .resolve("atlases")
                                .resolve("blocks.json");
                        this.context.writeJson(entry.atlas(), atlasPath);
                        packOverlays.addOverlay(new Overlay(min, max, directoryName));
                    }
                }
            }
        }

        private AtlasValidation.ValidationResult validateResourcePackWithOverlays(
                Path path,
                Path[] rootPaths,
                ModelReferenceCollector references,
                Set<Revision> revisions,
                boolean v1_21_6,
                boolean modern,
                boolean v26_1,
                boolean obfuscation) {
            ResourcePackView view = new ResourcePackView(rootPaths);
            AtlasValidation atlas = new AtlasValidation(this.context, view, modern);
            ResourceValidation.ModelReferences modelReferences = new ResourceValidation(this.context, references).collectAndValidate(rootPaths, view);
            ModelValidation.Models models = new ModelValidation(this.context).validate(path, rootPaths, view, modelReferences, revisions, v1_21_6, modern, v26_1);
            return atlas.validate(models, view, modern, obfuscation);
        }
    }
}
