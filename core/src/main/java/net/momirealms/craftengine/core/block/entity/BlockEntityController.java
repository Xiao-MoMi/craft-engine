package net.momirealms.craftengine.core.block.entity;

import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElement;
import net.momirealms.craftengine.core.block.entity.tick.BlockEntityTicker;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.sparrow.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public abstract class BlockEntityController {
    protected final BlockEntity blockEntity;

    public BlockEntityController(BlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    public BlockEntity blockEntity() {
        return this.blockEntity;
    }

    public <C> void let(Class<C> clazz, int index, Consumer<C> consumer) {
        if (index == 0 && clazz.isInstance(this)) {
            consumer.accept((C) this);
        }
    }
    
    public <C, V> V let(Class<C> clazz, int index, Function<C, V> function) {
        if (index == 0 && clazz.isInstance(this)) {
            return function.apply((C) this);
        }
        return null;
    }
    
    public <C> void let(Class<C> clazz, Consumer<C> consumer) {
        if (clazz.isInstance(this)) {
            consumer.accept((C) this);
        }
    }
    
    public <C> C getAt(Class<C> clazz, int index) {
        if (index == 0 && clazz.isInstance(this)) {
            return (C) this;
        }
        return null;
    }

    public <C> C get(Class<C> clazz, int order) {
        if (order == 0 && clazz.isInstance(this)) {
            return (C) this;
        }
        return null;
    }

    public boolean hasElement() {
        return false;
    }

    public void gatherElements(Consumer<BlockEntityElement> consumer) {
    }

    public void saveCustomData(CompoundTag data) {
    }

    public void loadCustomData(CompoundTag data) {
    }

    public void loadCustomDataFromItem(Item item) {
    }

    public void onRemove() {
    }

    public void preBlockStateChange(ImmutableBlockState newState) {
    }

    public <C extends BlockEntityController> BlockEntityTicker<C> createBlockEntityTicker(CEWorld world, ImmutableBlockState blockState) {
        return null;
    }

    public <C extends BlockEntityController> BlockEntityTicker<C> createAsyncBlockEntityTicker(CEWorld world, ImmutableBlockState blockState) {
        return null;
    }

    public static <C extends BlockEntityController, T extends BlockEntityController> BlockEntityTicker<C> createTickerHelper(BlockEntityTicker<? super T> ticker) {
        return (BlockEntityTicker<C>) ticker;
    }

    @Nullable
    public static BlockEntityController createFromList(BlockEntity blockEntity, List<BlockEntityController> list) {
        return switch (list.size()) {
            case 0 -> null;
            case 1 -> list.getFirst();
            case 2 -> new DualController(blockEntity, list.get(0), list.get(1));
            default -> new CompositeController(blockEntity, list.toArray(new BlockEntityController[0]));
        };
    }

    private static final class DualController extends BlockEntityController {
        private final BlockEntityController first;
        private final BlockEntityController second;

        private DualController(BlockEntity blockEntity, BlockEntityController first, BlockEntityController second) {
            super(blockEntity);
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean hasElement() {
            return this.first.hasElement() || this.second.hasElement();
        }

        @Override
        public void gatherElements(Consumer<BlockEntityElement> consumer) {
            this.first.gatherElements(consumer);
            this.second.gatherElements(consumer);
        }

        @Override
        public <C extends BlockEntityController> BlockEntityTicker<C> createBlockEntityTicker(CEWorld world, ImmutableBlockState blockState) {
            BlockEntityTicker<BlockEntityController> t1 = this.first.createBlockEntityTicker(world, blockState);
            BlockEntityTicker<BlockEntityController> t2 = this.second.createBlockEntityTicker(world, blockState);
            return createTickerHelper(getCombinedTicker(t1, t2));
        }

        @Override
        public <C extends BlockEntityController> BlockEntityTicker<C> createAsyncBlockEntityTicker(CEWorld world, ImmutableBlockState blockState) {
            BlockEntityTicker<BlockEntityController> t1 = this.first.createAsyncBlockEntityTicker(world, blockState);
            BlockEntityTicker<BlockEntityController> t2 = this.second.createAsyncBlockEntityTicker(world, blockState);
            return createTickerHelper(getCombinedTicker(t1, t2));
        }

        private BlockEntityTicker<DualController> getCombinedTicker(BlockEntityTicker<BlockEntityController> t1, BlockEntityTicker<BlockEntityController> t2) {
            if (t1 == null && t2 == null) return null;
            if (t1 == null) return (w, p, s, bi) -> t2.tick(w, p, s, bi.second);
            if (t2 == null) return (w, p, s, bi) -> t1.tick(w, p, s, bi.first);
            return (w, p, s, bi) -> {
                t1.tick(w, p, s, bi.first);
                t2.tick(w, p, s, bi.second);
            };
        }

        @Override
        public void preBlockStateChange(ImmutableBlockState newState) {
            this.first.preBlockStateChange(newState);
            this.second.preBlockStateChange(newState);
        }

        @Override
        public void onRemove() {
            this.first.onRemove();
            this.second.onRemove();
        }

        @Override
        public void saveCustomData(CompoundTag data) {
            this.first.saveCustomData(data);
            this.second.saveCustomData(data);
        }

        @Override
        public void loadCustomData(CompoundTag data) {
            this.first.loadCustomData(data);
            this.second.loadCustomData(data);
        }

        @Override
        public void loadCustomDataFromItem(Item item) {
            this.first.loadCustomDataFromItem(item);
            this.second.loadCustomDataFromItem(item);
        }

        @Override
        public <C> void let(Class<C> clazz, int index, Consumer<C> consumer) {
            if (index == 0) {
                if (clazz.isInstance(this.first)) {
                    consumer.accept((C) this.first);
                }
            } else if (index == 1) {
                if (clazz.isInstance(this.second)) {
                    consumer.accept((C) this.second);
                }
            }
        }
        
        @Override
        public <C, V> V let(Class<C> clazz, int index, Function<C, V> function) {
            if (index == 0) {
                if (clazz.isInstance(this.first)) {
                    return function.apply((C) this.first);
                }
            } else if (index == 1) {
                if (clazz.isInstance(this.second)) {
                    return function.apply((C) this.second);
                }
            }
            return null;
        }

        @Override
        public <C> void let(Class<C> clazz, Consumer<C> consumer) {
            this.first.let(clazz, consumer);
            this.second.let(clazz, consumer);
        }
        
        @Override
        public <C> C getAt(Class<C> clazz, int index) {
            if (index == 0) return clazz.isInstance(this.first) ? (C) this.first : null;
            if (index == 1) return clazz.isInstance(this.second) ? (C) this.second : null;
            return null;
        }

        @Override
        public <C> C get(Class<C> clazz, int order) {
            int count = 0;
            if (clazz.isInstance(this.first)) {
                if (count == order) return (C) this.first;
                count++;
            }
            if (clazz.isInstance(this.second)) {
                if (count == order) return (C) this.second;
                count++;
            }
            return null;
        }
    }

    private static final class CompositeController extends BlockEntityController {
        private final BlockEntityController[] controllers;

        public CompositeController(BlockEntity blockEntity, BlockEntityController[] controllers) {
            super(blockEntity);
            this.controllers = controllers;
        }

        @Override
        public boolean hasElement() {
            for (BlockEntityController controller : this.controllers) {
                if (controller.hasElement()) return true;
            }
            return false;
        }

        @Override
        public void gatherElements(Consumer<BlockEntityElement> consumer) {
            for (BlockEntityController controller : this.controllers) {
                controller.gatherElements(consumer);
            }
        }

        @Override
        public <C extends BlockEntityController> BlockEntityTicker<C> createBlockEntityTicker(CEWorld world, ImmutableBlockState blockState) {
            return createCombinedTicker(world, blockState, BlockEntityController::createBlockEntityTicker);
        }

        @Override
        public <C extends BlockEntityController> BlockEntityTicker<C> createAsyncBlockEntityTicker(CEWorld world, ImmutableBlockState blockState) {
            return createCombinedTicker(world, blockState, BlockEntityController::createAsyncBlockEntityTicker);
        }

        private <C extends BlockEntityController> BlockEntityTicker<C> createCombinedTicker(
                CEWorld world,
                ImmutableBlockState blockState,
                TickerExtractor extractor) {

            List<BlockEntityTicker<BlockEntityController>> tickers = new ArrayList<>(4);
            List<BlockEntityController> activeControllers = new ArrayList<>(4);

            for (BlockEntityController controller : this.controllers) {
                BlockEntityTicker<BlockEntityController> ticker = extractor.extract(controller, world, blockState);
                if (ticker != null) {
                    tickers.add(ticker);
                    activeControllers.add(controller);
                }
            }

            if (tickers.isEmpty()) return null;

            if (tickers.size() == 1) {
                BlockEntityTicker<BlockEntityController> firstTicker = tickers.getFirst();
                BlockEntityController firstController = activeControllers.getFirst();
                return (ceWorld, pos, state, controller) -> firstTicker.tick(ceWorld, pos, state, firstController);
            }

            BlockEntityTicker<BlockEntityController>[] tickersArray = tickers.toArray(new BlockEntityTicker[0]);
            BlockEntityController[] controllersArray = activeControllers.toArray(new BlockEntityController[0]);

            return (ceWorld, pos, state, controller) -> {
                for (int i = 0; i < tickersArray.length; i++) {
                    tickersArray[i].tick(ceWorld, pos, state, controllersArray[i]);
                }
            };
        }

        @FunctionalInterface
        private interface TickerExtractor {
            BlockEntityTicker<BlockEntityController> extract(BlockEntityController controller, CEWorld world, ImmutableBlockState blockState);
        }

        @Override
        public void preBlockStateChange(ImmutableBlockState newState) {
            for (BlockEntityController controller : this.controllers) {
                controller.preBlockStateChange(newState);
            }
        }

        @Override
        public <C> void let(Class<C> clazz, int index, Consumer<C> consumer) {
            if (index >= 0 && index < this.controllers.length) {
                BlockEntityController controller = this.controllers[index];
                if (clazz.isInstance(controller)) {
                    consumer.accept(clazz.cast(controller));
                }
            }
        }

        @Override
        public <C, V> V let(Class<C> clazz, int index, Function<C, V> function) {
            if (index >= 0 && index < this.controllers.length) {
                BlockEntityController controller = this.controllers[index];
                if (clazz.isInstance(controller)) {
                    return function.apply(clazz.cast(controller));
                }
            }
            return null;
        }

        @Override
        public <C> void let(Class<C> clazz, Consumer<C> consumer) {
            for (BlockEntityController controller : this.controllers) {
                controller.let(clazz, consumer);
            }
        }

        @Override
        public <C> C get(Class<C> clazz, int order) {
            int count = 0;
            for (BlockEntityController controller : this.controllers) {
                if (clazz.isInstance(controller)) {
                    if (count == order) return (C) controller;
                    count++;
                }
            }
            return null;
        }

        @Override
        public <C> C getAt(Class<C> clazz, int index) {
            if (index >= 0 && index < this.controllers.length) {
                return (C) this.controllers[index];
            }
            return null;
        }

        @Override
        public void saveCustomData(CompoundTag data) {
            for (BlockEntityController controller : this.controllers) {
                controller.saveCustomData(data);
            }
        }

        @Override
        public void loadCustomData(CompoundTag data) {
            for (BlockEntityController controller : this.controllers) {
                controller.loadCustomData(data);
            }
        }

        @Override
        public void loadCustomDataFromItem(Item item) {
            for (BlockEntityController controller : this.controllers) {
                controller.loadCustomDataFromItem(item);
            }
        }

        @Override
        public void onRemove() {
            for (BlockEntityController controller : this.controllers) {
                controller.onRemove();
            }
        }
    }
}
