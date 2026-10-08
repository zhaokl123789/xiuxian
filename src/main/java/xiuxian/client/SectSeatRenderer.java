package xiuxian.client;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xiuxian.block.SectBlocks;
import xiuxian.entity.SectSeatEntity;

@Mod.EventBusSubscriber(modid = "xiuxian", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SectSeatRenderer extends EntityRenderer<SectSeatEntity> {
    public SectSeatRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override public ResourceLocation getTextureLocation(SectSeatEntity entity) {
        return new ResourceLocation("minecraft", "textures/block/oak_planks.png");
    }

    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SectBlocks.SEAT_ENTITY.get(), SectSeatRenderer::new);
    }
}
