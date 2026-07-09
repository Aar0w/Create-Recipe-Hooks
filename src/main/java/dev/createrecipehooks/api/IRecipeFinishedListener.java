package dev.createrecipehooks.api;

// Listener for Create recipe completions, registered via CreateRecipeHooks.register.
// Called synchronously on the server tick thread, so return promptly and hand heavy work off to your own worker thread.
@FunctionalInterface
public interface IRecipeFinishedListener {

    // Called on the server tick thread whenever a recipe completes.
    void onRecipeFinished(RecipeFinishedContext ctx);
}
