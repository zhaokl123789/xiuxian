package xiuxian.cultivation;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public class CultivationData implements INBTSerializable<CompoundTag> {
    public static final int DATA_VERSION = 1;
    public static final String STARTING_TECHNIQUE = "xiuxian:basic_breathing";

    private boolean initialized;
    private FamilyOrigin familyOrigin = FamilyOrigin.MORTAL;
    private CultivationPath cultivationPath = CultivationPath.WANDERER;
    private String techniqueId = STARTING_TECHNIQUE;
    private CultivationRealm realm = CultivationRealm.QI_REFINING;
    private int realmLevel = 1;
    private int qi;
    private boolean meditating;
    private boolean hasLastPosition;
    private double lastX;
    private double lastY;
    private double lastZ;
    private int stillTicks;

    public boolean isInitialized() {
        return initialized;
    }

    public FamilyOrigin familyOrigin() {
        return familyOrigin;
    }

    public CultivationPath cultivationPath() {
        return cultivationPath;
    }

    public String techniqueId() {
        return techniqueId;
    }

    public CultivationRealm realm() {
        return realm;
    }

    public int realmLevel() {
        return realmLevel;
    }

    public int qi() {
        return qi;
    }

    public boolean isMeditating() {
        return meditating;
    }

    public int breakthroughCost() {
        return realm.breakthroughCost(realmLevel);
    }

    public void addQi(int amount) {
        if (amount > 0) {
            qi = (int) Math.min(Integer.MAX_VALUE, (long) qi + amount);
        }
    }

    public void begin(FamilyOrigin familyOrigin, CultivationPath cultivationPath) {
        this.initialized = true;
        this.familyOrigin = familyOrigin;
        this.cultivationPath = cultivationPath;
        this.techniqueId = STARTING_TECHNIQUE;
        this.realm = CultivationRealm.QI_REFINING;
        this.realmLevel = 1;
        this.qi = 0;
        stopMeditating();
    }

    public void startMeditating(double x, double y, double z) {
        meditating = true;
        hasLastPosition = true;
        lastX = x;
        lastY = y;
        lastZ = z;
        stillTicks = 0;
    }

    public void stopMeditating() {
        meditating = false;
        hasLastPosition = false;
        stillTicks = 0;
    }

    public boolean tickMeditation(double x, double y, double z) {
        if (!meditating) {
            return false;
        }

        if (!hasLastPosition) {
            hasLastPosition = true;
            lastX = x;
            lastY = y;
            lastZ = z;
            return false;
        }

        double dx = x - lastX;
        double dy = y - lastY;
        double dz = z - lastZ;
        lastX = x;
        lastY = y;
        lastZ = z;

        if (dx * dx + dy * dy + dz * dz > 0.0025D) {
            stillTicks = 0;
            return false;
        }

        stillTicks++;
        if (stillTicks >= 20) {
            stillTicks = 0;
            addQi(1);
            return true;
        }
        return false;
    }

    public boolean breakthrough() {
        int cost = breakthroughCost();
        if (!initialized || qi < cost) {
            return false;
        }

        CultivationRealm nextRealm = realmLevel == 9 ? realm.next() : realm;
        if (nextRealm == null) {
            return false;
        }

        qi -= cost;
        if (realmLevel == 9) {
            realm = nextRealm;
            realmLevel = 1;
        } else {
            realmLevel++;
        }
        return true;
    }

    public void copyFrom(CultivationData source) {
        deserializeNBT(source.serializeNBT());
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("dataVersion", DATA_VERSION);
        tag.putBoolean("initialized", initialized);
        if (initialized) {
            tag.putString("race", "human");
            tag.putString("familyOrigin", familyOrigin.id());
            tag.putString("cultivationPath", cultivationPath.id());
            tag.putString("technique", techniqueId);
            tag.putString("realm", realm.id());
            tag.putInt("realmLevel", realmLevel);
            tag.putInt("qi", qi);
        }
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        initialized = tag.getBoolean("initialized");
        if (!initialized) {
            return;
        }

        FamilyOrigin savedFamily = FamilyOrigin.byId(tag.getString("familyOrigin"));
        familyOrigin = savedFamily == null ? FamilyOrigin.MORTAL : savedFamily;
        CultivationPath savedPath = CultivationPath.byId(tag.getString("cultivationPath"));
        cultivationPath = savedPath == null ? CultivationPath.WANDERER : savedPath;
        techniqueId = tag.contains("technique") ? tag.getString("technique") : STARTING_TECHNIQUE;
        realm = CultivationRealm.byId(tag.getString("realm"));
        realmLevel = Math.max(1, Math.min(9, tag.getInt("realmLevel")));
        qi = Math.max(0, tag.getInt("qi"));
        stopMeditating();
    }
}
