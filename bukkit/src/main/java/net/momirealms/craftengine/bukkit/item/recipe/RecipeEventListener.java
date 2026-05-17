package net.momirealms.craftengine.bukkit.item.recipe;

import com.destroystokyo.paper.event.inventory.PrepareResultEvent;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.item.BukkitItem;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.item.DataComponentTypes;
import net.momirealms.craftengine.bukkit.nms.Clearable;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.item.*;
import net.momirealms.craftengine.core.item.equipment.TrimBasedEquipment;
import net.momirealms.craftengine.core.item.recipe.*;
import net.momirealms.craftengine.core.item.recipe.Recipe;
import net.momirealms.craftengine.core.item.recipe.input.CraftingInput;
import net.momirealms.craftengine.core.item.recipe.input.SingleItemInput;
import net.momirealms.craftengine.core.item.recipe.input.SmithingInput;
import net.momirealms.craftengine.core.item.setting.value.AnvilRepairItem;
import net.momirealms.craftengine.core.item.setting.value.ItemEquipment;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.CraftWorldProxy;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.inventory.CraftComplexRecipeProxy;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.inventory.CraftInventoryAnvilProxy;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.inventory.CraftInventoryProxy;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.inventory.CraftInventoryViewProxy;
import net.momirealms.craftengine.proxy.minecraft.network.chat.ComponentProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.ResourceKeyProxy;
import net.momirealms.craftengine.proxy.minecraft.world.ContainerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.player.PlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.inventory.AbstractContainerMenuProxy;
import net.momirealms.craftengine.proxy.minecraft.world.inventory.CraftingContainerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.inventory.SlotProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.crafting.*;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlocksProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.entity.AbstractFurnaceBlockEntityProxy;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("DuplicatedCode")
public final class RecipeEventListener implements Listener {
    private final ItemManager itemManager;
    private final BukkitRecipeManager recipeManager;
    private final BukkitCraftEngine plugin;

    public RecipeEventListener(BukkitCraftEngine plugin, BukkitRecipeManager recipeManager, ItemManager itemManager) {
        this.itemManager = itemManager;
        this.recipeManager = recipeManager;
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    @EventHandler(ignoreCancelled = true)
    public void onClickInventoryWithFuel(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory instanceof FurnaceInventory furnaceInventory)) return;
        ItemStack fuelStack = furnaceInventory.getFuel();
        Inventory clickedInventory = event.getClickedInventory();

        Player player = (Player) event.getWhoClicked();
        if (clickedInventory == player.getInventory()) {
            if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                ItemStack item = event.getCurrentItem();
                if (ItemStackUtils.isEmpty(item)) return;
                if (ItemStackUtils.isEmpty(fuelStack)) {
                    SingleItemInput input = new SingleItemInput(ItemStackUtils.getUniqueIdItem(item));
                    RecipeType recipeType;
                    if (furnaceInventory.getType() == InventoryType.FURNACE) {
                        recipeType = RecipeType.SMELTING;
                    } else if (furnaceInventory.getType() == InventoryType.BLAST_FURNACE) {
                        recipeType = RecipeType.BLASTING;
                    } else {
                        recipeType = RecipeType.SMOKING;
                    }

                    Recipe ceRecipe = this.recipeManager.recipeByInput(recipeType, input);
                    // The item is an ingredient, we should never consider it as fuel firstly
                    if (ceRecipe != null) return;

                    int fuelTime = getFuelTime(BukkitAdaptor.adapt(item).id());
                    if (fuelTime == 0) {
                        if (ItemStackUtils.isCustomItem(item) && item.getType().isFuel()) {
                            event.setCancelled(true);
                            ItemStack smelting = furnaceInventory.getSmelting();
                            if (ItemStackUtils.isEmpty(smelting)) {
                                furnaceInventory.setSmelting(item.clone());
                                item.setAmount(0);
                            } else if (smelting.isSimilar(item)) {
                                int maxStackSize = smelting.getMaxStackSize();
                                int canGiveMaxCount = item.getAmount();
                                if (maxStackSize > smelting.getAmount()) {
                                    if (canGiveMaxCount + smelting.getAmount() >= maxStackSize) {
                                        int givenCount = maxStackSize - smelting.getAmount();
                                        smelting.setAmount(maxStackSize);
                                        item.setAmount(item.getAmount() - givenCount);
                                    } else {
                                        smelting.setAmount(smelting.getAmount() + canGiveMaxCount);
                                        item.setAmount(0);
                                    }
                                }
                            }
                            player.updateInventory();
                        }
                        return;
                    }
                    event.setCancelled(true);
                    furnaceInventory.setFuel(item.clone());
                    item.setAmount(0);
                    player.updateInventory();
                } else {
                    if (fuelStack.isSimilar(item)) {
                        event.setCancelled(true);
                        int maxStackSize = fuelStack.getMaxStackSize();
                        int canGiveMaxCount = item.getAmount();
                        if (maxStackSize > fuelStack.getAmount()) {
                            if (canGiveMaxCount + fuelStack.getAmount() >= maxStackSize) {
                                int givenCount = maxStackSize - fuelStack.getAmount();
                                fuelStack.setAmount(maxStackSize);
                                item.setAmount(item.getAmount() - givenCount);
                            } else {
                                fuelStack.setAmount(fuelStack.getAmount() + canGiveMaxCount);
                                item.setAmount(0);
                            }
                            player.updateInventory();
                        }
                    }
                }
            }
        } else {
            // click the furnace inventory
            int slot = event.getSlot();
            // click the fuel slot
            if (slot != 1) {
                return;
            }
            ClickType clickType = event.getClick();
            switch (clickType) {
                case SWAP_OFFHAND, NUMBER_KEY -> {
                    ItemStack item;
                    int hotBarSlot = event.getHotbarButton();
                    if (clickType == ClickType.SWAP_OFFHAND) {
                        item = player.getInventory().getItemInOffHand();
                    } else {
                        item = player.getInventory().getItem(hotBarSlot);
                    }
                    if (ItemStackUtils.isEmpty(item)) return;
                    int fuelTime = getFuelTime(BukkitAdaptor.adapt(item).id());
                    // only handle custom items
                    if (fuelTime == 0) {
                        if (ItemStackUtils.isCustomItem(item) && item.getType().isFuel()) {
                            event.setCancelled(true);
                        }
                        return;
                    }

                    event.setCancelled(true);
                    if (fuelStack == null || fuelStack.getType() == Material.AIR) {
                        furnaceInventory.setFuel(item.clone());
                        item.setAmount(0);
                    } else {
                        if (clickType == ClickType.SWAP_OFFHAND) {
                            player.getInventory().setItemInOffHand(fuelStack);
                        } else {
                            player.getInventory().setItem(hotBarSlot, fuelStack);
                        }
                        furnaceInventory.setFuel(item.clone());
                    }
                    player.updateInventory();
                }
                case LEFT, RIGHT -> {
                    ItemStack itemOnCursor = event.getCursor();
                    // pick item
                    if (ItemStackUtils.isEmpty(itemOnCursor)) return;
                    int fuelTime = getFuelTime(BukkitAdaptor.adapt(itemOnCursor).id());
                    // only handle custom items
                    if (fuelTime == 0) {
                        if (ItemStackUtils.isCustomItem(itemOnCursor) && itemOnCursor.getType().isFuel()) {
                            event.setCancelled(true);
                        }
                        return;
                    }

                    event.setCancelled(true);
                    // The slot is empty
                    if (fuelStack == null || fuelStack.getType() == Material.AIR) {
                        if (clickType == ClickType.LEFT) {
                            furnaceInventory.setFuel(itemOnCursor.clone());
                            itemOnCursor.setAmount(0);
                            player.updateInventory();
                        } else {
                            ItemStack cloned = itemOnCursor.clone();
                            cloned.setAmount(1);
                            furnaceInventory.setFuel(cloned);
                            itemOnCursor.setAmount(itemOnCursor.getAmount() - 1);
                            player.updateInventory();
                        }
                    } else {
                        boolean isSimilar = itemOnCursor.isSimilar(fuelStack);
                        if (clickType == ClickType.LEFT) {
                            if (isSimilar) {
                                int maxStackSize = fuelStack.getMaxStackSize();
                                int canGiveMaxCount = itemOnCursor.getAmount();
                                if (maxStackSize > fuelStack.getAmount()) {
                                    if (canGiveMaxCount + fuelStack.getAmount() >= maxStackSize) {
                                        int givenCount = maxStackSize - fuelStack.getAmount();
                                        fuelStack.setAmount(maxStackSize);
                                        itemOnCursor.setAmount(itemOnCursor.getAmount() - givenCount);
                                    } else {
                                        fuelStack.setAmount(fuelStack.getAmount() + canGiveMaxCount);
                                        itemOnCursor.setAmount(0);
                                    }
                                    player.updateInventory();
                                }
                            } else {
                                // swap item
                                event.setCursor(fuelStack);
                                furnaceInventory.setFuel(itemOnCursor.clone());
                                player.updateInventory();
                            }
                        } else {
                            if (isSimilar) {
                                int maxStackSize = fuelStack.getMaxStackSize();
                                if (maxStackSize > fuelStack.getAmount()) {
                                    fuelStack.setAmount(fuelStack.getAmount() + 1);
                                    itemOnCursor.setAmount(itemOnCursor.getAmount() - 1);
                                    player.updateInventory();
                                }
                            } else {
                                // swap item
                                event.setCursor(fuelStack);
                                furnaceInventory.setFuel(itemOnCursor.clone());
                                player.updateInventory();
                            }
                        }
                    }
                }
            }
        }
    }

    private int getFuelTime(Key id) {
        return this.itemManager.getItemDefinition(id).map(it -> it.settings().fuelTime()).orElse(0);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        ItemStack fuel = event.getFuel();
        BukkitItem item = BukkitAdaptor.adapt(fuel);
        Optional<ItemDefinition> optionalDefinition = item.getDefinition();
        if (optionalDefinition.isPresent()) {
            ItemDefinition itemDefinition = optionalDefinition.get();
            int fuelTime = itemDefinition.settings().fuelTime();
            if (fuelTime != 0) {
                // 自定义燃烧时间
                event.setBurnTime(fuelTime);
                Key remainder = itemDefinition.settings().fuelRemainder();
                if (remainder != null && item.count() == 1) {
                    Block block = event.getBlock();
                    Object blockPos = LocationUtils.toBlockPos(block.getX(), block.getY(), block.getZ());
                    // 先获取方块实体
                    Object blockEntity = LevelProxy.INSTANCE.getBlockEntity(CraftWorldProxy.INSTANCE.getWorld(block.getWorld()), blockPos);
                    if (AbstractFurnaceBlockEntityProxy.CLASS.isInstance(blockEntity)) {
                        int litTimeRemaining = AbstractFurnaceBlockEntityProxy.INSTANCE.getLitTimeRemaining(blockEntity);
                        // 准备开始燃烧
                        if (litTimeRemaining <= 0) {
                            Item remainderItem = this.itemManager.createWrappedItem(remainder, null);
                            if (remainderItem == null) return;
                            List<Object> items = AbstractFurnaceBlockEntityProxy.INSTANCE.getItems(blockEntity);
                            event.setConsumeFuel(false);
                            item.shrink(1);
                            items.set(1, remainderItem.minecraftItem());
                        }
                    }
                }
            }
        }
    }

    // 当把物品放入熔炉时, 在熔炉实体的PDC内记录玩家的 UUID.
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onClickInventoryWithFurnaceInput(InventoryClickEvent event) {
        if (!Config.recipeInjectBlockEntities()) return; // 功能未开启.
        Inventory inventory = event.getInventory();
        if (!(inventory instanceof FurnaceInventory furnaceInventory)) return;
        InventoryHolder inventoryHolder = furnaceInventory.getHolder(false);
        if (!(inventoryHolder instanceof Furnace furnace)) return;
        Inventory clickedInventory = event.getClickedInventory();

        ItemStack smeltStack = furnaceInventory.getSmelting();
        Player player = (Player) event.getWhoClicked();
        boolean shouldRecord = false;

        // 如果玩家操作的自己背包, 用shift+左右键放入物品
        if (clickedInventory == player.getInventory()) {
            if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                BukkitItem item = ItemStackUtils.wrap(event.getCurrentItem());
                if (item.isEmpty()) return;

                // 如果输入槽位是空的, 则检查交互的物品是否拥有熔炉配方.
                if (ItemStackUtils.isEmpty(smeltStack)) {
                    RecipeType recipeType = this.getRecipeTypeByCookingInventoryHolder(inventoryHolder);
                    Recipe recipe = BukkitRecipeManager.instance().recipeByInput(recipeType, new SingleItemInput(UniqueIdItem.of(item)));
                    shouldRecord = recipe != null;
                }
                // 如果槽位不是空的, 则检查物品是否和已经存在的物品一致.
                else {
                    shouldRecord = smeltStack.isSimilar(item.getBukkitItem()) && smeltStack.getAmount() < smeltStack.getMaxStackSize();
                }
            }
        }
        // 如果玩家直接操作熔炉输入槽
        else if (event.getSlot() == 0) {
            ClickType clickType = event.getClick();
            shouldRecord = switch (clickType) {
                // 如果操作的是 F 或者 快捷栏, 则检查对应槽位是否有物品, 有物品就代表肯定放入成功了.
                case SWAP_OFFHAND, NUMBER_KEY -> {
                    ItemStack item = clickType == ClickType.SWAP_OFFHAND
                            ? player.getInventory().getItemInOffHand()
                            : player.getInventory().getItem(event.getHotbarButton());
                    yield !ItemStackUtils.isEmpty(smeltStack) || !ItemStackUtils.isEmpty(item);
                }
                // 如果操作的是左右键, 则检查目标槽位和光标是否至少有一个位置有物品, 有就代表有变动.
                case LEFT, RIGHT -> !ItemStackUtils.isEmpty(event.getCursor()) || !ItemStackUtils.isEmpty(smeltStack);
                default -> false;
            };
        }

        // 记录玩家的 UUID 到熔炉的 PDC 上.
        if (shouldRecord) {
            UUID uniqueId = player.getUniqueId();
            // 清理 QuickCache 的缓存.
            Chunk chunk = furnace.getBlock().getChunk();
            Object chunkAccess = WorldUtils.getMinecraftChunk(chunk);
            Object blockEntity = BlockGetterProxy.INSTANCE.getBlockEntity(chunkAccess, LocationUtils.toBlockPos(furnace.getX(), furnace.getY(), furnace.getZ()));
            if (AbstractFurnaceBlockEntityProxy.CLASS.isInstance(blockEntity)) {
                Object quickCheck = AbstractFurnaceBlockEntityProxy.INSTANCE.getQuickCheck(blockEntity);
                if (quickCheck instanceof Clearable clearable) {
                    clearable.clear();
                } else {
                    BukkitRecipeManager.injectFurnaceBlockEntity(blockEntity);
                }
            }
            // 检查旧的数据是否和当前要写入的一致, 一致就不写入了.
            long[] uuidLongs = furnace.getPersistentDataContainer().get(BukkitRecipeManager.FURNACE_LAST_USER, PersistentDataType.LONG_ARRAY);
            if (uuidLongs != null && new UUID(uuidLongs[0], uuidLongs[1]).equals(uniqueId)) {
                return;
            }
            // 写入 UUID.
            furnace.getPersistentDataContainer().set(BukkitRecipeManager.FURNACE_LAST_USER, PersistentDataType.LONG_ARRAY,
                    new long[]{uniqueId.getMostSignificantBits(), uniqueId.getLeastSignificantBits()}
            );
        }
    }

    // 当玩家往篝火上放入物品时, 检查配方条件.
    @EventHandler(ignoreCancelled = true)
    public void onPrepareCampfireRecipe(PlayerInteractEvent event) {
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;
        EquipmentSlot equipmentSlot = event.getHand();
        if (equipmentSlot == null) return;
        ItemStack itemInHand = event.getPlayer().getInventory().getItem(equipmentSlot);
        if (ItemStackUtils.isEmpty(itemInHand)) return;
        Object blockOwner = BlockStateUtils.getBlockOwner(BlockStateUtils.getBlockState(clickedBlock));
        if (blockOwner != BlocksProxy.CAMPFIRE && blockOwner != BlocksProxy.SOUL_CAMPFIRE) return;
        // 获取营火
        if (clickedBlock.getState() instanceof Campfire campfire) {
            // 检查营火是否已满
            boolean isFull = true;
            for (int i = 0; i < campfire.getSize(); i++) {
                ItemStack item = campfire.getItem(i);
                if (item == null) {
                    isFull = false;
                    break;
                }
            }
            if (isFull) return;
            // 获取配方
            SingleItemInput itemInput = new SingleItemInput(UniqueIdItem.of(ItemStackUtils.wrap(itemInHand)));
            ConditionalRecipe recipe = (ConditionalRecipe) BukkitRecipeManager.instance().recipeByInput(RecipeType.CAMPFIRE_COOKING, itemInput);
            if (recipe != null && recipe.hasCondition()) {
                boolean result = recipe.canUse(PlayerOptionalContext.of(BukkitAdaptor.adapt(event.getPlayer())));
                if (!result) {
                    event.setCancelled(true);
                }
            }
        }
    }

    // Paper only
    @EventHandler
    public void onPrepareResult(PrepareResultEvent event) {
        if (event.getInventory() instanceof CartographyInventory cartographyInventory) {
            if (ItemStackUtils.hasCustomItem(cartographyInventory.getStorageContents())) {
                event.setResult(new ItemStack(Material.AIR));
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onAnvilEvent(PrepareAnvilEvent event) {
        preProcess(event);
        processRepairable(event);
        processRename(event);
    }

    /*
    预处理会阻止一些不合理的原版材质造成的合并问题
     */
    private void preProcess(PrepareAnvilEvent event) {
        if (event.getResult() == null) return;
        AnvilInventory inventory = event.getInventory();
        ItemStack first = inventory.getFirstItem();
        ItemStack second = inventory.getSecondItem();
        if (first == null || second == null) return;
        Item wrappedFirst = BukkitItemManager.instance().wrap(first);
        Optional<ItemDefinition> firstCustom = wrappedFirst.getDefinition();
        Item wrappedSecond = BukkitItemManager.instance().wrap(second);
        Optional<ItemDefinition> secondCustom = wrappedFirst.getDefinition();
        // 两个都是原版物品
        if (firstCustom.isEmpty() && secondCustom.isEmpty()) {
            return;
        }
        // 如果第二个物品是附魔书，那么忽略
        if (wrappedSecond.vanillaId().equals(ItemKeys.ENCHANTED_BOOK)) {
            // 禁止不可附魔的物品被附魔书附魔
            if (firstCustom.isPresent() && !firstCustom.get().settings().canEnchant()) {
                event.setResult(null);
            }
            return;
        }

        // 被修的是自定义，材料不是自定义
        if (firstCustom.isPresent() && secondCustom.isEmpty()) {
            if (firstCustom.get().settings().respectRepairableComponent()) {
                if (second.canRepair(first)) return; // 尊重原版的repairable
            } else {
                event.setResult(null);
                return;
            }
        }

        // 被修的是原版，材料是自定义
        if (firstCustom.isEmpty() && secondCustom.isPresent()) {
            if (secondCustom.get().settings().respectRepairableComponent()) {
                if (second.canRepair(first)) return;
            } else {
                event.setResult(null);
                return;
            }
        }

        // 如果两个物品id不同，不能合并
        if (!wrappedFirst.customId().equals(wrappedSecond.customId())) {
            event.setResult(null);
            return;
        }

        if (firstCustom.isPresent()) {
            ItemDefinition firstItemDefinition = firstCustom.get();
            if (firstItemDefinition.settings().repairable().anvilCombine() == Tristate.FALSE) {
                event.setResult(null);
                return;
            }

            Item wrappedResult = BukkitItemManager.instance().wrap(event.getResult());
            if (!firstItemDefinition.settings().canEnchant()) {
                Object previousEnchantment = wrappedFirst.getExactComponent(DataComponentTypes.ENCHANTMENTS);
                if (previousEnchantment != null) {
                    wrappedResult.setExactComponent(DataComponentTypes.ENCHANTMENTS, previousEnchantment);
                } else {
                    wrappedResult.resetComponent(DataComponentTypes.ENCHANTMENTS);
                }
            }
        }
    }

    /*
    处理item settings中repair item属性。如果修补材料不是自定义物品，则不会参与后续逻辑。
    这会忽略preprocess里event.setResult(null);
     */
    @SuppressWarnings("UnstableApiUsage")
    private void processRepairable(PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory();
        ItemStack first = inventory.getFirstItem();
        ItemStack second = inventory.getSecondItem();
        if (ItemStackUtils.isEmpty(first) || ItemStackUtils.isEmpty(second)) return;

        Item wrappedSecond = BukkitItemManager.instance().wrap(second);
        // 如果材料不是自定义的，那么忽略
        Optional<ItemDefinition> customItemOptional = this.plugin.itemManager().getItemDefinition(wrappedSecond.id());
        if (customItemOptional.isEmpty()) {
            return;
        }

        ItemDefinition itemDefinition = customItemOptional.get();
        List<AnvilRepairItem> repairItems = itemDefinition.settings().repairItems();
        // 如果材料不支持修复物品，则忽略
        if (repairItems.isEmpty()) {
            return;
        }

        // 后续均为修复逻辑
        Item wrappedFirst = BukkitItemManager.instance().wrap(first.clone());
        int maxDamage = wrappedFirst.maxDamage();
        int damage = wrappedFirst.damage().orElse(0);
        // 物品无damage属性
        if (damage == 0 || maxDamage == 0) return;

        Key firstId = wrappedFirst.id();
        Optional<ItemDefinition> optionalCustomTool = wrappedFirst.getDefinition();
        // 物品无法被修复
        if (optionalCustomTool.isPresent() && optionalCustomTool.get().settings().repairable().anvilRepair() == Tristate.FALSE) {
            return;
        }

        AnvilRepairItem repairItem = null;
        for (AnvilRepairItem item : repairItems) {
            for (String target : item.targets()) {
                if (target.charAt(0) == '#') {
                    Key tag = Key.of(target.substring(1));
                    if (optionalCustomTool.isPresent() && optionalCustomTool.get().is(tag)) {
                        repairItem = item;
                        break;
                    }
                    if (wrappedFirst.hasItemTag(tag)) {
                        repairItem = item;
                        break;
                    }
                } else if (target.equals(firstId.toString())) {
                    repairItem = item;
                    break;
                }
            }
        }

        // 找不到匹配的修复
        if (repairItem == null) {
            return;
        }

        boolean hasResult = true;
        
        int realDurabilityPerItem = (int) (repairItem.amount() + repairItem.percent() * maxDamage);
        if (realDurabilityPerItem == 0) {
            return;
        }

        int consumeMaxAmount = damage / realDurabilityPerItem + 1;
        int actualConsumedAmount = Math.min(consumeMaxAmount, wrappedSecond.count());
        int actualRepairAmount = actualConsumedAmount * realDurabilityPerItem;
        int damageAfter = Math.max(damage - actualRepairAmount, 0);
        wrappedFirst.damage(damageAfter);

        String renameText;
        int maxRepairCost;
        //int previousCost;
        if (VersionHelper.isOrAbove1_21) {
            AnvilView anvilView = event.getView();
            renameText = anvilView.getRenameText();
            maxRepairCost = anvilView.getMaximumRepairCost();
            //previousCost = anvilView.getRepairCost();
        } else {
            renameText = LegacyInventoryUtils.getRenameText(inventory);
            maxRepairCost = LegacyInventoryUtils.getMaxRepairCost(inventory);
            //previousCost = LegacyInventoryUtils.getRepairCost(inventory);
        }

        int repairCost = actualConsumedAmount;
        int repairPenalty = wrappedFirst.repairCost().orElse(0) + wrappedSecond.repairCost().orElse(0);

        if (renameText != null && !renameText.isBlank()) {
            if (!renameText.equals(ComponentProxy.INSTANCE.getString(ComponentUtils.jsonToMinecraft(wrappedFirst.hoverNameJson().orElse(AdventureHelper.EMPTY_COMPONENT))))) {
                wrappedFirst.customNameJson(AdventureHelper.componentToJson(Component.text(renameText)));
                repairCost += 1;
            } else if (repairCost == 0) {
                hasResult = false;
            }
        } else if (VersionHelper.isOrAbove1_20_5 && wrappedFirst.hasComponent(DataComponentTypes.CUSTOM_NAME)) {
            repairCost += 1;
            wrappedFirst.customNameJson(null);
        } else if (!VersionHelper.isOrAbove1_20_5 && wrappedFirst.hasTag("display", "Name")) {
            repairCost += 1;
            wrappedFirst.customNameJson(null);
        }

        int finalCost = repairCost + repairPenalty;

        // To fix some client side visual issues
        Object anvilMenu;
        if (VersionHelper.isOrAbove1_21) {
            anvilMenu = CraftInventoryViewProxy.INSTANCE.getContainer(event.getView());
        } else {
            anvilMenu = CraftInventoryAnvilProxy.INSTANCE.getContainer(inventory);
        }
        AbstractContainerMenuProxy.INSTANCE.broadcastFullState(anvilMenu);

        if (VersionHelper.isOrAbove1_21) {
            AnvilView anvilView = event.getView();
            anvilView.setRepairCost(finalCost);
            anvilView.setRepairItemCountCost(actualConsumedAmount);
        } else {
            LegacyInventoryUtils.setRepairCost(inventory, finalCost);
            LegacyInventoryUtils.setRepairCostAmount(inventory, actualConsumedAmount);
        }

        Player player = InventoryUtils.getPlayerFromInventoryEvent(event);

        BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
        if (serverPlayer == null) return;
        if (finalCost >= maxRepairCost && !serverPlayer.canInstabuild()) {
            hasResult = false;
        }

        if (hasResult) {
            int afterPenalty = wrappedFirst.repairCost().orElse(0);
            int anotherPenalty = wrappedSecond.repairCost().orElse(0);
            if (afterPenalty < anotherPenalty) {
                afterPenalty = anotherPenalty;
            }
            afterPenalty = calculateIncreasedRepairCost(afterPenalty);
            wrappedFirst.repairCost(afterPenalty);
            event.setResult(ItemStackUtils.getBukkitStack(wrappedFirst));
        }
    }

    /*
    如果物品不可被重命名，则在最后处理。
     */
    @SuppressWarnings("UnstableApiUsage")
    private void processRename(PrepareAnvilEvent event) {
        if (event.getResult() == null) return;
        AnvilInventory inventory = event.getInventory();
        ItemStack first = inventory.getFirstItem();
        if (ItemStackUtils.isEmpty(first)) {
            return;
        }
        Item wrappedFirst = BukkitItemManager.instance().wrap(first);
        wrappedFirst.getDefinition().ifPresent(item -> {
            if (!item.settings().renameable()) {
                String renameText;
                if (VersionHelper.isOrAbove1_21) {
                    AnvilView anvilView = event.getView();
                    renameText = anvilView.getRenameText();
                } else {
                    renameText = LegacyInventoryUtils.getRenameText(inventory);
                }
                if (renameText != null && !renameText.isBlank()) {
                    if (!renameText.equals(ComponentProxy.INSTANCE.getString(ComponentUtils.jsonToMinecraft(wrappedFirst.hoverNameJson().orElse(AdventureHelper.EMPTY_COMPONENT))))) {
                        event.setResult(null);
                    }
                }
            }
        });
    }

    public static int calculateIncreasedRepairCost(int cost) {
        return (int) Math.min((long) cost * 2L + 1L, 2147483647L);
    }

    // only handle repair items for the moment
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onSpecialRecipe(PrepareItemCraftEvent event) {
        org.bukkit.inventory.Recipe recipe = event.getRecipe();
        if (!(recipe instanceof ComplexRecipe complexRecipe))
            return;
        CraftingInventory inventory = event.getInventory();
        ItemStack result = inventory.getResult();
        if (ItemStackUtils.isEmpty(result))
            return;
        boolean hasCustomItem = ItemStackUtils.hasCustomItem(inventory.getMatrix());
        if (!hasCustomItem)
            return;
        if (!CraftComplexRecipeProxy.CLASS.isInstance(complexRecipe)) {
            return;
        }
        try {
            Object mcRecipe = CraftComplexRecipeProxy.INSTANCE.getRecipe(complexRecipe);
            if (FireworkStarFadeRecipeProxy.CLASS.isInstance(mcRecipe)) {
                return;
            }

            if (VersionHelper.isOrAbove26_1) {
                // 在26.1以后dye不再应为special recipe
                if (DyeRecipeProxy.CLASS.isInstance(mcRecipe)) {
                    return;
                }
            } else {
                if (ArmorDyeRecipeProxy.CLASS.isInstance(mcRecipe)) {
                    return;
                }
            }

            // 处理修复配方，在此处理才能使用玩家参数构建物品
            if (RepairItemRecipeProxy.CLASS.isInstance(mcRecipe)) {
                Pair<ItemStack, ItemStack> theOnlyTwoItem = getTheOnlyTwoItem(inventory.getMatrix());
                if (theOnlyTwoItem == null) return;
                Item first = BukkitItemManager.instance().wrap(theOnlyTwoItem.left());
                Item right = BukkitItemManager.instance().wrap(theOnlyTwoItem.right());
                int max = Math.max(first.maxDamage(), right.maxDamage());
                int durability1 = first.maxDamage() - first.damage().orElse(0);
                int durability2 = right.maxDamage() - right.damage().orElse(0);
                int finalDurability = durability1 + durability2 + max * 5 / 100;
                Optional<ItemDefinition> customItemOptional = plugin.itemManager().getItemDefinition(first.id());
                if (customItemOptional.isEmpty()) {
                    inventory.setResult(null);
                    return;
                }
                Player player = InventoryUtils.getPlayerFromInventoryEvent(event);
                Item newItem = customItemOptional.get().buildItem(BukkitAdaptor.adapt(player));
                newItem.maxDamage(max);
                newItem.damage(Math.max(max - finalDurability, 0));
                inventory.setResult(ItemStackUtils.getBukkitStack(newItem));
                return;
            }

            // 其他配方不允许使用自定义物品
            inventory.setResult(null);
        } catch (Exception e) {
            this.plugin.logger().warn("Failed to handle custom recipe", e);
        }
    }

    private Pair<ItemStack, ItemStack> getTheOnlyTwoItem(ItemStack[] matrix) {
        ItemStack first = null;
        ItemStack second = null;
        for (ItemStack itemStack : matrix) {
            if (itemStack == null) continue;
            if (first == null) {
                first = itemStack;
            } else {
                if (second != null) {
                    return null;
                }
                second = itemStack;
            }
        }
        return new Pair<>(first, second);
    }

    // 准备结果阶段
    @EventHandler(ignoreCancelled = true)
    public void onPrepareCraftingRecipe(PrepareItemCraftEvent event) {
        if (!Config.enableRecipeSystem()) return;
        CraftingInventory inventory = event.getInventory();
        Key recipeId = getCurrentCraftingRecipeId(inventory);
        if (recipeId == null) return;
        Optional<Recipe> optionalRecipe = this.recipeManager.recipeById(recipeId);
        // 也许是其他插件注册的配方，直接无视
        if (optionalRecipe.isEmpty()) {
            return;
        }
        if (!(optionalRecipe.get() instanceof CustomCraftingTableRecipe craftingTableRecipe)) {
            inventory.setResult(null);
            return;
        }
        Player player = InventoryUtils.getPlayerFromInventoryEvent(event);
        BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
        if (craftingTableRecipe.hasCondition()) {
            if (!craftingTableRecipe.canUse(PlayerOptionalContext.of(serverPlayer))) {
                inventory.setResult(null);
                return;
            }
        }
        if (craftingTableRecipe.hasVisualResult() && VersionHelper.PREMIUM) {
            ItemBuildContext itemBuildContext = ItemBuildContext.of(serverPlayer);
            inventory.setResult(ItemStackUtils.getBukkitStack(craftingTableRecipe.assembleVisual(null, itemBuildContext)));
        } else {
            if (craftingTableRecipe.alwaysRebuildOutput()) {
                ItemBuildContext itemBuildContext = ItemBuildContext.of(serverPlayer);
                if (craftingTableRecipe instanceof CustomDyeRecipe dyeRecipe) {
                    inventory.setResult(ItemStackUtils.getBukkitStack(dyeRecipe.assemble(getCraftingInput(inventory), itemBuildContext)));
                } else {
                    inventory.setResult(ItemStackUtils.getBukkitStack(craftingTableRecipe.assemble(null, itemBuildContext)));
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onCraftingFinish(CraftItemEvent event) {
        if (!Config.enableRecipeSystem() || !VersionHelper.PREMIUM) return;
        CraftingInventory inventory = event.getInventory();
        ItemStack visualResultOrReal = inventory.getResult();
        // 可惜我们没有结果
        if (ItemStackUtils.isEmpty(visualResultOrReal)) return;
        Key recipeId = getCurrentCraftingRecipeId(inventory);
        if (recipeId == null) return;
        Optional<Recipe> optionalRecipe = this.recipeManager.recipeById(recipeId);
        // 也许是其他插件注册的配方，直接无视
        if (optionalRecipe.isEmpty() || !(optionalRecipe.get() instanceof CustomCraftingTableRecipe ceRecipe)) {
            return;
        }
        // 没有视觉结果和函数你凑什么热闹
        if (!ceRecipe.hasVisualResult() && !ceRecipe.hasFunctions()) {
            return;
        }
        InventoryAction action = event.getAction();
        // 无事发生，不要更新
        if (action == InventoryAction.NOTHING) {
            return;
        }

        Player player = InventoryUtils.getPlayerFromInventoryEvent(event);
        BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
        if (serverPlayer == null) return;

        // 对低版本nothing不全的兼容
        if (!VersionHelper.isOrAbove1_20_5 && LegacyInventoryUtils.isHotBarSwapAndReadd(action)) {
            int slot = event.getHotbarButton();
            if (slot == -1) {
                if (!serverPlayer.getItemInHand(InteractionHand.OFF_HAND).isEmpty()) {
                    return;
                }
            } else {
                ItemStack item = player.getInventory().getItem(slot);
                if (!ItemStackUtils.isEmpty(item)) {
                    return;
                }
            }
        }

        Object mcPlayer = serverPlayer.serverPlayer();
        Object craftingMenu = PlayerProxy.INSTANCE.getContainerMenu(mcPlayer);

        ClickType click = event.getClick();
        // 需要特殊处理的情况
        if (click == ClickType.CONTROL_DROP && !ceRecipe.hasVisualResult() && ceRecipe.hasFunctions()) {
            if (!ItemStackUtils.isEmpty(event.getCursor())) {
                return;
            }

            // 后续由插件处理
            event.setResult(Event.Result.DENY);

            Object resultSlot = AbstractContainerMenuProxy.INSTANCE.getSlot(craftingMenu, 0);

            for (;;) {
                // 这个时候配方已经更新了，如果变化了，那么就不要操作
                if (!recipeId.equals(getCurrentCraftingRecipeId(inventory))) {
                    break;
                }

                Object takenItem = SlotProxy.INSTANCE.safeTake(resultSlot, 1, Integer.MAX_VALUE, mcPlayer);
                if (ItemStackProxy.INSTANCE.isEmpty(takenItem)) {
                    break;
                }

                PlayerProxy.INSTANCE.drop(mcPlayer, takenItem, true);

                // 执行函数
                PlayerOptionalContext context = PlayerOptionalContext.of(serverPlayer);
                for (Function<Context> function : ceRecipe.functions()) {
                    function.run(context);
                }
            }
            return;
        }

        // 多次合成
        if (click.isShiftClick()) {
            // 由插件自己处理多次合成
            event.setResult(Event.Result.DENY);

            for (;;) {
                // 这个时候配方已经更新了，如果变化了，那么就不要操作
                if (!recipeId.equals(getCurrentCraftingRecipeId(inventory))) {
                    break;
                }

                // 配方不变，允许起飞
                // 如果有视觉结果，先临时替换为真实的
                if (ceRecipe.hasVisualResult()) {
                    inventory.setResult(ItemStackUtils.getBukkitStack(ceRecipe.assemble(null, ItemBuildContext.of(serverPlayer))));
                }

                // 连续获取
                Object itemMoved = AbstractContainerMenuProxy.INSTANCE.quickMoveStack(craftingMenu, mcPlayer, 0 /* result slot */);
                if (ItemStackProxy.INSTANCE.isEmpty(itemMoved)) {
                    // 发现取了个寂寞，根本没地方放，给他复原成视觉结果
                    inventory.setResult(visualResultOrReal);
                    break;
                }
                // 有函数的情况下，执行函数
                if (ceRecipe.hasFunctions()) {
                    PlayerOptionalContext context = PlayerOptionalContext.of(serverPlayer);
                    for (Function<Context> function : ceRecipe.functions()) {
                        function.run(context);
                    }
                }
            }
        }
        // 单次合成
        else {
            if (click == ClickType.MIDDLE) {
                if (ItemStackUtils.isEmpty(event.getCursor())) {
                    return;
                }
            }
            if (click == ClickType.DROP || click == ClickType.CONTROL_DROP) {
                if (!ItemStackUtils.isEmpty(event.getCursor())) {
                    return;
                }
            }
            // 有视觉结果的情况下，重新构造真实物品
            if (ceRecipe.hasVisualResult()) {
                // 指针物品不为空，且竟然和视觉物品一致，逆天，必须阻止
                if (click == ClickType.LEFT || click == ClickType.RIGHT) {
                    ItemStack cursor = event.getCursor();
                    if (!ItemStackUtils.isEmpty(cursor)) {
                        if (cursor.isSimilar(visualResultOrReal)) {
                            event.setResult(Event.Result.DENY);
                            return;
                        }
                    }
                }
                inventory.setResult(ItemStackUtils.getBukkitStack(ceRecipe.assemble(null, ItemBuildContext.of(serverPlayer))));
            }
            // 有函数的情况下，执行函数
            if (ceRecipe.hasFunctions()) {
                PlayerOptionalContext context = PlayerOptionalContext.of(serverPlayer);
                for (Function<Context> function : ceRecipe.functions()) {
                    function.run(context);
                }
            }
        }
    }

    // bukkit的getRecipe会生成新的recipe对象，过程较慢，只需要获取配方id即可
    @Nullable
    private Key getCurrentCraftingRecipeId(CraftingInventory inventory) {
        Object craftContainer = CraftInventoryProxy.INSTANCE.getInventory(inventory);
        Object recipeHolderOrRecipe;
        if (VersionHelper.isOrAbove1_21) {
            recipeHolderOrRecipe = CraftingContainerProxy.INSTANCE.getCurrentRecipe(craftContainer);
        } else {
            recipeHolderOrRecipe = ContainerProxy.INSTANCE.getCurrentRecipe(craftContainer);
        }
        if (recipeHolderOrRecipe == null) return null;
        if (VersionHelper.isOrAbove1_21_2) {
            return KeyUtils.identifierToKey(ResourceKeyProxy.INSTANCE.getIdentifier(RecipeHolderProxy.INSTANCE.getId(recipeHolderOrRecipe)));
        } else if (VersionHelper.isOrAbove1_20_2) {
            return KeyUtils.identifierToKey(RecipeHolderProxy.INSTANCE.getId(recipeHolderOrRecipe));
        } else {
            // 其实是recipe getId的实现
            return KeyUtils.identifierToKey(RecipeProxy.INSTANCE.getId(recipeHolderOrRecipe));
        }
    }

    private CraftingInput getCraftingInput(CraftingInventory inventory) {
        ItemStack[] ingredients = inventory.getMatrix();
        List<UniqueIdItem> uniqueIdItems = new ArrayList<>();
        for (ItemStack itemStack : ingredients) {
            uniqueIdItems.add(ItemStackUtils.getUniqueIdItem(itemStack));
        }
        CraftingInput input;
        if (ingredients.length == 9) {
            input = CraftingInput.of(3, 3, uniqueIdItems);
        } else if (ingredients.length == 4) {
            input = CraftingInput.of(2, 2, uniqueIdItems);
        } else {
            return null;
        }
        return input;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPrepareSmithingRecipe(PrepareSmithingEvent event) {
        SmithingInventory inventory = event.getInventory();
        if (ItemStackUtils.isEmpty(inventory.getResult())) return;
        org.bukkit.inventory.Recipe smithingRecipe = inventory.getRecipe();
        if (smithingRecipe instanceof SmithingTrimRecipe recipe) {
            ItemStack equipment = inventory.getInputEquipment();
            if (!ItemStackUtils.isEmpty(equipment)) {
                Item wrappedEquipment = this.itemManager.wrap(equipment);
                Optional<ItemDefinition> optionalCustomItem = wrappedEquipment.getDefinition();
                if (optionalCustomItem.isPresent()) {
                    ItemDefinition itemDefinition = optionalCustomItem.get();
                    ItemEquipment itemEquipmentSettings = itemDefinition.settings().equipment();
                    if (itemEquipmentSettings != null && itemEquipmentSettings.equipment() instanceof TrimBasedEquipment) {
                        // 不允许trim类型的盔甲再次被使用trim
                        event.setResult(null);
                        return;
                    }
                }
            }

            Key recipeId = Key.of(recipe.getKey().namespace(), recipe.getKey().value());
            Optional<Recipe> optionalRecipe = this.recipeManager.recipeById(recipeId);
            if (optionalRecipe.isEmpty()) {
                return;
            }
            if (!(optionalRecipe.get() instanceof CustomSmithingTrimRecipe smithingTrimRecipe)) {
                event.setResult(null);
                return;
            }
            Player player = InventoryUtils.getPlayerFromInventoryEvent(event);
            ItemBuildContext itemBuildContext = ItemBuildContext.of(BukkitAdaptor.adapt(player));
            if (!smithingTrimRecipe.canUse(itemBuildContext)) {
                event.setResult(null);
                return;
            }
            Item result = smithingTrimRecipe.assemble(getSmithingInput(inventory), itemBuildContext);
            event.setResult(ItemStackUtils.getBukkitStack(result));
        } else if (smithingRecipe instanceof SmithingTransformRecipe recipe) {
            Key recipeId = Key.of(recipe.getKey().namespace(), recipe.getKey().value());
            Optional<Recipe> optionalRecipe = this.recipeManager.recipeById(recipeId);
            if (optionalRecipe.isEmpty()) {
                return;
            }
            if (!(optionalRecipe.get() instanceof CustomSmithingTransformRecipe smithingTransformRecipe)) {
                event.setResult(null);
                return;
            }
            Player player = InventoryUtils.getPlayerFromInventoryEvent(event);
            ItemBuildContext itemBuildContext = ItemBuildContext.of(BukkitAdaptor.adapt(player));
            if (!smithingTransformRecipe.canUse(itemBuildContext)) {
                event.setResult(null);
                return;
            }
            SmithingInput input = getSmithingInput(inventory);
            if (smithingTransformRecipe.hasVisualResult() && VersionHelper.PREMIUM) {
                event.setResult(ItemStackUtils.getBukkitStack(smithingTransformRecipe.assembleVisual(input, itemBuildContext)));
            } else {
                event.setResult(ItemStackUtils.getBukkitStack(smithingTransformRecipe.assemble(input, itemBuildContext)));
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onSmithingFinish(SmithItemEvent event) {
        if (!Config.enableRecipeSystem() || !VersionHelper.PREMIUM) return;
        SmithingInventory inventory = event.getInventory();
        ItemStack visualResultOrReal = inventory.getResult();
        // 没有产物，肯定是被其他插件干没了
        if (ItemStackUtils.isEmpty(visualResultOrReal)) return;

        org.bukkit.inventory.Recipe recipe = inventory.getRecipe();
        Player player = InventoryUtils.getPlayerFromInventoryEvent(event);
        BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
        if (serverPlayer == null) return;

        if (recipe instanceof SmithingTransformRecipe transformRecipe) {
            Key recipeId = KeyUtils.namespacedKeyToKey(transformRecipe.getKey());
            Optional<Recipe> optionalRecipe = this.recipeManager.recipeById(recipeId);
            // 也许是其他插件注册的配方，直接无视
            if (optionalRecipe.isEmpty() || !(optionalRecipe.get() instanceof CustomSmithingTransformRecipe ceRecipe)) {
                return;
            }

            // 没有视觉结果和函数你凑什么热闹
            if (!ceRecipe.hasFunctions() && !ceRecipe.hasVisualResult() && !ceRecipe.ingredientCountSupport()) {
                return;
            }

            InventoryAction action = event.getAction();
            // 啥也没干
            if (action == InventoryAction.NOTHING) {
                return;
            }

            // 对低版本nothing不全的兼容
            if (!VersionHelper.isOrAbove1_20_5 && LegacyInventoryUtils.isHotBarSwapAndReadd(action)) {
                int slot = event.getHotbarButton();
                if (slot == -1) {
                    if (!serverPlayer.getItemInHand(InteractionHand.OFF_HAND).isEmpty()) {
                        return;
                    }
                } else {
                    ItemStack item = player.getInventory().getItem(slot);
                    if (!ItemStackUtils.isEmpty(item)) {
                        return;
                    }
                }
            }

            ClickType click = event.getClick();

            Object mcPlayer = serverPlayer.serverPlayer();
            Object smithingMenu = PlayerProxy.INSTANCE.getContainerMenu(mcPlayer);

            if (click == ClickType.CONTROL_DROP) {
                // 由插件自己处理多次合成
                event.setResult(Event.Result.DENY);

                Object resultSlot = AbstractContainerMenuProxy.INSTANCE.getSlot(smithingMenu, 3);

                for (;;) {
                    // 这个时候配方已经更新了，如果变化了，那么就不要操作
                    if (!(inventory.getRecipe() instanceof SmithingTransformRecipe newTransform) || !recipeId.equals(KeyUtils.namespacedKeyToKey(newTransform.getKey()))) {
                        break;
                    }

                    // 双重校验，mc不一定在此更新
                    if (!ceRecipe.matches(getSmithingInput(inventory))) {
                        break;
                    }

                    if (ceRecipe.hasVisualResult()) {
                        inventory.setResult(ItemStackUtils.getBukkitStack(ceRecipe.assemble(getSmithingInput(inventory), ItemBuildContext.of(serverPlayer))));
                    }

                    Object takenItem = SlotProxy.INSTANCE.safeTake(resultSlot, 1, Integer.MAX_VALUE, mcPlayer);
                    if (ItemStackProxy.INSTANCE.isEmpty(takenItem)) {
                        break;
                    }

                    // 扣除额外原料
                    if (ceRecipe.ingredientCountSupport()) {
                        ceRecipe.takeInput(getSmithingInput(inventory), 1);
                    }

                    PlayerProxy.INSTANCE.drop(mcPlayer, takenItem, true);

                    // 有函数的情况下，执行函数
                    if (ceRecipe.hasFunctions()) {
                        PlayerOptionalContext context = PlayerOptionalContext.of(serverPlayer);
                        for (Function<Context> function : ceRecipe.functions()) {
                            function.run(context);
                        }
                    }
                }

                return;
            }

            if (click.isShiftClick()) {
                // 由插件自己处理多次合成
                event.setResult(Event.Result.DENY);

                for (;;) {
                    // 这个时候配方已经更新了，如果变化了，那么就不要操作
                    if (!(inventory.getRecipe() instanceof SmithingTransformRecipe newTransform) || !recipeId.equals(KeyUtils.namespacedKeyToKey(newTransform.getKey()))) {
                        break;
                    }

                    // 双重校验，mc不一定在此更新
                    if (!ceRecipe.matches(getSmithingInput(inventory))) {
                        break;
                    }

                    // 配方不变，允许起飞
                    // 如果有视觉结果，先临时替换为真实的
                    if (ceRecipe.hasVisualResult()) {
                        inventory.setResult(ItemStackUtils.getBukkitStack(ceRecipe.assemble(getSmithingInput(inventory), ItemBuildContext.of(serverPlayer))));
                    }

                    // 连续获取
                    Object itemMoved = AbstractContainerMenuProxy.INSTANCE.quickMoveStack(smithingMenu, mcPlayer, 3 /* result slot */);
                    if (ItemStackProxy.INSTANCE.isEmpty(itemMoved)) {
                        // 发现取了个寂寞，根本没地方放，给他复原成视觉结果
                        inventory.setResult(visualResultOrReal);
                        break;
                    }
                    // 扣除额外原料
                    if (ceRecipe.ingredientCountSupport()) {
                        ceRecipe.takeInput(getSmithingInput(inventory), 1);
                    }
                    // 有函数的情况下，执行函数
                    if (ceRecipe.hasFunctions()) {
                        PlayerOptionalContext context = PlayerOptionalContext.of(serverPlayer);
                        for (Function<Context> function : ceRecipe.functions()) {
                            function.run(context);
                        }
                    }
                }
            } else {
                if (click == ClickType.MIDDLE) {
                    if (ItemStackUtils.isEmpty(event.getCursor())) {
                        return;
                    }
                }
                if (click == ClickType.DROP) {
                    if (!ItemStackUtils.isEmpty(event.getCursor())) {
                        return;
                    }
                }
                // 有视觉结果的情况下，重新构造真实物品
                if (ceRecipe.hasVisualResult()) {
                    // 指针物品不为空，且竟然和视觉物品一致，逆天，必须阻止
                    if (click == ClickType.LEFT || click == ClickType.RIGHT) {
                        ItemStack cursor = event.getCursor();
                        if (!ItemStackUtils.isEmpty(cursor)) {
                            if (cursor.isSimilar(visualResultOrReal)) {
                                event.setResult(Event.Result.DENY);
                                return;
                            }
                        }
                    }
                    inventory.setResult(ItemStackUtils.getBukkitStack(ceRecipe.assemble(getSmithingInput(inventory), ItemBuildContext.of(serverPlayer))));
                }
                // 有函数的情况下，执行函数
                if (ceRecipe.hasFunctions()) {
                    PlayerOptionalContext context = PlayerOptionalContext.of(serverPlayer);
                    for (Function<Context> function : ceRecipe.functions()) {
                        function.run(context);
                    }
                }
                // 扣除额外原料
                if (ceRecipe.ingredientCountSupport()) {
                    ceRecipe.takeInput(getSmithingInput(inventory), 1);
                }
            }
        }

        // trim 配方只能执行函数
        else if (recipe instanceof SmithingTrimRecipe trimRecipe) {
            Key recipeId = KeyUtils.namespacedKeyToKey(trimRecipe.getKey());
            Optional<Recipe> optionalRecipe = this.recipeManager.recipeById(recipeId);
            if (optionalRecipe.isEmpty() || !(optionalRecipe.get() instanceof CustomSmithingTrimRecipe ceRecipe)) {
                return;
            }
            // 没有函数你凑什么热闹
            if (!ceRecipe.hasFunctions()) {
                return;
            }

            InventoryAction action = event.getAction();
            // 啥也没干
            if (action == InventoryAction.NOTHING) {
                return;
            }

            // 对低版本nothing不全的兼容
            if (!VersionHelper.isOrAbove1_20_5 && LegacyInventoryUtils.isHotBarSwapAndReadd(action)) {
                int slot = event.getHotbarButton();
                if (slot == -1) {
                    if (!serverPlayer.getItemInHand(InteractionHand.OFF_HAND).isEmpty()) {
                        return;
                    }
                } else {
                    ItemStack item = player.getInventory().getItem(slot);
                    if (!ItemStackUtils.isEmpty(item)) {
                        return;
                    }
                }
            }

            if (event.isShiftClick()) {
                // 由插件自己处理多次合成
                event.setResult(Event.Result.DENY);

                Object mcPlayer = serverPlayer.serverPlayer();
                Object smithingMenu = PlayerProxy.INSTANCE.getContainerMenu(mcPlayer);

                for (;;) {
                    // 这个时候配方已经更新了，如果变化了，那么就不要操作
                    if (!(inventory.getRecipe() instanceof SmithingTrimRecipe newTrim) || !recipeId.equals(KeyUtils.namespacedKeyToKey(newTrim.getKey()))) {
                        break;
                    }

                    // 双重校验
                    if (!ceRecipe.matches(getSmithingInput(inventory))) {
                        break;
                    }

                    // 连续获取
                    Object itemMoved = AbstractContainerMenuProxy.INSTANCE.quickMoveStack(smithingMenu, mcPlayer, 3 /* result slot */);
                    if (ItemStackProxy.INSTANCE.isEmpty(itemMoved)) {
                        // 发现取了个寂寞，根本没地方放
                        break;
                    }
                    // 有函数的情况下，执行函数
                    if (ceRecipe.hasFunctions()) {
                        PlayerOptionalContext context = PlayerOptionalContext.of(serverPlayer);
                        for (Function<Context> function : ceRecipe.functions()) {
                            function.run(context);
                        }
                    }
                }

            } else {
                ClickType click = event.getClick();
                // 禁止非空手丢弃触发函数
                if (click == ClickType.DROP || click == ClickType.CONTROL_DROP) {
                    if (!ItemStackUtils.isEmpty(event.getCursor())) {
                        return;
                    }
                }
                // 执行函数
                Function<Context>[] functions = ceRecipe.functions();
                if (functions != null) {
                    PlayerOptionalContext context = PlayerOptionalContext.of(serverPlayer);
                    for (Function<Context> function : functions) {
                        function.run(context);
                    }
                }
            }
        }
    }

    private SmithingInput getSmithingInput(SmithingInventory inventory) {
        return new SmithingInput(
                ItemStackUtils.getUniqueIdItem(inventory.getInputEquipment()),
                ItemStackUtils.getUniqueIdItem(inventory.getInputTemplate()),
                ItemStackUtils.getUniqueIdItem(inventory.getInputMineral())
        );
    }

    @Nullable
    private RecipeType getRecipeTypeByCookingInventoryHolder(InventoryHolder inventoryHolder) {
        return switch (inventoryHolder) {
            case BlastFurnace ignored -> RecipeType.BLASTING;
            case Smoker ignored -> RecipeType.SMOKING;
            case Furnace ignored -> RecipeType.SMELTING;
            case null, default -> null;
        };
    }
}
