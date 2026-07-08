package dev.createrecipehooks.api;

import org.jetbrains.annotations.NotNull;

/**
 * API-safe registration surface passed to IHookProvider#register.
 *
 * Hides the internal RecipeHookRegistry from addon authors, they interact
 * only with types declared in this api package.
 *
 * The object passed at runtime IS the RecipeHookRegistry singleton,
 * but that detail is invisible to addon code.
 */
public interface IRegistrar {

    /**
     * Registers a listener that will be notified whenever any Create recipe completes.
     * Equivalent to CreateRecipeHooks#register.
     *
     * @param listener must not be null
     */
    void addListener(@NotNull IRecipeFinishedListener listener);
}
