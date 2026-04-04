package net.litecraft.examples;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import net.litecraft.registry.LiteRegistry;

import java.util.Map;

public class ExampleContent {

    public static void registerAll(LiteRegistry registry) {
        // Register a Custom Trident (using CustomModelData)
        registry.registerItem("ruby_trident", 2001);

        // Register a Shaped Recipe for the Ruby Trident
        registry.registerShapedRecipe(
                "ruby_trident_recipe",
                getRubyTrident(registry),
                new String[]{"RRR", " S ", " S "},
                Map.of('R', Material.REDSTONE_BLOCK, 'S', Material.STICK)
        );

        // Register Custom Stair (ByteBuddy logic handles the NMS subclassing)
        // registry.registerBlock("ruby_stairs", new CustomStairBehavior());
    }

    public static ItemStack getRubyTrident(LiteRegistry registry) {
        return registry.createCustomItem(new ItemStack(Material.TRIDENT), "ruby_trident");
    }
}
