package xiuxian.sect;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/**
 * Fast, server-free verification for the inner-realm persistence guard.
 * Run with the test runtime classpath when changing the generator's schema.
 */
public final class LuoxiaInnerRealmVerification {
    private LuoxiaInnerRealmVerification() {}

    public static void main(String[] args) {
        LuoxiaInnerRealmData original = new LuoxiaInnerRealmData();
        original.version = LuoxiaInnerRealmGenerator.VERSION;
        original.seed = 0x1234ABCDL;
        original.generated = true;
        original.marker("entry_gate", LuoxiaInnerDimension.ENTRY);
        original.marker("boss_center", new BlockPos(0, -38, 300));

        CompoundTag saved = original.save(new CompoundTag());
        LuoxiaInnerRealmData restored = LuoxiaInnerRealmData.load(saved);
        require(restored.generated, "generated guard did not survive NBT round-trip");
        require(restored.version == original.version, "generator version changed after reload");
        require(restored.seed == original.seed, "deterministic seed changed after reload");
        require(restored.markers.equals(original.markers), "landmark coordinates changed after reload");

        // This is the exact predicate used by ensureGenerated: a completed
        // versioned record must make a second generation pass a no-op.
        require(restored.generated && restored.version == LuoxiaInnerRealmGenerator.VERSION,
                "second load would regenerate the realm");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
