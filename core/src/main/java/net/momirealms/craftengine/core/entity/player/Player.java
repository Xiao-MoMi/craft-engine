package net.momirealms.craftengine.core.entity.player;

import com.google.common.cache.Cache;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.advancement.AdvancementType;
import net.momirealms.craftengine.core.attribute.damage.DamageVisibility;
import net.momirealms.craftengine.core.block.entity.render.ConstantBlockEntityRenderer;
import net.momirealms.craftengine.core.block.entity.render.DynamicBlockEntityRenderer;
import net.momirealms.craftengine.core.entity.LivingEntity;
import net.momirealms.craftengine.core.entity.culling.Cullable;
import net.momirealms.craftengine.core.entity.culling.CullableHolder;
import net.momirealms.craftengine.core.entity.furniture.behavior.FurnitureLightData;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.ContextKey;
import net.momirealms.craftengine.core.plugin.context.CooldownData;
import net.momirealms.craftengine.core.plugin.context.PlayerContext;
import net.momirealms.craftengine.core.plugin.context.parameter.PlayerParameterProvider;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.sound.SoundSource;
import net.momirealms.craftengine.core.util.GameEdition;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.Tristate;
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
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public interface Player extends NetWorkUser, LivingEntity {
    Key TYPE = Key.of("minecraft:player");

    PlayerContext constantContext();

    @Override
    default <T> Optional<T> getParameter(ContextKey<T> key) {
        return PlayerParameterProvider.INSTANCE.getOptionalParameter(key, this);
    }

    boolean isSecondaryUseActive();

    @NotNull
    Item getItemBySlot(int slot);

    Object platformPlayer();

    Object minecraftPlayer();

    default Object serverPlayer() {
        return minecraftPlayer();
    }

    void setClientSideWorld(World world);

    void entityCullingTick();

    void asyncTick();

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

    boolean discoverRecipe(Key recipe);

    boolean hasDiscoveredRecipe(Key recipe);

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

    /**
     * 更新并保存单个资源包的偏好。TRUE 为启用，FALSE 为禁用，UNDEFINED 为恢复配置默认值。
     * 本服实际选择发生变化时重新发送资源包；未托管的包仅保存偏好。
     *
     * @return 偏好保存和必要的发送操作完成后返回是否修改了偏好，不等待客户端加载完成
     */
    default CompletableFuture<Boolean> setPackPreference(@NotNull String pack, @NotNull Tristate enabled) {
        return plugin().packManager().setPackPreference(uuid(), pack, enabled);
    }

    /**
     * 批量更新资源包偏好，未提供的包保持原偏好。UNDEFINED 表示恢复该包的配置默认值。
     * 整批保存后至多重新发送一次资源包，不会逐包触发重载。
     *
     * @return 偏好保存和必要的发送操作完成后返回是否修改了偏好，不等待客户端加载完成
     */
    default CompletableFuture<Boolean> setPackPreference(@NotNull Map<String, @NotNull Tristate> preferences) {
        return plugin().packManager().setPackPreferences(uuid(), preferences);
    }

    void performCommand(String command, boolean asOp);

    void performCommandAsEvent(String command);

    void transfer(String server);

    void transfer(String host, int port);

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

    double entityCullingDistanceScale();

    void setDisplayEntityViewDistanceScale(double value);

    double displayEntityViewDistance();

    void setEnableEntityCulling(boolean enable);

    boolean enableEntityCulling();

    boolean enableFurnitureDebug();

    void setDamageVisibility(DamageVisibility visibility);

    DamageVisibility damageVisibility();

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

    void addTrackedDynamicBlockEntities(Map<BlockPos, DynamicBlockEntityRenderer> renderers);

    void addTrackedDynamicBlockEntity(BlockPos blockPos, DynamicBlockEntityRenderer renderer);

    CullableHolder getTrackedDynamicBlockEntity(BlockPos blockPos);

    default boolean setDynamicBlockEntityForceVisible(BlockPos pos, boolean forceVisible) {
        CullableHolder holder = this.getTrackedDynamicBlockEntity(pos);
        if (holder == null) {
            return false;
        }
        holder.setForceVisible(this, forceVisible);
        return true;
    }

    void removeTrackedDynamicBlockEntities(Collection<BlockPos> renders);

    void removeTrackedDynamicBlockEntity(BlockPos pos);

    default boolean isDynamicBlockEntityVisible(BlockPos pos) {
        if (!Config.enableEntityCulling()) {
            return true;
        }
        CullableHolder holder = this.getTrackedDynamicBlockEntity(pos);
        return holder != null && holder.isShown;
    }

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

    /** 仅移除实体的剔除追踪记录；客户端隐藏由调用方负责。 */
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
