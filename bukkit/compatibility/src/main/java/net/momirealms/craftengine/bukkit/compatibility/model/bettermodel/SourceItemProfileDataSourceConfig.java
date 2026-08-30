package net.momirealms.craftengine.bukkit.compatibility.model.bettermodel;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.manager.ProfileManager;
import kr.toxicity.model.api.profile.ModelProfile;
import kr.toxicity.model.api.profile.ModelProfileInfo;
import kr.toxicity.model.api.profile.ModelProfileSkin;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.data.FurnitureDataResolver;
import net.momirealms.craftengine.core.entity.furniture.data.FurnitureDataSourceConfig;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public final class SourceItemProfileDataSourceConfig implements FurnitureDataSourceConfig<ModelProfile.Uncompleted> {
    private static final SourceItemProfileDataSourceConfig INSTANCE = new SourceItemProfileDataSourceConfig();

    private SourceItemProfileDataSourceConfig() {
    }

    public static @NotNull SourceItemProfileDataSourceConfig fromConfig() {
        return INSTANCE;
    }


    private static @Nullable ModelProfileSkin skin(ProfileManager manager, String rawTextures) {
        try {
            return manager.skin(rawTextures);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    static @Nullable ProfileState readModernProfile(@Nullable Tag profileTag) {
        if (!(profileTag instanceof CompoundTag profile)) {
            return null;
        }
        return profileState(
                readUUID(profile.get("id")),
                readString(profile.get("name")),
                readModernTextures(profile.get("properties"))
        );
    }

    static @Nullable ProfileState readLegacyProfile(@Nullable Tag profileTag) {
        if (profileTag instanceof CompoundTag profile) {
            return profileState(
                    readUUID(profile.get("Id")),
                    readString(profile.get("Name")),
                    readLegacyTextures(profile.get("Properties"))
            );
        }
        if (profileTag instanceof StringTag owner) {
            return profileState(null, nonEmpty(owner.value()), null);
        }
        return null;
    }

    private static @Nullable ProfileState profileState(
            @Nullable UUID id,
            @Nullable String name,
            @Nullable String rawTextures
    ) {
        UUID normalizedId = hasUsableId(id) ? id : null;
        return normalizedId != null || name != null || rawTextures != null
                ? new ProfileState(normalizedId, name, rawTextures)
                : null;
    }

    private static @Nullable String readModernTextures(@Nullable Tag propertiesTag) {
        if (!(propertiesTag instanceof ListTag properties)) {
            return null;
        }
        for (int i = 0; i < properties.size(); i++) {
            if (!(properties.get(i) instanceof CompoundTag property)) {
                continue;
            }
            if ("textures".equals(readString(property.get("name")))) {
                String value = readString(property.get("value"));
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    private static @Nullable String readLegacyTextures(@Nullable Tag propertiesTag) {
        if (!(propertiesTag instanceof CompoundTag properties)) {
            return null;
        }
        Tag texturesTag = properties.get("textures");
        if (!(texturesTag instanceof ListTag textures) || textures.isEmpty()) {
            return null;
        }
        Tag textureTag = textures.getFirst();
        return textureTag instanceof CompoundTag texture ? readString(texture.get("Value")) : null;
    }

    private static @Nullable String readString(@Nullable Tag tag) {
        return tag instanceof StringTag string ? nonEmpty(string.value()) : null;
    }

    private static @Nullable String nonEmpty(String value) {
        return value.isEmpty() ? null : value;
    }

    private static @Nullable UUID readUUID(@Nullable Tag tag) {
        if (tag instanceof IntArrayTag array && array.size() == 4) {
            return array.getAsUUID();
        }
        return tag instanceof StringTag string ? parseUUID(string.value()) : null;
    }

    private static @Nullable UUID parseUUID(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @NotNull
    static ModelProfileInfo embeddedProfileInfo(ProfileState state) {
        UUID id = hasUsableId(state.id()) ? state.id() : (state.name() != null ? ModelProfileInfo.UNKNOWN.id() : stableTextureId(Objects.requireNonNull(state.rawTextures())));
        return new ModelProfileInfo(id, state.name());
    }

    @Nullable
    static ModelProfileInfo lookupProfileInfo(ProfileState state) {
        if (hasUsableId(state.id())) {
            return new ModelProfileInfo(state.id(), state.name());
        }
        return state.name() != null ? new ModelProfileInfo(ModelProfileInfo.UNKNOWN.id(), state.name()) : null;
    }

    private static boolean hasUsableId(UUID id) {
        return id != null && !id.equals(ModelProfileInfo.UNKNOWN.id());
    }

    private static UUID stableTextureId(String rawTextures) {
        return UUID.nameUUIDFromBytes(("ce:" + rawTextures).getBytes(StandardCharsets.UTF_8));
    }

    private static @Nullable ProfileState readProfile(@NotNull Item item) {
        if (VersionHelper.COMPONENT_RELEASE) {
            return readModernProfile(item.getComponentAsSparrowTag(DataComponentKeys.PROFILE));
        }
        return readLegacyProfile(item.getSparrowTag("SkullOwner"));
    }

    @Override
    public @NotNull FurnitureDataResolver<ModelProfile.Uncompleted> bind(@NotNull Furniture furniture) {
        return () -> resolve(furniture.sourceItem());
    }

    private @Nullable ModelProfile.Uncompleted resolve(@Nullable Item item) {
        if (item == null) {
            return null;
        }
        ProfileState state = readProfile(item);
        if (state == null) {
            return null;
        }

        ProfileManager manager = BetterModel.platform().manager(ProfileManager.class);
        if (state.rawTextures() != null) {
            ModelProfileSkin skin = skin(manager, state.rawTextures());
            if (skin != null) {
                return ModelProfile.of(embeddedProfileInfo(state), skin).asUncompleted();
            }
        }

        ModelProfileInfo info = lookupProfileInfo(state);
        if (info == null) {
            return null;
        }
        return manager.supplier().supply(info);
    }

    record ProfileState(@Nullable UUID id, @Nullable String name, @Nullable String rawTextures) {
    }
}
