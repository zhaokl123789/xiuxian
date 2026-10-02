package xiuxian.cultivation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import xiuxian.network.XiuxianNetwork;

/**
 * 胎息术法目录。术法与功法分开建模：功法决定可用的专属术法，术法本身
 * 只消耗真炁并拥有短冷却，因此不会把胎息阶段的战斗强度推到高阶境界。
 */
public final class CultivationSpells {
    private static final Map<String, CultivationSpell> SPELLS = new LinkedHashMap<>();
    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new LinkedHashMap<>();

    static {
        // 二十门通用胎息术法：每一门只有一个清晰用途，方便新玩家学习和后续扩展。
        add("qi_breath_guard", "引气护体", "以初生真炁凝成薄幕，短暂抵御伤害", 5, 80, 0, 0, 100, 0, CultivationSpell.Effect.EFFECT, false, null);
        add("clear_mind", "澄心定神", "清除自身的虚弱与挖掘疲劳", 4, 100, 0, 0, 1, 0, CultivationSpell.Effect.CLEANSE, false, null);
        add("light_body", "轻身步", "借一缕风炁提升移动速度", 4, 120, 0, 0, 100, 0, CultivationSpell.Effect.EFFECT, false, null);
        add("bright_eyes", "明目术", "照见幽暗，短时获得夜视", 3, 60, 0, 0, 240, 0, CultivationSpell.Effect.EFFECT, false, null);
        add("water_breath", "水息诀", "闭息入水，借水灵维持呼吸", 5, 160, 0, 0, 240, 0, CultivationSpell.Effect.EFFECT, false, null);
        add("fire_ward", "避火诀", "以真炁隔开火焰余烬", 5, 180, 0, 0, 180, 0, CultivationSpell.Effect.EFFECT, false, null);
        add("wooden_nourish", "木灵养息", "以木炁缓慢修复伤势", 7, 160, 0, 0, 100, 0, CultivationSpell.Effect.EFFECT, false, null);
        add("earth_skin", "土壁护身", "皮膜坚实，短时间降低所受伤害", 8, 200, 0, 0, 80, 0, CultivationSpell.Effect.EFFECT, false, null);
        add("golden_breath", "金息敛锋", "压低敌意，使自身短暂获得抗性", 8, 180, 0, 0, 80, 0, CultivationSpell.Effect.EFFECT, false, null);
        add("spirit_sense", "灵识探查", "让附近生灵显形片刻", 6, 140, 8, 0, 100, 0, CultivationSpell.Effect.EFFECT, true, null);
        add("ember_bolt", "微炎弹", "弹出一枚不稳定的火炁，灼伤近处敌人", 6, 100, 8, 3, 0, 0, CultivationSpell.Effect.DAMAGE, true, null);
        add("frost_needle", "凝霜针", "寒炁凝针，伤害并迟滞目标", 7, 120, 8, 2, 60, 0, CultivationSpell.Effect.EFFECT, true, null);
        add("wind_blade", "清风刃", "以风压割开目标的护体炁", 7, 110, 8, 3, 0, 0, CultivationSpell.Effect.DAMAGE, true, null);
        add("stone_prick", "地脉突刺", "借脚下土炁刺击一名近敌", 8, 140, 6, 4, 0, 0, CultivationSpell.Effect.DAMAGE, true, null);
        add("thunder_spark", "引雷星火", "召来微弱雷光，令目标短暂失神", 9, 160, 10, 3, 40, 0, CultivationSpell.Effect.EFFECT, true, null);
        add("spirit_bind", "缚灵丝", "真炁化丝，限制目标移动", 7, 130, 8, 1, 80, 1, CultivationSpell.Effect.EFFECT, true, null);
        add("soul_shake", "震魂音", "以音炁扰乱目标，削弱其力量", 8, 150, 8, 1, 80, 0, CultivationSpell.Effect.EFFECT, true, null);
        add("small_rejuvenation", "小回春术", "温养经脉，恢复少量气血", 7, 130, 0, 4, 0, 0, CultivationSpell.Effect.HEAL, false, null);
        add("true_qi_return", "归元纳息", "收束散炁，恢复少量真炁", 8, 180, 0, 12, 0, 0, CultivationSpell.Effect.RESTORE_TRUE_QI, false, null);
        add("repel_wave", "震炁波", "以近身炁浪推开周围敌人", 9, 170, 4, 1, 0, 0, CultivationSpell.Effect.PUSH, true, null);

        // 功法专有术法：胎息境可修的两门基础功法各有两门，不与通用术法叠成高额伤害。
        add("basic_breathing_cycle", "吐纳回环", "吐纳引气诀专属：每次呼吸都为下一次受击留下一线缓冲", 4, 160, 0, 0, 100, 0, CultivationSpell.Effect.EFFECT, false, "xiuxian:basic_breathing");
        add("basic_breathing_pulse", "引气脉冲", "吐纳引气诀专属：将积蓄的初炁推向近敌", 8, 140, 6, 2, 0, 0, CultivationSpell.Effect.DAMAGE, true, "xiuxian:basic_breathing");
        add("clear_origin_insight", "返照明心", "澄源返照专属：看破目标弱点并使其显形", 7, 140, 10, 1, 100, 0, CultivationSpell.Effect.EFFECT, true, "xiuxian:clear_origin");
        add("clear_origin_rebuke", "澄源反照", "澄源返照专属：受击后以清炁反震近敌", 9, 180, 6, 3, 0, 0, CultivationSpell.Effect.DAMAGE, true, "xiuxian:clear_origin");
        // 新增胎息传承的专属术法：每门至少一门，伤害、护盾和持续时间均保持低阶范围。
        add("taixu_guiding_breath_return", "引息回元", "太虚引息篇专属：收束散炁，恢复少量真炁", 7, 220, 0, 5, 0, 0, CultivationSpell.Effect.RESTORE_TRUE_QI, false, "xiuxian:taixu_guiding_breath");
        add("azurewood_nourishing_qi_rejuvenate", "青木回春", "青木养元诀专属：木炁温养伤势", 8, 220, 0, 2, 80, 0, CultivationSpell.Effect.HEAL, false, "xiuxian:azurewood_nourishing_qi");
        add("scarlet_cloud_qi_burn", "赤霞灼指", "赤霞炼气章专属：一缕赤火灼伤近敌", 8, 180, 8, 3, 0, 0, CultivationSpell.Effect.DAMAGE, true, "xiuxian:scarlet_cloud_qi");
        add("mysterious_water_tide_guard", "归潮护体", "玄水归潮诀专属：水幕短暂护身", 8, 240, 0, 0, 100, 0, CultivationSpell.Effect.EFFECT, false, "xiuxian:mysterious_water_tide");
        add("thick_earth_suppress_wall", "厚土障", "厚土镇元功专属：凝成薄土壁抵挡一击", 9, 240, 0, 0, 80, 0, CultivationSpell.Effect.EFFECT, false, "xiuxian:thick_earth_suppress");
        add("geng_metal_temper_cut", "庚金裂气", "庚金淬息录专属：金芒划过近敌", 9, 200, 8, 3, 0, 0, CultivationSpell.Effect.DAMAGE, true, "xiuxian:geng_metal_temper_breath");
        add("wind_listening_breath_step", "逐风步", "风行听息术专属：借风提升片刻身法", 6, 180, 0, 0, 80, 0, CultivationSpell.Effect.EFFECT, false, "xiuxian:wind_listening_breath");
        add("thunder_guiding_origin_spark", "引雷指", "雷引纳元诀专属：微弱雷光令目标失神", 10, 240, 10, 2, 30, 0, CultivationSpell.Effect.EFFECT, true, "xiuxian:thunder_guiding_origin");
        add("moonlight_calm_mind_clear", "月华定神", "月华静心篇专属：清除自身一层负面状态", 7, 240, 0, 0, 1, 0, CultivationSpell.Effect.CLEANSE, false, "xiuxian:moonlight_calm_mind");
        add("mysterious_crane_breath_change", "鹤影换气", "玄鹤吐纳法专属：短暂提高移动速度", 6, 200, 0, 0, 80, 0, CultivationSpell.Effect.EFFECT, false, "xiuxian:mysterious_crane_breath");
        add("wither_bloom_visualize_cycle", "枯荣息", "枯荣观想录专属：由枯转荣，恢复少量真炁", 8, 240, 0, 4, 0, 0, CultivationSpell.Effect.RESTORE_TRUE_QI, false, "xiuxian:wither_bloom_visualize");
        add("nine_breaths_return_guard", "九息归元", "九息归藏诀专属：守息片刻，获得薄弱护盾", 9, 260, 0, 0, 80, 0, CultivationSpell.Effect.EFFECT, false, "xiuxian:nine_breaths_return");
        add("cold_soul_condense_needle", "寒魄指", "寒魄凝神篇专属：寒意迟滞一名近敌", 9, 220, 8, 2, 50, 0, CultivationSpell.Effect.EFFECT, true, "xiuxian:cold_soul_condense");
        add("hundred_grass_nourish_return", "草木回元", "百草养息经专属：草木精气缓慢疗伤", 8, 240, 0, 2, 80, 0, CultivationSpell.Effect.HEAL, false, "xiuxian:hundred_grass_nourish");
        add("sunfire_temper_body_shock", "炎阳震", "炎阳炼体篇专属：近身阳劲击退敌人", 9, 220, 6, 2, 0, 0, CultivationSpell.Effect.PUSH, true, "xiuxian:sunfire_temper_body");
        add("flowing_sand_hide_step", "流砂遁", "流砂隐息法专属：土风护体，短暂减轻伤害", 8, 240, 0, 0, 80, 0, CultivationSpell.Effect.EFFECT, false, "xiuxian:flowing_sand_hide");
        add("canglang_listening_tide_resist", "听涛卸力", "沧浪听涛诀专属：借水势化开下一次冲击", 9, 240, 0, 0, 80, 0, CultivationSpell.Effect.EFFECT, false, "xiuxian:canglang_listening_tide");
        add("white_rainbow_qi_thrust", "白虹穿云", "白虹纳气篇专属：凝金成线，刺向近敌", 9, 220, 10, 3, 0, 0, CultivationSpell.Effect.DAMAGE, true, "xiuxian:white_rainbow_qi");
        add("star_chart_visualize_sense", "星罗识海", "星罗观想术专属：短暂提升感知并显形目标", 8, 240, 10, 0, 100, 0, CultivationSpell.Effect.EFFECT, true, "xiuxian:star_chart_visualize");
        add("return_void_nourish_origin", "归墟纳元", "归墟养息经专属：低血量时回收少量真炁", 10, 280, 0, 5, 0, 0, CultivationSpell.Effect.RESTORE_TRUE_QI, false, "xiuxian:return_void_nourish");
    }

    private CultivationSpells() {}

    private static void add(String id, String name, String description, int cost, int cooldown,
                             int range, float magnitude, int duration, int amplifier,
                             CultivationSpell.Effect effect, boolean targeted, String requiredTechnique) {
        SPELLS.put("xiuxian:" + id, new CultivationSpell("xiuxian:" + id, name, description,
                CultivationRealm.FETAL_BREATH, CultivationRealm.FETAL_BREATH, cost, cooldown,
                range, magnitude, duration, amplifier, elementFor(id), effect, targeted, requiredTechnique));
    }

    private static CultivationSpell.Element elementFor(String id) {
        if (id.contains("ember") || id.contains("fire") || id.contains("scarlet") || id.contains("sunfire")) {
            return CultivationSpell.Element.FIRE;
        }
        if (id.contains("frost") || id.contains("water") || id.contains("clear_origin") || id.contains("canglang")) {
            return CultivationSpell.Element.WATER;
        }
        if (id.contains("wind") || id.contains("light_body") || id.contains("sand_hide")) {
            return CultivationSpell.Element.WIND;
        }
        if (id.contains("stone") || id.contains("earth") || id.contains("qi_breath") || id.contains("thick_earth") || id.contains("flowing_sand")) {
            return CultivationSpell.Element.EARTH;
        }
        if (id.contains("thunder")) {
            return CultivationSpell.Element.THUNDER;
        }
        if (id.contains("soul") || id.contains("spirit") || id.contains("insight") || id.contains("moonlight") || id.contains("star_chart")) {
            return CultivationSpell.Element.SOUL;
        }
        if (id.contains("golden") || id.contains("geng_metal") || id.contains("rainbow_qi")) {
            return CultivationSpell.Element.METAL;
        }
        if (id.contains("wood") || id.contains("rejuvenation") || id.contains("grass") || id.contains("wither_bloom")) {
            return CultivationSpell.Element.WOOD;
        }
        return CultivationSpell.Element.NONE;
    }

    public static CultivationSpell byId(String rawId) {
        if (rawId == null || rawId.isBlank()) return null;
        return SPELLS.get(rawId.contains(":") ? rawId : "xiuxian:" + rawId);
    }

    public static List<CultivationSpell> all() {
        return List.copyOf(SPELLS.values());
    }

    public static List<CultivationSpell> available(CultivationData data) {
        return SPELLS.values().stream().filter(spell -> spell.availableAt(data)).toList();
    }

    public static String idList(CultivationData data) {
        List<String> names = available(data).stream()
                .map(spell -> spell.id().substring(spell.id().indexOf(':') + 1) + "（" + spell.displayName()
                        + "·" + spell.element().displayName() + "属性）")
                .toList();
        return names.isEmpty() ? "当前没有可用术法" : String.join("、", names);
    }

    public static CastResult cast(ServerPlayer player, String rawId) {
        CultivationData data = TaixuDimension.recoverTripData(player);
        CultivationSpell spell = byId(rawId);
        if (data == null || !data.isInitialized()) return CastResult.fail("请先确立修行身份。");
        if (spell == null) return CastResult.fail("未识得这门术法，请使用 /xiuxian spells 查看术法名。");
        if (!spell.availableAt(data)) {
            return CastResult.fail("当前境界或运转功法无法施展《" + spell.displayName() + "》。");
        }
        long now = player.level().getGameTime();
        Map<String, Long> playerCooldowns = COOLDOWNS.computeIfAbsent(player.getUUID(), ignored -> new LinkedHashMap<>());
        long readyAt = playerCooldowns.getOrDefault(spell.id(), 0L);
        if (readyAt > now) {
            return CastResult.fail("《" + spell.displayName() + "》还需 " + Math.max(1, (readyAt - now + 19) / 20) + " 秒才能再次施展。");
        }
        if (!data.spendTrueQi(spell.trueQiCost())) {
            return CastResult.fail("真炁不足，施展《" + spell.displayName() + "》需要 " + spell.trueQiCost() + " 点真炁。");
        }
        LivingEntity target = spell.targeted() ? nearestTarget(player, spell.range()) : null;
        if (spell.targeted() && target == null) {
            data.restoreTrueQi(spell.trueQiCost());
            return CastResult.fail("施展《" + spell.displayName() + "》需要附近有目标。");
        }
        apply(player, data, spell, target);
        playerCooldowns.put(spell.id(), now + spell.cooldownTicks());
        XiuxianNetwork.syncCultivation(player, data);
        return CastResult.success("你施展了《" + spell.displayName() + "》：" + spell.description());
    }

    private static LivingEntity nearestTarget(ServerPlayer player, int range) {
        AABB box = player.getBoundingBox().inflate(Math.max(1, range));
        return player.level().getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity != player && entity.isAlive())
                .stream().min((a, b) -> Double.compare(player.distanceToSqr(a), player.distanceToSqr(b))).orElse(null);
    }

    private static void apply(ServerPlayer player, CultivationData data, CultivationSpell spell, LivingEntity target) {
        int duration = Math.max(1, spell.durationTicks());
        int amplifier = spell.amplifier();
        switch (spell.effect()) {
            case DAMAGE -> target.hurt(player.damageSources().magic(), spell.magnitude());
            case HEAL -> player.heal(spell.magnitude());
            case RESTORE_TRUE_QI -> data.restoreTrueQi(Math.round(spell.magnitude()));
            case CLEANSE -> {
                player.removeEffect(MobEffects.WEAKNESS);
                player.removeEffect(MobEffects.DIG_SLOWDOWN);
            }
            case PUSH -> {
                if (target != null) {
                    target.push(target.getX() - player.getX(), 0.16D, target.getZ() - player.getZ());
                    target.hurtMarked = true;
                }
            }
            case EFFECT -> {
                if (target == null) {
                    player.addEffect(effectFor(spell.id(), duration, amplifier));
                } else {
                    target.addEffect(effectFor(spell.id(), duration, amplifier));
                }
            }
        }
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0D,
                player.getZ(), 12, 0.35D, 0.5D, 0.35D, 0.04D);
        player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.45F, 1.0F);
    }

    private static MobEffectInstance effectFor(String id, int duration, int amplifier) {
        if (id.contains("bright_eyes")) return new MobEffectInstance(MobEffects.NIGHT_VISION, duration, amplifier);
        if (id.contains("water_breath")) return new MobEffectInstance(MobEffects.WATER_BREATHING, duration, amplifier);
        if (id.contains("fire_ward")) return new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, amplifier);
        if (id.contains("light_body")) return new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, amplifier);
        if (id.contains("wooden_nourish")) return new MobEffectInstance(MobEffects.REGENERATION, duration, amplifier);
        if (id.contains("earth_skin")) return new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, amplifier);
        if (id.contains("golden_breath")) return new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, amplifier);
        if (id.contains("spirit_sense") || id.contains("clear_origin_insight")) return new MobEffectInstance(MobEffects.GLOWING, duration, amplifier);
        if (id.contains("frost_needle")) return new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, amplifier);
        if (id.contains("thunder_spark")) return new MobEffectInstance(MobEffects.BLINDNESS, duration, amplifier);
        if (id.contains("spirit_bind")) return new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, amplifier);
        if (id.contains("soul_shake")) return new MobEffectInstance(MobEffects.WEAKNESS, duration, amplifier);
        if (id.contains("qi_breath_guard") || id.contains("basic_breathing_cycle")) return new MobEffectInstance(MobEffects.ABSORPTION, duration, amplifier);
        return new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, amplifier);
    }

    public record CastResult(boolean success, String message) {
        static CastResult success(String message) { return new CastResult(true, message); }
        static CastResult fail(String message) { return new CastResult(false, message); }
    }
}
