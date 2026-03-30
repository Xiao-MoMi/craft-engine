package net.momirealms.craftengine.core.entity.furniture.behavior;

import net.momirealms.craftengine.core.entity.furniture.CustomFurniture;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElement;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBox;
import net.momirealms.craftengine.core.entity.furniture.tick.FurnitureTicker;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.world.context.InteractEntityContext;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class UnsafeCompositeFurnitureBehavior extends FurnitureBehavior<Object[]> {
    private final FurnitureBehavior<Object>[] behaviors;

    @SuppressWarnings("unchecked")
    public UnsafeCompositeFurnitureBehavior(CustomFurniture furniture, List<FurnitureBehavior<?>> behaviors) {
        super(furniture);
        this.behaviors = behaviors.toArray(new FurnitureBehavior[0]);
    }

    @Override
    public Object[] createData(Furniture furniture) {
        Object[] data = new Object[behaviors.length];
        for (int i = 0; i < data.length; i++) {
            data[i] = behaviors[i].createData(furniture);
        }
        return data;
    }

    @Override
    public <T extends Furniture> FurnitureTicker<T> createFurnitureTicker(T furniture, Object[] data) {
        List<FurnitureTicker<T>> tickers = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            FurnitureTicker<T> ticker = behaviors[i].createFurnitureTicker(furniture, data[i]);
            if (ticker == null) continue;
            tickers.add(ticker);
        }
        return combineTickers(tickers);
    }

    @Override
    public <T extends Furniture> FurnitureTicker<T> createAsyncFurnitureTicker(T furniture, Object[] data) {
        List<FurnitureTicker<T>> tickers = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            FurnitureTicker<T> ticker = behaviors[i].createAsyncFurnitureTicker(furniture, data[i]);
            if (ticker == null) continue;
            tickers.add(ticker);
        }
        return combineTickers(tickers);
    }

    private static <T extends Furniture> FurnitureTicker<T> combineTickers(List<FurnitureTicker<T>> tickers) {
        if (tickers.isEmpty()) return null;
        else if (tickers.size() == 1) return tickers.getFirst();
        else if (tickers.size() == 2) {
            FurnitureTicker<T> first = tickers.getFirst();
            FurnitureTicker<T> last = tickers.getLast();
            return f -> {
                first.tick(f);
                last.tick(f);
            };
        } else {
            @SuppressWarnings("unchecked")
            FurnitureTicker<T>[] tickersArray = tickers.toArray(new FurnitureTicker[0]);
            return f -> {
                for (FurnitureTicker<T> ticker : tickersArray) {
                    ticker.tick(f);
                }
            };
        }
    }

    @Override
    public InteractionResult useOnFurniture(Furniture furniture, FurnitureHitBox hitBox, InteractEntityContext context, Object[] data) {
        boolean hasPass = false;
        for (int i = 0; i < data.length; i++) {
            InteractionResult result = behaviors[i].useOnFurniture(furniture, hitBox, context, data[i]);
            if (result == InteractionResult.PASS) {
                hasPass = true;
                continue;
            }
            if (result != InteractionResult.TRY_EMPTY_HAND) {
                return result;
            }
        }
        return hasPass ? InteractionResult.PASS : super.useOnFurniture(furniture, hitBox, context, data);
    }

    @Override
    public InteractionResult useWithoutItem(Furniture furniture, InteractEntityContext context, Object[] data) {
        for (int i = 0; i < data.length; i++) {
            InteractionResult result = behaviors[i].useWithoutItem(furniture, context, data[i]);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return super.useWithoutItem(furniture, context, data);
    }

    @Override
    public void createFurnitureElements(Furniture furniture, Consumer<FurnitureElement> consumer, Object[] data) {
        for (int i = 0; i < data.length; i++) {
            behaviors[i].createFurnitureElements(furniture, consumer, data[i]);
        }
    }

    @Override
    public void createFurnitureHitboxes(Furniture furniture, Consumer<FurnitureHitBox> consumer, Object[] data) {
        for (int i = 0; i < data.length; i++) {
            behaviors[i].createFurnitureHitboxes(furniture, consumer, data[i]);
        }
    }

    @Override
    public void onDestroy(Furniture furniture, Object[] data) {
        for (int i = 0; i < data.length; i++) {
            behaviors[i].onDestroy(furniture, data[i]);
        }
    }

    @Override
    public void onPlace(Furniture furniture, UseOnContext context, Object[] data) {
        for (int i = 0; i < data.length; i++) {
            behaviors[i].onPlace(furniture, context, data[i]);
        }
    }

    @Override
    public void onUnload(Furniture furniture, Object[] data) {
        for (int i = 0; i < data.length; i++) {
            behaviors[i].onUnload(furniture, data[i]);
        }
    }

    @Override
    public void onLoad(Furniture furniture, Object[] data) {
        for (int i = 0; i < data.length; i++) {
            behaviors[i].onLoad(furniture, data[i]);
        }
    }

    @Override
    public @Nullable Item getItemToPickup(Furniture furniture, Player player, Object[] data) {
        for (int i = 0; i < data.length; i++) {
            Item item = behaviors[i].getItemToPickup(furniture, player, data[i]);
            if (item != null && !item.isEmpty()) {
                return item;
            }
        }
        return null;
    }

}
