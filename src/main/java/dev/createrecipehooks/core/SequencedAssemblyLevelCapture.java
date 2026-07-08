package dev.createrecipehooks.core;

import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Carries the Level from getRecipe() to advance() on the same tick. Lives outside the
 * mixin package because injected code cannot reference mixin classes.
 */
public final class SequencedAssemblyLevelCapture {

    private static final ThreadLocal<Level> LEVEL = new ThreadLocal<>();

    public static void set(Level level) {
        LEVEL.set(level);
    }

    @Nullable
    public static Level current() {
        return LEVEL.get();
    }

    /** Called after dispatch in MixinSequencedAssemblyRecipe to prevent stale reads. */
    public static void clear() {
        LEVEL.remove();
    }

    private SequencedAssemblyLevelCapture() {}
}
