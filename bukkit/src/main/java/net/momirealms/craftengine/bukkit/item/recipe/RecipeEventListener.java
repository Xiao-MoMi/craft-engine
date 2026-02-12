package net.momirealms.craftengine.bukkit.item.recipe;

import com.destroystokyo.paper.event.inventory.PrepareResultEvent;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.item.DataComponentTypes;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.reflection.bukkit.CraftBukkitReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
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
import net.momirealms.craftengine.core.item.setting.AnvilRepairItem;
import net.momirealms.craftengine.core.item.setting.ItemEquipment;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.ContextKey;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.util.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.inventory.ClickType;
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
                    SingleItemInput<ItemStack> input = new SingleItemInput<>(ItemStackUtils.getUniqueIdItem(item));
                    RecipeType recipeType;
                    if (furnaceInventory.getType() == InventoryType.FURNACE) {
                        recipeType = RecipeType.SMELTING;
                    } else if (furnaceInventory.getType() == InventoryType.BLAST_FURNACE) {
                        recipeType = RecipeType.BLASTING;
                    } else {
                        recipeType = RecipeType.SMOKING;
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
            CustomItem<ItemStack> firstCustomItem = firstCustom.get();
            if (firstCustomItem.settings().repairable().anvilCombine() == Tristate.FALSE) {
                event.setResult(null);
                return;
            }

            Item<ItemStack> wrappedResult = BukkitItemManager.instance().wrap(event.getResult());
            if (!firstCustomItem.settings().canEnchant()) {
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
        } else if (VersionHelper.isOrAbove1_20_5() && wrappedFirst.hasComponent(DataComponentTypes.CUSTOM_NAME)) {
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

        BukkitServerPlayer serverPlayer = BukkitAdaptors.adapt(player);
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
            event.setResult(wrappedFirst.getItem());
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
        if (!CraftBukkitReflections.clazz$CraftComplexRecipe.isInstance(complexRecipe)) {
            return;
        }
        try {
            Object mcRecipe = CraftBukkitReflections.field$CraftComplexRecipe$recipe.get(complexRecipe);
            if (CoreReflections.clazz$ArmorDyeRecipe.isInstance(mcRecipe) || CoreReflections.clazz$FireworkStarFadeRecipe.isInstance(mcRecipe)) {
                return;
            }
            // 处理修复配方，在此处理才能使用玩家参数构建物品
            if (CoreReflections.clazz$RepairItemRecipe.isInstance(mcRecipe)) {
                Pair<ItemStack, ItemStack> theOnlyTwoItem = getTheOnlyTwoItem(inventory.getMatrix());
                if (theOnlyTwoItem == null) return;
                Item<ItemStack> first = BukkitItemManager.instance().wrap(theOnlyTwoItem.left());
                Item<ItemStack> right = BukkitItemManager.instance().wrap(theOnlyTwoItem.right());
                int max = Math.max(first.maxDamage(), right.maxDamage());
                int durability1 = first.maxDamage() - first.damage().orElse(0);
                int durability2 = right.maxDamage() - right.damage().orElse(0);
                int finalDurability = durability1 + durability2 + max * 5 / 100;
                Optional<CustomItem<ItemStack>> customItemOptional = plugin.itemManager().getCustomItem(first.id());
                if (customItemOptional.isEmpty()) {
                    inventory.setResult(null);
                    return;
                }
                Player player = InventoryUtils.getPlayerFromInventoryEvent(event);
                Item<ItemStack> newItem = customItemOptional.get().buildItem(BukkitAdaptors.adapt(player));
                newItem.maxDamage(max);
                newItem.damage(Math.max(max - finalDurability, 0));
                inventory.setResult(newItem.getItem());
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
        Optional<Recipe<ItemStack>> optionalRecipe = this.recipeManager.recipeById(recipeId);
        // 也许是其他插件注册的配方，直接无视
        if (optionalRecipe.isEmpty()) {
            return;
        }
        if (!(optionalRecipe.get() instanceof CustomCraftingTableRecipe<ItemStack> craftingTableRecipe)) {
            inventory.setResult(null);
            return;
        }
        Player player = InventoryUtils.getPlayerFromInventoryEvent(event);
        BukkitServerPlayer serverPlayer = BukkitAdaptors.adapt(player);
        if (craftingTableRecipe.hasCondition()) {
            if (!craftingTableRecipe.canUse(PlayerOptionalContext.of(serverPlayer))) {
                inventory.setResult(null);
                return;
            }
        }
        if (craftingTableRecipe.hasVisualResult() && VersionHelper.PREMIUM) {
            ItemBuildContext itemBuildContext = ItemBuildContext.of(serverPlayer);
            inventory.setResult(craftingTableRecipe.assembleVisual(null, itemBuildContext));
        } else {
            if (craftingTableRecipe.alwaysRebuildOutput()) {
                ItemBuildContext itemBuildContext = ItemBuildContext.of(serverPlayer);
                inventory.setResult(craftingTableRecipe.assemble(null, itemBuildContext));
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
        Optional<Recipe<ItemStack>> optionalRecipe = this.recipeManager.recipeById(recipeId);
        // 也许是其他插件注册的配方，直接无视
        if (optionalRecipe.isEmpty() || !(optionalRecipe.get() instanceof CustomCraftingTableRecipe<ItemStack> ceRecipe)) {
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
        BukkitServerPlayer serverPlayer = BukkitAdaptors.adapt(player);
        if (serverPlayer == null) return;

        // 对低版本nothing不全的兼容
        if (!VersionHelper.isOrAbove1_20_5() && LegacyInventoryUtils.isHotBarSwapAndReadd(action)) {
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

        // 多次合成
        if (event.isShiftClick()) {
            // 由插件自己处理多次合成
            event.setResult(Event.Result.DENY);

            Object mcPlayer = serverPlayer.serverPlayer();
            Object craftingMenu = FastNMS.INSTANCE.field$Player$containerMenu(mcPlayer);

            // 如果有视觉结果，先临时替换为真实的
            if (ceRecipe.hasVisualResult()) {
                inventory.setResult(ceRecipe.assemble(null, ItemBuildContext.of(serverPlayer)));
            }
            // 先取一次
            Object itemMoved = FastNMS.INSTANCE.method$AbstractContainerMenu$quickMoveStack(craftingMenu, mcPlayer, 0 /* result slot */);
            if (FastNMS.INSTANCE.method$ItemStack$isEmpty(itemMoved)) {
                // 发现取了个寂寞，根本没地方放，给他复原成视觉结果
                inventory.setResult(visualResultOrReal);
                return;
            }
            // 有函数的情况下，执行函数
            if (ceRecipe.hasFunctions()) {
                PlayerOptionalContext context = PlayerOptionalContext.of(serverPlayer, ContextHolder.builder().withParameter(ContextKey.direct("first_time"), new Object()));
                for (Function<Context> function : ceRecipe.functions()) {
                    function.run(context);
                }
            }

            for (;;) {
                // 这个时候配方已经更新了，如果变化了，那么就不要操作
                if (!recipeId.equals(getCurrentCraftingRecipeId(inventory))) {
                    break;
                }

                // 配方不变，允许起飞
                // 如果有视觉结果，先临时替换为真实的
                if (ceRecipe.hasVisualResult()) {
                    inventory.setResult(ceRecipe.assemble(null, ItemBuildContext.of(serverPlayer)));
                }

                // 连续获取
                itemMoved = FastNMS.INSTANCE.method$AbstractContainerMenu$quickMoveStack(craftingMenu, mcPlayer, 0 /* result slot */);
                if (FastNMS.INSTANCE.method$ItemStack$isEmpty(itemMoved)) {
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
            ClickType click = event.getClick();
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
                inventory.setResult(ceRecipe.assemble(null, ItemBuildContext.of(serverPlayer)));
            }
            // 有函数的情况下，执行函数
            if (ceRecipe.hasFunctions()) {
                PlayerOptionalContext context = PlayerOptionalContext.of(serverPlayer, ContextHolder.builder().withParameter(ContextKey.direct("first_time"), new Object()));
                for (Function<Context> function : ceRecipe.functions()) {
                    function.run(context);
                }
            }
        }
    }

    // bukkit的getRecipe会生成新的recipe对象，过程较慢，只需要获取配方id即可
    @Nullable
    private Key getCurrentCraftingRecipeId(CraftingInventory inventory) {
        Object craftContainer = FastNMS.INSTANCE.method$CraftInventory$getInventory(inventory);
        Object recipeHolderOrRecipe = FastNMS.INSTANCE.method$CraftingContainer$getCurrentRecipe(craftContainer);
        if (recipeHolderOrRecipe == null) return null;
        if (VersionHelper.isOrAbove1_21_2()) {
            return KeyUtils.resourceLocationToKey(FastNMS.INSTANCE.field$ResourceKey$location(FastNMS.INSTANCE.field$RecipeHolder$id(recipeHolderOrRecipe)));
        } else if (VersionHelper.isOrAbove1_20_2()) {
            return KeyUtils.resourceLocationToKey(FastNMS.INSTANCE.field$RecipeHolder$id(recipeHolderOrRecipe));
        } else {
            // 其实是recipe getId的实现
            return KeyUtils.resourceLocationToKey(FastNMS.INSTANCE.field$RecipeHolder$id(recipeHolderOrRecipe));
        }
    }

    private CraftingInput<ItemStack> getCraftingInput(CraftingInventory inventory) {
        ItemStack[] ingredients = inventory.getMatrix();
        List<UniqueIdItem<ItemStack>> uniqueIdItems = new ArrayList<>();
        for (ItemStack itemStack : ingredients) {
            uniqueIdItems.add(ItemStackUtils.getUniqueIdItem(itemStack));
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPrepareSmithingRecipe(PrepareSmithingEvent event) {
        SmithingInventory inventory = event.getInventory();
        if (ItemStackUtils.isEmpty(inventory.getResult())) return;
        org.bukkit.inventory.Recipe smithingRecipe = inventory.getRecipe();
        if (smithingRecipe instanceof SmithingTrimRecipe recipe) {
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
            Optional<Recipe<ItemStack>> optionalRecipe = this.recipeManager.recipeById(recipeId);
            if (optionalRecipe.isEmpty()) {
                return;
            }
            if (!(optionalRecipe.get() instanceof CustomSmithingTrimRecipe<ItemStack> smithingTrimRecipe)) {
                event.setResult(null);
                return;
            }
            Player player = InventoryUtils.getPlayerFromInventoryEvent(event);
            ItemBuildContext itemBuildContext = ItemBuildContext.of(BukkitAdaptors.adapt(player));
            if (!smithingTrimRecipe.canUse(itemBuildContext)) {
                event.setResult(null);
                return;
            }
            ItemStack result = smithingTrimRecipe.assemble(getSmithingInput(inventory), itemBuildContext);
            event.setResult(result);
        } else if (smithingRecipe instanceof SmithingTransformRecipe recipe) {
            Key recipeId = Key.of(recipe.getKey().namespace(), recipe.getKey().value());
            Optional<Recipe<ItemStack>> optionalRecipe = this.recipeManager.recipeById(recipeId);
            if (optionalRecipe.isEmpty()) {
                return;
            }
            if (!(optionalRecipe.get() instanceof CustomSmithingTransformRecipe<ItemStack> smithingTransformRecipe)) {
                event.setResult(null);
                return;
            }
            Player player = InventoryUtils.getPlayerFromInventoryEvent(event);
            ItemBuildContext itemBuildContext = ItemBuildContext.of(BukkitAdaptors.adapt(player));
            if (!smithingTransformRecipe.canUse(itemBuildContext)) {
                event.setResult(null);
                return;
            }
            SmithingInput<ItemStack> input = getSmithingInput(inventory);
            if (smithingTransformRecipe.hasVisualResult() && VersionHelper.PREMIUM) {
                event.setResult(smithingTransformRecipe.assembleVisual(input, itemBuildContext));
            } else {
                event.setResult(smithingTransformRecipe.assemble(input, itemBuildContext));
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
        BukkitServerPlayer serverPlayer = BukkitAdaptors.adapt(player);
        if (serverPlayer == null) return;

        if (recipe instanceof SmithingTransformRecipe transformRecipe) {
            Key recipeId = KeyUtils.namespacedKey2Key(transformRecipe.getKey());
            Optional<Recipe<ItemStack>> optionalRecipe = this.recipeManager.recipeById(recipeId);
            // 也许是其他插件注册的配方，直接无视
            if (optionalRecipe.isEmpty() || !(optionalRecipe.get() instanceof CustomSmithingTransformRecipe<ItemStack> ceRecipe)) {
                return;
            }
            // 没有视觉结果和函数你凑什么热闹
            if (!ceRecipe.hasFunctions() && !ceRecipe.hasVisualResult()) {
                return;
            }

            InventoryAction action = event.getAction();
            // 啥也没干
            if (action == InventoryAction.NOTHING) {
                return;
            }

            // 对低版本nothing不全的兼容
            if (!VersionHelper.isOrAbove1_20_5() && LegacyInventoryUtils.isHotBarSwapAndReadd(action)) {
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
                Object smithingMenu = FastNMS.INSTANCE.field$Player$containerMenu(mcPlayer);

                // 如果有视觉结果，先临时替换为真实的
                if (ceRecipe.hasVisualResult()) {
                    inventory.setResult(ceRecipe.assemble(getSmithingInput(inventory), ItemBuildContext.of(serverPlayer)));
                }
                // 先取一次
                Object itemMoved = FastNMS.INSTANCE.method$AbstractContainerMenu$quickMoveStack(smithingMenu, mcPlayer, 3 /* result slot */);
                if (FastNMS.INSTANCE.method$ItemStack$isEmpty(itemMoved)) {
                    // 发现取了个寂寞，根本没地方放，给他复原成视觉结果
                    inventory.setResult(visualResultOrReal);
                    return;
                }
                // 有函数的情况下，执行函数
                if (ceRecipe.hasFunctions()) {
                    PlayerOptionalContext context = PlayerOptionalContext.of(serverPlayer, ContextHolder.builder().withParameter(ContextKey.direct("first_time"), new Object()));
                    for (Function<Context> function : ceRecipe.functions()) {
                        function.run(context);
                    }
                }

                for (;;) {
                    // 这个时候配方已经更新了，如果变化了，那么就不要操作
                    if (!(inventory.getRecipe() instanceof SmithingTransformRecipe newTransform) || !recipeId.equals(KeyUtils.namespacedKey2Key(newTransform.getKey()))) {
                        break;
                    }

                    // 配方不变，允许起飞
                    // 如果有视觉结果，先临时替换为真实的
                    if (ceRecipe.hasVisualResult()) {
                        inventory.setResult(ceRecipe.assemble(getSmithingInput(inventory), ItemBuildContext.of(serverPlayer)));
                    }

                    // 连续获取
                    itemMoved = FastNMS.INSTANCE.method$AbstractContainerMenu$quickMoveStack(smithingMenu, mcPlayer, 3 /* result slot */);
                    if (FastNMS.INSTANCE.method$ItemStack$isEmpty(itemMoved)) {
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
            } else {
                ClickType click = event.getClick();
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
                    inventory.setResult(ceRecipe.assemble(getSmithingInput(inventory), ItemBuildContext.of(serverPlayer)));
                }
                // 有函数的情况下，执行函数
                if (ceRecipe.hasFunctions()) {
                    PlayerOptionalContext context = PlayerOptionalContext.of(serverPlayer, ContextHolder.builder().withParameter(ContextKey.direct("first_time"), new Object()));
                    for (Function<Context> function : ceRecipe.functions()) {
                        function.run(context);
                    }
                }
            }
        }

        // trim 配方只能执行函数
        else if (recipe instanceof SmithingTrimRecipe trimRecipe) {
            Key recipeId = KeyUtils.namespacedKey2Key(trimRecipe.getKey());
            Optional<Recipe<ItemStack>> optionalRecipe = this.recipeManager.recipeById(recipeId);
            if (optionalRecipe.isEmpty() || !(optionalRecipe.get() instanceof CustomSmithingTrimRecipe<ItemStack> ceRecipe)) {
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
            if (!VersionHelper.isOrAbove1_20_5() && LegacyInventoryUtils.isHotBarSwapAndReadd(action)) {
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
                Object smithingMenu = FastNMS.INSTANCE.field$Player$containerMenu(mcPlayer);

                // 先取一次
                Object itemMoved = FastNMS.INSTANCE.method$AbstractContainerMenu$quickMoveStack(smithingMenu, mcPlayer, 3 /* result slot */);
                if (FastNMS.INSTANCE.method$ItemStack$isEmpty(itemMoved)) {
                    // 发现取了个寂寞，根本没地方放
                    return;
                }
                // 有函数的情况下，执行函数
                if (ceRecipe.hasFunctions()) {
                    PlayerOptionalContext context = PlayerOptionalContext.of(serverPlayer);
                    for (Function<Context> function : ceRecipe.functions()) {
                        function.run(context);
                    }
                }

                for (;;) {
                    // 这个时候配方已经更新了，如果变化了，那么就不要操作
                    if (!(inventory.getRecipe() instanceof SmithingTrimRecipe newTrim) || !recipeId.equals(KeyUtils.namespacedKey2Key(newTrim.getKey()))) {
                        break;
                    }
                    // 连续获取
                    itemMoved = FastNMS.INSTANCE.method$AbstractContainerMenu$quickMoveStack(smithingMenu, mcPlayer, 3 /* result slot */);
                    if (FastNMS.INSTANCE.method$ItemStack$isEmpty(itemMoved)) {
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

    private SmithingInput<ItemStack> getSmithingInput(SmithingInventory inventory) {
        return new SmithingInput<>(
                ItemStackUtils.getUniqueIdItem(inventory.getInputEquipment()),
                ItemStackUtils.getUniqueIdItem(inventory.getInputTemplate()),
                ItemStackUtils.getUniqueIdItem(inventory.getInputMineral())
        );
    }
}
