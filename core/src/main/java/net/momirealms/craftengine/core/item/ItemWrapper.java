package net.momirealms.craftengine.core.item;

public interface ItemWrapper<I> {

    I getItem();

    Object getLiteralObject();

    int count();

    void count(int amount);

    ItemWrapper<I> copyWithCount(int count);

    void shrink(int amount);
}
