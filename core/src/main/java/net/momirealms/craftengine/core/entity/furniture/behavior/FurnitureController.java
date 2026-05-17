package net.momirealms.craftengine.core.entity.furniture.behavior;

import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.FurnitureSnapshotState;
import net.momirealms.craftengine.core.entity.furniture.FurnitureVariant;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElement;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBox;
import net.momirealms.craftengine.core.entity.furniture.tick.FurnitureTicker;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.world.context.InteractEntityContext;
import net.momirealms.sparrow.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class FurnitureController {
    protected final Furniture furniture;

    protected FurnitureController(@NotNull Furniture furniture) {
        this.furniture = furniture;
    }

    public Furniture furniture() {
        return this.furniture;
    }

    @SuppressWarnings("unchecked")
    public <C extends FurnitureController> void let(@NotNull Class<C> controllerClass, int index, @NotNull Consumer<C> consumer) {
        if (controllerClass.isInstance(this)) {
            consumer.accept((C) this);
        }
    }

    @SuppressWarnings("unchecked")
    public <C extends FurnitureController, V> V let(Class<C> clazz, int index, Function<C, V> function) {
        if (clazz.isInstance(this)) {
            return function.apply((C) this);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <C extends FurnitureController> C get(Class<C> clazz, int index) {
        if (clazz.isInstance(this)) {
            return (C) this;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <C extends FurnitureController, T extends FurnitureController> FurnitureTicker<C> createTickerHelper(FurnitureTicker<? super T> ticker) {
        return (FurnitureTicker<C>) ticker;
    }

    public void loadCustomData(CompoundTag data) {
    }

    public void saveCustomData(CompoundTag data) {
    }

    public void loadCustomDataFromItem(Item item) {
    }

    /**
     * Creates a ticker that runs on the main server thread.
     */
    public <T extends FurnitureController> FurnitureTicker<T> createFurnitureTicker() {
        return null;
    }

    /**
     * Creates a ticker that runs asynchronously.
     */
    public <T extends FurnitureController> FurnitureTicker<T> createAsyncFurnitureTicker() {
        return null;
    }

    public InteractionResult useOnFurniture(FurnitureHitBox hitBox, InteractEntityContext context) {
        return InteractionResult.TRY_EMPTY_HAND;
    }

    public InteractionResult useWithoutItem(InteractEntityContext context) {
        return InteractionResult.PASS;
    }

    public void onVariantChange(FurnitureVariant previousVariant) {
    }

    public void gatherElements(Consumer<FurnitureElement> consumer) {
    }

    public void gatherHitboxes(Consumer<FurnitureHitBox> consumer) {
    }

    public void onAsyncPlayerTrack(Player player, FurnitureSnapshotState snapshotState) {
    }

    public void onAsyncPlayerUntrack(Player player, FurnitureSnapshotState snapshotState) {
    }

    public void onPlayerTrack(Player player) {
    }

    public void onPlayerUntrack(Player player) {
    }

    public InteractionResult onPlayerHit(Player player, FurnitureHitBox hitBox) {
        return InteractionResult.PASS;
    }

    /**
     * Triggered when the furniture is removed.
     */
    public void preRemove(@Nullable Player player) {
    }

    /**
     * Triggered when the furniture is removed.
     */
    public void postRemove(@Nullable Player player) {
    }

    /**
     * Triggered when the furniture is first placed in the world.
     */
    public void onPlace(@Nullable Player player) {
    }

    /**
     * Called when the furniture is unloaded from memory.
     * This can occur due to chunk unloading or the furniture being destroyed.
     */
    public void onUnload(boolean isStopping) {
    }

    /**
     * Called when the furniture is loaded into memory.
     * This can occur due to chunk loading or the furniture being placed.
     */
    public void onLoad() {
    }

    @Nullable
    public Item getItemToPickup(Player player, FurnitureHitBox hitBox) {
        return null;
    }

    public static FurnitureController createController(@NotNull Furniture furniture) {
        List<FurnitureBehaviorTemplate> behaviors = furniture.config.behaviors();
        return switch (behaviors.size()) {
            case 0 -> new EmptyFurnitureBehaviorTemplate.EmptyFurnitureController(furniture);
            case 1 -> behaviors.getFirst().createController(furniture);
            case 2 -> new DualController(furniture, behaviors.getFirst().createController(furniture), behaviors.getLast().createController(furniture));
            default -> {
                FurnitureController[] controllers = new FurnitureController[behaviors.size()];
                for (int i = 0; i < behaviors.size(); i++) {
                    controllers[i] = behaviors.get(i).createController(furniture);
                }
                yield new CompositeController(furniture, controllers);
            }
        };
    }

    private static final class DualController extends FurnitureController {
        private final FurnitureController first;
        private final FurnitureController second;

        private DualController(Furniture furniture, FurnitureController first, FurnitureController second) {
            super(furniture);
            this.first = first;
            this.second = second;
        }

        @Override
        public <C extends FurnitureController> FurnitureTicker<C> createFurnitureTicker() {
            FurnitureTicker<FurnitureController> firstFurnitureTicker = this.first.createFurnitureTicker();
            FurnitureTicker<FurnitureController> secondFurnitureTicker = this.second.createFurnitureTicker();
            return createTickerHelper(gettFurnitureTicker(firstFurnitureTicker, secondFurnitureTicker));
        }

        @Override
        public <C extends FurnitureController> FurnitureTicker<C> createAsyncFurnitureTicker() {
            FurnitureTicker<FurnitureController> firstFurnitureTicker = this.first.createAsyncFurnitureTicker();
            FurnitureTicker<FurnitureController> secondFurnitureTicker = this.second.createAsyncFurnitureTicker();
            return createTickerHelper(gettFurnitureTicker(firstFurnitureTicker, secondFurnitureTicker));
        }

        private static FurnitureTicker<DualController> gettFurnitureTicker(FurnitureTicker<FurnitureController> firstFurnitureTicker, FurnitureTicker<FurnitureController> secondFurnitureTicker) {
            if (firstFurnitureTicker == null && secondFurnitureTicker == null) return null;
            if (firstFurnitureTicker == null) return (f, dualController) -> secondFurnitureTicker.tick(f, dualController.second);
            if (secondFurnitureTicker == null) return (f, dualController) -> firstFurnitureTicker.tick(f, dualController.first);
            return (f, dualController) -> {
                firstFurnitureTicker.tick(f, dualController.first);
                secondFurnitureTicker.tick(f, dualController.second);
            };
        }

        @Override
        public InteractionResult useOnFurniture(FurnitureHitBox hitBox, InteractEntityContext context) {
            InteractionResult result = this.first.useOnFurniture(hitBox, context);
            if (result != InteractionResult.PASS && result != InteractionResult.TRY_EMPTY_HAND) {
                return result;
            }
            return this.second.useOnFurniture(hitBox, context);
        }

        @Override
        public InteractionResult useWithoutItem(InteractEntityContext context) {
            InteractionResult result = this.first.useWithoutItem(context);
            return result == InteractionResult.PASS ? this.second.useWithoutItem(context) : result;
        }

        @Override
        public void gatherElements(Consumer<FurnitureElement> consumer) {
            this.first.gatherElements(consumer);
            this.second.gatherElements(consumer);
        }

        @Override
        public void gatherHitboxes(Consumer<FurnitureHitBox> consumer) {
            this.first.gatherHitboxes(consumer);
            this.second.gatherHitboxes(consumer);
        }

        @Override
        public void onAsyncPlayerTrack(Player player, FurnitureSnapshotState snapshotState) {
            this.first.onAsyncPlayerTrack(player, snapshotState);
            this.second.onAsyncPlayerTrack(player, snapshotState);
        }

        @Override
        public void onAsyncPlayerUntrack(Player player, FurnitureSnapshotState snapshotState) {
            this.first.onAsyncPlayerUntrack(player, snapshotState);
            this.second.onAsyncPlayerUntrack(player, snapshotState);
        }

        @Override
        public void onPlayerTrack(Player player) {
            this.first.onPlayerTrack(player);
            this.second.onPlayerTrack(player);
        }

        @Override
        public void onPlayerUntrack(Player player) {
            this.first.onPlayerUntrack(player);
            this.second.onPlayerUntrack(player);
        }

        @Override
        public InteractionResult onPlayerHit(Player player, FurnitureHitBox hitBox) {
            InteractionResult result = this.first.onPlayerHit(player, hitBox);
            return result == InteractionResult.PASS ? this.second.onPlayerHit(player, hitBox) : result;
        }

        @Override
        public void preRemove(Player player) {
            this.first.preRemove(player);
            this.second.preRemove(player);
        }

        @Override
        public void postRemove(@Nullable Player player) {
            this.first.postRemove(player);
            this.second.postRemove(player);
        }

        @Override
        public void onPlace(Player player) {
            this.first.onPlace(player);
            this.second.onPlace(player);
        }

        @Override
        public void onUnload(boolean isStopping) {
            this.first.onUnload(isStopping);
            this.second.onUnload(isStopping);
        }

        @Override
        public void onLoad() {
            this.first.onLoad();
            this.second.onLoad();
        }

        @Override
        public void loadCustomData(CompoundTag data) {
            this.first.loadCustomData(data);
            this.second.loadCustomData(data);
        }

        @Override
        public void saveCustomData(CompoundTag data) {
            this.first.saveCustomData(data);
            this.second.saveCustomData(data);
        }

        @Override
        public void loadCustomDataFromItem(Item item) {
            this.first.loadCustomDataFromItem(item);
            this.second.loadCustomDataFromItem(item);
        }

        @Override
        public void onVariantChange(FurnitureVariant previousVariant) {
            this.first.onVariantChange(previousVariant);
            this.second.onVariantChange(previousVariant);
        }

        @Override
        public @Nullable Item getItemToPickup(Player player, FurnitureHitBox hitBox) {
            Item firstItemToPickup = this.first.getItemToPickup(player, hitBox);
            return firstItemToPickup != null ? firstItemToPickup : this.second.getItemToPickup(player, hitBox);
        }

        @Override
        public <C extends FurnitureController> void let(@NotNull Class<C> clazz, int index, @NotNull Consumer<C> consumer) {
            if (index == 0) this.first.let(clazz, index, consumer);
            else if (index == 1) this.second.let(clazz, index, consumer);
        }

        @Override
        public <C extends FurnitureController, V> V let(Class<C> clazz, int index, Function<C, V> function) {
            if (index == 0) return this.first.let(clazz, index, function);
            else if (index == 1) return this.second.let(clazz, index, function);
            return null;
        }

        @SuppressWarnings("unchecked")
        public <C extends FurnitureController> C get(Class<C> clazz, int index) {
            if (index == 0) return clazz.isInstance(this.first) ? (C) this.first : null;
            if (index == 1) return clazz.isInstance(this.second) ? (C) this.second : null;
            return null;
        }
    }

    private static final class CompositeController extends FurnitureController {
        private final FurnitureController[] controllers;

        private CompositeController(Furniture furniture, FurnitureController[] controllers) {
            super(furniture);
            this.controllers = controllers;
        }

        @Override
        public <T extends FurnitureController> FurnitureTicker<T> createFurnitureTicker() {
            return createCombinedTicker(FurnitureController::createFurnitureTicker);
        }

        @Override
        public <T extends FurnitureController> FurnitureTicker<T> createAsyncFurnitureTicker() {
            return createCombinedTicker(FurnitureController::createAsyncFurnitureTicker);
        }

        @SuppressWarnings("unchecked")
        private <T extends FurnitureController> FurnitureTicker<T> createCombinedTicker(
                java.util.function.Function<FurnitureController, FurnitureTicker<FurnitureController>> tickerExtractor) {

            List<FurnitureTicker<FurnitureController>> furnitureTickers = new ArrayList<>(4);
            List<FurnitureController> controllers = new ArrayList<>(4);

            for (FurnitureController controller : this.controllers) {
                FurnitureTicker<FurnitureController> ticker = tickerExtractor.apply(controller);
                if (ticker != null) {
                    furnitureTickers.add(ticker);
                    controllers.add(controller);
                }
            }

            if (furnitureTickers.isEmpty()) return null;

            if (furnitureTickers.size() == 1) {
                FurnitureController firstController = controllers.getFirst();
                FurnitureTicker<FurnitureController> firstTicker = furnitureTickers.getFirst();
                return (f, c) -> firstTicker.tick(f, firstController);
            }

            FurnitureTicker<FurnitureController>[] tickersArray = furnitureTickers.toArray(new FurnitureTicker[0]);
            FurnitureController[] controllersArray = controllers.toArray(new FurnitureController[0]);

            return (f, controller) -> {
                for (int i = 0; i < controllersArray.length; i++) {
                    tickersArray[i].tick(f, controllersArray[i]);
                }
            };
        }

        @Override
        public InteractionResult useOnFurniture(FurnitureHitBox hitBox, InteractEntityContext context) {
            for (FurnitureController controller : this.controllers) {
                InteractionResult result = controller.useOnFurniture(hitBox, context);
                if (result != InteractionResult.PASS && result != InteractionResult.TRY_EMPTY_HAND) {
                    return result;
                }
            }
            return InteractionResult.TRY_EMPTY_HAND;
        }

        @Override
        public InteractionResult useWithoutItem(InteractEntityContext context) {
            for (FurnitureController controller : this.controllers) {
                InteractionResult result = controller.useWithoutItem(context);
                if (result != InteractionResult.PASS) {
                    return result;
                }
            }
            return InteractionResult.PASS;
        }

        @Override
        public void gatherElements(Consumer<FurnitureElement> consumer) {
            for (FurnitureController controller : this.controllers) {
                controller.gatherElements(consumer);
            }
        }

        @Override
        public void gatherHitboxes(Consumer<FurnitureHitBox> consumer) {
            for (FurnitureController controller : this.controllers) {
                controller.gatherHitboxes(consumer);
            }
        }

        @Override
        public void onAsyncPlayerTrack(Player player, FurnitureSnapshotState snapshotState) {
            for (FurnitureController controller : this.controllers) {
                controller.onAsyncPlayerTrack(player, snapshotState);
            }
        }

        @Override
        public void onAsyncPlayerUntrack(Player player, FurnitureSnapshotState snapshotState) {
            for (FurnitureController controller : this.controllers) {
                controller.onAsyncPlayerUntrack(player, snapshotState);
            }
        }

        @Override
        public void onPlayerTrack(Player player) {
            for (FurnitureController controller : this.controllers) {
                controller.onPlayerTrack(player);
            }
        }

        @Override
        public void onPlayerUntrack(Player player) {
            for (FurnitureController controller : this.controllers) {
                controller.onPlayerUntrack(player);
            }
        }

        @Override
        public InteractionResult onPlayerHit(Player player, FurnitureHitBox hitBox) {
            for (FurnitureController controller : this.controllers) {
                InteractionResult result = controller.onPlayerHit(player, hitBox);
                if (InteractionResult.PASS != result) {
                    return result;
                }
            }
            return InteractionResult.PASS;
        }

        @Override
        public void preRemove(Player player) {
            for (FurnitureController controller : this.controllers) {
                controller.preRemove(player);
            }
        }

        @Override
        public void postRemove(@Nullable Player player) {
            for (FurnitureController controller : this.controllers) {
                controller.postRemove(player);
            }
        }

        @Override
        public void onPlace(Player player) {
            for (FurnitureController controller : this.controllers) {
                controller.onPlace(player);
            }
        }

        @Override
        public void onUnload(boolean isStopping) {
            for (FurnitureController controller : this.controllers) {
                controller.onUnload(isStopping);
            }
        }

        @Override
        public void onLoad() {
            for (FurnitureController controller : this.controllers) {
                controller.onLoad();
            }
        }

        @Override
        public void loadCustomData(CompoundTag data) {
            for (FurnitureController controller : this.controllers) {
                controller.loadCustomData(data);
            }
        }

        @Override
        public void saveCustomData(CompoundTag data) {
            for (FurnitureController controller : this.controllers) {
                controller.saveCustomData(data);
            }
        }

        @Override
        public void loadCustomDataFromItem(Item item) {
            for (FurnitureController controller : this.controllers) {
                controller.loadCustomDataFromItem(item);
            }
        }

        @Override
        public void onVariantChange(FurnitureVariant previousVariant) {
            for (FurnitureController controller : this.controllers) {
                controller.onVariantChange(previousVariant);
            }
        }

        @Override
        public @Nullable Item getItemToPickup(Player player, FurnitureHitBox hitBox) {
            for (FurnitureController controller : this.controllers) {
                Item itemToPickup = controller.getItemToPickup(player, hitBox);
                if (itemToPickup != null) {
                    return itemToPickup;
                }
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <C extends FurnitureController> void let(@NotNull Class<C> clazz, int index, @NotNull Consumer<C> consumer) {
            if (index >= 0 && index < this.controllers.length) {
                FurnitureController controller = this.controllers[index];
                if (clazz.isInstance(controller)) {
                    consumer.accept((C) controller);
                }
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public <C extends FurnitureController, V> V let(Class<C> clazz, int index, Function<C, V> function) {
            if (index >= 0 && index < this.controllers.length) {
                FurnitureController controller = this.controllers[index];
                if (clazz.isInstance(controller)) {
                    return function.apply((C) controller);
                }
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        public <C extends FurnitureController> C get(Class<C> clazz, int index) {
            if (index >= 0 && index < this.controllers.length) {
                FurnitureController controller = this.controllers[index];
                if (clazz.isInstance(controller)) {
                    return (C) controller;
                }
            }
            return null;
        }
    }
}
