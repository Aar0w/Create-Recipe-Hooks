package dev.createrecipehooks.fabric.mixin.saw;

import com.simibubi.create.content.kinetics.saw.TreeCutter;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

// Read access to TreeCutter.Tree's private block lists. Used by the saw mixins
// to distinguish a felled tree (non-empty logs) from a lone cut block, and to
// report the exact tree size without intercepting drops.
@Mixin(TreeCutter.Tree.class)
public interface CrhTreeAccessor {

    @Accessor("logs")
    List<BlockPos> crh$getLogs();

    @Accessor("leaves")
    List<BlockPos> crh$getLeaves();
}
