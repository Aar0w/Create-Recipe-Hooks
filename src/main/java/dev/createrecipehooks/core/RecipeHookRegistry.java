package dev.createrecipehooks.core;

import dev.createrecipehooks.api.IHookProvider;
import dev.createrecipehooks.api.IRecipeFinishedListener;
import dev.createrecipehooks.api.IRegistrar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;

// Registry for IHookProvider registrations, keyed by provider id (duplicates are rejected with a warning).
// Internal, addon authors should go through CreateRecipeHooks.registerProvider.
public final class RecipeHookRegistry implements IRegistrar {

    public static final RecipeHookRegistry INSTANCE = new RecipeHookRegistry();

    private static final Logger LOGGER = LogManager.getLogger("CreateRecipeHooks/Registry");

    private final ConcurrentHashMap<String, IHookProvider> providers = new ConcurrentHashMap<>();

    // Accepts a provider and immediately lets it register its listeners.
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

        try {
            provider.register(this);
        } catch (Exception e) {
            LOGGER.error("Hook provider '{}' threw during register(): {}", id, e.getMessage(), e);
        }
    }

    @Override
    public void addListener(IRecipeFinishedListener listener) {
        RecipeEventDispatcher.registerListener(listener);
    }

    // Number of registered providers, handy for logging.
    public int providerCount() {
        return providers.size();
    }

    private RecipeHookRegistry() {}
}
