package xiuxian.sect;

import com.google.gson.GsonBuilder;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import xiuxian.block.SectInstrumentBlock;
import xiuxian.block.SectLampBlock;
import xiuxian.block.SectSeatBlock;

/** Final voxel state, not just authored placement counts, drives route and decoration acceptance. */
final class SectComplexVerification {
    private static final int DX=449,DY=179,DZ=513;
    private final short[] voxels=new short[DX*DY*DZ];
    private final List<BlockState> palette=new ArrayList<>();
    private final Map<BlockState,Short> ids=new HashMap<>();
    private final List<String> problems=new ArrayList<>();
    private final SectComplexGenerator.Plan plan;

    SectComplexVerification(SectComplexGenerator.Plan plan) {
        this.plan=plan;palette.add(Blocks.AIR.defaultBlockState());ids.put(palette.get(0),(short)0);
        for(var op:plan.placements) {
            short id=ids.computeIfAbsent(op.state(),state->{palette.add(state);return(short)(palette.size()-1);});
            for(int z=op.minZ();z<=op.maxZ();z++)for(int x=op.minX();x<=op.maxX();x++)
                Arrays.fill(voxels,index(x,op.minY(),z),index(x,op.maxY(),z)+1,id);
        }
    }
    private static int index(int x,int y,int z){return ((z+256)*DX+x+224)*DY+y+24;}
    BlockState state(int x,int y,int z){return x < -224 || x > 224 || y < -24 || y > 154 || z < -256 || z > 256 ? palette.get(0) : palette.get(voxels[index(x,y,z)]);}
    void verify() throws Exception {
        export();
        require(plan.buildings.size()>=31,"Expected a complete complex, not isolated halls");
        for(var route:plan.routes) {
            int x=route.x(),y=route.floor(),z=route.z();
            require(!state(x,y,z).isAir(),"Missing route floor: "+route);
            require(state(x,y+1,z).isAir()&&state(x,y+2,z).isAir(),"Blocked route: "+route);
        }
        for(var building:plan.buildings)for(int level=0;level<building.levels();level++) {
            int y=building.floor()+level*10;
            for(int z=building.z()-building.halfZ();z<=building.z()+building.halfZ();z++) {
                require(!state(building.x(),y,z).isAir(),"Hall through-floor missing: "+building.id()+" floor "+level+" z "+z);
                require(state(building.x(),y+1,z).isAir()&&state(building.x(),y+2,z).isAir(),"Hall through-aisle blocked: "+building.id()+" floor "+level+" z "+z);
            }
            if(level<building.levels()-1)for(int step=0;step<=10;step++) {
                int x=building.x()+building.halfX()-4,z=building.z()+building.halfZ()-5-step,sy=y+step;
                require(state(x,sy,z).getBlock() instanceof StairBlock,"Tower staircase erased: "+building.id());
                require(state(x,sy+1,z).isAir()&&state(x,sy+2,z).isAir(),"Tower stair headroom blocked: "+building.id()+" step "+step);
            }
        }
        for(var entry:plan.visits.entrySet())if(!entry.getKey().equals("view")) {
            var pos=entry.getValue();require(state(pos.getX(),pos.getY(),pos.getZ()).isAir()
                    &&state(pos.getX(),pos.getY()+1,pos.getZ()).isAir()
                    &&!state(pos.getX(),pos.getY()-1,pos.getZ()).isAir(),"Unsafe visit: "+entry.getKey());
        }
        int[] counts=new int[palette.size()];for(short id:voxels)counts[id]++;
        var resources=new HashSet<String>();long occupied=0;int lights=0,seats=0,music=0;
        for(int i=1;i<counts.length;i++)if(counts[i]>0) {
            var block=palette.get(i).getBlock();String name=BuiltInRegistries.BLOCK.getKey(block).getPath();
            occupied+=counts[i];if(name.startsWith("sect_"))resources.add(name);
            if(block instanceof SectLampBlock)lights+=counts[i];
            if(block instanceof SectSeatBlock)seats+=counts[i];
            if(block instanceof SectInstrumentBlock)music+=counts[i];
        }
        require(resources.size()>=100,"Too few dedicated resources survive assembly: "+resources.size());
        require(lights>=400&&seats>=150&&music>=12,"Interactive furnishings missing: "+lights+"/"+seats+"/"+music);
        require(occupied>1_000_000,"Sect lacks estate-scale mass");
        if(!problems.isEmpty())throw new AssertionError(problems.size()+" geometry failures: "+String.join("; ",problems.stream().limit(20).toList()));
        System.out.println("SECT GEOMETRY PASS: "+plan.buildings.size()+" buildings, "+resources.size()+" dedicated resources, "+occupied+" blocks, "+lights+" lamps, "+seats+" seats, "+music+" instruments");
    }
    private void export() throws Exception {
        var output=Path.of(System.getProperty("xiuxian.verificationRoot"),"build/sect-complex-preview");Files.createDirectories(output);
        var ops=new ArrayList<int[]>();
        for(var op:plan.placements)ops.add(new int[]{op.minX(),op.minY(),op.minZ(),op.maxX(),op.maxY(),op.maxZ(),ids.get(op.state())});
        var names=palette.stream().map(s->BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString()).toList();
        var data=Map.of("bounds",new int[]{-224,-24,-256,224,154,256},"palette",names,"operations",ops,
                "buildings",plan.buildings,"visits",plan.visits.entrySet().stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,e->new int[]{e.getValue().getX(),e.getValue().getY(),e.getValue().getZ()})));
        Files.writeString(output.resolve("blueprint.json"),new GsonBuilder().create().toJson(data));
        render(output);
    }
    private static int color(BlockState state) {
        String name=BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        if(state.getBlock() instanceof SectLampBlock)return 0xf1d37b;
        if(name.contains("roof")||name.contains("ridge")||name.contains("finial"))return 0x286364;
        if(name.contains("gilded")||name.contains("gold")||name.contains("ceremony"))return 0xcba752;
        if(name.contains("vermilion")||name.contains("sunset")||name.contains("red_wool"))return 0xa64743;
        if(name.contains("water")&&!name.startsWith("sect_"))return 0x64b7c6;
        if(name.contains("herb")||name.contains("leaves")||name.contains("bamboo"))return 0x51805c;
        if(name.contains("wood")||name.contains("timber")||name.contains("log")||name.contains("chair")||name.contains("shelf"))return 0x663d40;
        if(name.equals("stone")||name.contains("foundation")||name.contains("ironstone"))return 0x777f7f;
        if(name.contains("bronze"))return 0x839c84;
        if(name.contains("white")||name.contains("plaster")||name.contains("quartz"))return 0xe3e8df;
        return 0xbbc9c0;
    }
    private void render(Path output) throws Exception {
        int width=2200,height=1500;double scale=2.3,ox=1100,oy=790;
        int[] pixels=new int[width*height];float[] depth=new float[pixels.length];
        Arrays.fill(pixels,0xe9eef0);Arrays.fill(depth,Float.NEGATIVE_INFINITY);
        int[] colors=palette.stream().mapToInt(SectComplexVerification::color).toArray();
        for(int z=-256;z<=256;z++)for(int x=-224;x<=224;x++)for(int y=-24;y<=154;y++) {
            short id=voxels[index(x,y,z)];if(id==0)continue;
            if(state(x,y+1,z).isAir())LuoxiaGeometryVerification.face(pixels,depth,width,height,ox,oy,scale,colors[id],new int[][]{{x,y+1,z},{x+1,y+1,z},{x+1,y+1,z+1},{x,y+1,z+1}});
            if(state(x+1,y,z).isAir())LuoxiaGeometryVerification.face(pixels,depth,width,height,ox,oy,scale,LuoxiaGeometryVerification.shade(colors[id],0.74),new int[][]{{x+1,y,z},{x+1,y+1,z},{x+1,y+1,z+1},{x+1,y,z+1}});
            if(state(x,y,z+1).isAir())LuoxiaGeometryVerification.face(pixels,depth,width,height,ox,oy,scale,LuoxiaGeometryVerification.shade(colors[id],0.9),new int[][]{{x,y,z+1},{x+1,y,z+1},{x+1,y+1,z+1},{x,y+1,z+1}});
        }
        var image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);image.setRGB(0,0,width,height,pixels,0,width);
        caption(image,"Luoxia Inner Sect | 449 x 513 | 31 buildings","Ordered blueprint geometry; simplified shapes/colors, not an in-game screenshot");ImageIO.write(image,"png",output.resolve("sect-isometric.png").toFile());
        var top=new BufferedImage(DX*3+60,DZ*3+110,BufferedImage.TYPE_INT_RGB);var g=top.createGraphics();
        g.setColor(new Color(0xe9eef0));g.fillRect(0,0,top.getWidth(),top.getHeight());
        for(int z=-256;z<=256;z++)for(int x=-224;x<=224;x++)for(int y=154;y>=-24;y--)if(!state(x,y,z).isAir()) {
            g.setColor(new Color(color(state(x,y,z))));g.fillRect(30+(x+224)*3,100+(z+256)*3,3,3);break;
        }
        g.dispose();caption(top,"Luoxia Inner Sect | Top view","North / library above; south / disciples and entrance below");ImageIO.write(top,"png",output.resolve("sect-top.png").toFile());
        var axis=new BufferedImage(DZ*3+60,DY*3+110,BufferedImage.TYPE_INT_RGB);g=axis.createGraphics();
        g.setColor(new Color(0xe9eef0));g.fillRect(0,0,axis.getWidth(),axis.getHeight());
        for(int z=-256;z<=256;z++)for(int y=-24;y<=154;y++)if(!state(0,y,z).isAir()) {
            g.setColor(new Color(color(state(0,y,z))));g.fillRect(30+(z+256)*3,100+(154-y)*3,3,3);
        }
        g.dispose();caption(axis,"Luoxia Inner Sect | Central-axis section","Library / main hall on left; entry on right; continuous stair ascent");ImageIO.write(axis,"png",output.resolve("sect-axis.png").toFile());
    }
    private static void caption(BufferedImage image,String title,String subtitle) {
        var g=image.createGraphics();g.setColor(new Color(0x374751));g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,24));g.drawString(title,30,40);
        g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,17));g.drawString(subtitle,30,70);g.dispose();
    }
    private void require(boolean condition,String message){if(!condition)problems.add(message);}
}
