package dev.createrecipehooks.core;

import dev.createrecipehooks.api.IHookProvider;
import dev.createrecipehooks.api.IRecipeFinishedListener;
import dev.createrecipehooks.api.IRegistrar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry that manages {@link IHookProvider} registrations.
 *
 * <p>Providers are keyed by their {@link IHookProvider#getId() id}. A provider
 * with a duplicate id is rejected with a warning — this prevents accidental
 * double-registration from class-loading order issues.
 *
 * <p>When a provider is accepted its {@link IHookProvider#register(IRegistrar)}
 * method is called immediately, passing {@code this} as the {@link IRegistrar}.
 *
 * <h3>Singleton</h3>
 * One shared instance {@link #INSTANCE} is used throughout the library. Addon code
 * should always go through {@link dev.createrecipehooks.api.CreateRecipeHooks#registerProvider}
 * rather than accessing this class directly.
 */
public final class RecipeHookRegistry implements IRegistrar {

    public static final RecipeHookRegistry INSTANCE = new RecipeHookRegistry();

    private static final Logger LOGGER = LogManager.getLogger("CreateRecipeHooks/Registry");

    /** Tracks accepted provider ids to detect duplicates. */
    private final ConcurrentHashMap<String, IHookProvider> providers = new ConcurrentHashMap<>();

    // ------------------------------------------------------------------ //
    //  Provider registration                                               //
    // ------------------------------------------------------------------ //

    /**
     * Registers an {@link IHookProvider} and immediately calls
     * {@link IHookProvider#register(IRegistrar)} on it, passing {@code this} as the registrar.
     *
     * @param provider the provider to register
     * @throws IllegalArgumentException if {@code provider.getId()} is null or empty
     */
    public void addProvider(IHookProvider provider) {
        if (provider == null) throw new NullPointerException("provider must not be null");

        String id = provider.getId();
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("IHookProvider.getId() must return a non-blank string");
        }

        IHookProvider existing = providers.putIfAbsent(id, provider);
        if (existing != null) {
            LOGGER.warn(
                "IHookProvider '{}' already registered (by {}). Ignoring duplicate registration by {}.",
                id,
                existing.getClass().getName(),
                provider.getClass().getName()
            );
            return;
        }

        LOGGER.info("[CreateRecipeHooks] Registered hook provider: '{}'", id);

        // Pass 'this' as IRegistrar — the provider sees only the api interface
        try {
            provider.register(this);
        } catch (Exception e) {
            LOGGER.error("Hook provider '{}' threw during register(): {}", id, e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------ //
    //  Convenience for providers to register listeners                     //
    // ------------------------------------------------------------------ //

    /**
     * Implements {@link IRegistrar}. Providers call this inside
     * {@link IHookProvider#register(IRegistrar)} to attach listeners.
     *
     * <p>Equivalent to {@link dev.createrecipehooks.api.CreateRecipeHooks#register}.
     */
    @Override
    public void addListener(IRecipeFinishedListener listener) {
        RecipeEventDispatcher.registerListener(listener);
    }

    // ------------------------------------------------------------------ //
    //  Diagnostics                                                         //
    // ------------------------------------------------------------------ //

    /** Returns the number of registered providers. Useful for testing. */
    public int providerCount() {
        return providers.size();
    }

    private RecipeHookRegistry() {}
}
