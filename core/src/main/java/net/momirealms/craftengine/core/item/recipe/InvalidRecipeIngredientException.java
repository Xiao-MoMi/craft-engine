package net.momirealms.craftengine.core.item.recipe;

public class InvalidRecipeIngredientException extends RuntimeException {
    private final String ingredient;

    public InvalidRecipeIngredientException(String ingredient) {
        this.ingredient = ingredient;
    }

    public String ingredient() {
        return ingredient;
    }
}
