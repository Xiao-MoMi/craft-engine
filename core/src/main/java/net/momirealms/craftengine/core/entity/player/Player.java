package net.momirealms.craftengine.core.entity.player;

import com.google.common.cache.Cache;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.advancement.AdvancementType;
import net.momirealms.craftengine.core.block.entity.render.ConstantBlockEntityRenderer;
import net.momirealms.craftengine.core.entity.LivingEntity;
import net.momirealms.craftengine.core.entity.culling.Cullable;
import net.momirealms.craftengine.core.entity.culling.CullableHolder;
import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureLightData;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.context.ContextKey;
import net.momirealms.craftengine.core.plugin.context.CooldownData;
import net.momirealms.craftengine.core.plugin.context.parameter.PlayerParameterProvider;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.sound.SoundSource;
import net.momirealms.craftengine.core.util.GameEdition;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Position;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public interface Player extends NetWorkUser, LivingEntity {
    Key TYPE = Key.of("minecraft:player");

    @Override
    default <T> Optional<T> getParameter(ContextKey<T> key) {
        return PlayerParameterProvider.INSTANCE.getOptionalParameter(key, this);
    }

    boolean isSecondaryUseActive();

    @NotNull
    Item getItemBySlot(int slot);

    @Override
    Object platformPlayer();

    @Override
    Object serverPlayer();

    void setClientSideWorld(World world);

    void entityCullingTick();

    float getDestroyProgress(Object blockState, BlockPos pos);

    void setClientSideCanBreakBlock(boolean canBreak);

    void finishMiningBlock();

    void preventMiningBlock();

    void stopMiningBlock();

    void abortMiningBlock();

    boolean clientSideCanBreak();

    void breakBlock(int x, int y, int z);

    double getCachedInteractionRange();

    void onSwingHand();

    boolean isMiningBlock();

    boolean shouldSyncAttribute();

    boolean isFlying();

    GameMode gameMode();

    void setGameMode(GameMode gameMode);

    boolean canBreak(BlockPos pos, Object state);

    boolean canPlace(BlockPos pos, Object state);

    void sendToast(Component text, Item icon, AdvancementType type);

    void sendActionBar(Component text);

    void sendMessage(Component text, boolean overlay);

    void sendTitle(Component title, Component subtitle, int fadeIn, int stay, int fadeOut);

    void setIsSimulatingInteraction(boolean isSimulating);

    boolean isSimulatingInteraction();

    boolean updateLastSuccessfulInteractionTick(int tick);

    int lastSuccessfulInteractionTick();

    void updateLastInteractEntityTick(@NotNull InteractionHand hand);

    boolean lastInteractEntityCheck(@NotNull InteractionHand hand);

    int gameTicks();

    boolean hasInteractionInThisTick();

    void swingHand(InteractionHand hand);

    boolean hasPermission(String permission);

    boolean canInstabuild();

    default void playSound(Key sound) {
        playSound(sound, 1f, 1f);
    }

    default void playSound(Key sound, float volume, float pitch) {
        playSound(sound, SoundSource.MASTER, volume, pitch);
    }

    void playSound(Key sound, SoundSource source, float volume, float pitch);

    void playSound(Position pos, Key sound, SoundSource source, float volume, float pitch);

    default void playSound(BlockPos pos, Key sound, SoundSource source, float volume, float pitch) {
        this.playSound(Vec3d.atCenterOf(pos), sound, source, volume, pitch);
    }

    default void playSound(BlockPos pos, SoundData data, SoundSource source) {
        this.playSound(pos, data.id(), source, data.volume().get(), data.pitch().get());
    }

    default void playSound(Position pos, SoundData data, SoundSource source) {
        this.playSound(pos, data.id(), source, data.volume().get(), data.pitch().get());
    }

    void giveItem(Item item, boolean spawnFakeEntity);

    default void giveItem(Item item) {
        giveItem(item, true);
    }

    void closeInventory();

    void clearEntityView();

    void unloadCurrentResourcePack();

    void performCommand(String command, boolean asOp);

    void performCommandAsEvent(String command);

    @Override
    default Key type() {
        return TYPE;
    }

    default boolean isCreativeMode() {
        return gameMode() == GameMode.CREATIVE;
    }

    default boolean isSpectatorMode() {
        return gameMode() == GameMode.SPECTATOR;
    }

    default boolean isSurvivalMode() {
        return gameMode() == GameMode.SURVIVAL;
    }

    default boolean isAdventureMode() {
        return gameMode() == GameMode.ADVENTURE;
    }

    int foodLevel();

    void setFoodLevel(int foodLevel);

    float saturation();

    void setSaturation(float saturation);

    CooldownData cooldown();

    Locale locale();

    void setClientLocale(Locale clientLocale);

    Locale selectedLocale();

    void setSelectedLocale(@Nullable Locale locale);

    void setEntityCullingDistanceScale(double value);

    void setDisplayEntityViewDistanceScale(double value);

    double displayEntityViewDistance();

    void setEnableEntityCulling(boolean enable);

    boolean enableEntityCulling();

    boolean enableFurnitureDebug();

    void setEnableFurnitureDebug(boolean enableFurnitureDebug);

    void giveExperiencePoints(int xpPoints);

    void giveExperienceLevels(int levels);

    int getXpNeededForNextLevel();

    void setExperiencePoints(int experiencePoints);

    void setExperienceLevels(int level);

    void sendTotemAnimation(Item totem, @Nullable SoundData sound, boolean silent);

    void addTrackedBlockEntities(Map<BlockPos, ConstantBlockEntityRenderer> renders);

    void addTrackedBlockEntity(BlockPos blockPos, ConstantBlockEntityRenderer renderer);

    CullableHolder getTrackedBlockEntity(BlockPos blockPos);

    void removeTrackedBlockEntities(Collection<BlockPos> renders);

    CullableHolder getTrackedEntity(int entityId);

    void addTrackedEntity(int entityId, Cullable cullable);

    void removeTrackedBlockEntities(BlockPos pos);

    void clearTrackedBlockEntities();

    int clearOrCountMatchingInventoryItems(Predicate<Item> predicate, int count);

    default int clearOrCountMatchingInventoryItems(Key itemId, int count) {
        return this.clearOrCountMatchingInventoryItems(item -> itemId.equals(item.id()), count);
    }

    GameEdition gameEdition();

    @Override
    default void remove() {
    }

    FurnitureLightData furnitureLightData();

    void playParticle(Key particleId, double x, double y, double z);

    void removeTrackedEntity(int entityId);

    void clearTrackedEntities();

    Cache<Object, Boolean> receivedMapData();

    boolean canInteractPoint(Vec3d vec3d, double range);

    @Override
    default boolean isValid() {
        return this.isOnline();
    }

    void setItemCooldown(Key id, int ticks);

    int getItemCooldown(Key id);
}
