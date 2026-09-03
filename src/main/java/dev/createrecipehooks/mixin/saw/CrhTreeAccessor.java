package dev.createrecipehooks.mixin.saw;

import com.simibubi.create.content.kinetics.saw.TreeCutter;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

// Read access to the private log and leaf lists of a found tree, used to size treeCut events.
@Mixin(value = TreeCutter.Tree.class, remap = false)
public interface CrhTreeAccessor {

    @Accessor(value = "logs", remap = false)
    List<BlockPos> crh$getLogs();

    @Accessor(value = "leaves", remap = false)
    List<BlockPos> crh$getLeaves();
}
