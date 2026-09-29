package xiuxian.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import xiuxian.client.CultivationClientState;
import xiuxian.block.AlchemyFurnaceTier;
import xiuxian.menu.AlchemyFurnaceMenu;

public class AlchemyFurnaceScreen extends AbstractContainerScreen<AlchemyFurnaceMenu> {
    private static final int GOLD = 0xFFD4B46A;

    public AlchemyFurnaceScreen(AlchemyFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 206;
        imageHeight = 194;
        titleLabelX = 9;
        titleLabelY = 7;
        inventoryLabelX = 11;
        inventoryLabelY = 99;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xF0161A17);
        graphics.fill(x, y, x + imageWidth, y + 2, GOLD);
        graphics.fill(x, y + imageHeight - 2, x + imageWidth, y + imageHeight, GOLD);
        graphics.fill(x, y, x + 2, y + imageHeight, 0xFF78633D);
        graphics.fill(x + imageWidth - 2, y, x + imageWidth, y + imageHeight, 0xFF78633D);
        graphics.fill(x + 9, y + 24, x + imageWidth - 9, y + 25, 0x997E6842);
        graphics.fill(x + 9, y + 95, x + imageWidth - 9, y + 96, 0x997E6842);

        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 3; column++) {
                drawSlotFrame(graphics, x + 26 + column * 20, y + 26 + row * 20);
            }
        }
        drawSlotFrame(graphics, x + 26, y + 76);
        drawSlotFrame(graphics, x + 158, y + 47);

        int flame = menu.getLitProgress();
        if (flame > 0) {
            graphics.fill(x + 90, y + 72 - flame, x + 97, y + 72, 0xFFFFAA45);
            graphics.fill(x + 92, y + 72 - flame / 2, x + 95, y + 72, 0xFFFFE2A0);
        }
        int progress = menu.getBurnProgress();
        graphics.fill(x + 101, y + 50, x + 141, y + 57, 0xFF101310);
        graphics.fill(x + 102, y + 51, x + 102 + Math.min(38, progress), y + 56, 0xFF9BC9AE);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFE9E1CE, false);
        String level = "炼丹师 " + CultivationClientState.alchemyLevel() + " 级 · 需";
        String tier = switch (menu.tier()) {
            case 2 -> "灵品炉";
            case 3 -> "地品炉";
            case 4 -> "天品炉";
            default -> "凡品炉";
        };
        graphics.drawString(font, tier, imageWidth - 9 - font.width(tier), 7, GOLD, false);
        graphics.drawString(font, level + AlchemyFurnaceTier.byLevel(menu.tier()).requiredAlchemyLevel()
                + " 级", 10, 15,
                0xFFC9C3B5, false);
        graphics.drawString(font, "燃料", 48, 79, 0xFFC9C3B5, false);
        graphics.drawString(font, "丹成", 164, 66, 0xFFE4DDC9, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFE2DCCB, false);
    }

    private void drawSlotFrame(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF806A43);
        graphics.fill(x, y, x + 16, y + 16, 0xFF292D28);
        graphics.fill(x + 1, y + 1, x + 15, y + 2, 0xFF171B18);
    }
}
