package dev.createrecipehooks.internal;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * ThreadLocal carrier for the UUID of the player who owns the machine currently
 * executing a recipe. Set by each machine's BlockEntity mixin before calling into
 * the shared utility class (RecipeApplier, FillingBySpout, etc.), and cleared on return.
 *
 * <p>The utility-class mixins (MixinRecipeApplier, MixinFillingBySpout, etc.) read
 * this value when building the {@link dev.createrecipehooks.api.RecipeFinishedContext}
 * and add it as metadata under key {@code "createrecipehooks:owner_uuid"}.
 *
 * <p>Thread safety: ThreadLocal — each server thread has its own value. No cross-thread
 * sharing; Create always dispatches machine ticks on the server tick thread.
 */
public final class CrhOwnerContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private CrhOwnerContext() {}

    public static void set(@Nullable UUID uuid) { CURRENT.set(uuid); }
    public static @Nullable UUID get()          { return CURRENT.get(); }
    public static void clear()                  { CURRENT.remove(); }
}
