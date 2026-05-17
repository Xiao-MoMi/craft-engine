package net.momirealms.craftengine.bukkit.painting;

import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.core.painting.AbstractPaintingManager;
import net.momirealms.craftengine.core.painting.Painting;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.HolderProxy;
import net.momirealms.craftengine.proxy.minecraft.core.HolderSetProxy;
import net.momirealms.craftengine.proxy.minecraft.core.MappedRegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.RegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.tags.PaintingVariantTagsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.decoration.painting.PaintingVariantProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BukkitPaintingManager extends AbstractPaintingManager {

    public BukkitPaintingManager(BukkitCraftEngine plugin) {
        super(plugin);
    }

    @Override
    public void runDelayedSyncTasks() {
        super.runDelayedSyncTasks();
    }

    @Override
    protected void registerPaintings(Map<Key, Painting> paintings) {
        if (paintings.isEmpty()) return;
        Object registry = RegistryUtils.lookupOrThrow(RegistriesProxy.PAINTING_VARIANT);
        try {
            MappedRegistryProxy.INSTANCE.setFrozen(registry, false);
            Iterable<Object> tag = MappedRegistryProxy.INSTANCE.getOrCreateTagForRegistration(registry, PaintingVariantTagsProxy.PLACEABLE);
            List<Object> contents = new ArrayList<>(HolderSetProxy.NamedProxy.INSTANCE.contents(tag));
            for (Map.Entry<Key, Painting> entry : paintings.entrySet()) {
                Key id = entry.getKey();
                Painting painting = entry.getValue();
                Object identifier = KeyUtils.toIdentifier(id);
                Object paintingVariant = RegistryUtils.getRegistryValue(registry, identifier);
                boolean showInOpTab = painting.showInOpTab();
                if (paintingVariant == null) {
                    paintingVariant = createPaintingVariant(painting);
                    Object holder = RegistryProxy.INSTANCE.registerForHolder$1(registry, identifier, paintingVariant);
                    HolderProxy.ReferenceProxy.INSTANCE.bindValue(holder, paintingVariant);
                    if (showInOpTab) {
                        HolderProxy.ReferenceProxy.INSTANCE.setTags(holder, Set.of());
                        contents.remove(holder);
                    } else {
                        HolderProxy.ReferenceProxy.INSTANCE.setTags(holder, Set.of(PaintingVariantTagsProxy.PLACEABLE));
                        contents.add(holder);
                    }
                } else {
                    PaintingVariantProxy.INSTANCE.setWidth(paintingVariant, painting.width());
                    PaintingVariantProxy.INSTANCE.setHeight(paintingVariant, painting.height());
                    PaintingVariantProxy.INSTANCE.setAssetId(paintingVariant, KeyUtils.toIdentifier(painting.assetId()));
                    PaintingVariantProxy.INSTANCE.setTitle(paintingVariant, painting.title().map(ComponentUtils::adventureToMinecraft));
                    PaintingVariantProxy.INSTANCE.setAuthor(paintingVariant, painting.author().map(ComponentUtils::adventureToMinecraft));
                    Object holder = RegistryUtils.getHolderById(registry, identifier);
                    if (showInOpTab) {
                        HolderProxy.ReferenceProxy.INSTANCE.setTags(holder, Set.of());
                        contents.remove(holder);
                    } else {
                        HolderProxy.ReferenceProxy.INSTANCE.setTags(holder, Set.of(PaintingVariantTagsProxy.PLACEABLE));
                        contents.add(holder);
                    }
                }
            }
            HolderSetProxy.NamedProxy.INSTANCE.bind(tag, contents);
        } catch (Throwable e) {
            this.plugin.logger().warn("Failed to register paintings", e);
        } finally {
            MappedRegistryProxy.INSTANCE.setFrozen(registry, true);
        }
    }

    private static Object createPaintingVariant(Painting painting) {
        if (VersionHelper.isOrAbove1_21_2) {
            return PaintingVariantProxy.INSTANCE.newInstance(
                    painting.width(), painting.height(),
                    KeyUtils.toIdentifier(painting.assetId()),
                    painting.title().map(ComponentUtils::adventureToMinecraft),
                    painting.author().map(ComponentUtils::adventureToMinecraft)
            );
        } else if (VersionHelper.isOrAbove1_21) {
            return PaintingVariantProxy.INSTANCE.newInstance(
                    painting.width(), painting.height(),
                    KeyUtils.toIdentifier(painting.assetId())
            );
        } else {
            return PaintingVariantProxy.INSTANCE.newInstance(painting.width(), painting.height());
        }
    }
}
