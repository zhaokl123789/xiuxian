package xiuxian.vein;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import xiuxian.block.OrientalDecorationBlock;

public final class VeinCraftingBlock extends OrientalDecorationBlock {
    public VeinCraftingBlock(Properties p, double[][] boxes) { super(p, boxes); }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) player.openMenu(new SimpleMenuProvider((id, inv, p) ->
                new CraftingMenu(id, inv, ContainerLevelAccess.create(level, pos)) {
                    @Override public boolean stillValid(Player user) {
                        return level.getBlockState(pos).is(VeinCraftingBlock.this) && user.distanceToSqr(pos.getX()+.5, pos.getY()+.5, pos.getZ()+.5) <= 64;
                    }
                }, Component.translatable(getDescriptionId())));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
