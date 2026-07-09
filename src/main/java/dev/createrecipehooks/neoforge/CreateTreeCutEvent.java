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

// Forge event fired when a Mechanical Saw fells a whole tree: one event per tree, with log and leaf counts.
// Lone blocks fire CreateBlockProcessedEvent instead.
// Posted on the Forge event bus, server side only.
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
