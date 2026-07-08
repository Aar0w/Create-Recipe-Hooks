package dev.createrecipehooks.api;

import dev.createrecipehooks.core.RecipeEventDispatcher;   // internal, intentional facade coupling
import dev.createrecipehooks.core.RecipeHookRegistry;       // internal, intentional facade coupling
import org.jetbrains.annotations.NotNull;

/**
 * Primary entry point of the library. Register listeners once in your mod initializer,
 * for example CreateRecipeHooks.register(ctx -> ...). Registration methods are
 * thread-safe; listeners are invoked on the server tick thread.
 */
public final class CreateRecipeHooks {

    private CreateRecipeHooks() {}

    /**
     * Registers a listener that will be notified whenever any Create recipe completes.
     *
     * Listeners are called in registration order. If a listener throws an exception
     * it is caught and logged; subsequent listeners still execute.
     *
     * Registrations are permanent for the lifetime of the JVM, there is no
     * unregister method. Register once in your mod initializer.
     *
     * @param listener the listener to add; must not be null
     */
    public static void register(@NotNull IRecipeFinishedListener listener) {
        RecipeEventDispatcher.registerListener(listener);
    }

    /**
     * Registers a listener for block-processing events: one call per block broken by a
     * Mechanical Drill (stationary or contraption), per crop cut by a Mechanical Harvester,
     * and per lone block cut by a Mechanical Saw that is not part of a tree.
     *
     * Same threading and lifetime rules as register.
     *
     * @param listener the listener to add; must not be null
     */
    public static void registerBlockProcessed(@NotNull IBlockProcessedListener listener) {
        RecipeEventDispatcher.registerBlockProcessedListener(listener);
    }

    /**
     * Registers a listener for tree-felling events: one call per tree felled by a
     * Mechanical Saw (stationary or contraption). The context carries
     * BlockProcessedContext#getLogCount() and
     * BlockProcessedContext#getLeafCount().
     *
     * Same threading and lifetime rules as register.
     *
     * @param listener the listener to add; must not be null
     */
    public static void registerTreeCut(@NotNull IBlockProcessedListener listener) {
        RecipeEventDispatcher.registerTreeCutListener(listener);
    }

    /**
     * Registers an IHookProvider, typically used by addon authors to declare
     * that their mod provides additional hook sources.
     *
     * The provider's IHookProvider#register(IRegistrar) method is called
     * immediately on the current thread, allowing it to attach listeners.
     *
     * @param provider the provider to register; must not be null
     * @throws IllegalArgumentException if a provider with the same id is already registered
     */
    public static void registerProvider(@NotNull IHookProvider provider) {
        RecipeHookRegistry.INSTANCE.addProvider(provider);
    }

    /**
     * Returns the version string of this library, e.g. "1.0.0+create-6.0.0".
     * Useful for compatibility checks in providers.
     */
    @NotNull
    public static String getVersion() {
        // Populated by the build system via a generated constant or manifest.
        // Falls back to "unknown" if the library is loaded without proper packaging.
        String v = CreateRecipeHooks.class.getPackage().getImplementationVersion();
        return v != null ? v : "unknown";
    }
}
