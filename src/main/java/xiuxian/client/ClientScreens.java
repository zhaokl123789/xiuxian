package xiuxian.client;

import net.minecraft.client.Minecraft;
import xiuxian.client.screen.IdentityCreationScreen;

public final class ClientScreens {
    private ClientScreens() {}

    public static void openIdentityScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof IdentityCreationScreen)) {
            minecraft.setScreen(new IdentityCreationScreen());
        }
    }

    public static void closeIdentityScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof IdentityCreationScreen) {
            minecraft.setScreen(null);
        }
    }
}
