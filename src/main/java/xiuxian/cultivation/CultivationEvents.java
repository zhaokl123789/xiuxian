package xiuxian.cultivation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import xiuxian.network.XiuxianNetwork;
import xiuxian.item.XiuxianItems;

public class CultivationEvents {
    private static final ResourceKey<Level> TAIXU_LEVEL = TaixuDimension.LEVEL;
    private static final float TAIXU_FLYING_SPEED = TaixuDimension.FLYING_SPEED;
    private static final Map<UUID, Integer> DIMENSION_SYNC_TICKS = new HashMap<>();

    @SubscribeEvent
    public void attachPlayerData(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            CultivationCapability.Provider provider = new CultivationCapability.Provider();
            event.addCapability(CultivationCapability.ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        // Forge may create the replacement entity before capability data is
        // copied. Carry the durable cultivation snapshots across explicitly so
        // the replacement cannot regress to the default identity.
        event.getEntity().getPersistentData().merge(event.getOriginal().getPersistentData().copy());
        event.getOriginal().getCapability(CultivationCapability.CULTIVATION).ifPresent(original ->
                event.getEntity().getCapability(CultivationCapability.CULTIVATION)
                        .ifPresent(copy -> {
                            copy.copyFrom(original);
                            if (event.isWasDeath() && original.isInitialized()
                                    && original.realm().ordinal() >= CultivationRealm.PURPLE_MANSION.ordinal()) {
                                copy.resetForDeath();
                            }
                            if (event.getEntity() instanceof ServerPlayer player && copy.isInitialized()) {
                                CultivationAttributeEffects.applyAndPreserveHealth(player, copy);
                                CultivationAttributeEffects.sync(player);
                            }
                            if (event.getEntity() instanceof ServerPlayer player && copy.isInitialized()) {
                                TaixuDimension.persistCultivationData(player, copy);
                            }
                        }));
        if (event.isWasDeath()) {
            if (event.getOriginal() instanceof ServerPlayer originalPlayer) {
                TaixuDimension.clearTripSnapshot(originalPlayer);
            }
            if (event.getEntity() instanceof ServerPlayer respawnedPlayer) {
                TaixuDimension.clearTripSnapshot(respawnedPlayer);
            }
        }
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        CultivationData data = TaixuDimension.isTaixu(player.level())
                ? TaixuDimension.recoverTripData(player) : getData(player);
        if (data == null) {
            return;
        }
        if (data.isInitialized()) {
            CultivationAttributeEffects.applyAndPreserveHealth(player, data);
            CultivationAttributeEffects.sync(player);
            DIMENSION_SYNC_TICKS.put(player.getUUID(), 20);
        }
        XiuxianNetwork.syncCultivation(player, data);
        if (!data.isInitialized()) {
            XiuxianNetwork.openIdentityScreen(player);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CultivationData data = getData(player);
            if (data != null) {
                if (data.isInitialized()) {
                    CultivationAttributeEffects.applyAndPreserveHealth(player, data);
                    CultivationAttributeEffects.sync(player);
                    DIMENSION_SYNC_TICKS.put(player.getUUID(), 20);
                }
                XiuxianNetwork.syncCultivation(player, data);
                if (!data.isInitialized()) {
                    player.sendSystemMessage(Component.literal("身死道消，仙缘尽断；请重新择取出身。"));
                    XiuxianNetwork.openIdentityScreen(player);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        CultivationData data = TaixuDimension.isTaixu(player.level())
                ? TaixuDimension.recoverTripData(player) : getData(player);
        if (data == null || !data.isInitialized()) {
            if (TaixuDimension.isTaixu(player.level())) {
                // A newly transferred ServerPlayer can expose its capability one
                // tick after the dimension event. A valid snapshot means the
                // trip is still recoverable, so never eject it during that gap.
                if (TaixuDimension.hasValidTripSnapshot(player)) {
                    return;
                }
                TaixuDimension.returnToWorld(player, data);
            }
            player.setNoGravity(false);
            if (!player.isCreative() && !player.isSpectator()
                    && (player.getAbilities().mayfly || player.getAbilities().flying)) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
            CultivationAttributeEffects.remove(player);
            if (data != null) {
                data.stopMeditating();
            }
            return;
        }

        Integer syncTicks = DIMENSION_SYNC_TICKS.get(player.getUUID());
        if (syncTicks != null) {
            CultivationAttributeEffects.applyAndPreserveHealth(player, data);
            CultivationAttributeEffects.sync(player);
            XiuxianNetwork.syncCultivation(player, data);
            if (syncTicks <= 1) {
                DIMENSION_SYNC_TICKS.remove(player.getUUID());
            } else {
                DIMENSION_SYNC_TICKS.put(player.getUUID(), syncTicks - 1);
            }
        }

        if (TaixuDimension.isTaixu(player.level())) {
            TaixuDimension.ensureTaixuState(player, data);
        } else {
            // Dimension changes can briefly rebuild the vanilla attribute map.
            // Reapply the persisted modifiers until the player is back on the
            // cultivation values instead of leaving the default 20 health.
            CultivationAttributeEffects.applyAndPreserveHealth(player, data);
        }

        player.setNoGravity(false);
        tickRealmMovement(player, data);
        tickCultivationHunger(player, data);
        if (player.isAlive() && !data.isMeditating() && !data.isStudyingTechnique()) {
            applyPassiveRecovery(player, data);
        }
        if ((data.isMeditating() || data.isStudyingTechnique()) && player.isPassenger()) {
            if (data.isMeditating()) {
                endMeditation(player, data);
                player.sendSystemMessage(Component.literal("乘骑打断了你的入定。"));
            }
            if (data.isStudyingTechnique()) {
                endTechniqueStudy(player, data, "乘骑打断了你的参悟，典籍未损。");
            }
            XiuxianNetwork.syncCultivation(player, data);
        }
        if (data.isMeditating() || data.isStudyingTechnique()) {
            double dx = player.getX() - data.meditationAnchorX();
            double dy = player.getY() - data.meditationAnchorY();
            double dz = player.getZ() - data.meditationAnchorZ();
            if (dx * dx + dy * dy + dz * dz > 0.0625D) {
                interruptChannel(player);
                player.sendSystemMessage(Component.literal("你主动移动，行功随之中断。"));
                return;
            }
            if (data.isMeditating()) {
                CultivationAttributeEffects.setMeditating(player, true);
                player.setShiftKeyDown(true);
            }
            player.setSprinting(false);
            player.setDeltaMovement(0.0D, 0.0D, 0.0D);

            if (data.isMeditating() && player.tickCount % 10 == 0) {
                showMeditationAura(player);
            }
            if (data.isMeditating() && player.tickCount % 80 == 0) {
                playMeditationNote(player);
            }

            if (data.isStudyingTechnique()) {
                boolean complete = data.tickTechniqueStudy();
                if (player.tickCount % 10 == 0) {
                    showTechniqueVision(player);
                }
                if (player.tickCount % 60 == 0) {
                    playMeditationNote(player);
                }
                if (complete) {
                    finishTechniqueStudy(player, data);
                } else if (player.tickCount % 5 == 0) {
                    XiuxianNetwork.syncCultivation(player, data);
                }
            }
        } else {
            CultivationAttributeEffects.setMeditating(player, false);
        }

        if (data.tickMeditation()) {
            XiuxianNetwork.syncCultivation(player, data);
            if (data.qi() % 10 == 0) {
                player.sendSystemMessage(Component.literal("吐纳渐进，当前修为：" + data.qi()));
            }
        }
        if (TaixuDimension.isTaixu(player.level())) {
            TaixuDimension.saveTripSnapshot(player, data);
        }
        TaixuDimension.persistCultivationData(player, data);
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("xiuxian")
                .then(Commands.literal("identity").executes(context -> identity(context.getSource())))
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(Commands.literal("techniques").executes(context -> techniques(context.getSource())))
                .then(Commands.literal("exchange")
                        .then(Commands.argument("technique", StringArgumentType.word())
                                .executes(context -> exchangeTechnique(context.getSource(),
                                        StringArgumentType.getString(context, "technique")))))
                .then(Commands.literal("meditate").executes(context -> toggleMeditation(context.getSource())))
                .then(Commands.literal("breakthrough").executes(context -> breakthrough(context.getSource())))
                .then(Commands.literal("return").executes(context -> returnFromTaixu(context.getSource())))
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal(
                            "命令：/xiuxian identity、/xiuxian status、/xiuxian meditate、/xiuxian breakthrough、/xiuxian return"), false);
                    return 1;
                }));
    }

    public static boolean selectIdentity(ServerPlayer player, String familyId, String pathId) {
        FamilyOrigin family = FamilyOrigin.byId(familyId);
        CultivationPath path = CultivationPath.byId(pathId);
        if (family == null || path == null) {
            return false;
        }

        CultivationData data = getData(player);
        if (data == null) {
            return false;
        }
        if (data.isInitialized()) {
            return false;
        }

        data.begin(family, path, player.getRandom());
        grantStartingKit(player, family, path);
        player.setNoGravity(false);
        XiuxianNetwork.syncCultivation(player, data);
        String techniqueStatus = data.hasLearnedTechnique()
                ? "你已习得入门功法《吐纳引气诀》。"
                : "你尚无功法，需寻得纸墨自制《吐纳引气诀》。";
        player.sendSystemMessage(Component.literal("你以人族之身踏入修行路，出身：" + family.displayName()
                + "，身份：" + path.displayName() + "。" + techniqueStatus));
        XiuxianNetwork.closeIdentityScreen(player);
        return true;
    }

    private static int identity(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CultivationData data = getData(player);
        if (data == null) {
            source.sendFailure(Component.literal("无法读取修行数据。"));
            return 0;
        }
        if (data.isInitialized()) {
            source.sendFailure(Component.literal("你的修行身份已经确立，暂不支持重新选择。"));
            return 0;
        }
        XiuxianNetwork.openIdentityScreen(player);
        return 1;
    }

    @SubscribeEvent
    public void onUninitializedPlayerBreaksBlock(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && isChanneling(player)) {
            interruptChannel(player);
        }
    }

    @SubscribeEvent
    public void onUninitializedPlayerInteracts(PlayerInteractEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CultivationData data = getData(player);
            if (data != null && data.isMeditating()) {
                interruptChannel(player);
            }
        }
    }

    @SubscribeEvent
    public void onUninitializedPlayerAttacks(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isChanneling(player)) {
            interruptChannel(player);
        }
    }

    @SubscribeEvent
    public void onUninitializedPlayerTakesDamage(LivingHurtEvent event) {
        if (!event.getEntity().level().isClientSide) {
            if (event.getSource().getEntity() instanceof ServerPlayer attacker
                    && event.getEntity() != attacker) {
                applyTechniqueCombatEffect(attacker, event.getEntity(), event);
            }
        }
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.level().isClientSide) {
            CultivationData data = getData(player);
            if (data != null) {
                if (data.isMeditating() && player instanceof ServerPlayer serverPlayer) {
                    endMeditation(serverPlayer, data);
                    XiuxianNetwork.syncCultivation(serverPlayer, data);
                    serverPlayer.sendSystemMessage(Component.literal("受外力惊扰，你暂时退出了入定。"));
                }
                if (data.isStudyingTechnique() && player instanceof ServerPlayer serverPlayer) {
                    endTechniqueStudy(serverPlayer, data, "受外力惊扰，你中断了参悟，典籍未损。");
                    XiuxianNetwork.syncCultivation(serverPlayer, data);
                }
                CultivationTechnique technique = CultivationTechniques.byId(data.techniqueId());
                float techniqueReduction = data.techniqueDamageReduction();
                if (technique != null && technique.elementalAffinity().equals("土")) {
                    techniqueReduction += 0.10F;
                }
                if (technique != null && technique.drawbackTrueQiCostPercent() > 0
                        && data.trueQi() < Math.max(1, data.trueQiMaximum() / 10)) {
                    techniqueReduction = Math.max(0.0F, techniqueReduction - 0.05F);
                }
                float reduction = Math.min(0.9F, data.constitution() * 0.004F + techniqueReduction
                        + data.realm().damageReductionAt(data.realmLevel()));
                event.setAmount(event.getAmount() * (1.0F - reduction));
            }
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && (event.getFrom().equals(TAIXU_LEVEL) || TaixuDimension.isTaixu(player.level()))) {
            TaixuDimension.onDimensionChanged(player);
            DIMENSION_SYNC_TICKS.put(player.getUUID(), 8);
            if (TaixuDimension.isTaixu(player.level())) {
                discoverTaixuInheritance(player);
            }
        }
    }

    private static void discoverTaixuInheritance(ServerPlayer player) {
        CultivationData data = TaixuDimension.recoverTripData(player);
        if (data == null || !data.isInitialized() || player.getRandom().nextInt(100) >= 18) return;
        CultivationTechnique technique = CultivationTechniques.randomAdventureTechnique(data, player.getRandom());
        if (technique == null) return;
        var manual = XiuxianItems.manualForTechnique(technique.id());
        if (manual == null) return;
        ItemStack stack = new ItemStack(manual.get());
        if (!player.getInventory().add(stack)) player.drop(stack, false);
        player.sendSystemMessage(Component.literal("太虚深处浮出一卷残金古册：你获得奇遇传承《"
                + technique.displayName() + "》。"));
        player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F, 1.2F);
    }

    private static int returnFromTaixu(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!TaixuDimension.isTaixu(player.level())) {
            source.sendFailure(Component.literal("你当前不在太虚之中。"));
            return 0;
        }
        TaixuDimension.returnToWorld(player, getData(player));
        return 1;
    }

    @SubscribeEvent
    public void onPassiveNaturalHealing(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player) || event.getAmount() != 1.0F
                || player.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION)) return;
        CultivationData data = player instanceof ServerPlayer serverPlayer
                && TaixuDimension.isTaixu(player.level())
                ? TaixuDimension.recoverTripData(serverPlayer) : getData(player);
        if (data != null && data.isInitialized() && !data.isTrueQiHealthRecovery()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onCultivatorJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CultivationData data = getData(player);
            performJumpEnhancement(player);
            if (data != null && data.isInitialized()
                    && data.realm().ordinal() >= CultivationRealm.QI_REFINING.ordinal()) {
                XiuxianNetwork.syncTrueQi(player, data.trueQi());
            }
        }
    }

    public static void performJumpEnhancement(ServerPlayer player) {
        CultivationData data = TaixuDimension.isTaixu(player.level())
                ? TaixuDimension.recoverTripData(player) : getData(player);
        if (data == null || !data.isInitialized()
                || data.realm().ordinal() < CultivationRealm.QI_REFINING.ordinal()) return;
        if (player.isPassenger() || player.isFallFlying()) return;
        int cost = 4 + data.realm().ordinal() * 2;
        if (data.trueQi() < cost || !data.canUseJumpBoostAt(player.tickCount)) return;
        if (!data.spendTrueQi(cost)) return;
        double lift = 0.18D + data.realm().ordinal() * 0.05D;
        player.setDeltaMovement(player.getDeltaMovement().add(0.0D, lift, 0.0D));
    }

    private static void grantStartingKit(ServerPlayer player, FamilyOrigin family, CultivationPath path) {
        if (family.receivesStartingManual(path)) {
            giveStartingItem(player, new ItemStack(XiuxianItems.BASIC_BREATHING_MANUAL.get()));
        }
        switch (family) {
            case MORTAL -> {
                giveStartingItem(player, new ItemStack(Items.WOODEN_PICKAXE));
                giveStartingItem(player, new ItemStack(Items.BREAD, 4));
                if (path == CultivationPath.SECT) {
                    giveStartingItem(player, new ItemStack(XiuxianItems.QI_GATHERING_PILL.get()));
                } else {
                    giveStartingItem(player, new ItemStack(Items.BREAD, 2));
                }
            }
            case CULTIVATOR -> {
                giveStartingItem(player, new ItemStack(XiuxianItems.QI_GATHERING_PILL.get()));
                giveStartingItem(player, new ItemStack(XiuxianItems.SPIRIT_STONE.get(), 2));
                if (path == CultivationPath.SECT) {
                    giveStartingItem(player, new ItemStack(XiuxianItems.QI_GATHERING_PILL.get()));
                } else {
                    giveStartingItem(player, new ItemStack(Items.BREAD, 2));
                }
            }
            case FALLEN -> {
                giveStartingItem(player, new ItemStack(XiuxianItems.QI_GATHERING_PILL.get()));
                giveStartingItem(player, new ItemStack(XiuxianItems.SPIRIT_STONE.get()));
                giveStartingItem(player, new ItemStack(Items.BREAD, 2));
                if (path == CultivationPath.SECT) {
                    giveStartingItem(player, new ItemStack(XiuxianItems.QI_GATHERING_PILL.get()));
                } else {
                    giveStartingItem(player, new ItemStack(Items.BREAD, 2));
                }
            }
        }
    }

    private static void giveStartingItem(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private static void applyTechniqueCombatEffect(ServerPlayer attacker, LivingEntity target,
                                                   LivingHurtEvent event) {
        CultivationData data = getData(attacker);
        if (data == null || !data.isInitialized()) return;
        CultivationTechnique technique = CultivationTechniques.byId(data.techniqueId());
        if (technique == null) return;
        if (technique.elementalAffinity().equals("火")) {
            if (target.getRemainingFireTicks() > 20 || !data.spendTrueQi(12)) return;
            target.setSecondsOnFire(4 + data.realm().ordinal() * 2);
            event.setAmount(event.getAmount() * 1.25F);
            attacker.level().playSound(null, target.blockPosition(), SoundEvents.FIRECHARGE_USE,
                    SoundSource.PLAYERS, 0.45F, 1.15F);
            XiuxianNetwork.syncCultivation(attacker, data);
        } else if (technique.elementalAffinity().equals("金")) {
            event.setAmount(event.getAmount() * 1.15F);
        }
    }

    private static int exchangeTechnique(CommandSourceStack source, String rawId) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CultivationData data = getData(player);
        if (data == null || !data.isInitialized()) {
            source.sendFailure(Component.literal("尚未确立修行身份，无法兑换传承。"));
            return 0;
        }
        String id = rawId.startsWith("xiuxian:") ? rawId : "xiuxian:" + rawId;
        CultivationTechnique technique = CultivationTechniques.byId(id);
        if (technique == null) {
            source.sendFailure(Component.literal("没有找到这门功法，使用功法短名查询，例如 azurewood_return。"));
            return 0;
        }
        if (technique.exchangeCost() <= 0) {
            source.sendFailure(Component.literal("《" + technique.displayName() + "》只会在奇遇中出现，不能通过兑换获得。"));
            return 0;
        }
        if (!technique.isCompatibleWithPath(data.cultivationPath())) {
            source.sendFailure(Component.literal("你的道途不承认《" + technique.displayName() + "》，它属于"
                    + technique.sectName() + "。"));
            return 0;
        }
        if (!technique.canBeLearnedAt(data.realm())) {
            source.sendFailure(Component.literal("当前境界还无法承载《" + technique.displayName() + "》。"));
            return 0;
        }
        if (data.prerequisiteMissing(technique)) {
            source.sendFailure(Component.literal("兑换《" + technique.displayName() + "》需要先掌握前置传承。"));
            return 0;
        }
        if (data.hasLearnedTechnique(technique.id())) {
            source.sendFailure(Component.literal("你已经掌握《" + technique.displayName() + "》。"));
            return 0;
        }
        if (!consumeSpiritStones(player, technique.exchangeCost())) {
            source.sendFailure(Component.literal("灵石不足，需要 " + technique.exchangeCost() + " 枚灵石。"));
            return 0;
        }
        var manual = XiuxianItems.manualForTechnique(technique.id());
        if (manual == null) {
            source.sendFailure(Component.literal("这门功法的传承卷尚未落册。"));
            return 0;
        }
        ItemStack stack = new ItemStack(manual.get());
        if (!player.getInventory().add(stack)) player.drop(stack, false);
        source.sendSuccess(() -> Component.literal("你以灵石向" + technique.sectName() + "换得《"
                + technique.displayName() + "》，请手持典籍蹲下参悟。"), false);
        return 1;
    }

    private static boolean consumeSpiritStones(ServerPlayer player, int cost) {
        int remaining = cost;
        int[] values = {64, 16, 4, 1};
        net.minecraft.world.item.Item[] items = {
                XiuxianItems.SUPREME_SPIRIT_STONE.get(), XiuxianItems.HIGH_SPIRIT_STONE.get(),
                XiuxianItems.MID_SPIRIT_STONE.get(), XiuxianItems.SPIRIT_STONE.get()};
        for (int i = 0; i < items.length; i++) {
            remaining -= player.getInventory().countItem(items[i]) * values[i];
        }
        if (remaining > 0) return false;
        remaining = cost;
        for (int i = 0; i < items.length && remaining > 0; i++) {
            for (int slot = 0; slot < player.getInventory().items.size() && remaining > 0; slot++) {
                ItemStack stack = player.getInventory().items.get(slot);
                if (!stack.is(items[i])) continue;
                int take = Math.min(stack.getCount(), (remaining + values[i] - 1) / values[i]);
                stack.shrink(take);
                remaining -= take * values[i];
            }
        }
        return true;
    }

    private static int status(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CultivationData data = getData(player);
        if (data == null || !data.isInitialized()) {
            source.sendFailure(Component.literal("你尚未选择修行身份。使用 /xiuxian identity 查看选项。"));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("种族：人类 | 出身：" + data.familyOrigin().displayName()
                + " | 修行身份：" + data.cultivationPath().displayName()), false);
        source.sendSuccess(() -> Component.literal("境界：" + data.realm().displayName() + " "
                + data.realm().stageLabel(data.realmLevel()) + " | 修为：" + data.qi() + "/" + data.breakthroughCost()), false);
        source.sendSuccess(() -> Component.literal("真炁：" + data.trueQi() + "/" + data.trueQiMaximum()
                + " | 炼丹师：" + data.alchemyLevel() + "级"), false);
        source.sendSuccess(() -> Component.literal("功法：" + data.techniqueName() + " | 状态："
                + (data.isMeditating() ? "打坐中" : "未打坐")), false);
        return 1;
    }

    private static int techniques(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CultivationData data = getData(player);
        if (data == null || !data.isInitialized()) {
            source.sendFailure(Component.literal("\u5c1a\u672a\u9009\u62e9\u4fee\u884c\u8eab\u4efd\uff0c\u65e0\u6cd5\u8bc4\u4f30\u529f\u6cd5\u5951\u5408\u3002"));
            return 0;
        }
        java.util.List<CultivationTechnique> recommendations = CultivationTechniques.recommendations(data, 3);
        if (recommendations.isEmpty()) {
            source.sendSuccess(() -> Component.literal("\u5f53\u524d\u5883\u754c\u6ca1\u6709\u53ef\u76f4\u63a5\u53c2\u609f\u7684\u65b0\u529f\u6cd5\uff0c\u8bf7\u5bfb\u627e\u66f4\u9ad8\u9636\u4f20\u627f\u3002"), false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal("\u6309\u5f53\u524d\u7075\u6839\u3001\u6839\u9aa8\u3001\u609f\u6027\u4e0e\u6c14\u8fd0\uff0c\u8f83\u9002\u5408\u4f60\u7684\u529f\u6cd5\uff1a"), false);
        for (int i = 0; i < recommendations.size(); i++) {
            CultivationTechnique technique = recommendations.get(i);
            int rank = i + 1;
            int aptitude = technique.meditationAptitude().value(data.spiritualRoot(), data.constitution(),
                    data.comprehension(), data.fortune());
            source.sendSuccess(() -> Component.literal(rank + ". \u300a" + technique.displayName() + "\u300b"
                    + " \u00b7 \u9002\u4fee " + technique.realmRangeLabel()
                    + " \u00b7 \u96be\u5ea6 " + technique.learningDifficultyLabel()
                    + " \u00b7 " + technique.meditationAptitude().displayName() + " " + aptitude
                    + " \u00b7 " + technique.combatStyle()), false);
        }
        source.sendSuccess(() -> Component.literal("\u83b7\u5f97\u5bf9\u5e94\u529f\u6cd5\u4e66\u540e\uff0c\u53f3\u952e\u5c55\u5f00\u9605\u8bfb\uff1b\u8e72\u4e0b\u53f3\u952e\u5f00\u59cb\u53c2\u609f\u3002"), false);
        return 1;
    }

    private static int toggleMeditation(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CultivationData data = getData(player);
        if (data == null || !data.isInitialized()) {
            source.sendFailure(Component.literal("先选择身份：使用 /xiuxian identity 并点击一项出身。"));
            return 0;
        }

        if (data.isMeditating()) {
            endMeditation(player, data);
            source.sendSuccess(() -> Component.literal("你结束了打坐，积累修为：" + data.qi()), false);
        } else {
            if (data.isStudyingTechnique()) {
                source.sendFailure(Component.literal("你正在参悟功法，暂时无法入定吐纳。"));
                return 0;
            }
            if (!data.hasLearnedTechnique()) {
                source.sendFailure(Component.literal("你尚未习得功法，无法行功吐纳。"));
                return 0;
            }
            if (!player.onGround() || player.isPassenger()) {
                source.sendFailure(Component.literal("需先落地并离开乘坐状态，才能盘膝入定。"));
                return 0;
            }
            data.startMeditating(player.getX(), player.getY(), player.getZ(), player.isShiftKeyDown());
            CultivationAttributeEffects.setMeditating(player, true);
            player.setShiftKeyDown(true);
            player.setSprinting(false);
            source.sendSuccess(() -> Component.literal("你盘膝入定，运转" + data.techniqueName()
                    + "。气机每数息凝成一点修为，受击会中断行功。"), false);
        }
        XiuxianNetwork.syncCultivation(player, data);
        return 1;
    }

    private static int breakthrough(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CultivationData data = getData(player);
        if (data == null || !data.isInitialized()) {
            source.sendFailure(Component.literal("先选择身份：使用 /xiuxian identity 并点击一项出身。"));
            return 0;
        }

        endMeditation(player, data);
        if (data.qi() < data.breakthroughCost()) {
            source.sendFailure(Component.literal("修为不足，需要 " + data.breakthroughCost() + " 点，目前有 " + data.qi() + " 点。"));
            return 0;
        }
        CultivationRealm targetRealm = data.breakthroughTargetRealm();
        if (targetRealm == null) {
            source.sendFailure(Component.literal("你已达到当前境界体系的上限。"));
            return 0;
        }
        if (!data.canBreakthroughWithCurrentTechnique()) {
            if (targetRealm == CultivationRealm.PURPLE_MANSION) {
                CultivationTechnique technique = CultivationTechniques.byId(data.techniqueId());
                if (technique != null && technique.matchesImmortalFoundation(data.immortalFoundation())) {
                    source.sendFailure(Component.literal("当前功法最高适修至"
                            + technique.maximumRealm().displayName() + "；还需参悟可修至紫府的功法。"));
                } else {
                    source.sendFailure(Component.literal("冲击紫府须与仙基相合或由其生扶。当前仙基为"
                            + data.immortalFoundation() + "，请参悟契基功法后再行冲关。"));
                }
                return 0;
            }
            CultivationTechnique technique = CultivationTechniques.byId(data.techniqueId());
            String techniqueName = technique == null ? "当前功法" : "《" + technique.displayName() + "》";
            String limit = technique == null ? "无可用境界" : technique.maximumRealm().displayName();
            source.sendFailure(Component.literal(techniqueName + "最高适修至" + limit
                    + "，需另行参悟适合" + targetRealm.displayName() + "的功法后再突破。"));
            return 0;
        }
        CultivationRealm oldRealm = data.realm();
        int oldRealmLevel = data.realmLevel();
        int breakthroughChance = data.breakthroughChance();
        CultivationData.BreakthroughResult result = data.breakthrough(player.getRandom());
        if (!result.success()) {
            if (result.lostQi() > 0) {
                String fatalRisk = result.fatalRiskChance() > 0
                        ? "本次陨落风险 " + result.fatalRiskChance() + "%；" : "";
                source.sendFailure(Component.literal("大境界冲关未成，成功率 " + breakthroughChance
                        + "%；" + fatalRisk + "气海受震，损耗修为 " + result.lostQi() + " 点。累计失败 "
                        + result.failures() + " 次，后续冲关更艰难。"));
                if (result.fatalRiskChance() > 0) {
                    if (result.fatal()) {
                        player.sendSystemMessage(Component.literal("冲关反噬直击神魂，你道基崩毁，身死道消！"));
                    } else {
                        float backlash = (float) Math.max(1.0D, player.getMaxHealth()
                                * (0.28D + oldRealm.ordinal() * 0.08D));
                        player.hurt(player.damageSources().magic(), backlash);
                        player.sendSystemMessage(Component.literal("冲关反噬震伤道基，承受 "
                                + Math.round(backlash) + " 点气血损伤；本次陨落风险 "
                                + result.fatalRiskChance() + "%。"));
                    }
                }
            } else {
                source.sendFailure(Component.literal("当前修为不足以完成突破。"));
            }
            XiuxianNetwork.syncCultivation(player, data);
            if (result.fatal()) player.kill();
            return 0;
        }

        boolean majorBreakthrough = oldRealm != data.realm();
        CultivationAttributeEffects.applyAfterBreakthrough(player, data);
        showBreakthroughAura(player, oldRealm, oldRealmLevel, majorBreakthrough);
        XiuxianNetwork.syncCultivation(player, data);
        source.sendSuccess(() -> Component.literal((majorBreakthrough ? "大境界突破成功！" : "小境界突破成功！")
                + "当前境界：" + data.realm().displayName()
                + data.realm().stageLabel(data.realmLevel())
                + (data.realm() == CultivationRealm.FOUNDATION_ESTABLISHMENT
                ? "；仙基初成，属" + data.immortalFoundation() + "。" : "。")), false);
        return 1;
    }

    private static CultivationData getData(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            return TaixuDimension.recoverTripData(serverPlayer);
        }
        return player.getCapability(CultivationCapability.CULTIVATION).orElse(null);
    }

    private static void tickRealmMovement(ServerPlayer player, CultivationData data) {
        boolean abilitiesChanged = false;
        int trueQiBefore = data.trueQi();
        var abilities = player.getAbilities();
        if (player.level().dimension().equals(TAIXU_LEVEL)) {
            if (data.realm().ordinal() < CultivationRealm.PURPLE_MANSION.ordinal()) {
                TaixuDimension.returnToWorld(player, data);
                return;
            }
            if (!abilities.mayfly || !abilities.flying) {
                abilities.mayfly = true;
                abilities.flying = true;
                abilitiesChanged = true;
            }
            if (Math.abs(abilities.getFlyingSpeed() - TAIXU_FLYING_SPEED) > 0.0001F) {
                abilities.setFlyingSpeed(TAIXU_FLYING_SPEED);
                abilitiesChanged = true;
            }
            if (player.tickCount % 20 == 0) {
                int travelCost = Math.max(1, 4 - data.comprehension() / 30);
                if (!data.spendTrueQi(travelCost)) {
                    player.sendSystemMessage(Component.literal("真炁将尽，太虚界层正在送你返世。"));
                    TaixuDimension.returnToWorld(player, data);
                    return;
                }
                XiuxianNetwork.syncTrueQi(player, data.trueQi());
                TaixuDimension.saveTripSnapshot(player, data);
            }
            if (abilitiesChanged) player.onUpdateAbilities();
            CultivationAttributeEffects.applyAndPreserveHealth(player, data);
            return;
        } else if (data.hasTaixuAnchor()) {
            abilities.setFlyingSpeed(data.taixuOriginFlyingSpeed());
            data.clearTaixuAnchor();
        }

        boolean canFly = data.realm().ordinal() >= CultivationRealm.FOUNDATION_ESTABLISHMENT.ordinal();
        if (!player.isCreative() && !player.isSpectator() && canFly && !abilities.mayfly) {
            abilities.mayfly = true;
            abilitiesChanged = true;
        }

        if (!player.isCreative() && !player.isSpectator() && abilities.flying
                && player.tickCount % 20 == 0) {
            int flightCost = 8 + Math.max(0,
                    data.realm().ordinal() - CultivationRealm.FOUNDATION_ESTABLISHMENT.ordinal()) * 5;
            if (!data.spendTrueQi(flightCost)) {
                abilities.flying = false;
                abilitiesChanged = true;
                player.sendSystemMessage(Component.literal("真炁枯竭，遁空之势散去。"));
            }
        }

        if (player.isSprinting() && player.tickCount % 20 == 0) {
            int sprintCost = 1 + data.realm().ordinal();
            data.spendTrueQi(sprintCost);
        }
        if (abilitiesChanged) player.onUpdateAbilities();
        if (data.trueQi() != trueQiBefore) XiuxianNetwork.syncCultivation(player, data);
        CultivationAttributeEffects.apply(player, data);
    }

    public static void performVoidWalk(ServerPlayer player) {
        TaixuDimension.toggle(player);
    }

    private static void performLegacyVoidWalk(ServerPlayer player) {
        CultivationData data = getData(player);
        if (data == null || !data.isInitialized() || data.realm() != CultivationRealm.PURPLE_MANSION) {
            player.sendSystemMessage(Component.literal("唯有紫府真人，方可踏入太虚。"));
            return;
        }
        if (data.isMeditating() || data.isStudyingTechnique() || player.isPassenger()) {
            player.sendSystemMessage(Component.literal("当前心神未定，不能行太虚步。"));
            return;
        }

        var level = player.serverLevel();
        var look = player.getLookAngle();
        double horizontalLength = Math.sqrt(look.x * look.x + look.z * look.z);
        if (horizontalLength < 1.0E-4D) {
            player.sendSystemMessage(Component.literal("先定下行路方向，方可施展太虚步。"));
            return;
        }
        double directionX = look.x / horizontalLength;
        double directionZ = look.z / horizontalLength;
        int destinationX = 0;
        int destinationY = 0;
        int destinationZ = 0;
        boolean found = false;
        for (int distance : new int[]{10_000, 5_000, 2_500, 1_000, 512, 256, 128, 64}) {
            int x = (int) Math.floor(player.getX() + directionX * distance);
            int z = (int) Math.floor(player.getZ() + directionZ * distance);
            if (!level.getWorldBorder().isWithinBounds(new BlockPos(x, player.blockPosition().getY(), z))) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y <= level.getMinBuildHeight() || y + 2 >= level.getMaxBuildHeight()) continue;
            BlockPos feet = new BlockPos(x, y, z);
            if (!level.getBlockState(feet.below()).blocksMotion()
                    || !level.getFluidState(feet).isEmpty()
                    || !level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                    || !level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) continue;
            double dx = x + 0.5D - player.getX();
            double dy = y - player.getY();
            double dz = z + 0.5D - player.getZ();
            if (!level.noCollision(player, player.getBoundingBox().move(dx, dy, dz))) continue;
            destinationX = x;
            destinationY = y;
            destinationZ = z;
            found = true;
            break;
        }
        if (!found) {
            player.sendSystemMessage(Component.literal("前方无可落足之处，太虚步未能成行。"));
            return;
        }
        int cost = 180;
        if (!data.spendTrueQi(cost)) {
            player.sendSystemMessage(Component.literal("太虚步需要 180 点真炁，当前真炁不足。"));
            return;
        }
        double oldX = player.getX();
        double oldY = player.getY();
        double oldZ = player.getZ();
        player.teleportTo(destinationX + 0.5D, destinationY, destinationZ + 0.5D);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, oldX, oldY + 0.8D, oldZ,
                36, 0.7D, 0.8D, 0.7D, 0.08D);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 0.8D,
                player.getZ(), 36, 0.7D, 0.8D, 0.7D, 0.08D);
        player.playNotifySound(SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 0.8F);
        player.sendSystemMessage(Component.literal("一步踏入太虚，缩地而行。"));
        XiuxianNetwork.syncCultivation(player, data);
    }

    private static void applyPassiveRecovery(ServerPlayer player, CultivationData data) {
        if (player.tickCount % 200 == 0 && !TaixuDimension.isTaixu(player.level())) {
            int previousTrueQi = data.trueQi();
            data.restoreTrueQi(data.passiveTrueQiRecoveryPerTenSeconds());
            if (data.trueQi() != previousTrueQi) {
                XiuxianNetwork.syncCultivation(player, data);
            }
        }

        int healthInterval = data.passiveHealthRecoveryIntervalTicks();
        if (healthInterval > 0 && player.tickCount % healthInterval == 0
                && player.getHealth() < player.getMaxHealth()
                && data.spendTrueQi(data.healthRecoveryTrueQiCost())) {
            data.setTrueQiHealthRecovery(true);
            try {
                player.heal(1.0F);
            } finally {
                data.setTrueQiHealthRecovery(false);
            }
            XiuxianNetwork.syncCultivation(player, data);
        }
    }

    private static void tickCultivationHunger(ServerPlayer player, CultivationData data) {
        int foodLevel = player.getFoodData().getFoodLevel();
        if (!player.isAlive() || !data.foodLevelDecreasedTo(foodLevel)) return;
        if (data.spendTrueQi(data.hungerTrueQiCost())) {
            int restoredFood = Math.min(20, foodLevel + 1);
            player.getFoodData().setFoodLevel(restoredFood);
            data.synchronizeObservedFoodLevel(restoredFood);
            XiuxianNetwork.syncCultivation(player, data);
        }
    }

    private static void endMeditation(ServerPlayer player, CultivationData data) {
        if (!data.isMeditating()) {
            CultivationAttributeEffects.setMeditating(player, false);
            return;
        }
        boolean wasCrouching = data.wasCrouchingBeforeMeditation();
        data.stopMeditating();
        CultivationAttributeEffects.setMeditating(player, false);
        player.setShiftKeyDown(wasCrouching);
    }

    public static void interruptChannel(ServerPlayer player) {
        CultivationData data = getData(player);
        if (data == null || (!data.isMeditating() && !data.isStudyingTechnique())) return;
        if (data.isMeditating()) endMeditation(player, data);
        if (data.isStudyingTechnique()) endTechniqueStudy(player, data, "你主动行动，中断了参悟，典籍未损。 ");
        XiuxianNetwork.syncCultivation(player, data);
    }

    private static void finishTechniqueStudy(ServerPlayer player, CultivationData data) {
        String id = data.studyingTechniqueId();
        int chance = data.techniqueStudyChance();
        int roll = data.techniqueStudyRoll();
        boolean wasCrouching = data.wasCrouchingBeforeMeditation();
        CultivationTechnique technique = CultivationTechniques.byId(id);
        ItemStack manual = findTechniqueManual(player, id);
        data.stopTechniqueStudy();
        player.setShiftKeyDown(wasCrouching);

        if (technique != null && !manual.isEmpty() && roll <= chance && data.learnTechnique(id)) {
            manual.shrink(1);
            CultivationAttributeEffects.applyAndPreserveHealth(player, data);
            player.sendSystemMessage(Component.literal("参悟有成！你已习得《" + technique.displayName()
                    + "》，道意：" + technique.doctrine()));
            player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.45F, 1.25F);
            player.serverLevel().sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0D,
                    player.getZ(), 18, 0.55D, 0.7D, 0.55D, 0.02D);
        } else if (technique != null && manual.isEmpty() && roll <= chance) {
            player.sendSystemMessage(Component.literal("典籍已不在身边，参悟无从印证；本次未能习得功法。"));
            player.serverLevel().sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 0.8D,
                    player.getZ(), 10, 0.4D, 0.5D, 0.4D, 0.01D);
        } else if (technique != null) {
            player.sendSystemMessage(Component.literal("心有所悟，却未能贯通《" + technique.displayName()
                    + "》。本次成功率 " + chance + "%（判定 " + roll + "），典籍仍在，可静心再试。"));
            player.serverLevel().sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 0.8D,
                    player.getZ(), 10, 0.4D, 0.5D, 0.4D, 0.01D);
        }
        XiuxianNetwork.syncCultivation(player, data);
    }

    private static void endTechniqueStudy(ServerPlayer player, CultivationData data, String message) {
        boolean wasCrouching = data.wasCrouchingBeforeMeditation();
        data.stopTechniqueStudy();
        player.setShiftKeyDown(wasCrouching);
        player.sendSystemMessage(Component.literal(message));
    }

    private static ItemStack findTechniqueManual(ServerPlayer player, String techniqueId) {
        ItemStack offhand = player.getOffhandItem();
        if (isTechniqueManual(offhand, techniqueId)) {
            return offhand;
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isTechniqueManual(stack, techniqueId)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean isTechniqueManual(ItemStack stack, String techniqueId) {
        return !stack.isEmpty() && stack.getItem() instanceof xiuxian.item.TechniqueManualItem manual
                && manual.techniqueId().equals(techniqueId);
    }

    private static void playMeditationNote(ServerPlayer player) {
        float[] notes = {0.72F, 0.86F, 1.0F, 1.12F, 1.0F, 0.86F};
        float pitch = notes[(player.tickCount / 80) % notes.length];
        player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.get(),
                SoundSource.PLAYERS, 0.12F, pitch);
    }

    private static void showMeditationAura(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        double orbit = player.tickCount * 0.025D;
        double[] radii = {1.25D, 2.45D};
        int[] points = {8, 12};
        for (int ring = 0; ring < radii.length; ring++) {
            for (int i = 0; i < points[ring]; i++) {
                double angle = orbit * (ring == 0 ? -1.0D : 1.0D)
                        + Math.PI * 2.0D * i / points[ring];
                double x = player.getX() + Math.cos(angle) * radii[ring];
                double z = player.getZ() + Math.sin(angle) * radii[ring];
                double y = player.getY() + (ring == 0 ? 0.18D : 0.68D)
                        + Math.sin(angle * 2.0D + orbit) * 0.12D;
                var particle = ring == 0 ? ParticleTypes.ENCHANT : ParticleTypes.END_ROD;
                level.sendParticles(particle, x, y, z, 1, 0.0D, 0.025D, 0.0D, 0.008D);
            }
        }
        for (int i = 0; i < 3; i++) {
            double angle = orbit * 1.6D + Math.PI * 2.0D * i / 3.0D;
            double x = player.getX() + Math.cos(angle) * 0.38D;
            double z = player.getZ() + Math.sin(angle) * 0.38D;
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, player.getY() + 0.45D + i * 0.35D,
                    z, 1, 0.0D, 0.04D, 0.0D, 0.005D);
        }
    }

    private static void showBreakthroughAura(ServerPlayer player, CultivationRealm oldRealm,
                                             int oldRealmLevel, boolean major) {
        ServerLevel level = player.serverLevel();
        if (major) {
            CultivationRealm nextRealm = oldRealm.next();
            ParticleOptions sceneParticle = breakthroughParticle(nextRealm == null ? oldRealm : nextRealm);
            int ringPoints = 20 + (nextRealm == null ? 0 : nextRealm.ordinal() * 4);
            double radius = 3.1D + (nextRealm == null ? 0 : nextRealm.ordinal() * 0.18D);
            for (int i = 0; i < ringPoints; i++) {
                double angle = Math.PI * 2.0D * i / ringPoints;
                level.sendParticles(sceneParticle,
                        player.getX() + Math.cos(angle) * radius, player.getY() + 0.12D,
                        player.getZ() + Math.sin(angle) * radius, 1, 0.0D, 0.08D, 0.0D, 0.015D);
            }
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0D,
                    player.getZ(), 30 + ringPoints, 1.0D, 1.0D, 1.0D, 0.06D);
            level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.0D,
                    player.getZ(), 18 + ringPoints / 2, 0.8D, 0.8D, 0.8D, 0.02D);
            player.playNotifySound(SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.35F, 1.1F);
        } else {
            int ringPoints = 8 + oldRealmLevel * 2;
            double radius = 1.1D + oldRealmLevel * 0.075D;
            ParticleOptions sceneParticle = breakthroughParticle(oldRealm);
            for (int i = 0; i < ringPoints; i++) {
                double angle = Math.PI * 2.0D * i / ringPoints + oldRealmLevel * 0.19D;
                level.sendParticles(i % 4 == 0 ? ParticleTypes.END_ROD : sceneParticle,
                        player.getX() + Math.cos(angle) * radius, player.getY() + 0.12D + oldRealmLevel * 0.045D,
                        player.getZ() + Math.sin(angle) * radius, 1, 0.0D, 0.035D, 0.0D, 0.01D);
            }
            player.playNotifySound(SoundEvents.NOTE_BLOCK_CHIME.get(), SoundSource.PLAYERS, 0.4F, 1.2F);
        }
    }

    private static ParticleOptions breakthroughParticle(CultivationRealm realm) {
        return switch (realm) {
            case FETAL_BREATH -> ParticleTypes.ENCHANT;
            case QI_REFINING -> ParticleTypes.END_ROD;
            case FOUNDATION_ESTABLISHMENT -> ParticleTypes.SOUL_FIRE_FLAME;
            case PURPLE_MANSION -> ParticleTypes.WITCH;
            case GOLDEN_CORE -> ParticleTypes.TOTEM_OF_UNDYING;
            case DAO_TAI -> ParticleTypes.REVERSE_PORTAL;
        };
    }

    private static void showTechniqueVision(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        double orbit = player.tickCount * 0.045D;
        for (int i = 0; i < 8; i++) {
            double angle = orbit + Math.PI * 2.0D * i / 8.0D;
            double x = player.getX() + Math.cos(angle) * 0.8D;
            double z = player.getZ() + Math.sin(angle) * 0.8D;
            double y = player.getY() + 0.25D + (i % 4) * 0.35D;
            level.sendParticles(i % 2 == 0 ? ParticleTypes.ENCHANT : ParticleTypes.END_ROD,
                    x, y, z, 1, 0.0D, 0.02D, 0.0D, 0.01D);
        }
    }

    private static boolean isChanneling(Player player) {
        CultivationData data = getData(player);
        return data != null && (data.isMeditating() || data.isStudyingTechnique());
    }
}
