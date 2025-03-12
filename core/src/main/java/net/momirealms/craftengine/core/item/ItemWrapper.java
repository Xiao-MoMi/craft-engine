package net.momirealms.craftengine.core.item;

public interface ItemWrapper<I> {

    I getItem();

    void update();

    I load();

    I loadCopy();

    Object getLiteralObject();

    boolean set(Object value, Object... path);

    boolean add(Object value, Object... path);

    <V> V get(Object... path);

    <V> V getExact(Object... path);

    boolean remove(Object... path);

    boolean hasTag(Object... path);

    void removeComponent(Object type);

    boolean hasComponent(Object type);

    void setComponent(Object type, Object value);

    Object getComponent(Object type);

    int count();

    void count(int amount);

    ItemWrapper<I> copyWithCount(int count);
}
