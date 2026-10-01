package xiuxian.cultivation;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.RandomSource;

/** Original manuals plus several linked lineages for combat, body and void cultivation. */
public final class CultivationTechniques {
    public static final CultivationTechnique BASIC_BREATHING = technique(
            "basic_breathing", "吐纳引气诀", "守静抱一，气从微息而生；积涓流而成真元。",
            "端坐安身，鼻息绵长；吸时意守脐下，呼时散去杂念。",
            CultivationRealm.FETAL_BREATH, CultivationRealm.FETAL_BREATH, 0, 100,
            CultivationTechnique.Aptitude.SPIRITUAL_ROOT, 100, 0.00F, 0.0D);
    public static final CultivationTechnique CLEAR_ORIGIN = technique(
            "clear_origin", "澄源返照篇", "涤除玄览，返观其心；心有定处，外气方能入内。",
            "晨昏各行九息，觉察杂念而不追随，待心湖自澄。",
            CultivationRealm.FETAL_BREATH, CultivationRealm.QI_REFINING, 1, 108,
            CultivationTechnique.Aptitude.COMPREHENSION, 105, 0.00F, 0.0D);
    public static final CultivationTechnique WUWEI_BREATH = technique(
            "wuwei_breath", "无为听息章", "无为而行，不强引灵；不求速成，气自循其常。",
            "只听息来息往，不计数、不憋气，任呼吸复归自然。",
            CultivationRealm.QI_REFINING, CultivationRealm.FOUNDATION_ESTABLISHMENT, 2, 112,
            CultivationTechnique.Aptitude.FORTUNE, 88, 0.00F, 1.0D);
    public static final CultivationTechnique EMBRACE_ONE = technique(
            "embrace_one", "抱一守中诀", "载营魄抱一，形神相守；不令心意逐境而散。",
            "舌抵上腭，意守中宫；神思纷起时，缓缓收回一念。",
            CultivationRealm.QI_REFINING, CultivationRealm.FOUNDATION_ESTABLISHMENT, 2, 115,
            CultivationTechnique.Aptitude.COMPREHENSION, 92, 0.02F, 0.0D);
    public static final CultivationTechnique VALLEY_SPIRIT = technique(
            "valley_spirit", "谷神养息篇", "谷神不竭，虚而能受；不争一时之满，方得绵长。",
            "胸腹放松如空谷，吸长呼缓；以细水长流代替强行纳气。",
            CultivationRealm.FOUNDATION_ESTABLISHMENT, CultivationRealm.PURPLE_MANSION, 3, 122,
            CultivationTechnique.Aptitude.CONSTITUTION, 96, 0.00F, 2.0D);
    public static final CultivationTechnique WATER_VIRTUE = technique(
            "water_virtue", "上善若水篇", "上善若水，利物而不争；柔能容纳，亦能化去阻滞。",
            "息沉如水，周流四肢；遇滞不逆冲，待其松解再续行气。",
            CultivationRealm.FOUNDATION_ESTABLISHMENT, CultivationRealm.PURPLE_MANSION, 4, 128,
            CultivationTechnique.Aptitude.SPIRITUAL_ROOT, 90, 0.05F, 0.0D);
    public static final CultivationTechnique RETURN_TO_ROOT = technique(
            "return_to_root", "复归于婴诀", "专气致柔，复归于婴；收外驰之神，守初生之真。",
            "意随呼吸归于下腹，四肢松柔；不以强念扰动气机。",
            CultivationRealm.PURPLE_MANSION, CultivationRealm.GOLDEN_CORE, 5, 136,
            CultivationTechnique.Aptitude.CONSTITUTION, 102, 0.03F, 0.0D);
    public static final CultivationTechnique MYSTERIOUS_GATE = technique(
            "mysterious_gate", "玄牝门观篇", "玄牝之门，是谓天地根；虚静之间，自有生机往来。",
            "观息在出入之间，不执有无；每次吐纳都留一分从容。",
            CultivationRealm.PURPLE_MANSION, CultivationRealm.GOLDEN_CORE, 6, 144,
            CultivationTechnique.Aptitude.COMPREHENSION, 100, 0.00F, 1.0D);
    public static final CultivationTechnique LESS_PRIVATE = technique(
            "less_private", "少私寡欲录", "少私寡欲，则心不外驰；所求渐简，气息渐匀。",
            "入定先放下挂念，心轻则息匀；息匀之后再缓缓纳气。",
            CultivationRealm.GOLDEN_CORE, CultivationRealm.DAO_TAI, 7, 150,
            CultivationTechnique.Aptitude.FORTUNE, 78, 0.00F, 3.0D);
    public static final CultivationTechnique FEMALE_SPIRIT = technique(
            "female_spirit", "知雄守雌篇", "知其雄，守其雌；处下而不失其守，柔中自有韧性。",
            "收敛外放之势，放松肩背与腰腹；以柔和吐纳积蓄根本。",
            CultivationRealm.GOLDEN_CORE, CultivationRealm.DAO_TAI, 8, 158,
            CultivationTechnique.Aptitude.CONSTITUTION, 85, 0.08F, 0.0D);
    public static final CultivationTechnique KNOW_STOP = technique(
            "know_stop", "知止不殆篇", "知足不辱，知止不殆；进退有度，气机方不躁进。",
            "每行十二息便稍作停歇，心念安稳后再续，忌强行催迫。",
            CultivationRealm.GOLDEN_CORE, CultivationRealm.DAO_TAI, 9, 166,
            CultivationTechnique.Aptitude.FORTUNE, 72, 0.04F, 2.0D);
    public static final CultivationTechnique RETURN_NATURE = technique(
            "return_nature", "见素抱朴章", "见素抱朴，少饰返真；不逐奇景，持平常心修平常息。",
            "只守一呼一吸，不求异象；久久行之，根基自会渐厚。",
            CultivationRealm.GOLDEN_CORE, CultivationRealm.DAO_TAI, 10, 174,
            CultivationTechnique.Aptitude.COMPREHENSION, 65, 0.02F, 4.0D);
    public static final CultivationTechnique FIVE_ELEMENTS_RETURN = special(
            "five_elements_return", "五行归元诀", "五行相生，五脏为炉，纳五方灵机归于一元。",
            "按木火土金水次序运转周天，根基越杂，归元越稳。", CultivationRealm.FOUNDATION_ESTABLISHMENT,
            CultivationRealm.DAO_TAI, 5, 182, CultivationTechnique.Aptitude.SPIRITUAL_ROOT, 82,
            0.04F, 12.0D, 145, 180, 6, 1.18F, 3, 0.01D, "中和", "五行", "五行归元");
    public static final CultivationTechnique SWORD_INTENT = special(
            "sword_intent", "太白剑心篇", "剑意先于剑招，斩念、斩妄、斩尽阻道之物。",
            "静坐听剑鸣，战斗中只留一线锋芒；心乱则反噬经脉。", CultivationRealm.QI_REFINING,
            CultivationRealm.DAO_TAI, 6, 176, CultivationTechnique.Aptitude.COMPREHENSION, 86,
            0.01F, 5.0D, 95, 120, 4, 1.30F, 9, 0.025D, "守正", "金", "剑修");
    public static final CultivationTechnique IRON_BODY = special(
            "iron_body", "玄岳锻体篇", "以山岳为师，以气血为鼎，百炼筋骨而后炼神。",
            "每日以真气淬体，受击越重越能积蓄反震之力，但吐纳速度较慢。", CultivationRealm.FOUNDATION_ESTABLISHMENT,
            CultivationRealm.GOLDEN_CORE, 5, 128, CultivationTechnique.Aptitude.CONSTITUTION, 112,
            0.12F, 32.0D, 170, 80, 3, 0.88F, 8, 0.0D, "厚土", "土", "体修");
    public static final CultivationTechnique VOID_SHADOW = special(
            "void_shadow", "太虚遁影经", "身化虚影，借太虚罅隙避开因果与锋芒。",
            "以真气维持遁影，移动迅疾却畏惧正面硬撼，真气枯竭时会显形。", CultivationRealm.PURPLE_MANSION,
            CultivationRealm.DAO_TAI, 7, 194, CultivationTechnique.Aptitude.FORTUNE, 94,
            0.02F, 2.0D, 90, 260, 7, 1.22F, 4, 0.05D, "无常", "虚", "遁修");
    public static final CultivationTechnique HEAVENLY_CYCLE = special(
            "heavenly_cycle", "周天星河录", "引星辉入窍，借天时运转大周天，步步相扣。",
            "夜间参悟效率更高，白昼强行运转会消耗额外真气。", CultivationRealm.PURPLE_MANSION,
            CultivationRealm.DAO_TAI, 7, 188, CultivationTechnique.Aptitude.COMPREHENSION, 76,
            0.06F, 10.0D, 160, 300, 8, 1.20F, 5, 0.015D, "天枢", "星辰", "星修");
    public static final CultivationTechnique STAR_FORGER = special(
            "star_forger", "星陨铸魂法", "以星陨之火锻神魂，神念一动便可牵引万钧。",
            "神魂强盛但极耗心神，连续战斗后需要更长时间调息。", CultivationRealm.GOLDEN_CORE,
            CultivationRealm.DAO_TAI, 9, 202, CultivationTechnique.Aptitude.COMPREHENSION, 68,
            0.03F, 18.0D, 130, 380, 8, 1.35F, 12, 0.01D, "铸魂", "星火", "神魂");

    /**
     * The Purple Mansion / Golden Core line now has a real catalogue instead
     * of a handful of placeholder manuals.  Each seed is an independent
     * inheritance.  A seed carries its sect/path gate, lineage and acquisition
     * channel; the numeric values are expanded into a complete technique below.
     */
    private static final List<TechniqueSeed> EXTENDED_SEEDS = List.of(
            seed("azurewood_return", "青木回春诀", CultivationRealm.QI_REFINING, CultivationRealm.DAO_TAI, "sect", "青木宗", "木脉", "clear_origin", "宗门兑换", 180),
            seed("scarlet_sun_script", "赤霄炼阳篇", CultivationRealm.FOUNDATION_ESTABLISHMENT, CultivationRealm.DAO_TAI, "sect", "赤霄宫", "火脉", "wuwei_breath", "宗门兑换", 260),
            seed("taie_pure_void", "太乙清微经", CultivationRealm.PURPLE_MANSION, CultivationRealm.DAO_TAI, "sect", "太乙门", "清微", "mysterious_gate", "宗门兑换", 420),
            seed("purple_thunder_register", "紫霄雷府经", CultivationRealm.PURPLE_MANSION, CultivationRealm.DAO_TAI, "sect", "紫霄府", "雷脉", "sword_intent", "奇遇传承", 0),
            seed("golden_watch_sword", "金阙剑章", CultivationRealm.QI_REFINING, CultivationRealm.DAO_TAI, "sect", "金阙剑宗", "剑道", "sword_intent", "宗门兑换", 320),
            seed("plain_mystic_gate", "太素玄门诀", CultivationRealm.FOUNDATION_ESTABLISHMENT, CultivationRealm.DAO_TAI, "sect", "太素宗", "太素", "five_elements_return", "宗门兑换", 240),
            seed("north_sea_true_water", "北冥真水诀", CultivationRealm.FOUNDATION_ESTABLISHMENT, CultivationRealm.DAO_TAI, "sect", "北冥水府", "水脉", "water_virtue", "奇遇传承", 0),
            seed("nine_yang_heavenly_gang", "九曜天罡录", CultivationRealm.PURPLE_MANSION, CultivationRealm.DAO_TAI, "sect", "九曜宫", "星辰", "heavenly_cycle", "宗门兑换", 500),
            seed("mysterious_mountain_suppress", "玄都镇岳功", CultivationRealm.FOUNDATION_ESTABLISHMENT, CultivationRealm.GOLDEN_CORE, "sect", "玄都山", "厚土", "iron_body", "宗门兑换", 280),
            seed("pill_cauldron_origin", "丹鼎养元经", CultivationRealm.FOUNDATION_ESTABLISHMENT, CultivationRealm.DAO_TAI, "sect", "丹鼎院", "丹道", "valley_spirit", "宗门兑换", 220),
            seed("spirit_platform_visualize", "灵台观想法", CultivationRealm.QI_REFINING, CultivationRealm.DAO_TAI, "sect", "灵台寺", "神魂", "embrace_one", "奇遇传承", 0),
            seed("evergreen_wood", "乙木长生经", CultivationRealm.PURPLE_MANSION, CultivationRealm.DAO_TAI, "sect", "青木宗", "木脉", "azurewood_return", "宗门兑换", 620),
            seed("away_fire_burning_sky", "离火焚天经", CultivationRealm.PURPLE_MANSION, CultivationRealm.DAO_TAI, "sect", "赤霄宫", "火脉", "scarlet_sun_script", "宗门兑换", 680),
            seed("geng_metal_soul_cleave", "庚金斩魄诀", CultivationRealm.PURPLE_MANSION, CultivationRealm.DAO_TAI, "sect", "金阙剑宗", "剑道", "golden_watch_sword", "宗门兑换", 720),
            seed("kan_water_tide_song", "坎水听潮诀", CultivationRealm.PURPLE_MANSION, CultivationRealm.DAO_TAI, "sect", "北冥水府", "水脉", "north_sea_true_water", "宗门兑换", 640),
            seed("wu_earth_thick_load", "戊土厚载经", CultivationRealm.PURPLE_MANSION, CultivationRealm.DAO_TAI, "sect", "玄都山", "厚土", "mysterious_mountain_suppress", "宗门兑换", 610),
            seed("two_rituals_transformation", "两仪化生经", CultivationRealm.PURPLE_MANSION, CultivationRealm.DAO_TAI, "sect", "太乙门", "两仪", "taie_pure_void", "宗门兑换", 760),
            seed("three_talents_origin", "三才归元篇", CultivationRealm.GOLDEN_CORE, CultivationRealm.DAO_TAI, "sect", "太素宗", "三才", "plain_mystic_gate", "奇遇传承", 0),
            seed("four_symbols_soul_guard", "四象镇魂录", CultivationRealm.GOLDEN_CORE, CultivationRealm.DAO_TAI, "sect", "灵台寺", "神魂", "spirit_platform_visualize", "宗门兑换", 880),
            seed("five_thunder_true_register", "五雷正法", CultivationRealm.GOLDEN_CORE, CultivationRealm.DAO_TAI, "sect", "紫霄府", "雷脉", "purple_thunder_register", "奇遇传承", 0),
            seed("jade_pure_cave_script", "玉清洞玄经", CultivationRealm.GOLDEN_CORE, CultivationRealm.DAO_TAI, "sect", "太乙门", "清微", "two_rituals_transformation", "宗门兑换", 960),
            seed("star_dipper_mystery", "太微星斗经", CultivationRealm.GOLDEN_CORE, CultivationRealm.DAO_TAI, "sect", "九曜宫", "星辰", "nine_yang_heavenly_gang", "宗门兑换", 1100),
            seed("purple_mansion_nourish_soul", "紫府养神篇", CultivationRealm.PURPLE_MANSION, CultivationRealm.DAO_TAI, "sect", "灵台寺", "神魂", "spirit_platform_visualize", "宗门兑换", 520),
            seed("golden_core_jade_fluid", "金丹玉液经", CultivationRealm.GOLDEN_CORE, CultivationRealm.DAO_TAI, "sect", "丹鼎院", "丹道", "pill_cauldron_origin", "宗门兑换", 1000),
            seed("pure_yang_temper_form", "纯阳炼形篇", CultivationRealm.GOLDEN_CORE, CultivationRealm.DAO_TAI, "sect", "赤霄宫", "火脉", "away_fire_burning_sky", "奇遇传承", 0),
            seed("lunar_soul_condense", "太阴凝魄诀", CultivationRealm.GOLDEN_CORE, CultivationRealm.DAO_TAI, "sect", "北冥水府", "水脉", "kan_water_tide_song", "奇遇传承", 0),
            seed("cloud_drifts_free", "流云散手诀", CultivationRealm.QI_REFINING, CultivationRealm.DAO_TAI, "wanderer", "散修盟", "身法", "wuwei_breath", "散修集市", 150),
            seed("smoke_cloud_escape", "烟霞遁法", CultivationRealm.FOUNDATION_ESTABLISHMENT, CultivationRealm.DAO_TAI, "wanderer", "散修盟", "遁法", "cloud_drifts_free", "奇遇传承", 0),
            seed("white_bone_sha", "白骨养煞经", CultivationRealm.FOUNDATION_ESTABLISHMENT, CultivationRealm.DAO_TAI, "wanderer", "白骨洞", "煞道", "iron_body", "奇遇传承", 0),
            seed("chaotic_star_shift", "乱星换位术", CultivationRealm.PURPLE_MANSION, CultivationRealm.DAO_TAI, "wanderer", "星盗会", "星辰", "void_shadow", "散修集市", 580),
            seed("cold_river_moon_hook", "寒江钓月功", CultivationRealm.FOUNDATION_ESTABLISHMENT, CultivationRealm.GOLDEN_CORE, "wanderer", "寒江客", "水脉", "water_virtue", "散修集市", 300),
            seed("earth_hide_blade", "厚土藏锋诀", CultivationRealm.FOUNDATION_ESTABLISHMENT, CultivationRealm.GOLDEN_CORE, "wanderer", "玄岩寨", "厚土", "iron_body", "散修集市", 280),
            seed("formless_roaming", "无相游身篇", CultivationRealm.PURPLE_MANSION, CultivationRealm.DAO_TAI, "wanderer", "无相门", "身法", "cloud_drifts_free", "奇遇传承", 0),
            seed("thousand_li_wind_listen", "千里听风诀", CultivationRealm.PURPLE_MANSION, CultivationRealm.DAO_TAI, "wanderer", "听风楼", "风脉", "female_spirit", "散修集市", 520),
            seed("scarlet_blood_temper", "赤血淬骨法", CultivationRealm.FOUNDATION_ESTABLISHMENT, CultivationRealm.GOLDEN_CORE, "wanderer", "血河寨", "体修", "iron_body", "奇遇传承", 0),
            seed("lunar_hidden_form", "太阴匿形经", CultivationRealm.PURPLE_MANSION, CultivationRealm.DAO_TAI, "wanderer", "幽月楼", "遁法", "void_shadow", "散修集市", 700),
            seed("karma_cleave_blade", "斩业刀经", CultivationRealm.GOLDEN_CORE, CultivationRealm.DAO_TAI, "wanderer", "断业客", "刀道", "less_private", "奇遇传承", 0),
            seed("hundred_poison_temper", "百毒炼身诀", CultivationRealm.FOUNDATION_ESTABLISHMENT, CultivationRealm.GOLDEN_CORE, "wanderer", "万毒谷", "毒道", "iron_body", "散修集市", 360),
            seed("carefree_wind_drive", "逍遥御风篇", CultivationRealm.PURPLE_MANSION, CultivationRealm.DAO_TAI, "wanderer", "逍遥客", "风脉", "female_spirit", "散修集市", 760),
            seed("nether_soul_capture", "幽冥摄魂录", CultivationRealm.GOLDEN_CORE, CultivationRealm.DAO_TAI, "wanderer", "幽冥渡", "神魂", "star_forger", "奇遇传承", 0),
            seed("yellow_spring_crossing", "黄泉渡厄经", CultivationRealm.GOLDEN_CORE, CultivationRealm.DAO_TAI, "wanderer", "黄泉客", "水脉", "lunar_soul_condense", "奇遇传承", 0),
            seed("dragon_gate_transform", "龙门化蛟诀", CultivationRealm.PURPLE_MANSION, CultivationRealm.DAO_TAI, "wanderer", "龙门散人", "水脉", "north_sea_true_water", "奇遇传承", 0),
            seed("phoenix_nine_heavens", "凤鸣九天经", CultivationRealm.GOLDEN_CORE, CultivationRealm.DAO_TAI, "wanderer", "凤鸣客", "火脉", "pure_yang_temper_form", "奇遇传承", 0),
            seed("heavenly_machination", "天机推演录", CultivationRealm.PURPLE_MANSION, CultivationRealm.DAO_TAI, "wanderer", "天机阁", "推演", "know_stop", "散修集市", 900),
            seed("mountain_sea_forgetfulness", "山海忘忧诀", CultivationRealm.GOLDEN_CORE, CultivationRealm.DAO_TAI, "wanderer", "忘忧谷", "道心", "return_nature", "奇遇传承", 0),
            seed("red_dust_refine_heart", "红尘炼心篇", CultivationRealm.PURPLE_MANSION, CultivationRealm.DAO_TAI, "wanderer", "红尘客栈", "道心", "less_private", "散修集市", 620),
            seed("void_seal_return", "虚空归藏法", CultivationRealm.GOLDEN_CORE, CultivationRealm.DAO_TAI, "wanderer", "虚空行者", "太虚", "void_shadow", "奇遇传承", 0),
            seed("universal_nine_turns", "九转混元功", CultivationRealm.FOUNDATION_ESTABLISHMENT, CultivationRealm.DAO_TAI, null, "无主传承", "混元", "five_elements_return", "奇遇传承", 0),
            seed("innate_one_qi", "先天一炁经", CultivationRealm.PURPLE_MANSION, CultivationRealm.DAO_TAI, null, "古仙遗府", "混元", "universal_nine_turns", "奇遇传承", 0),
            seed("great_dao_simple", "大道至简录", CultivationRealm.GOLDEN_CORE, CultivationRealm.DAO_TAI, null, "古仙遗府", "混元", "innate_one_qi", "奇遇传承", 0));

    private static final List<CultivationTechnique> EXTENDED = EXTENDED_SEEDS.stream()
            .map(CultivationTechniques::fromSeed).toList();

    public static List<CultivationTechnique> ALL = List.of(
            BASIC_BREATHING, CLEAR_ORIGIN, WUWEI_BREATH, EMBRACE_ONE, VALLEY_SPIRIT,
            WATER_VIRTUE, RETURN_TO_ROOT, MYSTERIOUS_GATE, LESS_PRIVATE, FEMALE_SPIRIT,
            KNOW_STOP, RETURN_NATURE, FIVE_ELEMENTS_RETURN, SWORD_INTENT, IRON_BODY,
            VOID_SHADOW, HEAVENLY_CYCLE, STAR_FORGER);
    static {
        java.util.ArrayList<CultivationTechnique> expanded = new java.util.ArrayList<>(ALL);
        expanded.addAll(EXTENDED);
        ALL = List.copyOf(expanded);
    }
    private static final Map<String, CultivationTechnique> TECHNIQUES = createIndex();
    private static final Map<String, TechniqueProfile> PROFILES = createProfiles();

    private CultivationTechniques() {}

    public static CultivationTechnique byId(String id) {
        return TECHNIQUES.get(id);
    }

    public static List<CultivationTechnique> all() {
        return ALL;
    }

    public record TechniqueProfile(String requiredPath, String sect, String lineage,
                                   boolean universal, String prerequisiteId,
                                   String acquisition, int exchangeCost,
                                   int resonanceMeditationBonus, int resonanceTrueQiBonus,
                                   int drawbackMeditationPercent, int drawbackTrueQiCostPercent) {
        public boolean isAdventure() {
            return "奇遇传承".equals(acquisition);
        }
    }

    private record TechniqueSeed(String id, String name, CultivationRealm minimum,
                                 CultivationRealm maximum, String requiredPath, String sect,
                                 String lineage, String prerequisiteId, String acquisition,
                                 int exchangeCost) {}

    private static TechniqueSeed seed(String id, String name, CultivationRealm minimum,
                                      CultivationRealm maximum, String requiredPath, String sect,
                                      String lineage, String prerequisiteId, String acquisition,
                                      int exchangeCost) {
        return new TechniqueSeed(id, name, minimum, maximum, requiredPath, sect, lineage,
                prerequisiteId, acquisition, exchangeCost);
    }

    private static CultivationTechnique fromSeed(TechniqueSeed seed) {
        int i = EXTENDED_SEEDS.indexOf(seed);
        CultivationTechnique.Aptitude aptitude = CultivationTechnique.Aptitude.values()[i % 4];
        return special(seed.id, seed.name,
                seed.sect + "所传，道统以" + seed.lineage + "为本，重在紫府立道、金丹定品。",
                "依" + seed.lineage + "行周天，先纳灵机，再以神念收束；" +
                        (i % 2 == 0 ? "行功稳健而擅长久战。" : "爆发凌厉但真炁消耗更重。"),
                seed.minimum, seed.maximum, 3 + i % 7, 146 + i * 3, aptitude,
                72 + i % 40, 0.02F + (i % 6) * 0.012F, 4.0D + i * 0.8D,
                100 + (i % 9) * 10, 72 + i * 11, 2 + i % 8,
                1.03F + (i % 6) * 0.045F, i % 9, 0.005D + (i % 6) * 0.006D,
                seed.lineage, seed.lineage, (i % 3 == 0 ? "蓄势" : i % 3 == 1 ? "攻守" : "机变"));
    }

    private static Map<String, TechniqueProfile> createProfiles() {
        Map<String, TechniqueProfile> profiles = new LinkedHashMap<>();
        for (TechniqueSeed seed : EXTENDED_SEEDS) {
            int i = EXTENDED_SEEDS.indexOf(seed);
            profiles.put("xiuxian:" + seed.id, new TechniqueProfile(seed.requiredPath, seed.sect,
                    seed.lineage, seed.requiredPath == null, emptyToNull(seed.prerequisiteId),
                    seed.acquisition, seed.exchangeCost, 3 + i % 7, 1 + i % 4,
                    i % 5 == 0 ? 6 : 0, i % 7 == 0 ? 8 : 0));
        }
        profiles.put("xiuxian:basic_breathing", new TechniqueProfile(null, "散修与宗门共传", "根基", true, null, "初始传承", 0, 0, 0, 0, 0));
        profiles.put("xiuxian:clear_origin", new TechniqueProfile(null, "散修与宗门共传", "根基", true, "basic_breathing", "散修集市", 120, 8, 0, 0, 0));
        profiles.put("xiuxian:wuwei_breath", new TechniqueProfile(null, "散修与宗门共传", "根基", true, "clear_origin", "散修集市", 140, 0, 0, 0, 0));
        profiles.put("xiuxian:embrace_one", new TechniqueProfile(null, "散修与宗门共传", "根基", true, "clear_origin", "宗门兑换", 150, 0, 0, 0, 0));
        profiles.put("xiuxian:valley_spirit", new TechniqueProfile(null, "散修与宗门共传", "肉身", true, "embrace_one", "宗门兑换", 180, 0, 0, 0, 0));
        profiles.put("xiuxian:water_virtue", new TechniqueProfile(null, "北冥水府", "肉身", false, "valley_spirit", "宗门兑换", 220, 5, 2, 0, 0));
        profiles.put("xiuxian:return_to_root", new TechniqueProfile(null, "散修与宗门共传", "肉身", true, "valley_spirit", "奇遇传承", 0, 5, 0, 0, 0));
        profiles.put("xiuxian:mysterious_gate", new TechniqueProfile(null, "太乙门", "太虚", false, "return_to_root", "宗门兑换", 260, 0, 3, 0, 0));
        profiles.put("xiuxian:less_private", new TechniqueProfile(null, "散修与宗门共传", "道心", true, "mysterious_gate", "散修集市", 300, 0, 0, 5, 8));
        profiles.put("xiuxian:female_spirit", new TechniqueProfile(null, "散修与宗门共传", "道心", true, "less_private", "散修集市", 320, 0, 0, 0, 0));
        profiles.put("xiuxian:know_stop", new TechniqueProfile(null, "散修与宗门共传", "道心", true, "female_spirit", "宗门兑换", 340, 0, 0, 0, 0));
        profiles.put("xiuxian:return_nature", new TechniqueProfile(null, "散修与宗门共传", "道心", true, "know_stop", "奇遇传承", 0, 7, 4, 0, 0));
        profiles.put("xiuxian:five_elements_return", new TechniqueProfile(null, "太素宗", "根基", true, "embrace_one", "宗门兑换", 260, 8, 2, 0, 0));
        profiles.put("xiuxian:sword_intent", new TechniqueProfile("sect", "金阙剑宗", "剑道", false, "clear_origin", "宗门兑换", 300, 6, 0, 8, 12));
        profiles.put("xiuxian:iron_body", new TechniqueProfile("wanderer", "玄岩寨", "肉身", false, "valley_spirit", "散修集市", 260, 0, 0, 8, 12));
        profiles.put("xiuxian:void_shadow", new TechniqueProfile("wanderer", "虚空行者", "太虚", false, "mysterious_gate", "奇遇传承", 0, 0, 0, 5, 8));
        profiles.put("xiuxian:heavenly_cycle", new TechniqueProfile("sect", "九曜宫", "星辰", false, "mysterious_gate", "宗门兑换", 360, 6, 3, 0, 0));
        profiles.put("xiuxian:star_forger", new TechniqueProfile("wanderer", "星盗会", "神魂", false, "heavenly_cycle", "奇遇传承", 0, 7, 4, 0, 0));
        return profiles;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public static TechniqueProfile profile(String id) {
        TechniqueProfile profile = PROFILES.get(id);
        return profile == null ? new TechniqueProfile(null, "无名传承", "混元", true,
                null, "奇遇传承", 0, 0, 0, 0, 0) : profile;
    }

    /**
     * Stable resonance buckets used by the stat system.  The original data
     * predates the extended catalogue and contains localized lineage labels;
     * ids are the only representation that stays stable across save files.
     */
    public static String resonanceGroupFor(String id) {
        String key = id == null ? "" : id.substring(id.indexOf(':') + 1);
        return switch (key) {
            case "basic_breathing", "clear_origin", "wuwei_breath", "embrace_one",
                    "five_elements_return", "azurewood_return", "scarlet_sun_script",
                    "taie_pure_void", "plain_mystic_gate", "pill_cauldron_origin",
                    "universal_nine_turns", "innate_one_qi", "great_dao_simple" -> "foundation";
            case "valley_spirit", "water_virtue", "return_to_root", "iron_body",
                    "north_sea_true_water", "mysterious_mountain_suppress", "wu_earth_thick_load",
                    "cold_river_moon_hook", "earth_hide_blade", "white_bone_sha",
                    "scarlet_blood_temper", "hundred_poison_temper", "dragon_gate_transform" -> "body";
            case "mysterious_gate", "void_shadow", "heavenly_cycle", "star_forger",
                    "purple_thunder_register", "nine_yang_heavenly_gang", "two_rituals_transformation",
                    "five_thunder_true_register", "jade_pure_cave_script", "star_dipper_mystery",
                    "chaotic_star_shift", "smoke_cloud_escape", "lunar_hidden_form",
                    "void_seal_return", "thousand_li_wind_listen", "carefree_wind_drive" -> "void";
            case "less_private", "female_spirit", "know_stop", "return_nature", "sword_intent",
                    "golden_watch_sword", "geng_metal_soul_cleave", "spirit_platform_visualize",
                    "four_symbols_soul_guard", "purple_mansion_nourish_soul", "golden_core_jade_fluid",
                    "pure_yang_temper_form", "lunar_soul_condense", "cloud_drifts_free",
                    "formless_roaming", "karma_cleave_blade", "nether_soul_capture",
                    "yellow_spring_crossing", "phoenix_nine_heavens", "heavenly_machination",
                    "mountain_sea_forgetfulness", "red_dust_refine_heart" -> "dao";
            default -> "misc";
        };
    }

    public static CultivationTechnique randomAdventureTechnique(CultivationData data, RandomSource random) {
        if (data == null || !data.isInitialized()) return null;
        List<CultivationTechnique> candidates = ALL.stream()
                .filter(t -> profile(t.id()).isAdventure())
                .filter(t -> t.canBeLearnedAt(data.realm()))
                .filter(t -> t.isCompatibleWithPath(data.cultivationPath()))
                .filter(t -> !data.hasLearnedTechnique(t.id()))
                .toList();
        return candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size()));
    }

    /** Returns manuals that fit the cultivator's current realm and aptitude. */
    public static List<CultivationTechnique> recommendations(CultivationData data, int limit) {
        if (data == null || !data.isInitialized() || limit <= 0) {
            return List.of();
        }
        int spiritualRoot = data.spiritualRoot();
        int constitution = data.constitution();
        int comprehension = data.comprehension();
        int fortune = data.fortune();
        CultivationRealm realm = data.realm();
        return ALL.stream()
                .filter(technique -> technique.canBeLearnedAt(realm))
                .filter(technique -> technique.isCompatibleWithPath(data.cultivationPath()))
                .filter(technique -> technique.prerequisiteId() == null
                        || data.hasLearnedTechnique("xiuxian:" + technique.prerequisiteId()))
                .filter(technique -> !data.hasLearnedTechnique(technique.id()))
                .sorted(Comparator.comparingInt((CultivationTechnique technique) ->
                        recommendationScore(technique, realm, spiritualRoot, constitution,
                                comprehension, fortune)).reversed())
                .limit(limit)
                .toList();
    }

    private static int recommendationScore(CultivationTechnique technique, CultivationRealm realm,
                                           int spiritualRoot, int constitution, int comprehension,
                                           int fortune) {
        int aptitude = technique.meditationAptitude().value(spiritualRoot, constitution, comprehension, fortune);
        int fit = technique.aptitudeMatchPercent(spiritualRoot, constitution, comprehension, fortune);
        int future = Math.max(0, technique.maximumRealm().ordinal() - realm.ordinal()) * 18;
        int difficulty = technique.learningDifficulty() * 7;
        return fit + aptitude * 2 + future + technique.trueQiRecoveryPerSecond() * 3
                + technique.trueQiBonus() / 10 - difficulty;
    }

    private static CultivationTechnique technique(String id, String name, String doctrine, String method,
                                                   CultivationRealm minimumRealm, CultivationRealm maximumRealm,
                                                   int difficulty, int meditation, CultivationTechnique.Aptitude aptitude,
                                                   int breakthrough,
                                                   float reduction, double health) {
        int rank = switch (id) {
            case "basic_breathing" -> 0;
            case "clear_origin" -> 1;
            case "wuwei_breath" -> 2;
            case "embrace_one" -> 3;
            case "valley_spirit" -> 4;
            case "water_virtue" -> 5;
            case "return_to_root" -> 6;
            case "mysterious_gate" -> 7;
            case "less_private" -> 8;
            case "female_spirit" -> 9;
            case "know_stop" -> 10;
            default -> 11;
        };
        String[] virtues = {"信德", "智德", "仁德", "礼德", "仁德", "义德",
                "信德", "智德", "义德", "礼德", "信德", "仁德"};
        String[] qiKinds = {"子水炁", "丑土炁", "寅木炁", "卯木炁", "辰木炁", "巳水炁",
                "午土炁", "未火炁", "申火炁", "酉金炁", "戌土炁", "亥水炁"};
        String[] styles = {"均衡筑基", "洞察破绽", "游斗续航", "稳守反击", "厚势蓄力", "卸力化劲",
                "韧性近战", "术法增幅", "爆发进攻", "防御反击", "稳健攻守", "高额真炁"};
        int[] qiCaps = {0, 35, 60, 45, 100, 125, 170, 240, 200, 230, 250, 320};
        int[] recovery = {1, 2, 4, 2, 3, 5, 4, 7, 4, 5, 6, 7};
        int[] healthRecovery = {100, 110, 85, 125, 120, 105, 130, 90, 85, 115, 120, 100};
        float[] spellPower = {1.0F, 1.05F, 1.0F, 1.0F, 1.0F, 1.08F, 1.05F, 1.25F,
                1.20F, 1.08F, 1.10F, 1.15F};
        int[] attack = {0, 0, 1, 0, 1, 0, 2, 1, 4, 1, 2, 3};
        double[] speed = {0.0D, 0.01D, 0.04D, 0.0D, 0.0D, 0.02D, 0.025D, 0.01D,
                0.03D, 0.035D, 0.0D, 0.015D};
        return new CultivationTechnique("xiuxian:" + id, name, doctrineFor(id, doctrine), methodFor(id, method),
                minimumRealm, maximumRealm, difficulty, meditation, aptitude, breakthrough, reduction, health,
                healthRecovery[rank],
                qiCaps[rank], recovery[rank], spellPower[rank], attack[rank], speed[rank],
                virtues[rank], qiKinds[rank], styles[rank]);
    }

    private static CultivationTechnique special(String id, String name, String doctrine, String method,
                                                CultivationRealm minimumRealm, CultivationRealm maximumRealm,
                                                int difficulty, int meditation, CultivationTechnique.Aptitude aptitude,
                                                int breakthrough, float reduction, double health, int passiveRecovery,
                                                int trueQiBonus, int trueQiRecovery, float spellPower, int attack,
                                                double speed, String virtue, String affinity, String style) {
        return new CultivationTechnique("xiuxian:" + id, name, doctrine, method, minimumRealm, maximumRealm,
                difficulty, meditation, aptitude, breakthrough, reduction, health, passiveRecovery, trueQiBonus,
                trueQiRecovery, spellPower, attack, speed, virtue, affinity, style);
    }

    private static String doctrineFor(String id, String fallback) {
        return switch (id) {
            case "basic_breathing" -> "初学者不求纳炁如潮，先求一息一念皆有所归。天地之炁循四时而变，修士当以身为炉、以心为候；守住平常呼吸，方能辨出外炁与妄念。此诀重在立基，攻守皆无偏胜。";
            case "clear_origin" -> "心源澄澈，则外物来去皆有迹可循。观敌不止观其形，更观其炁机将发未发之处；然而洞察不是追逐，念头若先一步奔出，反会被虚招牵引。此篇以明辨破妄，最忌急于求成。";
            case "wuwei_breath" -> "行炁贵顺其势，不以强念折其自然。进退如风过林隙，避其锋而续其息；久战时仍能收摄散炁，却不擅正面硬撼。所谓无为，是不妄为，并非临敌不动。";
            case "embrace_one" -> "形神各安其位，诸炁归中而不相争。守中者不轻易追击，也不因一击受挫而乱了章法；敌势盛时固守气海，势衰时以积蓄之力还击。其要在稳，不在迟。";
            case "valley_spirit" -> "虚处能纳，满处能化；气海如谷，受而不争。行炁先松其形，再让炁机沉入四肢百骸，招式虽不凌厉，却能逐步叠起沉厚之势。若根基未稳而贪多纳炁，反易壅滞。";
            case "water_virtue" -> "水不与石争一时之强，久行自能改其形。临阵以柔劲卸去来力，借敌势转开攻路，再以绵长炁息牵制对手；善守善化，爆发不足。此法要求修士识势，不可把退让误作畏战。";
            case "return_to_root" -> "外炁千变，根本只在一身之内。每逢气机纷乱，先收神归腹，再使一缕温炁周流四肢；如此可在近身缠斗中稳住身形，受创后也不易散乱。炼此诀不可屏息强忍，柔守方能久长。";
            case "mysterious_gate" -> "出入之间有无相生，炁机未显之处正是法门。行功者观其开合而不执其形，真炁因而深厚，施术时亦能将一念送得更远；但此法不长于蛮力，须以清明神识驾驭。";
            case "less_private" -> "所欲愈繁，心炁愈散；减去多余念头，方能把力量聚在一处。此录教人蓄势后骤然发劲，以短促的爆发撕开守势；代价是炁息起伏较大，若一击不中便要收势重整。";
            case "female_spirit" -> "知刚而守柔，外示退让，内里自有韧劲。以灵动步法避开正锋，在侧面连缀小术与近身招式；不与强敌正面相耗，而以变化争取先机。其妙在转圜，身法受限时威力亦减。";
            case "know_stop" -> "知进，也须知止；炁满而不知收，终会伤及根本。每次出手留有余地，守住气海的一线清炁，待对方力竭再稳稳反击。它不以险招取胜，却能降低行炁紊乱与受创失守的风险。";
            case "return_nature" -> "返朴不是退回蒙昧，而是在万法纷纭后仍识得本心。此篇不争一时锐气，长期涵养真炁并使攻守归于平衡；每一招都留有后手，适合以深厚修为应对多变战局。";
            default -> fallback;
        };
    }

    private static String methodFor(String id, String fallback) {
        return switch (id) {
            case "basic_breathing" -> "每日分三次行功，每次九轮：吸息时意守脐下，呼息时放松肩背。初学只在安全处吐纳，不强行导炁冲脉；真炁渐满后再以缓息温养气海。";
            case "clear_origin" -> "先静坐三息，再观周身与敌手的炁机起伏。每次出招前留一线神识辨别虚实，确认气机将发再进；若连续追击而心念浮躁，立即退步重整。";
            case "wuwei_breath" -> "步随呼吸轻移，不憋气，不强催。遭遇攻击时先偏身避锋，借移动维持吐纳节奏；只有对方露出破绽时才回身反击，避免在正面硬拼中耗尽真炁。";
            case "embrace_one" -> "意守中宫，双手护住身前要害，先稳住脚下再接敌招。受击时以沉肩收腹分散冲力，待敌势已尽再将积蓄的炁贯入一击；不可连续追击破坏呼吸节律。";
            case "valley_spirit" -> "先放松胸腹，使吸息深而不滞；再依次引炁至肩、臂、腰、腿。每轮行功都留三分余地，战斗中则以稳步推进叠加气势，不要一次耗空气海。";
            case "water_virtue" -> "临敌时侧身引导来势，脚下走弧，不与对手正面相抵。卸力成功后沿其空门还手，招式务必连贯如水；若地形狭窄无法转身，应改守不宜强行绕步。";
            case "return_to_root" -> "先将神思收归下腹，再以柔和长息贯通四肢。近身时保持重心低而可转，受创后缓息一轮再续攻；莫以屏息和硬扛换取一时速度。";
            case "mysterious_gate" -> "观每次呼吸将尽未尽之际，在心中留出空隙。施术前先把真炁收束为一线，再顺念送出；术后立即回神守息，避免连续施术令识海浮动。";
            case "less_private" -> "以短息蓄劲，等对方招式落空时将积炁一次发出。出手后立刻收势，不恋战、不连发；若第一击未中，借步法撤开，重新积蓄真炁。";
            case "female_spirit" -> "交战时小步变向，始终让身侧对着来势；以轻术试探，再接一记近身还击。体力和真炁都要留有余量，若被困角落，先以防守找出脱身方向。";
            case "know_stop" -> "每三次出手便回守一息，察看自身气海和对手节奏。真炁低于半数时停止追击，保持防守等待恢复；敌势衰竭后再以稳定招式终结。";
            case "return_nature" -> "出手与收势皆守同一呼吸，不追求异相或奇招。面对不同敌手先以平稳招式试探，再依其攻势调整强弱；每次交锋结束后收炁归元，勿使战意延续扰乱行功。";
            default -> fallback;
        };
    }

    private static Map<String, CultivationTechnique> createIndex() {
        Map<String, CultivationTechnique> techniques = new LinkedHashMap<>();
        for (CultivationTechnique technique : ALL) {
            techniques.put(technique.id(), technique);
        }
        return Collections.unmodifiableMap(techniques);
    }
}
