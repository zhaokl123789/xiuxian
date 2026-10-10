package xiuxian.vein;

import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import xiuxian.menu.XiuxianMenus;

/** Shared input, reagent, fuel, output and remainder slots for six processing stations. */
public final class VeinStationMenu extends AbstractContainerMenu {
    private final Container station;
    private final ContainerData data;
    public VeinStationMenu(int id, Inventory inv) { this(id, inv, new SimpleContainer(5), new SimpleContainerData(4)); }
    public VeinStationMenu(int id, Inventory inv, VeinStationBlockEntity station) {
        this(id, inv, station, station.data);
    }
    private VeinStationMenu(int id, Inventory inv, Container station, ContainerData data) {
        super(XiuxianMenus.VEIN_STATION.get(), id); this.station = station; this.data = data; addDataSlots(data);
        for (int i=0;i<5;i++) addSlot(new Slot(station,i,16+32*i,32) {
            @Override public boolean mayPlace(ItemStack stack) { return station.canPlaceItem(getContainerSlot(), stack); }
        });
        for (int row=0;row<3;row++) for (int col=0;col<9;col++) addSlot(new Slot(inv,col+row*9+9,8+18*col,74+18*row));
        for (int col=0;col<9;col++) addSlot(new Slot(inv,col,8+18*col,132));
    }
    public int percent() { return Math.min(100, data.get(0)*100/Math.max(1,data.get(1))); }
    public boolean paused() { return data.get(3) == 1; }
    public int fuel() { return data.get(2); }
    @Override public boolean stillValid(Player p) { return station.stillValid(p); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(), original=stack.copy();
        if (index<5) { if (!moveItemStackTo(stack,5,41,true)) return ItemStack.EMPTY; }
        else if (!moveItemStackTo(stack,0,3,false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack); return original;
    }
}
