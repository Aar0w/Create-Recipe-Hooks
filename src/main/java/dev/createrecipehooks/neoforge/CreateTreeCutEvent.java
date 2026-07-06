package dev.createrecipehooks.neoforge;

import dev.createrecipehooks.api.BlockProcessedContext;
import dev.createrecipehooks.api.RecipeSource;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Forge event fired whenever a Mechanical Saw (stationary or contraption actor) fells
 * a whole tree. One event per tree, with the log and leaf counts of the felled tree.
 *
 * <p>Deliberately <em>not</em> a subclass of {@link CreateBlockProcessedEvent}: the two
 * are mutually exclusive (a cut is either a tree or a lone block, never both) and must
 * not trigger each other's bus listeners.
 *
 * <p>Posted on <strong>{@code MinecraftForge.EVENT_BUS}</strong>. Not cancellable.
 * Fired on the server tick thread only.
 */
public final class CreateTreeCutEvent extends Event {

    private final BlockProcessedContext context;

    CreateTreeCutEvent(BlockProcessedContext context) {
        this.context = context;
    }

    @NotNull  public BlockProcessedContext getContext()  { return context; }
    @NotNull  public RecipeSource getSource()            { return context.getSource(); }
    @NotNull  public Level getLevel()                    { return context.getLevel(); }
    @NotNull  public BlockState getBlockState()          { return context.getBlockState(); }
    @NotNull  public ResourceLocation getBlockId()       { return context.getBlockId(); }
              public boolean isContraption()             { return context.isContraption(); }
              public int getLogCount()                   { return context.getLogCount(); }
              public int getLeafCount()                  { return context.getLeafCount(); }
    @Nullable public BlockPos getBlockPos()              { return context.getBlockPos(); }
    @NotNull  public Map<String, Object> getMetadata()   { return context.getMetadata(); }
              public long getTimestamp()                 { return context.getTimestamp(); }
}
