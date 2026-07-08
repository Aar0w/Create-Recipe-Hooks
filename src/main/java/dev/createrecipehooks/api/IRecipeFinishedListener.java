package dev.createrecipehooks.api;

/**
 * Listener for Create recipe completions. Register via
 * {@link CreateRecipeHooks#register(IRecipeFinishedListener)}.
 *
 * <p>Called synchronously on the server tick thread: return promptly and hand heavy work
 * (network, databases, files) off to your own worker thread, reading only the immutable
 * context fields there (see {@link RecipeFinishedContext}). Exceptions are caught and
 * logged; one broken listener does not prevent others from running.
 */
@FunctionalInterface
public interface IRecipeFinishedListener {

    /**
     * Called when a Create (or addon) recipe has successfully completed.
     *
     * <p>Called on the server tick thread. Must return promptly.
     * See threading contract in the interface javadoc.
     *
     * @param ctx Snapshot of the completed recipe. Never {@code null}.
     *            Safe to retain structurally; only immutable fields are safe cross-thread.
     *            See threading contract in the interface javadoc.
     */
    void onRecipeFinished(RecipeFinishedContext ctx);
}
