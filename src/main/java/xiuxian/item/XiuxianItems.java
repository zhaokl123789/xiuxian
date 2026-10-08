package xiuxian.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import xiuxian.block.XiuxianBlocks;
import xiuxian.cultivation.CultivationTechniques;
import xiuxian.cultivation.CultivationRealm;
import xiuxian.cultivation.CultivationSpells;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class XiuxianItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "xiuxian");

    public static final RegistryObject<Item> LUOXIA_CONSTRUCTION_DECREE = ITEMS.register("luoxia_construction_decree",
            LuoxiaConstructionDecreeItem::new);
    public static final RegistryObject<Item> LUOXIA_INNER_TEST_KEY = ITEMS.register("luoxia_inner_test_key",
            LuoxiaInnerTestKeyItem::new);
    public static final RegistryObject<Item> MORTAL_TOWN_INSPECTION_TOKEN = ITEMS.register("mortal_town_inspection_token",
            MortalTownInspectionItem::new);
    public static final RegistryObject<Item> DAOTAI_RESIDENCE_INSPECTION_TOKEN = ITEMS.register(
            "daotai_residence_inspection_token", DaotaiResidenceInspectionItem::new);
    public static final RegistryObject<Item> JINDAN_RESIDENCE_INSPECTION_TOKEN = ITEMS.register(
            "jindan_residence_inspection_token", JindanResidenceInspectionItem::new);
    public static final RegistryObject<Item> SECT_COMPLEX_INSPECTION_TOKEN = ITEMS.register(
            "sect_complex_inspection_token", SectComplexInspectionItem::new);

    public static final RegistryObject<Item> QI_GATHERING_PILL = ITEMS.register("qi_gathering_pill",
            () -> new QiGatheringPillItem(new Item.Properties().stacksTo(16), 20));
    public static final RegistryObject<Item> MID_QI_GATHERING_PILL = ITEMS.register("mid_qi_gathering_pill",
            () -> new QiGatheringPillItem(new Item.Properties().stacksTo(16), 60));
    public static final RegistryObject<Item> HIGH_QI_GATHERING_PILL = ITEMS.register("high_qi_gathering_pill",
            () -> new QiGatheringPillItem(new Item.Properties().stacksTo(16), 120));
    public static final RegistryObject<Item> SUPREME_QI_GATHERING_PILL = ITEMS.register("supreme_qi_gathering_pill",
            () -> new QiGatheringPillItem(new Item.Properties().stacksTo(16), 280));
    public static final RegistryObject<Item> TEST_BREAKTHROUGH_PILL = ITEMS.register("test_breakthrough_pill",
            () -> new QiGatheringPillItem(new Item.Properties().stacksTo(16), true));
    public static final RegistryObject<Item> TEST_FETAL_BREATH_PILL = ITEMS.register("test_fetal_breath_pill",
            () -> new RealmAscensionPillItem(CultivationRealm.FETAL_BREATH));
    public static final RegistryObject<Item> TEST_QI_REFINING_PILL = ITEMS.register("test_qi_refining_pill",
            () -> new RealmAscensionPillItem(CultivationRealm.QI_REFINING));
    public static final RegistryObject<Item> TEST_FOUNDATION_PILL = ITEMS.register("test_foundation_pill",
            () -> new RealmAscensionPillItem(CultivationRealm.FOUNDATION_ESTABLISHMENT));
    public static final RegistryObject<Item> TEST_PURPLE_MANSION_PILL = ITEMS.register("test_purple_mansion_pill",
            () -> new RealmAscensionPillItem(CultivationRealm.PURPLE_MANSION));
    public static final RegistryObject<Item> TEST_GOLDEN_CORE_PILL = ITEMS.register("test_golden_core_pill",
            () -> new RealmAscensionPillItem(CultivationRealm.GOLDEN_CORE));
    public static final RegistryObject<Item> TEST_DAO_TAI_PILL = ITEMS.register("test_dao_tai_pill",
            () -> new RealmAscensionPillItem(CultivationRealm.DAO_TAI));
    public static final RegistryObject<Item> QI_GATHERING_PILL_BASE = ITEMS.register("qi_gathering_pill_base",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MID_PILL_BASE = ITEMS.register("mid_pill_base",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> HIGH_PILL_BASE = ITEMS.register("high_pill_base",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SUPREME_PILL_BASE = ITEMS.register("supreme_pill_base",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> REJUVENATION_PILL_BASE = ingredient("rejuvenation_pill_base");
    public static final RegistryObject<Item> FASTING_PILL_BASE = ingredient("fasting_pill_base");
    public static final RegistryObject<Item> CLEARING_PILL_BASE = ingredient("clearing_pill_base");
    public static final RegistryObject<Item> PROTECTIVE_PILL_BASE = ingredient("protective_pill_base");
    public static final RegistryObject<Item> LIGHTNESS_PILL_BASE = ingredient("lightness_pill_base");
    public static final RegistryObject<Item> FIRE_WARDING_PILL_BASE = ingredient("fire_warding_pill_base");
    public static final RegistryObject<Item> WATER_BREATHING_PILL_BASE = ingredient("water_breathing_pill_base");
    public static final RegistryObject<Item> BRIGHT_SIGHT_PILL_BASE = ingredient("bright_sight_pill_base");
    public static final RegistryObject<Item> REJUVENATION_PILL = ITEMS.register("rejuvenation_pill",
            () -> new CultivationPillItem(new Item.Properties().stacksTo(16), 8, 0, false,
                    MobEffects.REGENERATION, 2400, 0, "暖意入腹，气血渐复。"));
    public static final RegistryObject<Item> FASTING_PILL = ITEMS.register("fasting_pill",
            () -> new CultivationPillItem(new Item.Properties().stacksTo(16), 0, 20, false,
                    null, 0, 0, "谷气充盈，饥意暂消。"));
    public static final RegistryObject<Item> CLEARING_PILL = ITEMS.register("clearing_pill",
            () -> new CultivationPillItem(new Item.Properties().stacksTo(16), 0, 0, true,
                    null, 0, 0, "药力行遍周身，浊秽之气渐散。"));
    public static final RegistryObject<Item> PROTECTIVE_PILL = ITEMS.register("protective_pill",
            () -> new CultivationPillItem(new Item.Properties().stacksTo(16), 0, 0, false,
                    MobEffects.ABSORPTION, 6000, 0, "药力化作护脉真元，暂护周身。"));
    public static final RegistryObject<Item> LIGHTNESS_PILL = ITEMS.register("lightness_pill",
            () -> new CultivationPillItem(new Item.Properties().stacksTo(16), 0, 0, false,
                    MobEffects.SLOW_FALLING, 6000, 0, "身轻如羽，坠势暂缓。"));
    public static final RegistryObject<Item> FIRE_WARDING_PILL = ITEMS.register("fire_warding_pill",
            () -> new CultivationPillItem(new Item.Properties().stacksTo(16), 0, 0, false,
                    MobEffects.FIRE_RESISTANCE, 12000, 0, "药力护住周身，暂避烈焰侵袭。"));
    public static final RegistryObject<Item> WATER_BREATHING_PILL = ITEMS.register("water_breathing_pill",
            () -> new CultivationPillItem(new Item.Properties().stacksTo(16), 0, 0, false,
                    MobEffects.WATER_BREATHING, 12000, 0, "一息绵长，可于水下从容行气。"));
    public static final RegistryObject<Item> BRIGHT_SIGHT_PILL = ITEMS.register("bright_sight_pill",
            () -> new CultivationPillItem(new Item.Properties().stacksTo(16), 0, 0, false,
                    MobEffects.NIGHT_VISION, 12000, 0, "灵台清明，幽暗之中亦可辨物。"));
    public static final RegistryObject<Item> SPIRIT_STONE = ITEMS.register("spirit_stone",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MID_SPIRIT_STONE = ITEMS.register("mid_spirit_stone",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> HIGH_SPIRIT_STONE = ITEMS.register("high_spirit_stone",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SUPREME_SPIRIT_STONE = ITEMS.register("supreme_spirit_stone",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SPIRIT_STONE_ORE_ITEM = ITEMS.register("spirit_stone_ore",
            () -> new BlockItem(XiuxianBlocks.SPIRIT_STONE_ORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> MID_SPIRIT_STONE_ORE_ITEM = ITEMS.register("mid_spirit_stone_ore",
            () -> new BlockItem(XiuxianBlocks.MID_SPIRIT_STONE_ORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> HIGH_SPIRIT_STONE_ORE_ITEM = ITEMS.register("high_spirit_stone_ore",
            () -> new BlockItem(XiuxianBlocks.HIGH_SPIRIT_STONE_ORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> SUPREME_SPIRIT_STONE_ORE_ITEM = ITEMS.register("supreme_spirit_stone_ore",
            () -> new BlockItem(XiuxianBlocks.SUPREME_SPIRIT_STONE_ORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> ALCHEMY_FURNACE_ITEM = ITEMS.register("alchemy_furnace",
            () -> new BlockItem(XiuxianBlocks.ALCHEMY_FURNACE.get(), new Item.Properties()));
    public static final RegistryObject<Item> ALCHEMY_FURNACE_SPIRIT_ITEM = ITEMS.register("alchemy_furnace_spirit",
            () -> new BlockItem(XiuxianBlocks.ALCHEMY_FURNACE_SPIRIT.get(), new Item.Properties()));
    public static final RegistryObject<Item> ALCHEMY_FURNACE_EARTH_ITEM = ITEMS.register("alchemy_furnace_earth",
            () -> new BlockItem(XiuxianBlocks.ALCHEMY_FURNACE_EARTH.get(), new Item.Properties()));
    public static final RegistryObject<Item> ALCHEMY_FURNACE_HEAVEN_ITEM = ITEMS.register("alchemy_furnace_heaven",
            () -> new BlockItem(XiuxianBlocks.ALCHEMY_FURNACE_HEAVEN.get(), new Item.Properties()));

    public static final RegistryObject<Item> BASIC_BREATHING_MANUAL = manual("manual_basic_breathing", CultivationTechniques.BASIC_BREATHING);
    public static final RegistryObject<Item> CLEAR_ORIGIN_MANUAL = manual("manual_clear_origin", CultivationTechniques.CLEAR_ORIGIN);
    public static final RegistryObject<Item> WUWEI_BREATH_MANUAL = manual("manual_wuwei_breath", CultivationTechniques.WUWEI_BREATH);
    public static final RegistryObject<Item> EMBRACE_ONE_MANUAL = manual("manual_embrace_one", CultivationTechniques.EMBRACE_ONE);
    public static final RegistryObject<Item> VALLEY_SPIRIT_MANUAL = manual("manual_valley_spirit", CultivationTechniques.VALLEY_SPIRIT);
    public static final RegistryObject<Item> WATER_VIRTUE_MANUAL = manual("manual_water_virtue", CultivationTechniques.WATER_VIRTUE);
    public static final RegistryObject<Item> RETURN_TO_ROOT_MANUAL = manual("manual_return_to_root", CultivationTechniques.RETURN_TO_ROOT);
    public static final RegistryObject<Item> MYSTERIOUS_GATE_MANUAL = manual("manual_mysterious_gate", CultivationTechniques.MYSTERIOUS_GATE);
    public static final RegistryObject<Item> LESS_PRIVATE_MANUAL = manual("manual_less_private", CultivationTechniques.LESS_PRIVATE);
    public static final RegistryObject<Item> FEMALE_SPIRIT_MANUAL = manual("manual_female_spirit", CultivationTechniques.FEMALE_SPIRIT);
    public static final RegistryObject<Item> KNOW_STOP_MANUAL = manual("manual_know_stop", CultivationTechniques.KNOW_STOP);
    public static final RegistryObject<Item> RETURN_NATURE_MANUAL = manual("manual_return_nature", CultivationTechniques.RETURN_NATURE);
    public static final RegistryObject<Item> FIVE_ELEMENTS_RETURN_MANUAL = manual("manual_five_elements_return", CultivationTechniques.FIVE_ELEMENTS_RETURN);
    public static final RegistryObject<Item> SWORD_INTENT_MANUAL = manual("manual_sword_intent", CultivationTechniques.SWORD_INTENT);
    public static final RegistryObject<Item> IRON_BODY_MANUAL = manual("manual_iron_body", CultivationTechniques.IRON_BODY);
    public static final RegistryObject<Item> VOID_SHADOW_MANUAL = manual("manual_void_shadow", CultivationTechniques.VOID_SHADOW);
    public static final RegistryObject<Item> HEAVENLY_CYCLE_MANUAL = manual("manual_heavenly_cycle", CultivationTechniques.HEAVENLY_CYCLE);
    public static final RegistryObject<Item> STAR_FORGER_MANUAL = manual("manual_star_forger", CultivationTechniques.STAR_FORGER);
    public static final RegistryObject<Item> BRIGHT_EYES_SPELL_MANUAL = spellManual("spell_manual_bright_eyes", "xiuxian:bright_eyes");
    public static final RegistryObject<Item> FROST_NEEDLE_SPELL_MANUAL = spellManual("spell_manual_frost_needle", "xiuxian:frost_needle");
    public static final RegistryObject<Item> WIND_BLADE_SPELL_MANUAL = spellManual("spell_manual_wind_blade", "xiuxian:wind_blade");
    public static final RegistryObject<Item> STONE_PRICK_SPELL_MANUAL = spellManual("spell_manual_stone_prick", "xiuxian:stone_prick");
    public static final RegistryObject<Item> THUNDER_SPARK_SPELL_MANUAL = spellManual("spell_manual_thunder_spark", "xiuxian:thunder_spark");
    public static final RegistryObject<Item> SPIRIT_BIND_SPELL_MANUAL = spellManual("spell_manual_spirit_bind", "xiuxian:spirit_bind");
    public static final RegistryObject<Item> SOUL_SHAKE_SPELL_MANUAL = spellManual("spell_manual_soul_shake", "xiuxian:soul_shake");
    public static final RegistryObject<Item> SMALL_REJUVENATION_SPELL_MANUAL = spellManual("spell_manual_small_rejuvenation", "xiuxian:small_rejuvenation");
    public static final RegistryObject<Item> TRUE_QI_RETURN_SPELL_MANUAL = spellManual("spell_manual_true_qi_return", "xiuxian:true_qi_return");
    public static final RegistryObject<Item> REPEL_WAVE_SPELL_MANUAL = spellManual("spell_manual_repel_wave", "xiuxian:repel_wave");
    public static final List<RegistryObject<Item>> SPELL_MANUALS = List.of(
            BRIGHT_EYES_SPELL_MANUAL, FROST_NEEDLE_SPELL_MANUAL, WIND_BLADE_SPELL_MANUAL,
            STONE_PRICK_SPELL_MANUAL, THUNDER_SPARK_SPELL_MANUAL, SPIRIT_BIND_SPELL_MANUAL,
            SOUL_SHAKE_SPELL_MANUAL, SMALL_REJUVENATION_SPELL_MANUAL, TRUE_QI_RETURN_SPELL_MANUAL,
            REPEL_WAVE_SPELL_MANUAL);
    private static final Set<String> LEGACY_MANUAL_IDS = Set.of(
            "xiuxian:basic_breathing", "xiuxian:clear_origin", "xiuxian:wuwei_breath",
            "xiuxian:embrace_one", "xiuxian:valley_spirit", "xiuxian:water_virtue",
            "xiuxian:return_to_root", "xiuxian:mysterious_gate", "xiuxian:less_private",
            "xiuxian:female_spirit", "xiuxian:know_stop", "xiuxian:return_nature",
            "xiuxian:five_elements_return", "xiuxian:sword_intent", "xiuxian:iron_body",
            "xiuxian:void_shadow", "xiuxian:heavenly_cycle", "xiuxian:star_forger");
    private static final List<RegistryObject<Item>> EXTENDED_CULTIVATION_MANUALS =
            CultivationTechniques.all().stream()
                    .filter(technique -> !LEGACY_MANUAL_IDS.contains(technique.id()))
                    .map(technique -> manual("manual_" + technique.id().substring(technique.id().indexOf(':') + 1), technique))
                    .toList();
    public static final List<RegistryObject<Item>> CULTIVATION_MANUALS = combineManuals(
            BASIC_BREATHING_MANUAL, CLEAR_ORIGIN_MANUAL, WUWEI_BREATH_MANUAL, EMBRACE_ONE_MANUAL,
            VALLEY_SPIRIT_MANUAL, WATER_VIRTUE_MANUAL, RETURN_TO_ROOT_MANUAL, MYSTERIOUS_GATE_MANUAL,
            LESS_PRIVATE_MANUAL, FEMALE_SPIRIT_MANUAL, KNOW_STOP_MANUAL, RETURN_NATURE_MANUAL,
            FIVE_ELEMENTS_RETURN_MANUAL, SWORD_INTENT_MANUAL, IRON_BODY_MANUAL,
            VOID_SHADOW_MANUAL, HEAVENLY_CYCLE_MANUAL, STAR_FORGER_MANUAL);
    private static final Map<String, RegistryObject<Item>> MANUALS_BY_TECHNIQUE = indexManuals();

    private static List<RegistryObject<Item>> combineManuals(RegistryObject<Item>... legacy) {
        List<RegistryObject<Item>> result = new ArrayList<>(List.of(legacy));
        result.addAll(EXTENDED_CULTIVATION_MANUALS);
        return List.copyOf(result);
    }

    private static Map<String, RegistryObject<Item>> indexManuals() {
        Map<String, RegistryObject<Item>> result = new LinkedHashMap<>();
        for (int i = 0; i < CultivationTechniques.all().size(); i++) {
            result.put(CultivationTechniques.all().get(i).id(), CULTIVATION_MANUALS.get(i));
        }
        return Map.copyOf(result);
    }

    public static RegistryObject<Item> manualForTechnique(String techniqueId) {
        return MANUALS_BY_TECHNIQUE.get(techniqueId);
    }

    private XiuxianItems() {}

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        modEventBus.addListener(XiuxianItems::addCreativeTabContents);
    }

    private static void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(QI_GATHERING_PILL);
            event.accept(MID_QI_GATHERING_PILL);
            event.accept(HIGH_QI_GATHERING_PILL);
            event.accept(SUPREME_QI_GATHERING_PILL);
            event.accept(TEST_BREAKTHROUGH_PILL);
            event.accept(TEST_FETAL_BREATH_PILL);
            event.accept(TEST_QI_REFINING_PILL);
            event.accept(TEST_FOUNDATION_PILL);
            event.accept(TEST_PURPLE_MANSION_PILL);
            event.accept(TEST_GOLDEN_CORE_PILL);
            event.accept(TEST_DAO_TAI_PILL);
            event.accept(QI_GATHERING_PILL_BASE);
            event.accept(MID_PILL_BASE);
            event.accept(HIGH_PILL_BASE);
            event.accept(SUPREME_PILL_BASE);
            event.accept(REJUVENATION_PILL_BASE);
            event.accept(FASTING_PILL_BASE);
            event.accept(CLEARING_PILL_BASE);
            event.accept(PROTECTIVE_PILL_BASE);
            event.accept(LIGHTNESS_PILL_BASE);
            event.accept(FIRE_WARDING_PILL_BASE);
            event.accept(WATER_BREATHING_PILL_BASE);
            event.accept(BRIGHT_SIGHT_PILL_BASE);
            event.accept(REJUVENATION_PILL);
            event.accept(FASTING_PILL);
            event.accept(CLEARING_PILL);
            event.accept(PROTECTIVE_PILL);
            event.accept(LIGHTNESS_PILL);
            event.accept(FIRE_WARDING_PILL);
            event.accept(WATER_BREATHING_PILL);
            event.accept(BRIGHT_SIGHT_PILL);
            event.accept(SPIRIT_STONE);
            event.accept(MID_SPIRIT_STONE);
            event.accept(HIGH_SPIRIT_STONE);
            event.accept(SUPREME_SPIRIT_STONE);
        } else if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(SPIRIT_STONE_ORE_ITEM);
            event.accept(MID_SPIRIT_STONE_ORE_ITEM);
            event.accept(HIGH_SPIRIT_STONE_ORE_ITEM);
            event.accept(SUPREME_SPIRIT_STONE_ORE_ITEM);
            event.accept(ALCHEMY_FURNACE_ITEM);
            event.accept(ALCHEMY_FURNACE_SPIRIT_ITEM);
            event.accept(ALCHEMY_FURNACE_EARTH_ITEM);
            event.accept(ALCHEMY_FURNACE_HEAVEN_ITEM);
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(LUOXIA_CONSTRUCTION_DECREE);
            event.accept(LUOXIA_INNER_TEST_KEY);
            event.accept(MORTAL_TOWN_INSPECTION_TOKEN);
            event.accept(DAOTAI_RESIDENCE_INSPECTION_TOKEN);
            event.accept(JINDAN_RESIDENCE_INSPECTION_TOKEN);
            event.accept(SECT_COMPLEX_INSPECTION_TOKEN);
            CULTIVATION_MANUALS.forEach(event::accept);
            SPELL_MANUALS.forEach(event::accept);
        }
    }

    private static RegistryObject<Item> manual(String id, xiuxian.cultivation.CultivationTechnique technique) {
        return ITEMS.register(id, () -> new TechniqueManualItem(technique.id()));
    }

    private static RegistryObject<Item> spellManual(String id, String spellId) {
        return ITEMS.register(id, () -> new SpellManualItem(spellId));
    }

    private static RegistryObject<Item> ingredient(String id) {
        return ITEMS.register(id, () -> new Item(new Item.Properties()));
    }
}
