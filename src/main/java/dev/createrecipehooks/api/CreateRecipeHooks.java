package dev.createrecipehooks.api;

import dev.createrecipehooks.core.RecipeEventDispatcher;
import dev.createrecipehooks.core.RecipeHookRegistry;
import org.jetbrains.annotations.NotNull;

/**
 * Primary entry point of the library. Register listeners once in your mod initializer,
 * for example CreateRecipeHooks.register(ctx -> ...). Listeners are invoked on the
 * server tick thread.
 */
public final class CreateRecipeHooks {

    private CreateRecipeHooks() {}

    /** Called whenever any Create recipe completes. Register once. */
    public static void register(@NotNull IRecipeFinishedListener listener) {
        RecipeEventDispatcher.registerListener(listener);
    }

    /** One call per block broken by a Drill, harvested plant or lone sawed block. */
    public static void registerBlockProcessed(@NotNull IBlockProcessedListener listener) {
        RecipeEventDispatcher.registerBlockProcessedListener(listener);
    }

    /** One call per tree felled by a Saw, log count included in the context. */
    public static void registerTreeCut(@NotNull IBlockProcessedListener listener) {
        RecipeEventDispatcher.registerTreeCutListener(listener);
    }

    /** Lets an addon declare extra hook sources. The provider id must be unique. */
    public static void registerProvider(@NotNull IHookProvider provider) {
        RecipeHookRegistry.INSTANCE.addProvider(provider);
    }

    /** Version string of this library, e.g. "1.1.0+create-6.0.8", or "unknown". */
    @NotNull
    public static String getVersion() {
        String v = CreateRecipeHooks.class.getPackage().getImplementationVersion();
        return v != null ? v : "unknown";
    }
}
