package xiuxian.cultivation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Twelve original breathing manuals shaped by classical Daoist ideas. */
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

    public static final List<CultivationTechnique> ALL = List.of(
            BASIC_BREATHING, CLEAR_ORIGIN, WUWEI_BREATH, EMBRACE_ONE, VALLEY_SPIRIT,
            WATER_VIRTUE, RETURN_TO_ROOT, MYSTERIOUS_GATE, LESS_PRIVATE, FEMALE_SPIRIT,
            KNOW_STOP, RETURN_NATURE);
    private static final Map<String, CultivationTechnique> TECHNIQUES = createIndex();

    private CultivationTechniques() {}

    public static CultivationTechnique byId(String id) {
        return TECHNIQUES.get(id);
    }

    public static List<CultivationTechnique> all() {
        return ALL;
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
        String[] qiKinds = {"子水炁", "丑土炁", "寅木炁", "卯木炁", "辰土炁", "巳火炁",
                "午火炁", "未土炁", "申金炁", "酉金炁", "戌土炁", "亥水炁"};
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
