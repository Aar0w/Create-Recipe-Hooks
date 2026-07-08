package dev.createrecipehooks.internal;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

// ThreadLocal carrier for the owner UUID of the machine currently processing. Machine
// block entity mixins set it before calling into Create's shared utility classes, the
// utility-class mixins read it when building the event, and it is cleared on return.
public final class CrhOwnerContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private CrhOwnerContext() {}

    public static void set(@Nullable UUID uuid) { CURRENT.set(uuid); }
    public static @Nullable UUID get()          { return CURRENT.get(); }
    public static void clear()                  { CURRENT.remove(); }
}
