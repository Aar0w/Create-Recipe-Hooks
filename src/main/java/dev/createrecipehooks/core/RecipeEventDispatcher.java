package dev.createrecipehooks.core;

import dev.createrecipehooks.api.BlockProcessedContext;
import dev.createrecipehooks.api.IBlockProcessedListener;
import dev.createrecipehooks.api.IRecipeFinishedListener;
import dev.createrecipehooks.api.RecipeFinishedContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// Central dispatcher: all mixin hooks post their events through this class.
// Dispatch is synchronous on the server tick thread; listener exceptions are caught and logged.
// Internal, addon authors should use CreateRecipeHooks instead.
public final class RecipeEventDispatcher {

    private static final Logger LOGGER = LogManager.getLogger("CreateRecipeHooks/Dispatcher");

    private static final CopyOnWriteArrayList<IRecipeFinishedListener> LISTENERS =
            new CopyOnWriteArrayList<>();

    private static final CopyOnWriteArrayList<IBlockProcessedListener> BLOCK_LISTENERS =
            new CopyOnWriteArrayList<>();

    private static final CopyOnWriteArrayList<IBlockProcessedListener> TREE_LISTENERS =
            new CopyOnWriteArrayList<>();

    // Adds a recipeFinished listener, thread-safe.
    public static void registerListener(IRecipeFinishedListener listener) {
        if (listener == null) throw new NullPointerException("listener must not be null");
        LISTENERS.add(listener);
    }

    // Adds a blockProcessed listener, thread-safe.
    public static void registerBlockProcessedListener(IBlockProcessedListener listener) {
        if (listener == null) throw new NullPointerException("listener must not be null");
        BLOCK_LISTENERS.add(listener);
    }

    // Adds a treeCut listener, thread-safe.
    public static void registerTreeCutListener(IBlockProcessedListener listener) {
        if (listener == null) throw new NullPointerException("listener must not be null");
        TREE_LISTENERS.add(listener);
    }

    // Fires a recipeFinished event. Called by the mixin hooks, server thread only.
    public static void dispatch(RecipeFinishedContext ctx) {
        if (ctx == null) {
            LOGGER.warn("dispatch() called with null context, ignoring");
            return;
        }

        for (IRecipeFinishedListener listener : LISTENERS) {
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

    // Fires a blockProcessed event. Called by the mixin hooks, server thread only.
    public static void dispatchBlockProcessed(BlockProcessedContext ctx) {
        dispatchBlockContext(ctx, BLOCK_LISTENERS, "blockProcessed");
    }

    // Fires a treeCut event. Called by the mixin hooks, server thread only.
    public static void dispatchTreeCut(BlockProcessedContext ctx) {
        dispatchBlockContext(ctx, TREE_LISTENERS, "treeCut");
    }

    private static void dispatchBlockContext(BlockProcessedContext ctx,
                                             List<IBlockProcessedListener> listeners,
                                             String channel) {
        if (ctx == null) {
            LOGGER.warn("{} dispatch called with null context, ignoring", channel);
            return;
        }

        for (IBlockProcessedListener listener : listeners) {
            try {
                listener.onBlockProcessed(ctx);
            } catch (Exception e) {
                LOGGER.error(
                    "{} listener {} threw an exception for source={} block={}: {}",
                    channel,
                    listener.getClass().getName(),
                    ctx.getSource(),
                    ctx.getBlockId(),
                    e.getMessage(),
                    e
                );
            }
        }
    }

    // Number of registered recipeFinished listeners, handy for logging.
    public static int listenerCount() {
        return LISTENERS.size();
    }

    private RecipeEventDispatcher() {}
}
