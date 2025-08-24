package net.momirealms.craftengine.core.attribute;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

public class AttributeModifier {
    private final String type;
    private final Slot slot;
    private final Key id;
    private final double amount;
    private final Operation operation;
    @Nullable
    private final Display display;

    public AttributeModifier(String type, Slot slot, Key id, double amount, Operation operation, @Nullable Display display) {
        this.amount = amount;
        this.display = display;
        this.id = id;
        this.operation = operation;
        this.slot = slot;
        this.type = type;
    }

    public double amount() {
        return amount;
    }

    public @Nullable Display display() {
        return display;
    }

    public Key id() {
        return id;
    }

    public Operation operation() {
        return operation;
    }

    public Slot slot() {
        return slot;
    }

    public String type() {
        return type;
    }

    public enum Slot {
        ANY,
        HAND,
        ARMOR,
        MAINHAND,
        OFFHAND,
        HEAD,
        CHEST,
        LEGS,
        FEET,
        BODY
    }

    public enum Operation {
        ADD_VALUE("add_value"), ADD_MULTIPLIED_BASE("add_multiplied_base"), ADD_MULTIPLIED_TOTAL("add_multiplied_total");

        private final String id;

        Operation(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    public record Display(AttributeModifier.Display.Type type, Component value) {

        public enum Type {
                DEFAULT, HIDDEN, OVERRIDE
        }
    }
}
