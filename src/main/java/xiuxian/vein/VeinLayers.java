package xiuxian.vein;

/** Stable vertical material bands for the future per-chunk underground generator. */
public final class VeinLayers {
    public static final String[] IDS = {"dew", "frost", "azure", "jade", "ember", "cinnabar", "gold", "violet", "origin"};
    private VeinLayers() {}
    public static int atY(int y) { return y < -64 || y > 63 ? 0 : Math.min(9, (63 - y) / 14 + 1); }
    public static String id(int layer) { return IDS[layer - 1]; }
}
