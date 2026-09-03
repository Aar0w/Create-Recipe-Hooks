package dev.createrecipehooks.core;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.IdentityHashMap;
import java.util.Map;

// Reverse lookup from a recipe object to its registry id. Since 1.21 recipes no longer
// know their own id and most Create hooks only see the unwrapped recipe.
// The map is identity based and rebuilt once per RecipeManager instance (datapack reload).
public final class RecipeIdLookup {

    private static volatile Map<Recipe<?>, ResourceLocation> ids = Map.of();
    private static WeakReference<RecipeManager> source = new WeakReference<>(null);

    // Returns the registry id of the given recipe, or null for recipes
    // not known to the RecipeManager (addon-generated ones).
    @Nullable
    public static ResourceLocation idOf(Level level, Recipe<?> recipe) {
        RecipeManager manager = level.getRecipeManager();
        if (source.get() != manager) {
            Map<Recipe<?>, ResourceLocation> fresh = new IdentityHashMap<>();
            for (RecipeHolder<?> holder : manager.getRecipes())
                fresh.put(holder.value(), holder.id());
            ids = fresh;
            source = new WeakReference<>(manager);
        }
        return ids.get(recipe);
    }

    private RecipeIdLookup() {}
}
