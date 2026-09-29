package xiuxian.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import xiuxian.block.AlchemyFurnaceBlockEntity;

public class AlchemyFurnaceMenu extends AbstractContainerMenu {
    private static final int INPUT_SLOTS = 6;
    private static final int FUEL_SLOT = 6;
    private static final int OUTPUT_SLOT = 7;
    private static final int CONTAINER_SLOTS = 8;
    private final Container container;
    private final ContainerData data;
    private final int tier;

    public AlchemyFurnaceMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, extraData == null ? 1 : extraData.readVarInt(),
                new SimpleContainer(CONTAINER_SLOTS), new SimpleContainerData(4));
    }

    public AlchemyFurnaceMenu(int containerId, Inventory inventory, int tier, FriendlyByteBuf extraData) {
        this(containerId, inventory, extraData == null ? tier : extraData.readVarInt(),
                new SimpleContainer(CONTAINER_SLOTS), new SimpleContainerData(4));
    }

    public AlchemyFurnaceMenu(int containerId, Inventory inventory, int tier,
                              Container container, ContainerData data) {
        super(XiuxianMenus.forTier(tier).get(), containerId);
        checkContainerSize(container, CONTAINER_SLOTS);
        checkContainerDataCount(data, 4);
        this.container = container;
        this.data = data;
        this.tier = tier;
        container.startOpen(inventory.player);

        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new Slot(container, row * 3 + column, 27 + column * 20, 27 + row * 20));
            }
        }
        addSlot(new Slot(container, FUEL_SLOT, 27, 77) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AbstractFurnaceBlockEntity.isFuel(stack);
            }
        });
        addSlot(new Slot(container, OUTPUT_SLOT, 159, 48) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }

            @Override
            public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
                if (container instanceof AlchemyFurnaceBlockEntity furnace && player instanceof ServerPlayer serverPlayer) {
                    furnace.awardAlchemyExperience(serverPlayer);
                }
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 11 + column * 20, 111 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 11 + column * 20, 169));
        }
        addDataSlots(data);
    }

    public int tier() { return tier; }

    public int getBurnProgress() {
        int total = data.get(3);
        return total <= 0 ? 0 : Math.min(24, data.get(2) * 24 / total);
    }

    public int getLitProgress() {
        int total = data.get(1);
        return total <= 0 ? 0 : Math.min(14, data.get(0) * 14 / total);
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return original;
        ItemStack stack = slot.getItem();
        original = stack.copy();
        if (index < CONTAINER_SLOTS) {
            if (!moveItemStackTo(stack, CONTAINER_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (AbstractFurnaceBlockEntity.isFuel(stack)) {
            if (!moveItemStackTo(stack, FUEL_SLOT, FUEL_SLOT + 1, false)
                    && !moveItemStackTo(stack, 0, INPUT_SLOTS, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, INPUT_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}
