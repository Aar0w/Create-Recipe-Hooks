package dev.createrecipehooks.neoforge;

import dev.createrecipehooks.api.BlockProcessedContext;
import dev.createrecipehooks.api.RecipeSource;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

// NeoForge event fired when a Drill breaks a block, a Harvester cuts a plant, or a Saw cuts a lone block that is not part of a tree.
// Tree felling fires CreateTreeCutEvent instead, never both.
// Posted on the game event bus, server side only.
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
