package xiuxian.cultivation;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;

public final class CultivationCapability {
    public static final Capability<CultivationData> CULTIVATION = CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation ID = new ResourceLocation("xiuxian", "cultivation");

    private CultivationCapability() {}

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(CultivationData.class);
    }

    public static final class Provider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
        private final CultivationData data = new CultivationData();
        private final LazyOptional<CultivationData> optional = LazyOptional.of(() -> data);

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
            return capability == CULTIVATION ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return data.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            data.deserializeNBT(tag);
        }

        public void invalidate() {
            optional.invalidate();
        }
    }
}
