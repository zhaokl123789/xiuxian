package xiuxian.block;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.crafting.Recipe;
import xiuxian.cultivation.CultivationData;
import xiuxian.menu.AlchemyFurnaceMenu;
import xiuxian.recipe.AlchemyRecipe;
import xiuxian.recipe.XiuxianRecipes;

public class AlchemyFurnaceBlockEntity extends BlockEntity implements Container, MenuProvider {
    private static final int INPUT_SLOTS = 6;
    private static final int FUEL_SLOT = 6;
    private static final int OUTPUT_SLOT = 7;
    private final NonNullList<ItemStack> items = NonNullList.withSize(8, ItemStack.EMPTY);
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> burnTime;
                case 1 -> burnDuration;
                case 2 -> cookingProgress;
                case 3 -> cookingTotalTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> burnTime = value;
                case 1 -> burnDuration = value;
                case 2 -> cookingProgress = value;
                case 3 -> cookingTotalTime = value;
            }
        }

        @Override
        public int getCount() { return 4; }
    };
    private int burnTime;
    private int burnDuration;
    private int cookingProgress;
    private int cookingTotalTime = 200;
    private int pendingAlchemyExperience;

    public AlchemyFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(XiuxianBlockEntities.ALCHEMY_FURNACE.get(), pos, state);
    }

    public int tier() { return AlchemyFurnaceBlock.tier(getBlockState()); }

    @Override
    public Component getDisplayName() {
        return Component.translatable(switch (tier()) {
            case 2 -> "block.xiuxian.alchemy_furnace_spirit";
            case 3 -> "block.xiuxian.alchemy_furnace_earth";
            case 4 -> "block.xiuxian.alchemy_furnace_heaven";
            default -> "block.xiuxian.alchemy_furnace";
        });
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new AlchemyFurnaceMenu(containerId, inventory, tier(), this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putInt("burnTime", burnTime);
        tag.putInt("burnDuration", burnDuration);
        tag.putInt("cookingProgress", cookingProgress);
        tag.putInt("cookingTotalTime", cookingTotalTime);
        tag.putInt("pendingAlchemyExperience", pendingAlchemyExperience);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ContainerHelper.loadAllItems(tag, items);
        burnTime = tag.getInt("burnTime");
        burnDuration = tag.getInt("burnDuration");
        cookingProgress = tag.getInt("cookingProgress");
        cookingTotalTime = Math.max(1, tag.getInt("cookingTotalTime"));
        pendingAlchemyExperience = Math.max(0, tag.getInt("pendingAlchemyExperience"));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlchemyFurnaceBlockEntity furnace) {
        if (level.isClientSide) return;
        boolean wasLit = state.getValue(FurnaceBlock.LIT);
        if (furnace.burnTime > 0) furnace.burnTime--;

        Optional<AlchemyRecipe> recipeHolder = level.getRecipeManager()
                .getRecipeFor(XiuxianRecipes.typeForTier(furnace.tier()), furnace, level);
        AlchemyRecipe recipe = recipeHolder.orElse(null);
        boolean canCook = recipe != null && furnace.canAcceptResult(recipe.getResultItem(level.registryAccess()));
        boolean burning = furnace.burnTime > 0;

        if (!burning && canCook && !furnace.items.get(FUEL_SLOT).isEmpty()) {
            ItemStack fuel = furnace.items.get(FUEL_SLOT);
            int duration = fuelDuration(fuel);
            if (duration > 0) {
                furnace.burnTime = (int) Math.ceil(duration * tierFuelEfficiency(furnace.tier()));
                furnace.burnDuration = furnace.burnTime;
                burning = true;
                ItemStack remainder = fuel.getItem().getCraftingRemainingItem(fuel);
                fuel.shrink(1);
                if (fuel.isEmpty()) furnace.items.set(FUEL_SLOT, remainder);
            }
        }

        if (burning && canCook) {
            furnace.cookingTotalTime = Math.max(1, recipe.cookingTime());
            furnace.cookingProgress++;
            if (furnace.cookingProgress >= furnace.cookingTotalTime && recipe.consumeIngredients(furnace)) {
                furnace.cookingProgress = 0;
                ItemStack result = recipe.getResultItem(level.registryAccess());
                furnace.placeResult(result);
                furnace.pendingAlchemyExperience += Math.max(8, 12 + Math.round(recipe.experience() * 40.0F))
                        * result.getCount();
            }
        } else if (!canCook) {
            furnace.cookingProgress = 0;
        }

        boolean isLit = furnace.burnTime > 0;
        if (wasLit != isLit) {
            level.setBlock(pos, state.setValue(FurnaceBlock.LIT, isLit), 3);
        }
        furnace.setChanged();
    }

    private boolean canAcceptResult(ItemStack result) {
        ItemStack output = items.get(OUTPUT_SLOT);
        return output.isEmpty() || (ItemStack.isSameItemSameTags(output, result)
                && output.getCount() + result.getCount() <= Math.min(getMaxStackSize(), output.getMaxStackSize()));
    }

    private void placeResult(ItemStack result) {
        ItemStack output = items.get(OUTPUT_SLOT);
        if (output.isEmpty()) items.set(OUTPUT_SLOT, result.copy());
        else output.grow(result.getCount());
    }

    public int takePendingAlchemyExperience() {
        int experience = pendingAlchemyExperience;
        pendingAlchemyExperience = 0;
        setChanged();
        return experience;
    }

    private static double tierFuelEfficiency(int tier) {
        return switch (tier) {
            case 2 -> 1.15D;
            case 3 -> 1.30D;
            case 4 -> 1.50D;
            default -> 1.0D;
        };
    }

    private static int fuelDuration(ItemStack stack) {
        if (!AbstractFurnaceBlockEntity.isFuel(stack)) return 0;
        if (stack.is(Items.LAVA_BUCKET)) return 20000;
        if (stack.is(Items.COAL_BLOCK)) return 16000;
        if (stack.is(Items.BLAZE_ROD)) return 2400;
        if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) return 1600;
        if (stack.is(Items.DRIED_KELP_BLOCK)) return 4001;
        if (stack.is(ItemTags.LOGS) || stack.is(ItemTags.PLANKS) || stack.is(ItemTags.WOODEN_SLABS)) return 300;
        if (stack.is(ItemTags.SAPLINGS) || stack.is(Items.STICK) || stack.is(Items.BAMBOO)) return 100;
        return 200;
    }

    @Override
    public int getContainerSize() { return items.size(); }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= 0 && slot < INPUT_SLOTS || slot == FUEL_SLOT && AbstractFurnaceBlockEntity.isFuel(stack);
    }

    @Override
    public void setChanged() {
        super.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
        burnTime = 0;
        cookingProgress = 0;
        pendingAlchemyExperience = 0;
        setChanged();
    }

    public void awardAlchemyExperience(ServerPlayer player) {
        CultivationData data = xiuxian.cultivation.TaixuDimension.recoverTripData(player);
        int earned = takePendingAlchemyExperience();
        if (data == null || earned <= 0) return;
        int oldLevel = data.alchemyLevel();
        data.addAlchemyExperience(earned);
        player.sendSystemMessage(Component.literal("炼丹有成，炼丹经验 +" + earned + "（" + data.alchemyExperience()
                + "/" + data.alchemyExperienceToNextLevel() + "）"));
        if (data.alchemyLevel() > oldLevel) {
            player.sendSystemMessage(Component.literal("炼丹修为精进，当前炼丹师等级：" + data.alchemyLevel()));
        }
        xiuxian.network.XiuxianNetwork.syncCultivation(player, data);
    }
}
