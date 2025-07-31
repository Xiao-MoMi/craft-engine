package net.momirealms.craftengine.bukkit.item.recipe;

import com.destroystokyo.paper.event.inventory.PrepareResultEvent;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.item.ComponentTypes;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.injector.RecipeInjector;
import net.momirealms.craftengine.bukkit.plugin.reflection.bukkit.CraftBukkitReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRecipeTypes;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.bukkit.util.InventoryUtils;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.LegacyInventoryUtils;
import net.momirealms.craftengine.core.item.*;
import net.momirealms.craftengine.core.item.equipment.TrimBasedEquipment;
import net.momirealms.craftengine.core.item.recipe.*;
import net.momirealms.craftengine.core.item.recipe.Recipe;
import net.momirealms.craftengine.core.item.recipe.input.CraftingInput;
import net.momirealms.craftengine.core.item.recipe.input.SingleItemInput;
import net.momirealms.craftengine.core.item.recipe.input.SmithingInput;
import net.momirealms.craftengine.core.item.setting.AnvilRepairItem;
import net.momirealms.craftengine.core.item.setting.ItemEquipment;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.util.*;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Campfire;
import org.bukkit.block.Furnace;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.view.AnvilView;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("DuplicatedCode")
public class RecipeEventListener implements Listener {
    private final ItemManager<ItemStack> itemManager;
    private final BukkitRecipeManager recipeManager;
    private final BukkitCraftEngine plugin;

    public RecipeEventListener(BukkitCraftEngine plugin, BukkitRecipeManager recipeManager, ItemManager<ItemStack> itemManager) {
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
                    SingleItemInput<ItemStack> input = new SingleItemInput<>(getUniqueIdItem(item));
                    Key recipeType;
                    if (furnaceInventory.getType() == InventoryType.FURNACE) {
                        recipeType = RecipeTypes.SMELTING;
                    } else if (furnaceInventory.getType() == InventoryType.BLAST_FURNACE) {
                        recipeType = RecipeTypes.BLASTING;
                    } else {
                        recipeType = RecipeTypes.SMOKING;
                    }

                    Recipe<ItemStack> ceRecipe = this.recipeManager.recipeByInput(recipeType, input);
                    // The item is an ingredient, we should never consider it as fuel firstly
                    if (ceRecipe != null) return;

                    int fuelTime = this.itemManager.fuelTime(item);
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
                    int fuelTime = this.plugin.itemManager().fuelTime(item);
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
                    int fuelTime = this.plugin.itemManager().fuelTime(itemOnCursor);
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        ItemStack fuel = event.getFuel();
        int fuelTime = this.itemManager.fuelTime(fuel);
        if (fuelTime != 0) {
            event.setBurnTime(fuelTime);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onFurnaceInventoryOpen(InventoryOpenEvent event) {
        if (!Config.enableRecipeSystem()) return;
        if (!(event.getInventory() instanceof FurnaceInventory furnaceInventory)) {
            return;
        }
        Furnace furnace = furnaceInventory.getHolder();
        try {
            Object blockEntity = CraftBukkitReflections.field$CraftBlockEntityState$tileEntity.get(furnace);
            RecipeInjector.injectCookingBlockEntity(blockEntity);
        } catch (Exception e) {
            this.plugin.logger().warn("Failed to inject cooking block entity", e);
        }
    }

    // for 1.20.1-1.21.1
    @EventHandler(ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (!Config.enableRecipeSystem()) return;
        if (VersionHelper.isOrAbove1_21_2()) return;
        Block block = event.getBlock();
        Material material = block.getType();
        if (material == Material.CAMPFIRE) {
            if (block.getState() instanceof Campfire campfire) {
                try {
                    Object blockEntity = CraftBukkitReflections.field$CraftBlockEntityState$tileEntity.get(campfire);
                    RecipeInjector.injectCookingBlockEntity(blockEntity);
                } catch (Exception e) {
                    this.plugin.logger().warn("Failed to inject cooking block entity", e);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlaceBlock(BlockPlaceEvent event) {
        if (!Config.enableRecipeSystem()) return;
        Block block = event.getBlock();
        Material material = block.getType();
        if (material == Material.FURNACE || material == Material.BLAST_FURNACE || material == Material.SMOKER) {
            if (block.getState() instanceof Furnace furnace) {
                try {
                    Object blockEntity = CraftBukkitReflections.field$CraftBlockEntityState$tileEntity.get(furnace);
                    RecipeInjector.injectCookingBlockEntity(blockEntity);
                } catch (Exception e) {
                    plugin.logger().warn("Failed to inject cooking block entity", e);
                }
            }
        } else if (!VersionHelper.isOrAbove1_21_2() && material == Material.CAMPFIRE) {
            if (block.getState() instanceof Campfire campfire) {
                try {
                    Object blockEntity = CraftBukkitReflections.field$CraftBlockEntityState$tileEntity.get(campfire);
                    RecipeInjector.injectCookingBlockEntity(blockEntity);
                } catch (Exception e) {
                    this.plugin.logger().warn("Failed to inject cooking block entity", e);
                }
            }
        }
    }

    // for 1.21.2+
    @EventHandler(ignoreCancelled = true)
    public void onPutItemOnCampfire(PlayerInteractEvent event) {
        if (!Config.enableRecipeSystem()) return;
        if (!VersionHelper.isOrAbove1_21_2()) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block clicked = event.getClickedBlock();
        if (clicked == null) return;
        Material type = clicked.getType();
        if (type != Material.CAMPFIRE && type != Material.SOUL_CAMPFIRE) return;
        if (clicked.getState() instanceof Campfire campfire) {
            try {
                Object blockEntity = CraftBukkitReflections.field$CraftBlockEntityState$tileEntity.get(campfire);
                RecipeInjector.injectCookingBlockEntity(blockEntity);
            } catch (Exception e) {
                this.plugin.logger().warn("Failed to inject cooking block entity", e);
            }
        }

        ItemStack itemStack = event.getItem();
        if (ItemStackUtils.isEmpty(itemStack)) return;
        try {
            @SuppressWarnings("unchecked")
            Optional<Object> optionalMCRecipe = FastNMS.INSTANCE.method$RecipeManager$getRecipeFor(
                    BukkitRecipeManager.nmsRecipeManager(),
                    MRecipeTypes.CAMPFIRE_COOKING,
                    CoreReflections.constructor$SingleRecipeInput.newInstance(FastNMS.INSTANCE.method$CraftItemStack$asNMSCopy(itemStack)),
                    FastNMS.INSTANCE.field$CraftWorld$ServerLevel(event.getPlayer().getWorld()),
                    null
            );
            if (optionalMCRecipe.isEmpty()) {
                return;
            }
            SingleItemInput<ItemStack> input = new SingleItemInput<>(getUniqueIdItem(itemStack));
            CustomCampfireRecipe<ItemStack> ceRecipe = (CustomCampfireRecipe<ItemStack>) this.recipeManager.recipeByInput(RecipeTypes.CAMPFIRE_COOKING, input);
            if (ceRecipe == null) {
                event.setCancelled(true);
            }
        } catch (Exception e) {
            this.plugin.logger().warn("Failed to handle interact campfire", e);
        }
    }

    // for 1.21.2+
    @SuppressWarnings("UnstableApiUsage")
    @EventHandler(ignoreCancelled = true)
    public void onCampfireCook(CampfireStartEvent event) {
        if (!Config.enableRecipeSystem()) return;
        if (!VersionHelper.isOrAbove1_21_2()) return;
        CampfireRecipe recipe = event.getRecipe();
        Key recipeId = new Key(recipe.getKey().namespace(), recipe.getKey().value());

        boolean isCustom = this.recipeManager.isCustomRecipe(recipeId);
        if (!isCustom) {
            return;
        }

        ItemStack itemStack = event.getSource();
        SingleItemInput<ItemStack> input = new SingleItemInput<>(getUniqueIdItem(itemStack));
        CustomCampfireRecipe<ItemStack> ceRecipe = (CustomCampfireRecipe<ItemStack>) this.recipeManager.recipeByInput(RecipeTypes.CAMPFIRE_COOKING, input);
        if (ceRecipe == null) {
            event.setTotalCookTime(Integer.MAX_VALUE);
            return;
        }

        event.setTotalCookTime(ceRecipe.cookingTime());
    }

    // for 1.21.2+
    @EventHandler(ignoreCancelled = true)
    public void onCampfireCook(BlockCookEvent event) {
        if (!Config.enableRecipeSystem()) return;
        if (!VersionHelper.isOrAbove1_21_2()) return;
        Material type = event.getBlock().getType();
        if (type != Material.CAMPFIRE && type != Material.SOUL_CAMPFIRE) return;
        CampfireRecipe recipe = (CampfireRecipe) event.getRecipe();
        if (recipe == null) return;

        Key recipeId = new Key(recipe.getKey().namespace(), recipe.getKey().value());

        boolean isCustom = this.recipeManager.isCustomRecipe(recipeId);
        if (!isCustom) {
            return;
        }

        ItemStack itemStack = event.getSource();
        SingleItemInput<ItemStack> input = new SingleItemInput<>(getUniqueIdItem(itemStack));
        CustomCampfireRecipe<ItemStack> ceRecipe = (CustomCampfireRecipe<ItemStack>) this.recipeManager.recipeByInput(RecipeTypes.CAMPFIRE_COOKING, input);
        if (ceRecipe == null) {
            event.setCancelled(true);
            return;
        }

        event.setResult(ceRecipe.result(ItemBuildContext.EMPTY));
    }

    // Paper only
    @EventHandler
    public void onPrepareResult(PrepareResultEvent event) {
//        if (!ConfigManager.enableRecipeSystem()) return;
        if (event.getInventory() instanceof CartographyInventory cartographyInventory) {
            if (ItemStackUtils.hasCustomItem(cartographyInventory.getStorageContents())) {
                event.setResult(new ItemStack(Material.AIR));
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onAnvilEvent(PrepareAnvilEvent event) {
        preProcess(event);
        processRepairable(event);
        processRename(event);
    }

    /*
    预处理会阻止一些不合理的原版材质造成的合并问题
     */
    private void preProcess(PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory();
        ItemStack first = inventory.getFirstItem();
        ItemStack second = inventory.getSecondItem();
        if (first == null || second == null) return;
        Item<ItemStack> wrappedFirst = BukkitItemManager.instance().wrap(first);
        Optional<CustomItem<ItemStack>> firstCustom = wrappedFirst.getCustomItem();
        Item<ItemStack> wrappedSecond = BukkitItemManager.instance().wrap(second);
        Optional<CustomItem<ItemStack>> secondCustom = wrappedFirst.getCustomItem();
        // 两个都是原版物品
        if (firstCustom.isEmpty() && secondCustom.isEmpty()) {
            return;
        }
        // 如果第二个物品是附魔书，那么忽略
        if (wrappedSecond.vanillaId().equals(ItemKeys.ENCHANTED_BOOK)) {
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

        // 如果禁止在铁砧使用两个相同物品修复
        firstCustom.ifPresent(it -> {
            if (!it.settings().canRepair()) {
                event.setResult(null);
            }
        });
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

        Item<ItemStack> wrappedSecond = BukkitItemManager.instance().wrap(second);
        // 如果材料不是自定义的，那么忽略
        Optional<CustomItem<ItemStack>> customItemOptional = this.plugin.itemManager().getCustomItem(wrappedSecond.id());
        if (customItemOptional.isEmpty()) {
            return;
        }

        CustomItem<ItemStack> customItem = customItemOptional.get();
        List<AnvilRepairItem> repairItems = customItem.settings().repairItems();
        // 如果材料不支持修复物品，则忽略
        if (repairItems.isEmpty()) {
            return;
        }

        // 后续均为修复逻辑
        Item<ItemStack> wrappedFirst = BukkitItemManager.instance().wrap(first.clone());
        int maxDamage = wrappedFirst.maxDamage();
        int damage = wrappedFirst.damage().orElse(0);
        // 物品无damage属性
        if (damage == 0 || maxDamage == 0) return;

        Key firstId = wrappedFirst.id();
        Optional<CustomItem<ItemStack>> optionalCustomTool = wrappedFirst.getCustomItem();
        // 物品无法被修复
        if (optionalCustomTool.isPresent() && !optionalCustomTool.get().settings().canRepair()) {
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
                    if (wrappedFirst.is(tag)) {
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
        int consumeMaxAmount = damage / realDurabilityPerItem + 1;
        int actualConsumedAmount = Math.min(consumeMaxAmount, wrappedSecond.count());
        int actualRepairAmount = actualConsumedAmount * realDurabilityPerItem;
        int damageAfter = Math.max(damage - actualRepairAmount, 0);
        wrappedFirst.damage(damageAfter);

        String renameText;
        int maxRepairCost;
        //int previousCost;
        if (VersionHelper.isOrAbove1_21()) {
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
            try {
                if (!renameText.equals(CoreReflections.method$Component$getString.invoke(ComponentUtils.jsonToMinecraft(wrappedFirst.hoverNameJson().orElse(AdventureHelper.EMPTY_COMPONENT))))) {
                    wrappedFirst.customNameJson(AdventureHelper.componentToJson(Component.text(renameText)));
                    repairCost += 1;
                } else if (repairCost == 0) {
                    hasResult = false;
                }
            } catch (ReflectiveOperationException e) {
                plugin.logger().warn("Failed to get hover name", e);
            }
        } else if (VersionHelper.isOrAbove1_20_5() && wrappedFirst.hasComponent(ComponentTypes.CUSTOM_NAME)) {
            repairCost += 1;
            wrappedFirst.customNameJson(null);
        } else if (!VersionHelper.isOrAbove1_20_5() && wrappedFirst.hasTag("display", "Name")) {
            repairCost += 1;
            wrappedFirst.customNameJson(null);
        }

        int finalCost = repairCost + repairPenalty;

        // To fix some client side visual issues
        try {
            Object anvilMenu;
            if (VersionHelper.isOrAbove1_21()) {
                anvilMenu = CraftBukkitReflections.field$CraftInventoryView$container.get(event.getView());
            } else {
                anvilMenu = CraftBukkitReflections.field$CraftInventoryAnvil$menu.get(inventory);
            }
            CoreReflections.method$AbstractContainerMenu$broadcastFullState.invoke(anvilMenu);
        } catch (ReflectiveOperationException e) {
            this.plugin.logger().warn("Failed to broadcast changes", e);
        }

        if (VersionHelper.isOrAbove1_21()) {
            AnvilView anvilView = event.getView();
            anvilView.setRepairCost(finalCost);
            anvilView.setRepairItemCountCost(actualConsumedAmount);
        } else {
            LegacyInventoryUtils.setRepairCost(inventory, finalCost);
            LegacyInventoryUtils.setRepairCostAmount(inventory, actualConsumedAmount);
        }

        Player player = InventoryUtils.getPlayerFromInventoryEvent(event);

        if (finalCost >= maxRepairCost && !plugin.adapt(player).canInstabuild()) {
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
            event.setResult(wrappedFirst.getItem());
        }
    }

    /*
    如果物品不可被重命名，则在最后处理。
     */
    @SuppressWarnings("UnstableApiUsage")
    private void processRename(PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory();
        ItemStack first = inventory.getFirstItem();
        if (ItemStackUtils.isEmpty(first)) {
            return;
        }
        if (event.getResult() == null) {
            return;
        }
        Item<ItemStack> wrappedFirst = BukkitItemManager.instance().wrap(first);
        wrappedFirst.getCustomItem().ifPresent(item -> {
            if (!item.settings().renameable()) {
                String renameText;
                if (VersionHelper.isOrAbove1_21()) {
                    AnvilView anvilView = event.getView();
                    renameText = anvilView.getRenameText();
                } else {
                    renameText = LegacyInventoryUtils.getRenameText(inventory);
                }
                if (renameText != null && !renameText.isBlank()) {
                    try {
                        if (!renameText.equals(CoreReflections.method$Component$getString.invoke(ComponentUtils.jsonToMinecraft(wrappedFirst.hoverNameJson().orElse(AdventureHelper.EMPTY_COMPONENT))))) {
                            event.setResult(null);
                        }
                    } catch (Exception e) {
                        this.plugin.logger().warn("Failed to get hover name", e);
                    }
                }
            }
        });
    }

    public static int calculateIncreasedRepairCost(int cost) {
        return (int) Math.min((long) cost * 2L + 1L, 2147483647L);
    }

    // only handle repair items for the moment
    @EventHandler(ignoreCancelled = true)
    public void onSpecialRecipe(PrepareItemCraftEvent event) {
//        if (!ConfigManager.enableRecipeSystem()) return;
        org.bukkit.inventory.Recipe recipe = event.getRecipe();
        if (recipe == null)
            return;
        if (!(recipe instanceof ComplexRecipe complexRecipe))
            return;
        CraftingInventory inventory = event.getInventory();
        boolean hasCustomItem = ItemStackUtils.hasCustomItem(inventory.getMatrix());
        if (!hasCustomItem) {
            return;
        }

        if (!CraftBukkitReflections.clazz$CraftComplexRecipe.isInstance(complexRecipe)) {
            inventory.setResult(null);
            return;
        }

        try {
            Object mcRecipe = CraftBukkitReflections.field$CraftComplexRecipe$recipe.get(complexRecipe);

            // Repair recipe
            if (CoreReflections.clazz$RepairItemRecipe.isInstance(mcRecipe)) {
                // repair item
                ItemStack[] itemStacks = inventory.getMatrix();
                Pair<ItemStack, ItemStack> onlyTwoItems = getTheOnlyTwoItem(itemStacks);
                if (onlyTwoItems.left() == null || onlyTwoItems.right() == null) {
                    inventory.setResult(null);
                    return;
                }

                Item<ItemStack> left = plugin.itemManager().wrap(onlyTwoItems.left());
                Item<ItemStack> right = plugin.itemManager().wrap(onlyTwoItems.right());
                if (!left.id().equals(right.id())) {
                    inventory.setResult(null);
                    return;
                }

                int totalDamage = right.damage().orElse(0) + left.damage().orElse(0);
                int totalMaxDamage = left.maxDamage() + right.maxDamage();
                // should be impossible, but take care
                if (totalDamage >= totalMaxDamage) {
                    inventory.setResult(null);
                    return;
                }

                Player player = InventoryUtils.getPlayerFromInventoryEvent(event);

                Optional<CustomItem<ItemStack>> customItemOptional = plugin.itemManager().getCustomItem(left.id());
                if (customItemOptional.isEmpty()) {
                    inventory.setResult(null);
                    return;
                }

                CustomItem<ItemStack> customItem = customItemOptional.get();
                if (!customItem.settings().canRepair()) {
                    inventory.setResult(null);
                    return;
                }

                Item<ItemStack> newItem = customItem.buildItem(ItemBuildContext.of(plugin.adapt(player)));
                int remainingDurability = totalMaxDamage - totalDamage;
                int newItemDamage = Math.max(0, newItem.maxDamage() - remainingDurability);
                newItem.damage(newItemDamage);
                inventory.setResult(newItem.getItem());
            } else if (CoreReflections.clazz$ArmorDyeRecipe.isInstance(mcRecipe)) {
                handlePossibleDyeRecipe(event, false);
            } else if (CoreReflections.clazz$FireworkStarFadeRecipe.isInstance(mcRecipe)) {
                ItemStack[] itemStacks = inventory.getMatrix();
                for (ItemStack itemStack : itemStacks) {
                    if (itemStack == null) continue;
                    Item<ItemStack> item = plugin.itemManager().wrap(itemStack);
                    Optional<CustomItem<ItemStack>> optionalCustomItem = item.getCustomItem();
                    if (optionalCustomItem.isPresent() && optionalCustomItem.get().settings().dyeable() == Tristate.FALSE) {
                        inventory.setResult(null);
                        return;
                    }
                }
            } else {
                inventory.setResult(null);
                return;
            }
        } catch (Exception e) {
            this.plugin.logger().warn("Failed to handle minecraft custom recipe", e);
        }
    }

    private Pair<ItemStack, ItemStack> getTheOnlyTwoItem(ItemStack[] matrix) {
        ItemStack first = null;
        ItemStack second = null;
        for (ItemStack itemStack : matrix) {
            if (itemStack == null) continue;
            if (first == null) {
                first = itemStack;
            } else if (second == null) {
                second = itemStack;
            }
        }
        return new Pair<>(first, second);
    }

    // 不是完美的解决方案，仍然需要更多的探讨
    // TODO 生成类代理掉ResultSlot，并注入menu的slots对象，修改掉onTake方法
    // TODO 对于耐久度降低的配方，应该注册special recipe？
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onCraft(CraftItemEvent event) {
        org.bukkit.inventory.Recipe recipe = event.getRecipe();
        if (!(recipe instanceof ShapelessRecipe) && !(recipe instanceof ShapedRecipe)) return;
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player player)) return;
        CraftingInventory inventory = event.getInventory();
        ItemStack result = inventory.getResult();
        if (result == null) return;
        ItemStack[] usedItems = inventory.getMatrix();
        ItemStack[] replacements = new ItemStack[usedItems.length];
        boolean hasReplacement = false;
        for (int i = 0; i < usedItems.length; i++) {
            ItemStack usedItem = usedItems[i];
            if (ItemStackUtils.isEmpty(usedItem)) continue;
            if (usedItem.getAmount() != 1) continue;
            Item<ItemStack> wrapped = BukkitItemManager.instance().wrap(usedItem);
            if (ItemUtils.isEmpty(wrapped)) continue;
            Optional<CustomItem<ItemStack>> optionalCustomItem = wrapped.getCustomItem();
            if (optionalCustomItem.isPresent()) {
                CustomItem<ItemStack> customItem = optionalCustomItem.get();
                Key remainingItem = customItem.settings().craftRemainder();
                if (remainingItem != null) {
                    replacements[i] = BukkitItemManager.instance().buildItemStack(remainingItem, this.plugin.adapt(player));
                    hasReplacement = true;
                }
            }
        }
        if (!hasReplacement) return;
        Runnable delayedTask = () -> {
            for (int i = 0; i < replacements.length; i++) {
                if (replacements[i] == null) continue;
                inventory.setItem(i + 1, replacements[i]);
            }
        };
        if (VersionHelper.isFolia()) {
            player.getScheduler().run(this.plugin.javaPlugin(), (t) -> delayedTask.run(), () -> {});
        } else {
            this.plugin.scheduler().sync().runDelayed(delayedTask);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDyeRecipe(PrepareItemCraftEvent event) {
        org.bukkit.inventory.Recipe recipe = event.getRecipe();
        if (recipe != null) {
            return;
        }
        handlePossibleDyeRecipe(event, true);
    }

    private void handlePossibleDyeRecipe(PrepareItemCraftEvent event, boolean correct) {
        // dye recipe
        CraftingInventory inventory = event.getInventory();
        CraftingInput<ItemStack> input = getCraftingInput(inventory);
        if (input == null) return;
        if (BukkitRecipeManager.DYE_RECIPE.matches(input)) {
            Player player = InventoryUtils.getPlayerFromInventoryEvent(event);
            event.getInventory().setResult(BukkitRecipeManager.DYE_RECIPE.assemble(input, ItemBuildContext.of(this.plugin.adapt(player))));
            if (correct) {
                correctCraftingRecipeUsed(inventory, BukkitRecipeManager.DYE_RECIPE);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraftingRecipe(PrepareItemCraftEvent event) {
        if (!Config.enableRecipeSystem()) return;
        org.bukkit.inventory.Recipe recipe = event.getRecipe();

        // we only handle shaped and shapeless recipes
        boolean shapeless = recipe instanceof ShapelessRecipe;
        boolean shaped = recipe instanceof ShapedRecipe;
        if (!shaped && !shapeless) return;

        CraftingRecipe craftingRecipe = (CraftingRecipe) recipe;
        Key recipeId = Key.of(craftingRecipe.getKey().namespace(), craftingRecipe.getKey().value());

        boolean isCustom = this.recipeManager.isCustomRecipe(recipeId);
        // Maybe it's recipe from other plugins, then we ignore it
        if (!isCustom) {
            return;
        }

        CraftingInventory inventory = event.getInventory();
        CraftingInput<ItemStack> input = getCraftingInput(inventory);
        if (input == null) return;

        Player player = InventoryUtils.getPlayerFromInventoryEvent(event);
        BukkitServerPlayer serverPlayer = this.plugin.adapt(player);
        Key lastRecipe = serverPlayer.lastUsedRecipe();

        Recipe<ItemStack> ceRecipe = this.recipeManager.recipeByInput(RecipeTypes.SHAPELESS, input, lastRecipe);
        if (ceRecipe != null) {
            inventory.setResult(ceRecipe.assemble(input, new ItemBuildContext(serverPlayer, ContextHolder.EMPTY)));
            serverPlayer.setLastUsedRecipe(ceRecipe.id());
            if (!ceRecipe.id().equals(recipeId)) {
                correctCraftingRecipeUsed(inventory, ceRecipe);
            }
            return;
        }
        ceRecipe = this.recipeManager.recipeByInput(RecipeTypes.SHAPED, input, lastRecipe);
        if (ceRecipe != null) {
            inventory.setResult(ceRecipe.assemble(input, new ItemBuildContext(serverPlayer, ContextHolder.EMPTY)));
            serverPlayer.setLastUsedRecipe(ceRecipe.id());
            if (!ceRecipe.id().equals(recipeId)) {
                correctCraftingRecipeUsed(inventory, ceRecipe);
            }
            return;
        }
        // clear result if not met
        inventory.setResult(null);
    }

    private CraftingInput<ItemStack> getCraftingInput(CraftingInventory inventory) {
        ItemStack[] ingredients = inventory.getMatrix();

        List<UniqueIdItem<ItemStack>> uniqueIdItems = new ArrayList<>();
        for (ItemStack itemStack : ingredients) {
            uniqueIdItems.add(getUniqueIdItem(itemStack));
        }

        CraftingInput<ItemStack> input;
        if (ingredients.length == 9) {
            input = CraftingInput.of(3, 3, uniqueIdItems);
        } else if (ingredients.length == 4) {
            input = CraftingInput.of(2, 2, uniqueIdItems);
        } else {
            return null;
        }
        return input;
    }

    private void correctCraftingRecipeUsed(CraftingInventory inventory, Recipe<ItemStack> recipe) {
        Object holderOrRecipe = this.recipeManager.nmsRecipeHolderByRecipe(recipe);
        if (holderOrRecipe == null) {
            return;
        }
        Object resultInventory = FastNMS.INSTANCE.method$CraftInventoryCrafting$getResultInventory(inventory);
        FastNMS.INSTANCE.method$ResultContainer$setRecipeUsed(resultInventory, holderOrRecipe);
        Object matrixInventory = FastNMS.INSTANCE.method$CraftInventoryCrafting$getMatrixInventory(inventory);
        FastNMS.INSTANCE.method$CraftingContainer$setCurrentRecipe(matrixInventory, holderOrRecipe);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSmithingTrim(PrepareSmithingEvent event) {
        SmithingInventory inventory = event.getInventory();
        if (!(inventory.getRecipe() instanceof SmithingTrimRecipe recipe)) return;

        ItemStack equipment = inventory.getInputEquipment();
        if (!ItemStackUtils.isEmpty(equipment)) {
            Item<ItemStack> wrappedEquipment = this.itemManager.wrap(equipment);
            Optional<CustomItem<ItemStack>> optionalCustomItem = wrappedEquipment.getCustomItem();
            if (optionalCustomItem.isPresent()) {
                CustomItem<ItemStack> customItem = optionalCustomItem.get();
                ItemEquipment itemEquipmentSettings = customItem.settings().equipment();
                if (itemEquipmentSettings != null && itemEquipmentSettings.equipment() instanceof TrimBasedEquipment) {
                    // 不允许trim类型的盔甲再次被使用trim
                    event.setResult(null);
                    return;
                }
            }
        }

        Key recipeId = Key.of(recipe.getKey().namespace(), recipe.getKey().value());
        boolean isCustom = this.recipeManager.isCustomRecipe(recipeId);
        // Maybe it's recipe from other plugins, then we ignore it
        if (!isCustom) {
            return;
        }

        SmithingInput<ItemStack> input = new SmithingInput<>(
                getUniqueIdItem(inventory.getInputEquipment()),
                getUniqueIdItem(inventory.getInputTemplate()),
                getUniqueIdItem(inventory.getInputMineral())
        );

        Recipe<ItemStack> ceRecipe = this.recipeManager.recipeByInput(RecipeTypes.SMITHING_TRIM, input);
        if (ceRecipe == null) {
            event.setResult(null);
            return;
        }

        Player player = InventoryUtils.getPlayerFromInventoryEvent(event);
        CustomSmithingTrimRecipe<ItemStack> trimRecipe = (CustomSmithingTrimRecipe<ItemStack>) ceRecipe;
        ItemStack result = trimRecipe.assemble(input, new ItemBuildContext(this.plugin.adapt(player), ContextHolder.EMPTY));
        event.setResult(result);
        if (!ceRecipe.id().equals(recipeId)) {
            correctSmithingRecipeUsed(inventory, ceRecipe);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSmithingTransform(PrepareSmithingEvent event) {
        if (!Config.enableRecipeSystem()) return;
        SmithingInventory inventory = event.getInventory();
        if (!(inventory.getRecipe() instanceof SmithingTransformRecipe recipe)) return;

        Key recipeId = Key.of(recipe.getKey().namespace(), recipe.getKey().value());
        boolean isCustom = this.recipeManager.isCustomRecipe(recipeId);
        // Maybe it's recipe from other plugins, then we ignore it
        if (!isCustom) {
            return;
        }

        ItemStack base = inventory.getInputEquipment();
        ItemStack template = inventory.getInputTemplate();
        ItemStack addition = inventory.getInputMineral();

        SmithingInput<ItemStack> input = new SmithingInput<>(
                getUniqueIdItem(base),
                getUniqueIdItem(template),
                getUniqueIdItem(addition)
        );

        Recipe<ItemStack> ceRecipe = this.recipeManager.recipeByInput(RecipeTypes.SMITHING_TRANSFORM, input);
        if (ceRecipe == null) {
            event.setResult(null);
            return;
        }

        Player player = InventoryUtils.getPlayerFromInventoryEvent(event);

        CustomSmithingTransformRecipe<ItemStack> transformRecipe = (CustomSmithingTransformRecipe<ItemStack>) ceRecipe;
        ItemStack processed = transformRecipe.assemble(input, new ItemBuildContext(this.plugin.adapt(player), ContextHolder.EMPTY));
        event.setResult(processed);
        if (!ceRecipe.id().equals(recipeId)) {
            correctSmithingRecipeUsed(inventory, ceRecipe);
        }
    }

    private void correctSmithingRecipeUsed(SmithingInventory inventory, Recipe<ItemStack> recipe) {
        Object holderOrRecipe = this.recipeManager.nmsRecipeHolderByRecipe(recipe);
        if (holderOrRecipe == null) {
            return;
        }
        try {
            Object resultInventory = CraftBukkitReflections.field$CraftResultInventory$resultInventory.get(inventory);
            CoreReflections.field$ResultContainer$recipeUsed.set(resultInventory, holderOrRecipe);
        } catch (ReflectiveOperationException e) {
            this.plugin.logger().warn("Failed to correct used recipe", e);
        }
    }

    private UniqueIdItem<ItemStack> getUniqueIdItem(@Nullable ItemStack itemStack) {
        if (ItemStackUtils.isEmpty(itemStack)) {
            return this.itemManager.uniqueEmptyItem();
        } else {
            Item<ItemStack> wrappedItem = this.itemManager.wrap(itemStack);
            return new UniqueIdItem<>(wrappedItem.recipeIngredientId(), wrappedItem);
        }
    }
}
