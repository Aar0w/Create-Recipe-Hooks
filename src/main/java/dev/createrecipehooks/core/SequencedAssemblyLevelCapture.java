package dev.createrecipehooks.core;

import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * ThreadLocal storage that carries the {@link Level} from
 * {@code SequencedAssemblyRecipe.getRecipe*()} into the later
 * {@code advance()} call within the same server tick thread.
 *
 * <p>Must live outside {@code dev.createrecipehooks.mixin.*} — that package is
 * owned by the Mixin transformer and classes there cannot be referenced directly
 * from injected bytecode running inside target classes.
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
