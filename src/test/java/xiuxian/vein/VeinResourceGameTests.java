package xiuxian.vein;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import xiuxian.block.*;
import xiuxian.cultivation.*;
import xiuxian.recipe.*;

@GameTestHolder("xiuxian_vein")
@PrefixGameTestTemplate(false)
public final class VeinResourceGameTests {
    @GameTest(templateNamespace="xiuxian_vein", template="empty", timeoutTicks=100)
    public static void everyModelStateMatchesItsCollision(GameTestHelper h) throws Exception {
        h.assertTrue(VeinBlocks.resources().size()==81 && VeinBlocks.materials().size()==29,"Incomplete catalog");
        Path root=Path.of(System.getProperty("xiuxian.verificationRoot"),"src/main/resources/assets/xiuxian");
        for(var entry:VeinBlocks.resources().entrySet()) {
            Block block=entry.getValue().get();
            h.assertTrue(block.asItem()!=Items.AIR,"Missing block item "+entry.getKey());
            var variants=JsonParser.parseString(Files.readString(root.resolve("blockstates/"+entry.getKey()+".json"))).getAsJsonObject().getAsJsonObject("variants");
            for(BlockState state:block.getStateDefinition().getPossibleStates()) {
                var variant=variants.entrySet().stream().filter(v -> selectorMatches(v.getKey(),state)).findFirst().orElseThrow();
                if(block instanceof SlabBlock) continue;
                var model=variant.getValue().getAsJsonObject();
                var elements=JsonParser.parseString(Files.readString(root.resolve("models/block/"+model.get("model").getAsString().replace("xiuxian:block/","")+".json"))).getAsJsonObject().getAsJsonArray("elements");
                int turns=model.has("y")?model.get("y").getAsInt()/90:0;
                VoxelShape expected=Shapes.empty();
                for(var e:elements) {
                    var from=e.getAsJsonObject().getAsJsonArray("from"); var to=e.getAsJsonObject().getAsJsonArray("to");
                    double x1=from.get(0).getAsDouble(),x2=to.get(0).getAsDouble(),z1=from.get(2).getAsDouble(),z2=to.get(2).getAsDouble();
                    for(int i=0;i<turns;i++) { double a=x1,b=x2; x1=16-z2;x2=16-z1;z1=a;z2=b; }
                    expected=Shapes.or(expected,Block.box(x1,from.get(1).getAsDouble(),z1,x2,to.get(1).getAsDouble(),z2));
                }
                h.assertTrue(!Shapes.joinIsNotEmpty(expected,state.getCollisionShape(h.getLevel(),BlockPos.ZERO),BooleanOp.NOT_SAME),"Model collision mismatch "+state);
            }
        }
        h.succeed();
    }
    private static boolean selectorMatches(String selector,BlockState state) {
        if(selector.isEmpty()) return true;
        for(String term:selector.split(",")) {
            String[] pair=term.split("="); var p=state.getBlock().getStateDefinition().getProperty(pair[0]);
            if(p==null || !state.getValue(p).toString().equalsIgnoreCase(pair[1])) return false;
        }
        return true;
    }
    @GameTest(templateNamespace="xiuxian_vein", template="empty", timeoutTicks=100)
    public static void allProcessingRecipesConsumeExactInputsAndSync(GameTestHelper h) {
        var recipes=h.getLevel().getRecipeManager().getAllRecipesFor(XiuxianRecipes.VEIN_TYPE);
        h.assertTrue(recipes.size()==40,"Missing processing recipes: "+recipes.size());
        BlockPos pos=site(h);
        for(var r:recipes) {
            var be=station(h,pos,r.station());
            ItemStack input=r.input().getItems()[0].copy(); input.setCount(r.inputCount());
            ItemStack reagent=r.reagent().getItems()[0].copy(); reagent.setCount(r.reagentCount());
            boolean water = reagent.is(Items.WATER_BUCKET);
            be.setItem(0,input); be.setItem(1,reagent); be.setItem(2,new ItemStack(Items.COAL));
            tick(h,pos,be,r.ticks());
            h.assertTrue(be.getItem(0).isEmpty() && be.getItem(1).isEmpty(),"Inputs not consumed exactly: "+r.id());
            h.assertTrue(ItemStack.matches(be.getItem(3),r.output()),"Wrong output: "+r.id());
            if(water) h.assertTrue(be.getItem(4).is(Items.BUCKET),"Bucket remainder lost");
            var buffer=new FriendlyByteBuf(Unpooled.buffer());
            var serializer=new VeinProcessingRecipe.Serializer();serializer.toNetwork(buffer,r);
            var decoded=serializer.fromNetwork(r.id(),buffer);buffer.release();
            h.assertTrue(decoded.station().equals(r.station()) && decoded.ticks()==r.ticks()
                    && ItemStack.matches(decoded.output(),r.output()) && decoded.inputCount()==r.inputCount()
                    && decoded.reagentCount()==r.reagentCount(),"Recipe synchronization changed results");
        }
        h.succeed();
    }
    @GameTest(templateNamespace="xiuxian_vein", template="empty", timeoutTicks=100)
    public static void outputAndRemainderLimitsProtectInputs(GameTestHelper h) {
        BlockPos pos=site(h); var be=station(h,pos,"washer");
        be.setItem(0,material("dew_shard",2));be.setItem(1,material("filter_salt",2));be.setItem(2,new ItemStack(Items.COAL));
        be.setItem(3,material("dew_concentrate",63));tick(h,pos,be,140);
        h.assertTrue(be.getItem(0).getCount()==2 && be.getItem(2).getCount()==1 && be.progress()==0,"Full output consumed inputs/fuel");
        be.setItem(3,material("dew_concentrate",62));tick(h,pos,be,120);
        h.assertTrue(be.getItem(3).getCount()==64 && be.getItem(0).getCount()==1,"Output stack limit violated");
        be.setItem(3,ItemStack.EMPTY);be.setItem(1,new ItemStack(Items.WATER_BUCKET));be.setItem(4,new ItemStack(Items.DIRT,64));
        tick(h,pos,be,130);h.assertTrue(be.getItem(1).is(Items.WATER_BUCKET),"Blocked bucket remainder consumed reagent");
        be.setItem(4,ItemStack.EMPTY);tick(h,pos,be,120);
        h.assertTrue(be.getItem(4).is(Items.BUCKET) && be.getItem(3).getCount()==1,"Remainder did not resume");h.succeed();
    }
    @GameTest(templateNamespace="xiuxian_vein", template="empty", timeoutTicks=100)
    public static void redstonePauseReloadAndRecipeChange(GameTestHelper h) {
        BlockPos pos=site(h);var be=station(h,pos,"washer");
        be.setItem(0,material("dew_shard",3));be.setItem(1,material("filter_salt",3));be.setItem(2,new ItemStack(Items.COAL));
        tick(h,pos,be,40);int fuel=be.burnTime();
        h.getLevel().setBlock(pos.east(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);tick(h,pos,be,40);
        h.assertTrue(be.progress()==40 && be.burnTime()==fuel,"Redstone did not pause progress and fuel");
        var saved=be.saveWithoutMetadata();h.getLevel().removeBlock(pos,false);be=station(h,pos,"washer");be.load(saved);
        h.getLevel().removeBlock(pos.east(),false);tick(h,pos,be,79);
        h.assertTrue(be.getItem(3).isEmpty() && be.progress()==119,"Reload skipped/lost progress");tick(h,pos,be,1);
        h.assertTrue(be.getItem(3).getCount()==2,"Reload did not complete saved recipe");
        tick(h,pos,be,50);be.setItem(0,material("frost_shard",2));be.setItem(3,ItemStack.EMPTY);tick(h,pos,be,1);
        h.assertTrue(be.progress()==1,"Different material inherited previous progress");h.succeed();
    }
    @GameTest(templateNamespace="xiuxian_vein", template="empty", timeoutTicks=100)
    public static void automationSlotsAndStorageAreReal(GameTestHelper h) {
        BlockPos pos=site(h);var be=station(h,pos,"washer");
        var top=be.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.UP).orElseThrow(IllegalStateException::new);
        var side=be.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.NORTH).orElseThrow(IllegalStateException::new);
        var bottom=be.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.DOWN).orElseThrow(IllegalStateException::new);
        h.assertTrue(top.insertItem(0,material("dew_shard",1),false).isEmpty(),"Top rejected input");
        h.assertTrue(!side.insertItem(0,new ItemStack(Items.COAL),false).isEmpty(),"Fuel entered reagent slot");
        h.assertTrue(side.insertItem(1,new ItemStack(Items.COAL),false).isEmpty(),"Side rejected fuel");
        h.assertTrue(side.insertItem(0,material("filter_salt",1),false).isEmpty(),"Side rejected reagent");
        h.assertTrue(side.extractItem(0,1,false).isEmpty(),"Side extracted processing input");
        h.assertTrue(!bottom.insertItem(0,new ItemStack(Items.DIRT),false).isEmpty(),"Bottom inserted into output");
        tick(h,pos,be,120);h.assertTrue(bottom.extractItem(0,64,false).getCount()==2,"Bottom did not extract result");
        for(String id:List.of("ore_cache","reagent_cache","reward_cache")) {
            be=station(h,pos,id);h.assertTrue(be.getContainerSize()==27,"Storage not 27 slots");be.setItem(26,new ItemStack(Items.DIAMOND,5));
            var nbt=be.saveWithoutMetadata();h.getLevel().removeBlock(pos,false);be=station(h,pos,id);be.load(nbt);
            h.assertTrue(be.getItem(26).getCount()==5,"Storage lost last slot on reload");
        }
        h.succeed();
    }
    @GameTest(templateNamespace="xiuxian_vein", template="empty", timeoutTicks=180)
    public static void vanillaHoppersSupplyAndExtract(GameTestHelper h) {
        BlockPos pos=site(h);var be=station(h,pos,"crusher");be.setItem(1,new ItemStack(Items.FLINT));be.setItem(2,new ItemStack(Items.COAL));
        h.getLevel().setBlock(pos.above(),Blocks.HOPPER.defaultBlockState(),3);
        ((HopperBlockEntity)h.getLevel().getBlockEntity(pos.above())).setItem(0,material("dew_shard",1));
        h.getLevel().setBlock(pos.below(),Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING,Direction.NORTH),3);
        h.runAfterDelay(130,() -> {
            var hopper=(HopperBlockEntity)h.getLevel().getBlockEntity(pos.below());
            h.assertTrue(hopper.getItem(0).is(VeinBlocks.material("vein_mineral_dust")) && hopper.getItem(0).getCount()==2,"Real hoppers did not complete workflow");h.succeed();
        });
    }
    @GameTest(templateNamespace="xiuxian_vein", template="empty", timeoutTicks=100)
    public static void clustersGrowHarvestOnceAndDropCorrectLoot(GameTestHelper h) {
        BlockPos pos=site(h);var player=player(h,pos);
        for(String id:VeinLayers.IDS) {
            var state=VeinBlocks.state("vein_"+id+"_cluster");var block=(VeinClusterBlock)state.getBlock();
            h.getLevel().setBlock(pos,state,3);for(int i=0;i<200;i++) block.randomTick(h.getLevel().getBlockState(pos),h.getLevel(),pos,h.getLevel().random);
            h.assertTrue(h.getLevel().getBlockState(pos).getValue(VeinClusterBlock.AGE)==0,"Cluster grew without host rock");
            h.getLevel().setBlock(pos.below(),VeinBlocks.state("vein_"+id+"_rock"),3);
            for(int i=0;i<500 && h.getLevel().getBlockState(pos).getValue(VeinClusterBlock.AGE)<3;i++) block.randomTick(h.getLevel().getBlockState(pos),h.getLevel(),pos,h.getLevel().random);
            h.assertTrue(h.getLevel().getBlockState(pos).getValue(VeinClusterBlock.AGE)==3,"Cluster failed to grow on host");
            use(h,pos,player);h.assertTrue(h.getLevel().getBlockState(pos).getValue(VeinClusterBlock.AGE)==0,"Harvest failed to reset growth");
            int drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(pos).inflate(1)).size();
            use(h,pos,player);h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(pos).inflate(1)).size()==drops,"Repeat harvest duplicated drops");
            var ore=VeinBlocks.state("vein_"+id+"_ore");
            var loot=Block.getDrops(ore,h.getLevel(),pos,null,player,new ItemStack(Items.DIAMOND_PICKAXE));
            h.assertTrue(loot.size()==1 && loot.get(0).is(VeinBlocks.material("vein_"+id+"_shard")) && loot.get(0).getCount()==2,"Ore loot incorrect "+id);
        }
        h.succeed();
    }
    @GameTest(templateNamespace="xiuxian_vein", template="empty", timeoutTicks=100)
    public static void keysSwitchesGatesAndSensorsInteract(GameTestHelper h) {
        BlockPos pos=site(h);var player=player(h,pos);
        for(String id:List.of("azure","ember","origin")) {
            h.getLevel().setBlock(pos,VeinBlocks.state("vein_"+id+"_seal_gate"),3);
            player.setItemInHand(InteractionHand.MAIN_HAND,material("blank_key",1));use(h,pos,player);
            h.assertTrue(!h.getLevel().getBlockState(pos).getValue(VeinSealBlock.OPEN),"Blank key opened seal");
            player.setItemInHand(InteractionHand.MAIN_HAND,material(id+"_key",1));use(h,pos,player);
            h.assertTrue(h.getLevel().getBlockState(pos).getValue(VeinSealBlock.OPEN) && player.getMainHandItem().getCount()==1,"Matching key failed or was consumed");
            use(h,pos,player);h.assertTrue(!h.getLevel().getBlockState(pos).getValue(VeinSealBlock.OPEN),"Key did not close seal");
            h.getLevel().setBlock(pos.east(),VeinBlocks.state("vein_resonance_pedestal"),3);use(h,pos.east(),player);
            h.assertTrue(h.getLevel().getBlockState(pos).getValue(VeinSealBlock.OPEN),"Switch did not open linked seal");
            use(h,pos.east(),player);h.assertTrue(!h.getLevel().getBlockState(pos).getValue(VeinSealBlock.OPEN),"Switch did not close linked seal");h.getLevel().removeBlock(pos.east(),false);
        }
        h.getLevel().setBlock(pos,VeinBlocks.state("vein_assay_table"),3);h.getLevel().setBlock(pos.east(),VeinBlocks.state("vein_origin_ore"),3);
        var sensor=(VeinSensorBlock)h.getLevel().getBlockState(pos).getBlock();
        h.assertTrue(sensor.nodes(h.getLevel(),pos)>=1 && sensor.getAnalogOutputSignal(h.getLevel().getBlockState(pos),h.getLevel(),pos)>=1,"Assay sensor did not detect ore");
        h.succeed();
    }
    @GameTest(templateNamespace="xiuxian_vein", template="empty", timeoutTicks=100)
    public static void tonicsReturnBottlesAndCokeFuelsAlchemy(GameTestHelper h) throws Exception {
        BlockPos pos=site(h);var player=player(h,pos);player.setHealth(10);
        var tonic=material("spring_tonic",1);var bottle=tonic.finishUsingItem(h.getLevel(),player);
        h.assertTrue(bottle.is(Items.GLASS_BOTTLE) && player.getHealth()>10,"Tonic failed to heal/return bottle");
        var method=AlchemyFurnaceBlockEntity.class.getDeclaredMethod("fuelDuration",ItemStack.class);method.setAccessible(true);
        h.assertTrue((int)method.invoke(null,material("spirit_coke",1))==3200 && VeinStationBlockEntity.fuelTime(material("spirit_coke",1))==3200,"Coke did not link with furnaces");
        var alchemy=h.getLevel().getRecipeManager().getAllRecipesFor(XiuxianRecipes.ALCHEMY_TYPE);
        for(String id:List.of("azure","ember","dew")) h.assertTrue(alchemy.stream().anyMatch(r -> r.getId().getPath().equals("vein_"+id+"_alchemy")),"Missing alchemy link "+id);
        h.succeed();
    }
    @GameTest(templateNamespace="xiuxian_vein", template="empty", timeoutTicks=100)
    public static void containersDropContentsOnceOnReplacement(GameTestHelper h) {
        BlockPos pos=site(h);
        for(String id:List.of("crusher","reward_cache")) {
            var be=station(h,pos,id);be.setItem(be.getContainerSize()-1,new ItemStack(Items.DIAMOND,7));
            h.getLevel().setBlock(pos,Blocks.STONE.defaultBlockState(),3);
            var entities=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(pos).inflate(2));
            int diamonds=entities.stream().filter(e -> e.getItem().is(Items.DIAMOND)).mapToInt(e -> e.getItem().getCount()).sum();
            h.assertTrue(diamonds==7,"Container contents lost/duplicated on replacement "+id);
            entities.forEach(net.minecraft.world.entity.Entity::discard);
        }
        h.succeed();
    }
    @GameTest(templateNamespace="xiuxian_vein", template="empty", timeoutTicks=100)
    public static void lampsSeatsBossSensorAndDistantScan(GameTestHelper h) {
        BlockPos pos=site(h);var player=player(h,pos);
        h.getLevel().setBlock(pos,VeinBlocks.state("vein_mining_lamp"),3);use(h,pos,player);
        h.assertTrue(h.getLevel().getBlockState(pos).getLightEmission()==0,"Mining lamp did not switch off");
        use(h,pos,player);h.assertTrue(h.getLevel().getBlockState(pos).getLightEmission()==15,"Mining lamp did not switch on");
        h.getLevel().setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);
        h.getLevel().setBlock(pos,VeinBlocks.state("vein_rest_seat"),3);use(h,pos,player);
        h.assertTrue(player.isPassenger(),"Rest seat is not usable");
        h.getLevel().removeBlock(pos,false);h.assertTrue(!player.isPassenger(),"Removing seat did not dismount");
        h.getLevel().setBlock(pos,VeinBlocks.state("vein_arena_sensor"),3);
        var sensor=(VeinSensorBlock)h.getLevel().getBlockState(pos).getBlock();
        var zombie=net.minecraft.world.entity.EntityType.ZOMBIE.create(h.getLevel());
        zombie.setPos(pos.getX()+2,pos.getY(),pos.getZ()+2);h.getLevel().addFreshEntity(zombie);
        h.assertTrue(sensor.getAnalogOutputSignal(h.getLevel().getBlockState(pos),h.getLevel(),pos)>0,"Arena sensor did not detect living enemy");
        zombie.discard();h.assertTrue(sensor.getAnalogOutputSignal(h.getLevel().getBlockState(pos),h.getLevel(),pos)==0,"Arena sensor retained removed enemy");
        int loaded=h.getLevel().getChunkSource().getLoadedChunksCount();
        h.assertTrue(sensor.nodes(h.getLevel(),pos.offset(100000,0,100000))==0 && h.getLevel().getChunkSource().getLoadedChunksCount()==loaded,"Ore scan forced remote chunk loading");
        h.succeed();
    }
    private static ItemStack material(String id,int count) { return new ItemStack(VeinBlocks.material("vein_"+id),count); }
    private static BlockPos site(GameTestHelper h) { return h.absolutePos(new BlockPos(4,3,4)); }
    private static VeinStationBlockEntity station(GameTestHelper h,BlockPos pos,String id) {
        h.getLevel().removeBlock(pos,false);h.getLevel().setBlock(pos,VeinBlocks.state("vein_"+id),3);return (VeinStationBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    private static void tick(GameTestHelper h,BlockPos pos,VeinStationBlockEntity be,int ticks) { for(int i=0;i<ticks;i++) VeinStationBlockEntity.tick(h.getLevel(),pos,h.getLevel().getBlockState(pos),be); }
    private static FakePlayer player(GameTestHelper h,BlockPos pos) {
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"vein_test"));p.setGameMode(GameType.SURVIVAL);
        CultivationEvents.selectIdentity(p,FamilyOrigin.MORTAL.id(),CultivationPath.WANDERER.id());p.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);return p;
    }
    private static void use(GameTestHelper h,BlockPos pos,Player p) { var s=h.getLevel().getBlockState(pos);s.getBlock().use(s,h.getLevel(),pos,p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.NORTH,pos,false)); }
}
