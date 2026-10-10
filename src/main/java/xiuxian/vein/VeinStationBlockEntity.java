package xiuxian.vein;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;
import net.minecraftforge.items.wrapper.*;
import xiuxian.block.VeinBlocks;
import xiuxian.recipe.*;

public final class VeinStationBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    private NonNullList<ItemStack> items;
    private int burn, progress, total = 100;
    private String recipeId = "";
    private ItemStack lastInput = ItemStack.EMPTY, lastReagent = ItemStack.EMPTY;
    private LazyOptional<IItemHandlerModifiable>[] sided;
    private LazyOptional<IItemHandler> unsided;
    public VeinStationBlockEntity(BlockPos pos, BlockState state) {
        super(VeinBlocks.STATION_ENTITY.get(), pos, state);
        items = NonNullList.withSize(cache() ? 27 : 5, ItemStack.EMPTY); initHandlers();
    }
    private boolean cache() { return ((VeinStationBlock)getBlockState().getBlock()).cache(); }
    private void initHandlers() { sided = SidedInvWrapper.create(this, Direction.UP, Direction.DOWN, Direction.NORTH); unsided = LazyOptional.of(() -> new InvWrapper(this)); }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER && !isRemoved()) return (side == null ? unsided : sided[side == Direction.UP ? 0 : side == Direction.DOWN ? 1 : 2]).cast();
        return super.getCapability(cap,side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); for (var handler:sided) handler.invalidate(); unsided.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); initHandlers(); }
    public int progress() { return progress; }
    public int burnTime() { return burn; }
    public final ContainerData data = new ContainerData() {
        @Override public int get(int index) { return switch(index) { case 0 -> progress; case 1 -> total; case 2 -> burn; case 3 -> level != null && level.hasNeighborSignal(worldPosition) ? 1 : 0; default -> 0; }; }
        @Override public void set(int index, int value) { }
        @Override public int getCount() { return 4; }
    };
    public static int fuelTime(ItemStack stack) { return ForgeHooks.getBurnTime(stack, RecipeType.SMELTING); }
    private static boolean fits(ItemStack current, ItemStack extra) {
        return extra.isEmpty() || (current.isEmpty() ? extra.getCount() <= extra.getMaxStackSize() :
                ItemStack.isSameItemSameTags(current,extra) && current.getCount()+extra.getCount() <= current.getMaxStackSize());
    }
    private static ItemStack merge(ItemStack current, ItemStack extra) {
        if (extra.isEmpty()) return current.copy();
        if (current.isEmpty()) return extra.copy();
        ItemStack next = current.copy(); next.grow(extra.getCount()); return next;
    }
    private ItemStack byproducts(VeinProcessingRecipe recipe) {
        ItemStack result = items.get(4).copy();
        ItemStack[] additions = {recipe.residue(), remaining(items.get(0),recipe.inputCount()), remaining(items.get(1),recipe.reagentCount())};
        for (ItemStack extra:additions) { if (!fits(result,extra)) return null; result=merge(result,extra); }
        return result;
    }
    private static ItemStack remaining(ItemStack stack, int count) {
        ItemStack remainder = stack.getCraftingRemainingItem();
        if (!remainder.isEmpty()) remainder.setCount(remainder.getCount()*count);
        return remainder;
    }
    public static void tick(Level level, BlockPos pos, BlockState state, VeinStationBlockEntity be) {
        if (be.cache()) return;
        var station = (VeinStationBlock)state.getBlock();
        int oldProgress = be.progress, oldBurn = be.burn;
        VeinProcessingRecipe recipe = null;
        if (!be.recipeId.isEmpty()) {
            var previous = level.getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation(be.recipeId)).orElse(null);
            if (previous instanceof VeinProcessingRecipe candidate && candidate.station().equals(station.station()) && candidate.matches(be,level)) recipe=candidate;
        }
        if (recipe == null && !be.items.get(0).isEmpty() && !be.items.get(1).isEmpty())
            recipe = level.getRecipeManager().getAllRecipesFor(XiuxianRecipes.VEIN_TYPE).stream()
                    .filter(r -> r.station().equals(station.station()) && r.matches(be,level)).findFirst().orElse(null);
        String id=recipe==null?"":recipe.getId().toString();
        boolean changedRecipe = !id.equals(be.recipeId);
        be.total = recipe == null ? 100 : recipe.ticks();
        if (!id.equals(be.recipeId) || !ItemStack.isSameItemSameTags(be.lastInput,be.items.get(0)) || !ItemStack.isSameItemSameTags(be.lastReagent,be.items.get(1))) be.progress=0;
        be.recipeId=id; be.lastInput=be.items.get(0).copy(); be.lastReagent=be.items.get(1).copy();
        boolean paused=level.hasNeighborSignal(pos);
        boolean canProcess=recipe!=null && fits(be.items.get(3),recipe.output()) && be.byproducts(recipe)!=null;
        if (canProcess && !paused) {
            if (be.burn<=0) {
                ItemStack fuel=be.items.get(2); int duration=fuelTime(fuel);
                ItemStack remainder=fuel.getCraftingRemainingItem();
                if (duration>0 && (fuel.getCount()==1 || fits(be.items.get(4),remainder))) {
                    fuel.shrink(1); be.burn=duration;
                    if (fuel.isEmpty()) be.items.set(2,remainder);
                    else be.items.set(4,merge(be.items.get(4),remainder));
                }
            }
            if (be.burn>0 && be.byproducts(recipe)!=null) {
                be.burn--; be.progress++;
                if (be.progress>=recipe.ticks()) {
                    // Verify all outputs together before consuming either input.
                    be.items.set(4,be.byproducts(recipe)); be.items.get(0).shrink(recipe.inputCount()); be.items.get(1).shrink(recipe.reagentCount());
                    be.items.set(3,merge(be.items.get(3),recipe.output())); be.progress=0;
                }
            }
        }
        boolean lit=canProcess && !paused && be.burn>0;
        if (state.getValue(VeinStationBlock.LIT)!=lit) level.setBlock(pos,state.setValue(VeinStationBlock.LIT,lit),3);
        if (oldProgress != be.progress || oldBurn != be.burn || changedRecipe) be.setChanged();
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); ContainerHelper.saveAllItems(tag,items); tag.putInt("Burn",burn); tag.putInt("Progress",progress);
        tag.putString("Recipe",recipeId); tag.put("LastInput",lastInput.save(new CompoundTag())); tag.put("LastReagent",lastReagent.save(new CompoundTag()));
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); items=NonNullList.withSize(cache()?27:5,ItemStack.EMPTY); ContainerHelper.loadAllItems(tag,items);
        burn=Math.max(0,tag.getInt("Burn")); progress=Math.max(0,tag.getInt("Progress")); recipeId=tag.getString("Recipe");
        lastInput=ItemStack.of(tag.getCompound("LastInput")); lastReagent=ItemStack.of(tag.getCompound("LastReagent"));
    }
    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot,int count) { ItemStack result=ContainerHelper.removeItem(items,slot,count); if (!result.isEmpty()) setChanged(); return result; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items,slot); }
    @Override public void setItem(int slot,ItemStack stack) { items.set(slot,stack); if (stack.getCount()>Math.min(64,stack.getMaxStackSize())) stack.setCount(Math.min(64,stack.getMaxStackSize())); setChanged(); }
    @Override public boolean stillValid(Player p) { return level!=null && level.getBlockEntity(worldPosition)==this && p.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64; }
    @Override public void clearContent() { items.clear(); setChanged(); }
    @Override public boolean canPlaceItem(int slot,ItemStack stack) {
        if (cache()) return true;
        if (slot == 2) return fuelTime(stack) > 0;
        if (slot > 1 || level == null) return false;
        String station = ((VeinStationBlock)getBlockState().getBlock()).station();
        return level.getRecipeManager().getAllRecipesFor(XiuxianRecipes.VEIN_TYPE).stream()
                .anyMatch(r -> r.station().equals(station) && (slot == 0 ? r.input() : r.reagent()).test(stack));
    }
    @Override public int[] getSlotsForFace(Direction side) { return cache()?java.util.stream.IntStream.range(0,27).toArray():side==Direction.UP?new int[]{0}:side==Direction.DOWN?new int[]{3,4}:new int[]{1,2}; }
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side) { return canPlaceItem(slot,stack); }
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side) { return cache() || (side==Direction.DOWN && (slot==3 || slot==4)); }
    @Override public Component getDisplayName() { return Component.translatable(getBlockState().getBlock().getDescriptionId()); }
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p) { return cache()?ChestMenu.threeRows(id,inv,this):new VeinStationMenu(id,inv,this); }
}
