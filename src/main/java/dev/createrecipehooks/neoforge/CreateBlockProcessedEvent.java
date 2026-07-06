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
 * Forge event fired whenever a Create machine processes a world block on the server:
 * a Mechanical Drill breaks a block, a Mechanical Harvester cuts a crop, or a
 * Mechanical Saw cuts a lone block that is not part of a tree.
 *
 * <p>Posted on <strong>{@code MinecraftForge.EVENT_BUS}</strong>, same as
 * {@link CreateRecipeFinishedEvent}. Not cancellable — the block is already gone.
 * Fired on the server tick thread only.
 *
 * <p>Tree felling is a separate event: {@link CreateTreeCutEvent}. The two never fire
 * for the same cut.
 */
public final class CreateBlockProcessedEvent extends Event {

    private final BlockProcessedContext context;

    CreateBlockProcessedEvent(BlockProcessedContext context) {
        this.context = context;
    }

    @NotNull  public BlockProcessedContext getContext()  { return context; }
    @NotNull  public RecipeSource getSource()            { return context.getSource(); }
    @NotNull  public Level getLevel()                    { return context.getLevel(); }
    @NotNull  public BlockState getBlockState()          { return context.getBlockState(); }
    @NotNull  public ResourceLocation getBlockId()       { return context.getBlockId(); }
              public boolean isContraption()             { return context.isContraption(); }
    @Nullable public BlockPos getBlockPos()              { return context.getBlockPos(); }
    @NotNull  public Map<String, Object> getMetadata()   { return context.getMetadata(); }
              public long getTimestamp()                 { return context.getTimestamp(); }
}
