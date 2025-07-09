package net.momirealms.craftengine.core.block;

import net.momirealms.craftengine.core.util.ObjectHolder;

/**
 * Interface representing a delegatable block that can dynamically modify its shape and behavior at runtime.
 * Implementations of this interface serve as actual block instances injected into Minecraft's
 * {@code BuiltInRegistries.BLOCK}, enabling real-time modifications to physical properties and geometry.
 *
 * <p>Utilizes the {@code ObjectHolder} pattern to delegate block characteristics, allowing runtime updates
 * to collision boxes and interaction logic without reconstructing block instances.</p>
 */
public interface DelegatingBlock {

    /**
     * Gets the mutable holder for the block's shape delegate.
     * Modifying the contained {@code BlockShape} will dynamically update
     * collision bounding boxes and supporting shape.
     *
     * @return Non-null object holder containing current block shape
     */
    ObjectHolder<BlockShape> shapeDelegate();

    /**
     * Gets the mutable holder for the block's behavior delegate.
     * Modifying the contained {@code BlockBehavior} will dynamically adjust:
     * - Physics properties
     * - Interaction logic (e.g. click responses)
     * - State update rules
     *
     * @return Non-null object holder containing current block behavior
     */
    ObjectHolder<BlockBehavior> behaviorDelegate();

    @Deprecated
    boolean isNoteBlock();

    @Deprecated
    boolean isTripwire();
}
