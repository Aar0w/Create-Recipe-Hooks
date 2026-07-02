package dev.createrecipehooks.core;

import dev.createrecipehooks.api.IRecipeFinishedListener;
import dev.createrecipehooks.api.RecipeFinishedContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central dispatcher — the <strong>only</strong> entry point from which all Mixin
 * hooks post their events.
 *
 * <h3>Architecture position</h3>
 * <pre>
 * Mixin (Layer 3) → RecipeEventDispatcher.dispatch(ctx) → IRecipeFinishedListener (Layer 1)
 *                                                        → NeoForgeAdapter → MinecraftForge.EVENT_BUS
 * </pre>
 *
 * <h3>Thread safety</h3>
 * Listener list is backed by a {@link CopyOnWriteArrayList}, making reads during dispatch
 * allocation-free and writes (registrations) safe from any thread.
 *
 * <h3>Dispatch is synchronous and blocking</h3>
 * {@link #dispatch} runs all listeners sequentially on the calling thread (server tick thread).
 * Listener execution time adds directly to the machine's processing time per tick.
 * Listeners are expected to return in microseconds, not milliseconds.
 * See {@link dev.createrecipehooks.api.IRecipeFinishedListener} for the async hand-off pattern.
 *
 * <h3>Error isolation</h3>
 * Exceptions thrown by individual listeners are caught, logged, and do not prevent
 * subsequent listeners from executing.
 *
 * <p><strong>Internal API</strong> — Mixin classes call this directly.
 * Addon authors should use {@link dev.createrecipehooks.api.CreateRecipeHooks}.
 */
public final class RecipeEventDispatcher {

    private static final Logger LOGGER = LogManager.getLogger("CreateRecipeHooks/Dispatcher");

    /**
     * Thread-safe list of registered listeners.
     * CopyOnWriteArrayList is ideal here: registrations happen once at startup
     * (cheap writes), dispatch happens every tick (cheap reads, no locking).
     */
    private static final CopyOnWriteArrayList<IRecipeFinishedListener> LISTENERS =
            new CopyOnWriteArrayList<>();

    // ------------------------------------------------------------------ //
    //  Registration (called by CreateRecipeHooks and RecipeHookRegistry)   //
    // ------------------------------------------------------------------ //

    /**
     * Adds a listener. Thread-safe. Called by
     * {@link dev.createrecipehooks.api.CreateRecipeHooks#register}.
     *
     * @param listener must not be null
     */
    public static void registerListener(IRecipeFinishedListener listener) {
        if (listener == null) throw new NullPointerException("listener must not be null");
        LISTENERS.add(listener);
    }

    // ------------------------------------------------------------------ //
    //  Dispatch (called by Mixin hooks only)                               //
    // ------------------------------------------------------------------ //

    /**
     * Dispatches a completed recipe event to all registered listeners.
     *
     * <p>Called exclusively by Mixin hook classes in Layer 3.
     * Always invoked on the server tick thread. Client-side calls are filtered
     * in the individual Mixin before reaching here.
     *
     * @param ctx the immutable context; must not be null
     */
    public static void dispatch(RecipeFinishedContext ctx) {
        if (ctx == null) {
            LOGGER.warn("dispatch() called with null context — ignoring");
            return;
        }

        // Snapshot the list reference; CopyOnWriteArrayList guarantees this is
        // a consistent view even if listeners are registered concurrently.
        List<IRecipeFinishedListener> snapshot = LISTENERS;

        for (IRecipeFinishedListener listener : snapshot) {
            try {
                listener.onRecipeFinished(ctx);
            } catch (Exception e) {
                LOGGER.error(
                    "Listener {} threw an exception for source={} recipeId={}: {}",
                    listener.getClass().getName(),
                    ctx.getSource(),
                    ctx.getRecipeId(),
                    e.getMessage(),
                    e
                );
            }
        }
    }

    // ------------------------------------------------------------------ //
    //  Diagnostics                                                         //
    // ------------------------------------------------------------------ //

    /** Returns the number of currently registered listeners. Useful for testing. */
    public static int listenerCount() {
        return LISTENERS.size();
    }

    private RecipeEventDispatcher() {}
}
