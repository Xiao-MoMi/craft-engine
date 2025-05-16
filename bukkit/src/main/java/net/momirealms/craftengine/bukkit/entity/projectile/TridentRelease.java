package net.momirealms.craftengine.bukkit.entity.projectile;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.core.util.MCUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

public class TridentRelease {

    private TridentRelease() {}

    public static boolean releaseUsing(Object stack, Object level, Object entity) {
        if (VersionHelper.isOrAbove1_21_2()) {
            return releaseUsing_1_21_2(stack, level, entity);
        } else if (VersionHelper.isOrAbove1_21()) {
            return releaseUsing_1_21(stack, level, entity);
        } else if (VersionHelper.isOrAbove1_20_5()) {
            return releaseUsing_1_20_5(stack, level, entity);
        } else if (VersionHelper.isOrAbove1_20_3()) {
            return releaseUsing_1_20_3(stack, level, entity);
        } else if (VersionHelper.isOrAbove1_20()) {
            return releaseUsing_1_20(stack, level, entity);
        }
        return false;
    }

    private static boolean releaseUsing_1_21_2(Object stack, Object level, Object entity) {
        Object copyStack = FastNMS.INSTANCE.method$ItemStack$copyWithCount(stack, 1);
        if (FastNMS.INSTANCE.method$ItemStack$isEmpty(copyStack)) return false;
        Object copyStack1 = FastNMS.INSTANCE.method$ItemStack$copy(stack);
        if (FastNMS.INSTANCE.method$ItemStack$isEmpty(copyStack1)) return false;
        float spinStrength = FastNMS.INSTANCE.method$EnchantmentHelper$getTridentSpinAttackStrength(stack, entity);
        if ((spinStrength > 0.0F && !FastNMS.INSTANCE.method$Entity$isInWaterOrRain(entity)) || FastNMS.INSTANCE.method$ItemStack$nextDamageWillBreak(stack)) {
            return false;
        }
        FastNMS.INSTANCE.method$ItemStack$setDamageValue(copyStack, FastNMS.INSTANCE.method$ItemStack$getDamageValue(stack) + 1);
        FastNMS.INSTANCE.method$ItemStack$setDamageValue(copyStack1, FastNMS.INSTANCE.method$ItemStack$getDamageValue(stack) + 1);

        Object sound = FastNMS.INSTANCE.method$EnchantmentHelper$pickHighestLevel(stack);

        if (spinStrength == 0.0F) {
            Object projectile = FastNMS.INSTANCE.method$Projectile$ThrownTrident$spawnProjectileFromRotationDelayed(
                    level,
                    copyStack,
                    entity,
                    0.0F,
                    2.5F,
                    1.0F
            );
            PlayerLaunchProjectileEvent event = new PlayerLaunchProjectileEvent(
                    (Player) FastNMS.INSTANCE.method$Entity$getBukkitEntity(entity),
                    FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(stack),
                    (Projectile) FastNMS.INSTANCE.method$Entity$getBukkitEntity(FastNMS.INSTANCE.method$Projectile$Delayed$projectile(projectile))
            );

            if (!event.callEvent() || !FastNMS.INSTANCE.method$Projectile$Delayed$attemptSpawn(projectile)) {
                FastNMS.INSTANCE.method$AbstractContainerMenu$sendAllDataToRemote(FastNMS.INSTANCE.field$Player$containerMenu(entity));
                return false;
            }

            Object trident = FastNMS.INSTANCE.method$Projectile$Delayed$projectile(projectile);
            if (event.shouldConsume()) {
                FastNMS.INSTANCE.method$ItemStack$hurtWithoutBreaking(stack, 1, entity);
                FastNMS.INSTANCE.method$ItemStack$consume(stack, 1, entity);
            }

            FastNMS.INSTANCE.field$AbstractArrow$pickupItemStack(trident, copyStack1);
            if (FastNMS.INSTANCE.method$Player$hasInfiniteMaterials(entity)) {
                FastNMS.INSTANCE.field$AbstractArrow$pickup(trident, Reflections.instance$AbstractArrow$Pickup$CREATIVE_ONLY);
            }

            FastNMS.INSTANCE.method$Level$playSound(
                    level,
                    null,
                    trident,
                    FastNMS.INSTANCE.method$Holder$value(sound),
                    Reflections.instance$SoundSource$PLAYERS,
                    1.0F, 1.0F
            );
            return true;
        }

        float yaw = FastNMS.INSTANCE.method$Entity$getYRot(entity);
        float pitch = FastNMS.INSTANCE.method$Entity$getXRot(entity);
        float x = -MCUtils.sin(yaw * MCUtils.DEG_TO_RAD) * MCUtils.cos(pitch * MCUtils.DEG_TO_RAD);
        float y = -MCUtils.sin(pitch * MCUtils.DEG_TO_RAD);
        float z = MCUtils.cos(yaw * MCUtils.DEG_TO_RAD) * MCUtils.cos(pitch * MCUtils.DEG_TO_RAD);

        float length = MCUtils.sqrt(x * x + y * y + z * z);
        x = x / length * spinStrength;
        y = y / length * spinStrength;
        z = z / length * spinStrength;

        FastNMS.INSTANCE.method$CraftEventFactory$callPlayerRiptideEvent(entity, stack, x, y, z);
        FastNMS.INSTANCE.method$Entity$push(entity, x, y, z);
        FastNMS.INSTANCE.field$Entity$hurtMarked(entity, true);
        FastNMS.INSTANCE.method$ItemStack$setDamageValue(stack, FastNMS.INSTANCE.method$ItemStack$getDamageValue(stack) + 1);
        FastNMS.INSTANCE.method$Player$startAutoSpinAttack(entity, 20, 8.0F, stack);

        if (FastNMS.INSTANCE.method$Entity$onGround(entity)) {
            FastNMS.INSTANCE.method$Entity$move(entity, Reflections.instance$MoverType$SELF, FastNMS.INSTANCE.constructor$Vec3(0.0D, 1.1999999D, 0.0D));
        }

        FastNMS.INSTANCE.method$Level$playSound(
                level,
                null,
                entity,
                FastNMS.INSTANCE.method$Holder$value(sound),
                Reflections.instance$SoundSource$PLAYERS,
                1.0F, 1.0F
        );
        return true;
    }

    private static boolean releaseUsing_1_21(Object stack, Object level, Object entity) {
        Object copyStack = FastNMS.INSTANCE.method$ItemStack$copy(stack);
        if (FastNMS.INSTANCE.method$ItemStack$isEmpty(copyStack)) return false;

        float spinStrength = FastNMS.INSTANCE.method$EnchantmentHelper$getTridentSpinAttackStrength(stack, entity);

        if ((spinStrength > 0.0F && !FastNMS.INSTANCE.method$Entity$isInWaterOrRain(entity)) || FastNMS.INSTANCE.method$ItemStack$nextDamageWillBreak(stack)) {
            return false;
        }
        FastNMS.INSTANCE.method$ItemStack$setDamageValue(copyStack, FastNMS.INSTANCE.method$ItemStack$getDamageValue(stack) + 1);

        Object sound = FastNMS.INSTANCE.method$EnchantmentHelper$pickHighestLevel(stack);

        if (spinStrength == 0.0F) {
            Object entitythrowntrident = FastNMS.INSTANCE.constructor$ThrownTrident(level, entity, stack);
            FastNMS.INSTANCE.method$ThrownTrident$shootFromRotation(
                    entitythrowntrident,
                    entity,
                    FastNMS.INSTANCE.method$Entity$getXRot(entity),
                    FastNMS.INSTANCE.method$Entity$getYRot(entity),
                    0.0F, 2.5F, 1.0F
            );
            if (FastNMS.INSTANCE.method$Player$hasInfiniteMaterials(entity)) {
                FastNMS.INSTANCE.field$AbstractArrow$pickup(entitythrowntrident, Reflections.instance$AbstractArrow$Pickup$CREATIVE_ONLY);
            }

            PlayerLaunchProjectileEvent event = new PlayerLaunchProjectileEvent(
                    (Player) FastNMS.INSTANCE.method$Entity$getBukkitEntity(entity),
                    FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(stack),
                    (Projectile) FastNMS.INSTANCE.method$Entity$getBukkitEntity(entitythrowntrident)
            );
            if (!event.callEvent() || !FastNMS.INSTANCE.method$LevelWriter$addFreshEntity(level, entitythrowntrident)) {
                Entity bukkitEntity = FastNMS.INSTANCE.method$Entity$getBukkitEntity(entity);
                if (bukkitEntity instanceof Player player) {
                    player.updateInventory();
                }

                return false;
            }

            if (event.shouldConsume()) {
                FastNMS.INSTANCE.method$ItemStack$hurtAndBreak(
                        stack, 1, entity,
                        FastNMS.INSTANCE.method$LivingEntity$getSlotForHand(FastNMS.INSTANCE.method$LivingEntity$getUsedItemHand(entity))
                );
            }

            FastNMS.INSTANCE.field$AbstractArrow$pickupItemStack(entitythrowntrident, copyStack);
            FastNMS.INSTANCE.method$Level$playSound(
                    level,
                    null,
                    entitythrowntrident,
                    FastNMS.INSTANCE.method$Holder$value(sound),
                    Reflections.instance$SoundSource$PLAYERS,
                    1.0F, 1.0F
            );
            if (event.shouldConsume() && !FastNMS.INSTANCE.method$Player$hasInfiniteMaterials(entity)) {
                FastNMS.INSTANCE.method$Inventory$removeItem(FastNMS.INSTANCE.method$Player$getInventory(entity), stack);
            }
            return true;
        }

        float yaw = FastNMS.INSTANCE.method$Entity$getYRot(entity);
        float pitch = FastNMS.INSTANCE.method$Entity$getXRot(entity);
        float x = -MCUtils.sin(yaw * MCUtils.DEG_TO_RAD) * MCUtils.cos(pitch * MCUtils.DEG_TO_RAD);
        float y = -MCUtils.sin(pitch * MCUtils.DEG_TO_RAD);
        float z = MCUtils.cos(yaw * MCUtils.DEG_TO_RAD) * MCUtils.cos(pitch * MCUtils.DEG_TO_RAD);

        float length = MCUtils.sqrt(x * x + y * y + z * z);
        x = x / length * spinStrength;
        y = y / length * spinStrength;
        z = z / length * spinStrength;

        FastNMS.INSTANCE.method$CraftEventFactory$callPlayerRiptideEvent(entity, stack, x, y, z);
        FastNMS.INSTANCE.method$Entity$push(entity, x, y, z);
        FastNMS.INSTANCE.field$Entity$hurtMarked(entity, true);
        FastNMS.INSTANCE.method$ItemStack$setDamageValue(stack, FastNMS.INSTANCE.method$ItemStack$getDamageValue(stack) + 1);
        FastNMS.INSTANCE.method$Player$startAutoSpinAttack(entity, 20, 8.0F, stack);

        if (FastNMS.INSTANCE.method$Entity$onGround(entity)) {
            FastNMS.INSTANCE.method$Entity$move(entity, Reflections.instance$MoverType$SELF, FastNMS.INSTANCE.constructor$Vec3(0.0D, 1.1999999D, 0.0D));
        }

        FastNMS.INSTANCE.method$Level$playSound(
                level,
                null,
                entity,
                FastNMS.INSTANCE.method$Holder$value(sound),
                Reflections.instance$SoundSource$PLAYERS,
                1.0F, 1.0F
        );
        return true;
    }

    private static boolean releaseUsing_1_20_5(Object stack, Object level, Object entity) {
        Object copyStack = FastNMS.INSTANCE.method$ItemStack$copy(stack);
        if (FastNMS.INSTANCE.method$ItemStack$isEmpty(copyStack)) return false;

        float spinStrength = FastNMS.INSTANCE.method$EnchantmentHelper$getTridentSpinAttackStrength(stack, entity);

        if ((spinStrength > 0.0F && !FastNMS.INSTANCE.method$Entity$isInWaterOrRain(entity)) || FastNMS.INSTANCE.method$ItemStack$nextDamageWillBreak(stack)) {
            return false;
        }
        FastNMS.INSTANCE.method$ItemStack$setDamageValue(copyStack, FastNMS.INSTANCE.method$ItemStack$getDamageValue(stack) + 1);

        if (spinStrength == 0.0F) {
            Object entitythrowntrident = FastNMS.INSTANCE.constructor$ThrownTrident(level, entity, stack);
            FastNMS.INSTANCE.method$ThrownTrident$shootFromRotation(
                    entitythrowntrident,
                    entity,
                    FastNMS.INSTANCE.method$Entity$getXRot(entity),
                    FastNMS.INSTANCE.method$Entity$getYRot(entity),
                    0.0F, 2.5F, 1.0F
            );
            if (FastNMS.INSTANCE.method$Player$hasInfiniteMaterials(entity)) {
                FastNMS.INSTANCE.field$AbstractArrow$pickup(entitythrowntrident, Reflections.instance$AbstractArrow$Pickup$CREATIVE_ONLY);
            }

            PlayerLaunchProjectileEvent event = new PlayerLaunchProjectileEvent(
                    (Player) FastNMS.INSTANCE.method$Entity$getBukkitEntity(entity),
                    FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(stack),
                    (Projectile) FastNMS.INSTANCE.method$Entity$getBukkitEntity(entitythrowntrident)
            );
            if (!event.callEvent() || !FastNMS.INSTANCE.method$LevelWriter$addFreshEntity(level, entitythrowntrident)) {
                Entity bukkitEntity = FastNMS.INSTANCE.method$Entity$getBukkitEntity(entity);
                if (bukkitEntity instanceof Player player) {
                    player.updateInventory();
                }

                return false;
            }

            if (event.shouldConsume()) {
                FastNMS.INSTANCE.method$ItemStack$hurtAndBreak(
                        stack, 1, entity,
                        FastNMS.INSTANCE.method$LivingEntity$getSlotForHand(FastNMS.INSTANCE.method$LivingEntity$getUsedItemHand(entity))
                );
            }

            FastNMS.INSTANCE.field$AbstractArrow$pickupItemStack(entitythrowntrident, copyStack);
            FastNMS.INSTANCE.method$Level$playSound(
                    level,
                    null,
                    entitythrowntrident,
                    Reflections.instance$SoundEvent$TRIDENT_THROW,
                    Reflections.instance$SoundSource$PLAYERS,
                    1.0F, 1.0F
            );
            if (event.shouldConsume() && !FastNMS.INSTANCE.method$Player$hasInfiniteMaterials(entity)) {
                FastNMS.INSTANCE.method$Inventory$removeItem(FastNMS.INSTANCE.method$Player$getInventory(entity), stack);
            }
            return true;
        }

        float yaw = FastNMS.INSTANCE.method$Entity$getYRot(entity);
        float pitch = FastNMS.INSTANCE.method$Entity$getXRot(entity);
        float x = -MCUtils.sin(yaw * MCUtils.DEG_TO_RAD) * MCUtils.cos(pitch * MCUtils.DEG_TO_RAD);
        float y = -MCUtils.sin(pitch * MCUtils.DEG_TO_RAD);
        float z = MCUtils.cos(yaw * MCUtils.DEG_TO_RAD) * MCUtils.cos(pitch * MCUtils.DEG_TO_RAD);

        float length = MCUtils.sqrt(x * x + y * y + z * z);
        x = x / length * spinStrength;
        y = y / length * spinStrength;
        z = z / length * spinStrength;

        FastNMS.INSTANCE.method$CraftEventFactory$callPlayerRiptideEvent(entity, stack, x, y, z);
        FastNMS.INSTANCE.method$Entity$push(entity, x, y, z);
        FastNMS.INSTANCE.field$Entity$hurtMarked(entity, true);
        FastNMS.INSTANCE.method$ItemStack$setDamageValue(stack, FastNMS.INSTANCE.method$ItemStack$getDamageValue(stack) + 1);
        FastNMS.INSTANCE.method$Player$startAutoSpinAttack(entity, 20, -1.0F, null);

        if (FastNMS.INSTANCE.method$Entity$onGround(entity)) {
            FastNMS.INSTANCE.method$Entity$move(entity, Reflections.instance$MoverType$SELF, FastNMS.INSTANCE.constructor$Vec3(0.0D, 1.1999999D, 0.0D));
        }

        Object soundeffect;
        if (spinStrength >= 3) {
            soundeffect = Reflections.instance$SoundEvent$TRIDENT_RIPTIDE_3;
        } else if (spinStrength == 2) {
            soundeffect = Reflections.instance$SoundEvent$TRIDENT_RIPTIDE_2;
        } else {
            soundeffect = Reflections.instance$SoundEvent$TRIDENT_RIPTIDE_1;
        }

        FastNMS.INSTANCE.method$Level$playSound(
                level,
                null,
                entity,
                soundeffect,
                Reflections.instance$SoundSource$PLAYERS,
                1.0F, 1.0F
        );
        return true;
    }

    private static boolean releaseUsing_1_20_3(Object stack, Object level, Object entity) {
        Object copyStack = FastNMS.INSTANCE.method$ItemStack$copy(stack);
        if (FastNMS.INSTANCE.method$ItemStack$isEmpty(copyStack)) return false;

        float spinStrength = FastNMS.INSTANCE.method$EnchantmentHelper$getTridentSpinAttackStrength(stack, entity);

        if ((spinStrength > 0.0F && !FastNMS.INSTANCE.method$Entity$isInWaterOrRain(entity)) || FastNMS.INSTANCE.method$ItemStack$nextDamageWillBreak(stack)) {
            return false;
        }
        FastNMS.INSTANCE.method$ItemStack$setDamageValue(copyStack, FastNMS.INSTANCE.method$ItemStack$getDamageValue(stack) + 1);

        if (spinStrength == 0.0F) {
            Object entitythrowntrident = FastNMS.INSTANCE.constructor$ThrownTrident(level, entity, stack);
            FastNMS.INSTANCE.method$ThrownTrident$shootFromRotation(
                    entitythrowntrident,
                    entity,
                    FastNMS.INSTANCE.method$Entity$getXRot(entity),
                    FastNMS.INSTANCE.method$Entity$getYRot(entity),
                    0.0F, 2.5F, 1.0F
            );
            if (FastNMS.INSTANCE.field$Abilities$instabuild(FastNMS.INSTANCE.method$Player$getAbilities(entity))) {
                FastNMS.INSTANCE.field$AbstractArrow$pickup(entitythrowntrident, Reflections.instance$AbstractArrow$Pickup$CREATIVE_ONLY);
            }

            PlayerLaunchProjectileEvent event = new PlayerLaunchProjectileEvent(
                    (Player) FastNMS.INSTANCE.method$Entity$getBukkitEntity(entity),
                    FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(stack),
                    (Projectile) FastNMS.INSTANCE.method$Entity$getBukkitEntity(entitythrowntrident)
            );
            if (!event.callEvent() || !FastNMS.INSTANCE.method$LevelWriter$addFreshEntity(level, entitythrowntrident)) {
                Entity bukkitEntity = FastNMS.INSTANCE.method$Entity$getBukkitEntity(entity);
                if (bukkitEntity instanceof Player player) {
                    player.updateInventory();
                }

                return false;
            }

            if (event.shouldConsume()) {
                FastNMS.INSTANCE.method$ItemStack$hurtAndBreak(
                        stack, 1, entity,
                        (player1) -> FastNMS.INSTANCE.method$LivingEntity$broadcastBreakEvent(player1, FastNMS.INSTANCE.method$LivingEntity$getUsedItemHand(entity))
                );
            }

            FastNMS.INSTANCE.field$AbstractArrow$pickupItemStack(entitythrowntrident, copyStack);
            FastNMS.INSTANCE.method$Level$playSound(
                    level,
                    null,
                    entitythrowntrident,
                    Reflections.instance$SoundEvent$TRIDENT_THROW,
                    Reflections.instance$SoundSource$PLAYERS,
                    1.0F, 1.0F
            );
            if (event.shouldConsume() && !FastNMS.INSTANCE.field$Abilities$instabuild(FastNMS.INSTANCE.method$Player$getAbilities(entity))) {
                FastNMS.INSTANCE.method$Inventory$removeItem(FastNMS.INSTANCE.method$Player$getInventory(entity), stack);
            }
            return true;
        }

        float yaw = FastNMS.INSTANCE.method$Entity$getYRot(entity);
        float pitch = FastNMS.INSTANCE.method$Entity$getXRot(entity);
        float x = -MCUtils.sin(yaw * MCUtils.DEG_TO_RAD) * MCUtils.cos(pitch * MCUtils.DEG_TO_RAD);
        float y = -MCUtils.sin(pitch * MCUtils.DEG_TO_RAD);
        float z = MCUtils.cos(yaw * MCUtils.DEG_TO_RAD) * MCUtils.cos(pitch * MCUtils.DEG_TO_RAD);

        float length = MCUtils.sqrt(x * x + y * y + z * z);
        x = x / length * spinStrength;
        y = y / length * spinStrength;
        z = z / length * spinStrength;

        FastNMS.INSTANCE.method$CraftEventFactory$callPlayerRiptideEvent(entity, stack, x, y, z);
        FastNMS.INSTANCE.method$Entity$push(entity, x, y, z);
        FastNMS.INSTANCE.field$Entity$hurtMarked(entity, true);
        FastNMS.INSTANCE.method$ItemStack$setDamageValue(stack, FastNMS.INSTANCE.method$ItemStack$getDamageValue(stack) + 1);
        FastNMS.INSTANCE.method$Player$startAutoSpinAttack(entity, 20, -1.0F, null);

        if (FastNMS.INSTANCE.method$Entity$onGround(entity)) {
            FastNMS.INSTANCE.method$Entity$move(entity, Reflections.instance$MoverType$SELF, FastNMS.INSTANCE.constructor$Vec3(0.0D, 1.1999999D, 0.0D));
        }

        Object soundeffect;
        if (spinStrength >= 3) {
            soundeffect = Reflections.instance$SoundEvent$TRIDENT_RIPTIDE_3;
        } else if (spinStrength == 2) {
            soundeffect = Reflections.instance$SoundEvent$TRIDENT_RIPTIDE_2;
        } else {
            soundeffect = Reflections.instance$SoundEvent$TRIDENT_RIPTIDE_1;
        }

        FastNMS.INSTANCE.method$Level$playSound(
                level,
                null,
                entity,
                soundeffect,
                Reflections.instance$SoundSource$PLAYERS,
                1.0F, 1.0F
        );
        return true;
    }

    private static boolean releaseUsing_1_20(Object stack, Object level, Object entity) {
        Object copyStack = FastNMS.INSTANCE.method$ItemStack$copy(stack);
        if (FastNMS.INSTANCE.method$ItemStack$isEmpty(copyStack)) return false;

        float spinStrength = FastNMS.INSTANCE.method$EnchantmentHelper$getTridentSpinAttackStrength(stack, entity);

        if ((spinStrength > 0.0F && !FastNMS.INSTANCE.method$Entity$isInWaterOrRain(entity)) || FastNMS.INSTANCE.method$ItemStack$nextDamageWillBreak(stack)) {
            return false;
        }
        FastNMS.INSTANCE.method$ItemStack$setDamageValue(copyStack, FastNMS.INSTANCE.method$ItemStack$getDamageValue(stack) + 1);

        if (spinStrength == 0.0F) {
            Object entitythrowntrident = FastNMS.INSTANCE.constructor$ThrownTrident(level, entity, stack);
            FastNMS.INSTANCE.method$ThrownTrident$shootFromRotation(
                    entitythrowntrident,
                    entity,
                    FastNMS.INSTANCE.method$Entity$getXRot(entity),
                    FastNMS.INSTANCE.method$Entity$getYRot(entity),
                    0.0F, 2.5F, 1.0F
            );
            if (FastNMS.INSTANCE.field$Abilities$instabuild(FastNMS.INSTANCE.method$Player$getAbilities(entity))) {
                FastNMS.INSTANCE.field$AbstractArrow$pickup(entitythrowntrident, Reflections.instance$AbstractArrow$Pickup$CREATIVE_ONLY);
            }

            PlayerLaunchProjectileEvent event = new PlayerLaunchProjectileEvent(
                    (Player) FastNMS.INSTANCE.method$Entity$getBukkitEntity(entity),
                    FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(stack),
                    (Projectile) FastNMS.INSTANCE.method$Entity$getBukkitEntity(entitythrowntrident)
            );
            if (!event.callEvent() || !FastNMS.INSTANCE.method$LevelWriter$addFreshEntity(level, entitythrowntrident)) {
                Entity bukkitEntity = FastNMS.INSTANCE.method$Entity$getBukkitEntity(entity);
                if (bukkitEntity instanceof Player player) {
                    player.updateInventory();
                }

                return false;
            }

            if (event.shouldConsume()) {
                FastNMS.INSTANCE.method$ItemStack$hurtAndBreak(
                        stack, 1, entity,
                        (player1) -> FastNMS.INSTANCE.method$LivingEntity$broadcastBreakEvent(player1, FastNMS.INSTANCE.method$LivingEntity$getUsedItemHand(entity))
                );
            }

            FastNMS.INSTANCE.field$ThrownTrident$tridentItem(entitythrowntrident, copyStack);
            FastNMS.INSTANCE.method$Level$playSound(
                    level,
                    null,
                    entitythrowntrident,
                    Reflections.instance$SoundEvent$TRIDENT_THROW,
                    Reflections.instance$SoundSource$PLAYERS,
                    1.0F, 1.0F
            );
            if (event.shouldConsume() && !FastNMS.INSTANCE.field$Abilities$instabuild(FastNMS.INSTANCE.method$Player$getAbilities(entity))) {
                FastNMS.INSTANCE.method$Inventory$removeItem(FastNMS.INSTANCE.method$Player$getInventory(entity), stack);
            }
            return true;
        }

        float yaw = FastNMS.INSTANCE.method$Entity$getYRot(entity);
        float pitch = FastNMS.INSTANCE.method$Entity$getXRot(entity);
        float x = -MCUtils.sin(yaw * MCUtils.DEG_TO_RAD) * MCUtils.cos(pitch * MCUtils.DEG_TO_RAD);
        float y = -MCUtils.sin(pitch * MCUtils.DEG_TO_RAD);
        float z = MCUtils.cos(yaw * MCUtils.DEG_TO_RAD) * MCUtils.cos(pitch * MCUtils.DEG_TO_RAD);

        float length = MCUtils.sqrt(x * x + y * y + z * z);
        x = x / length * spinStrength;
        y = y / length * spinStrength;
        z = z / length * spinStrength;

        FastNMS.INSTANCE.method$Entity$push(entity, x, y, z);
        FastNMS.INSTANCE.field$Entity$hurtMarked(entity, true);
        FastNMS.INSTANCE.method$ItemStack$setDamageValue(stack, FastNMS.INSTANCE.method$ItemStack$getDamageValue(stack) + 1);
        FastNMS.INSTANCE.method$Player$startAutoSpinAttack(entity, 20, -1.0F, null);

        if (FastNMS.INSTANCE.method$Entity$onGround(entity)) {
            FastNMS.INSTANCE.method$Entity$move(entity, Reflections.instance$MoverType$SELF, FastNMS.INSTANCE.constructor$Vec3(0.0D, 1.1999999D, 0.0D));
        }

        Object soundeffect;
        if (spinStrength >= 3) {
            soundeffect = Reflections.instance$SoundEvent$TRIDENT_RIPTIDE_3;
        } else if (spinStrength == 2) {
            soundeffect = Reflections.instance$SoundEvent$TRIDENT_RIPTIDE_2;
        } else {
            soundeffect = Reflections.instance$SoundEvent$TRIDENT_RIPTIDE_1;
        }

        FastNMS.INSTANCE.method$Level$playSound(
                level,
                null,
                entity,
                soundeffect,
                Reflections.instance$SoundSource$PLAYERS,
                1.0F, 1.0F
        );
        return true;
    }

}
