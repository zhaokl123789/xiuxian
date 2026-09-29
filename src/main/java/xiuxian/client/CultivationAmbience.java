package xiuxian.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "xiuxian", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CultivationAmbience {
    private static final ResourceLocation SOUND = ResourceLocation.fromNamespaceAndPath("xiuxian", "meditation_ambience");
    private static SimpleSoundInstance activeSound;

    private CultivationAmbience() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        boolean shouldPlay = minecraft.player != null && CultivationClientState.isMeditating();
        if (!shouldPlay) {
            stop(minecraft);
            return;
        }

        if (activeSound == null || !minecraft.getSoundManager().isActive(activeSound)) {
            activeSound = new SimpleSoundInstance(SOUND, SoundSource.AMBIENT, 0.45F, 1.0F,
                    RandomSource.create(), true, 0, net.minecraft.client.resources.sounds.SoundInstance.Attenuation.NONE,
                    0.0D, 0.0D, 0.0D, true);
            minecraft.getSoundManager().play(activeSound);
        }
    }

    private static void stop(Minecraft minecraft) {
        if (activeSound != null) {
            minecraft.getSoundManager().stop(activeSound);
            activeSound = null;
        }
    }
}
