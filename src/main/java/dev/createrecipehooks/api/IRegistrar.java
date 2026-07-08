package dev.createrecipehooks.api;

import org.jetbrains.annotations.NotNull;

/**
 * What an IHookProvider gets to register its listeners with.
 */
public interface IRegistrar {

    /** Adds a recipe completion listener, same as CreateRecipeHooks.register. */
    void addListener(@NotNull IRecipeFinishedListener listener);
}
