package dev.createrecipehooks.api;

import org.jetbrains.annotations.NotNull;

/**
 * API-safe registration surface passed to {@link IHookProvider#register}.
 *
 * <p>Hides the internal {@code RecipeHookRegistry} from addon authors — they interact
 * only with types declared in this {@code api} package.
 *
 * <p>The object passed at runtime IS the {@code RecipeHookRegistry} singleton,
 * but that detail is invisible to addon code.
 */
public interface IRegistrar {

    /**
     * Registers a listener that will be notified whenever any Create recipe completes.
     * Equivalent to {@link CreateRecipeHooks#register}.
     *
     * @param listener must not be {@code null}
     */
    void addListener(@NotNull IRecipeFinishedListener listener);
}
