package xiuxian.sect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import xiuxian.block.SectBlocks;
import xiuxian.block.XiuxianBlocks;

/** Standalone acceptance blueprint; origins remain independent of the cave-heaven landmarks. */
public final class SectComplexGenerator {
    public static final int VERSION = 1;
    public static final int MIN_X = -224, MAX_X = 224, MIN_Z = -256, MAX_Z = 256;
    public static final int MIN_Y = -24, MAX_Y = 154;
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();

    public record Placement(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state) {}
    public record Building(String id, int x, int floor, int z, int halfX, int halfZ, int levels) {}
    public record RoutePoint(int x, int floor, int z, boolean north, int halfWidth) {}
    public static final class Plan {
        final List<Placement> placements = new ArrayList<>();
        final List<Building> buildings = new ArrayList<>();
        final List<RoutePoint> routes = new ArrayList<>();
        final Map<String, BlockPos> visits = new LinkedHashMap<>();
        final List<SiteClearance.Region> clearances = new ArrayList<>(List.of(new SiteClearance.Region(MIN_X,MIN_Y,MIN_Z,MAX_X,MAX_Z)));

        void add(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
            var op = new Placement(Math.min(x1,x2), Math.min(y1,y2), Math.min(z1,z2),
                    Math.max(x1,x2), Math.max(y1,y2), Math.max(z1,z2), state);
            if (op.minX < MIN_X || op.maxX > MAX_X || op.minZ < MIN_Z || op.maxZ > MAX_Z
                    || op.minY < MIN_Y || op.maxY > MAX_Y) throw new IllegalArgumentException("Sect placement outside site: " + op);
            placements.add(op);
        }
    }

    private SectComplexGenerator() {}

    public static boolean generate(ServerLevel level, BlockPos origin) {
        if (level == null || origin == null || level.dimension() != Level.OVERWORLD || !validSite(level, origin)) return false;
        return SectComplexConstruction.start(level, origin, createPlan());
    }

    static boolean validSite(ServerLevel level, BlockPos origin) {
        if ((long) origin.getY() + MIN_Y < level.getMinBuildHeight()
                || (long) origin.getY() + MAX_Y >= level.getMaxBuildHeight()) return false;
        for (int x : new int[]{MIN_X, MAX_X}) for (int z : new int[]{MIN_Z, MAX_Z})
            if (!level.getWorldBorder().isWithinBounds(origin.offset(x,0,z))) return false;
        return true;
    }

    public static Plan createPlan() {
        var p = new Plan();
        landscape(p);
        terrace(p, 0, 12, 185, 54, 30, "cloud_paving");
        terrace(p, 0, 24, 88, 62, 42, "ceremony_floor");
        terrace(p, 0, 46, -30, 96, 50, "white_jade");
        terrace(p, 0, 70, -165, 66, 38, "sunset_marble");
        terrace(p, 0, 84, -224, 58, 25, "lecture_floor");
        for (int side : new int[]{-1,1}) {
            terrace(p, side*126, 30, 62, 61, 63, "cloud_paving");
            terrace(p, side*147, 46, -31, 49, 43, "star_floor");
            terrace(p, side*141, 70, -133, 58, 47, side < 0 ? "fireproof_brick" : "forge_ironstone");
            terrace(p, side*147, 84, -215, 59, 31, "lotus_floor");
            terrace(p, side*124, 8, 208, 72, 35, "discipline_floor");
        }
        hall(p,"gate",0,12,188,28,10,18,1,"gate");
        hall(p,"main",0,70,-166,48,28,25,2,"main");
        hall(p,"lecture",-124,30,40,32,21,16,1,"lecture");
        hall(p,"administration",124,30,40,32,21,16,1,"administration");
        hall(p,"tea",-124,30,101,21,13,12,1,"tea");
        hall(p,"music",124,30,101,21,13,12,1,"music");
        hall(p,"alchemy",-141,70,-131,28,22,17,1,"alchemy");
        hall(p,"forge",141,70,-131,28,22,17,1,"forge");
        tower(p,"library",0,84,-230,26,17,4);
        tower(p,"bell",-74,46,-34,10,10,3);
        tower(p,"drum",74,46,-34,10,10,3);
        for (int side : new int[]{-1,1}) {
            for (int x : new int[]{74,124,174}) for (int z : new int[]{195,226})
                hall(p,"disciple_"+side+"_"+x+"_"+z,side*x,8,z,13,9,9,1,"disciple");
            for (int x : new int[]{119,177})
                hall(p,"elder_"+side+"_"+x,side*x,84,-215,17,17,12,1,"elder");
            ritualCourt(p,side*147,46,-31,side);
        }
        herbGarden(p);
        waterGarden(p);
        pavilion(p,"waterside",147,16,155,12,12);
        pavilion(p,"garden",-147,16,155,12,12);
        pavilion(p,"west_view",-193,70,-76,10,10);
        pavilion(p,"east_view",193,70,-76,10,10);
        courtGardens(p);
        roads(p);
        // Furnishings come after all terrain/shell work, so overlapping roofs and decks cannot erase them.
        for (var building : p.buildings) furnish(p,building);
        p.visits.put("entrance",new BlockPos(0,5,240));
        p.visits.put("main",new BlockPos(0,71,-144));
        p.visits.put("library",new BlockPos(0,85,-217));
        p.visits.put("music",new BlockPos(124,31,106));
        p.visits.put("garden",new BlockPos(-147,17,168));
        p.visits.put("water",new BlockPos(147,17,168));
        p.visits.put("view",new BlockPos(218,145,248));
        return p;
    }

    public static int axisHeight(int z) {
        int[][] nodes = {{240,4},{212,4},{200,12},{148,12},{124,24},{68,24},{24,46},
                {-80,46},{-104,58},{-128,70},{-194,70},{-208,84},{-242,84}};
        if (z >= nodes[0][0]) return nodes[0][1];
        for (int i=1;i<nodes.length;i++) if (z>=nodes[i][0]) {
            int[] a=nodes[i-1], b=nodes[i];
            return a[1]+(a[0]-z)*(b[1]-a[1])/(a[0]-b[0]);
        }
        return nodes[nodes.length-1][1];
    }

    private static void landscape(Plan p) {
        // A tapered cliff mass supports the terraces; its silhouette is not a rectangular city slab.
        for (int z=-252;z<=248;z++) {
            int width = 85 + (int)(95*Math.pow(Math.sin((z+252)*Math.PI/500),0.5));
            int top = axisHeight(z)-3;
            for (int band=0;band<5;band++) {
                if(top-band*9<MIN_Y)continue;
                int extent = Math.min(218,width+band*7);
                int low = Math.max(MIN_Y,top-12-band*9);
                fill(p,-extent,low,z,extent,top-band*9,z,STONE);
            }
        }
        for (int side : new int[]{-1,1}) for (int z : new int[]{-216,-132,-30,62,206}) {
            int floor = z<-190 ? 84 : z<-80 ? 70 : z<0 ? 46 : z<130 ? 30 : 8;
            disk(p,side*140,floor-8,z,65,7,STONE);
        }
    }

    private static void terrace(Plan p,int x,int y,int z,int hx,int hz,String floor) {
        fill(p,x-hx,y-3,z-hz,x+hx,y-1,z+hz,m("dark_foundation"));
        fill(p,x-hx,y,z-hz,x+hx,y,z+hz,m(floor));
        for (int dx=-hx;dx<=hx;dx+=4) {
            if (Math.abs(dx)<9) continue;
            set(p,x+dx,y+1,z-hz,m("jade_railing"));
            set(p,x+dx,y+1,z+hz,m("jade_railing"));
        }
        for (int dz=-hz+4;dz<hz;dz+=4) if(Math.abs(dz)>8) {
            set(p,x-hx,y+1,z+dz,face("bronze_railing",Direction.EAST));
            set(p,x+hx,y+1,z+dz,face("bronze_railing",Direction.EAST));
        }
        for (int dx : new int[]{-hx+4,hx-4}) for(int dz=-hz+4;dz<=hz-4;dz+=12)
            set(p,x+dx,y+1,z+dz,m("courtyard_lamp"));
    }

    private static void hall(Plan p,String id,int x,int y,int z,int hx,int hz,int h,int roofs,String role) {
        p.buildings.add(new Building(id,x,y,z,hx,hz,1));
        fill(p,x-hx-3,y-2,z-hz-3,x+hx+3,y,z+hz+3,m("white_jade"));
        fill(p,x-hx+1,y,z-hz+1,x+hx-1,y,z+hz-1,m(floorFor(role)));
        if (!role.equals("gate")) {
            fill(p,x-hx,y+1,z-hz,x+hx,y+h-1,z-hz,m("plaster_wall"));
            fill(p,x-hx,y+1,z+hz,x+hx,y+h-1,z+hz,m("plaster_wall"));
            fill(p,x-hx,y+1,z-hz,x-hx,y+h-1,z+hz,m("plaster_wall"));
            fill(p,x+hx,y+1,z-hz,x+hx,y+h-1,z+hz,m("plaster_wall"));
        }
        for(int dx=-hx;dx<=hx;dx+=8) {
            if(Math.abs(dx)<5)continue;
            column(p,x+dx,y+1,z-hz,y+h);
            column(p,x+dx,y+1,z+hz,y+h);
            if(dx+4>-hx+3 && dx+4<hx-3 && Math.abs(dx+4)>8) for(int dz : new int[]{-hz,hz}) {
                fill(p,x+dx+2,y+3,z+dz,x+dx+6,y+6,z+dz,Blocks.GLASS.defaultBlockState());
                set(p,x+dx+4,y+4,z+dz,face("cloud_window",Direction.NORTH));
            }
        }
        for(int dz=-hz+8;dz<hz;dz+=8) for(int side:new int[]{-1,1}) column(p,x+side*hx,y+1,z+dz,y+h);
        for(int dz:new int[]{-hz,hz}) {
            fill(p,x-4,y+1,z+dz-1,x+4,y+7,z+dz+1,AIR);
            fill(p,x-5,y+8,z+dz,x+5,y+9,z+dz,m("vermilion_timber"));
            set(p,x,y+9,z+dz+(dz<0?-1:1),m(role.equals("lecture")?"lecture_plaque":"main_plaque"));
            set(p,x-6,y+2,z+dz,m(role.equals("main")?"hall_door":"library_door"));
            set(p,x+6,y+2,z+dz,m("watercourt_door"));
        }
        // The upper roof is a smaller, raised volume above a continuous lower eave band.
        roof(p,x,y+h,z,hx+5,hz+5,roofs>1 ? hx-8 : -1,roofs>1 ? hz-8 : -1);
        if(roofs>1) {
            for(int side:new int[]{-1,1}) {
                fill(p,x-hx+8,y+h,z+side*(hz-8),x+hx-8,y+h+8,z+side*(hz-8),m("vermilion_timber"));
                fill(p,x+side*(hx-8),y+h,z-hz+8,x+side*(hx-8),y+h+8,z+hz-8,m("vermilion_timber"));
            }
            roof(p,x,y+h+8,z,hx-3,hz-3,-1,-1);
        }
    }

    private static String floorFor(String role) {
        return switch(role) {
            case "lecture" -> "lecture_floor"; case "administration" -> "discipline_floor";
            case "alchemy" -> "fireproof_brick"; case "forge" -> "forge_ironstone";
            case "music","tea" -> "rosewood_board"; case "elder" -> "lotus_floor";
            case "disciple" -> "cedar_beam"; default -> "ceremony_floor";
        };
    }

    private static void column(Plan p,int x,int y,int z,int top) {
        fill(p,x-1,y,z-1,x+1,top,z+1,m("vermilion_timber"));
        set(p,x,y,z-2,m("pillar_base"));
        set(p,x,top,z-2,m("hall_dougong"));
        set(p,x,top+1,z,m("pillar_cap"));
    }

    private static void roof(Plan p,int x,int y,int z,int hx,int hz,int holeX,int holeZ) {
        for(int dz=-hz;dz<=hz;dz++) {
            int start=-hx;
            while(start<=hx) {
                int edge=Math.min(hx-Math.abs(start),hz-Math.abs(dz));
                int lift=Math.min(12,edge/2)+(Math.abs(start)>hx-4&&Math.abs(dz)>hz-4?3:0);
                boolean hole=holeX>=0&&Math.abs(start)<=holeX&&Math.abs(dz)<=holeZ;
                int end=start;
                while(end<hx) {
                    int ne=Math.min(hx-Math.abs(end+1),hz-Math.abs(dz));
                    int nl=Math.min(12,ne/2)+(Math.abs(end+1)>hx-4&&Math.abs(dz)>hz-4?3:0);
                    boolean nh=holeX>=0&&Math.abs(end+1)<=holeX&&Math.abs(dz)<=holeZ;
                    if(nl!=lift||nh!=hole||(ne<1)!=(edge<1))break;
                    end++;
                }
                if(!hole)fill(p,x+start,y+lift,z+dz,x+end,y+lift,z+dz,m(edge<1?"gilded_panel":"blue_roof_base"));
                start=end+1;
            }
        }
        if(holeX<0)for(int dx=-Math.max(0,hx-hz);dx<=Math.max(0,hx-hz);dx++)set(p,x+dx,y+Math.min(12,hz/2)+1,z,m("main_ridge"));
        for(int side:new int[]{-1,1}) {
            set(p,x+side*hx,y+4,z-hz,m("dragon_finial"));
            set(p,x+side*hx,y+4,z+hz,m("phoenix_finial"));
        }
        for(int dx=-hx+6;dx<hx;dx+=8)for(int side:new int[]{-1,1})set(p,x+dx,y+1,z+side*hz,m("blue_roof_tiles"));
    }

    private static void tower(Plan p,String id,int x,int floor,int z,int hx,int hz,int levels) {
        p.buildings.add(new Building(id,x,floor,z,hx,hz,levels));
        for(int level=0;level<levels;level++) {
            int y=floor+level*10;
            fill(p,x-hx,y,z-hz,x+hx,y,z+hz,m("scroll_wall"));
            for(int side:new int[]{-1,1}) {
                fill(p,x-hx,y+1,z+side*hz,x+hx,y+7,z+side*hz,m("plaster_wall"));
                fill(p,x+side*hx,y+1,z-hz,x+side*hx,y+7,z+hz,m("plaster_wall"));
            }
            for(int dx:new int[]{-hx,hx})for(int dz:new int[]{-hz,hz})column(p,x+dx,y+1,z+dz,y+8);
            for(int dz:new int[]{-hz,hz})fill(p,x-3,y+1,z+dz-1,x+3,y+5,z+dz+1,AIR);
            roof(p,x,y+8,z,hx+4,hz+4,level==levels-1?-1:hx,level==levels-1?-1:hz);
        }
        // Floors and roofs are complete before stair wells are cut, including three blocks of headroom.
        for(int level=0;level<levels-1;level++)for(int step=0;step<=10;step++) {
            int sx=x+hx-4, sy=floor+level*10+step, sz=z+hz-5-step;
            fill(p,sx-1,sy+1,sz,sx+1,sy+3,sz,AIR);
            fill(p,sx-1,sy,sz,sx+1,sy,sz,Blocks.SMOOTH_QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.NORTH));
        }
    }

    private static void pavilion(Plan p,String id,int x,int floor,int z,int hx,int hz) {
        p.buildings.add(new Building(id,x,floor,z,hx,hz,1));
        fill(p,x-hx,floor-1,z-hz,x+hx,floor,z+hz,m("watercourt_tile"));
        for(int dx:new int[]{-hx+2,hx-2})for(int dz:new int[]{-hz+2,hz-2})column(p,x+dx,floor+1,z+dz,floor+11);
        roof(p,x,floor+12,z,hx+4,hz+4,-1,-1);
    }

    private static void ritualCourt(Plan p,int x,int y,int z,int side) {
        disk(p,x,y,z,41,1,m("carved_stone"));
        for(int radius:new int[]{28,35})for(int dz=-radius;dz<=radius;dz++) {
            int dx=(int)Math.sqrt(radius*radius-dz*dz);
            set(p,x-dx,y,z+dz,m("gilded_panel"));set(p,x+dx,y,z+dz,m("gilded_panel"));
        }
        set(p,x,y+1,z,m(side<0?"cloud_array":"five_elements_array"));
        for(int dx:new int[]{-22,22})for(int dz:new int[]{-22,22})set(p,x+dx,y+1,z+dz,m("guardian_banner"));
        for(int dz=-22;dz<=22;dz+=11)set(p,x+side*29,y+1,z+dz,m("training_target"));
    }

    private static void herbGarden(Plan p) {
        terrace(p,-147,16,147,51,36,"herb_border");
        for(int x=-186;x<=-110;x+=16)for(int z=124;z<=143;z+=8) {
            fill(p,x,16,z,x+9,16,z+3,m("herb_border"));
            for(int dx=1;dx<9;dx+=2)set(p,x+dx,17,z+1,m("herb_planter"));
        }
        set(p,-104,17,145,m("garden_stele"));
    }

    private static void waterGarden(Plan p) {
        terrace(p,147,16,147,51,36,"watercourt_tile");
        fill(p,109,15,120,187,15,174,m("white_jade"));
        fill(p,110,16,121,186,16,173,WATER);
        // The pavilion and nine-block crossing are raised above the contained pool.
        for(int z=114;z<=179;z++)fill(p,143,16,z,151,16,z,m("cloud_paving"));
        for(int z=125;z<=174;z+=12)set(p,154,17,z,m("lotus_basin"));
        for(int side:new int[]{-1,1}) {
            int x=side*204,z=-80,top=70;
            fill(p,x-3,MIN_Y,z-2,x+3,top,z+2,m("carved_stone"));
            fill(p,x-2,MIN_Y+1,z+3,x+2,top-1,z+3,WATER);
            fill(p,x-7,MIN_Y,z+3,x+7,MIN_Y,z+10,m("carved_stone"));
            fill(p,x-6,MIN_Y+1,z+3,x+6,MIN_Y+1,z+9,WATER);
        }
    }

    private static void roads(Plan p) {
        for(int z=-242;z<=242;z++) {
            int floor=axisHeight(z);
            roadCell(p,0,floor,z,Direction.NORTH,floor>axisHeight(z+1),5);
            if(z%12==0)for(int side:new int[]{-1,1})set(p,side*9,floor+1,z,m("bridge_lamp"));
        }
        for(int side:new int[]{-1,1}) {
            branch(p,side,214,4,8,174);
            branch(p,side,164,12,16,147);
            branch(p,side,80,24,30,124);
            branch(p,side,-24,46,46,147);
            branch(p,side,-166,70,70,193);
            branch(p,side,-236,84,84,177);
            for(int z=183;z<=239;z++)roadCell(p,side*124,8,z,Direction.NORTH,false,3);
            for(int x:new int[]{74,174})for(int z=183;z<=239;z++)roadCell(p,side*x,8,z,Direction.NORTH,false,2);
            for(int z=15;z<=115;z++)roadCell(p,side*124,30,z,Direction.NORTH,false,3);
            for(int z=-171;z<=-109;z++)roadCell(p,side*141,70,z,Direction.NORTH,false,3);
            for(int z=-236;z<=-193;z++)for(int x:new int[]{119,177})roadCell(p,side*x,84,z,Direction.NORTH,false,2);
            for(int z=148;z<=181;z++)roadCell(p,side*147,16,z,Direction.NORTH,false,3);
            for(int z=-166;z<=-76;z++)roadCell(p,side*193,70,z,Direction.NORTH,false,3);
        }
    }

    private static void branch(Plan p,int side,int z,int from,int to,int end) {
        int previous=from;
        for(int dx=0;dx<=end;dx++) {
            int height=from+Math.min(to-from,Math.max(0,dx-40)/2);
            roadCell(p,side*dx,height,z,side<0?Direction.WEST:Direction.EAST,height>previous,3);
            if(dx%12==0&&dx>12)for(int edge:new int[]{-1,1})set(p,side*dx,height+1,z+edge*6,m("corridor_sconce"));
            previous=height;
        }
        for(int dx=68;dx<=end-15;dx++) {
            if(z==-24&&dx<90)continue;
            int y=to+8;
            for(int dz=-6;dz<=6;dz++)set(p,side*dx,y+(6-Math.abs(dz))/2,z+dz,m(Math.abs(dz)==6?"gilded_panel":"blue_roof_base"));
            if(dx%12==8)for(int dz:new int[]{-5,5}) {
                fill(p,side*dx,to+1,z+dz,side*dx,y-1,z+dz,m("vermilion_timber"));
                set(p,side*dx,y-1,z+dz,m("corridor_dougong"));
                set(p,side*dx,y-2,z+dz,m("red_lantern"));
            }
        }
    }

    private static void roadCell(Plan p,int x,int floor,int z,Direction direction,boolean stair,int half) {
        boolean north=direction.getAxis()==Direction.Axis.Z;
        BlockState state=stair?Blocks.SMOOTH_QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING,direction):m("cloud_paving");
        fill(p,x-(north?half:0),floor,z-(north?0:half),x+(north?half:0),floor,z+(north?0:half),state);
        fill(p,x-(north?half:0),floor+1,z-(north?0:half),x+(north?half:0),floor+3,z+(north?0:half),AIR);
        p.routes.add(new RoutePoint(x,floor,z,north,half));
    }

    private static void furnish(Plan p,Building b) {
        int x=b.x,y=b.floor+1,z=b.z,hx=b.halfX,hz=b.halfZ;
        String id=b.id;
        String[] chairs={"elder_throne","master_chair","lecture_chair","disciple_chair","corridor_bench","watercourt_bench","meditation_cushion","lotus_cushion"};
        String[] lamps={"red_lantern","gold_lantern","blue_lantern","lotus_lantern","library_lantern","watercourt_lantern",
                "hall_lamp","courtyard_lamp","bridge_lamp","herb_lamp","ceremony_lamp","star_lamp","disciple_lamp","tea_lamp",
                "hall_sconce","library_sconce","forge_sconce","corridor_sconce","lotus_sconce","water_sconce"};
        int variant=p.buildings.indexOf(b);
        for(int dx=-hx+5;dx<=hx-5;dx+=10)for(int dz=-hz+5;dz<=hz-5;dz+=10) {
            prop(p,x+dx,y+4,z+dz,m(lamps[Math.floorMod(variant+dx+dz,lamps.length)]));
            if(Math.abs(dx)>4 && Math.abs(dz)>3)prop(p,x+dx,y,z+dz,face(chairs[Math.floorMod(variant+dx+dz,chairs.length)],Direction.SOUTH));
        }
        String[] props = switch(id) {
            case "main" -> new String[]{"master_chair","elder_throne","ancestral_tablet","offering_table","grand_incense_burner","lotus_incense_burner","dragon_incense_burner","lineage_stele","oath_stele","ceremony_banner","hall_pillar","bronze_pillar"};
            case "lecture" -> new String[]{"lecture_desk","scribe_desk","manual_shelf","lecture_banner","scripture_stele","lecture_pillar","brush_stand","lecture_chair"};
            case "administration" -> new String[]{"registration_desk","seal_stand","map_stand","discipline_stele","discipline_floor","sunset_banner","bronze_panel","gilded_panel"};
            case "music" -> new String[]{"qin_table","jade_qin","bronze_se","moon_pipa","bamboo_flute","jade_xiao","cloud_sheng","ritual_chimes","jade_chimes","morning_bell","evening_drum","bronze_gong"};
            case "tea" -> new String[]{"tea_table","tea_service","cloud_screen","sunset_screen","bamboo_screen","tea_lamp","wash_basin","spring_basin"};
            case "alchemy" -> new String[]{"pill_desk","herb_cabinet","pill_cabinet","herb_planter","herb_lamp","lotus_cushion","five_elements_array","lotus_incense_burner"};
            case "forge" -> new String[]{"forge_desk","tool_shelf","forge_sconce","formation_core","star_array","bronze_gong","forge_ironstone","fireproof_brick"};
            case "library" -> new String[]{"scroll_shelf","manual_shelf","scribe_desk","brush_stand","star_screen","library_sconce","scroll_wall","star_floor"};
            case "bell" -> new String[]{"morning_bell","ritual_chimes","ceremony_lamp","ceremony_banner"};
            case "drum" -> new String[]{"evening_drum","bronze_gong","guardian_banner","star_lamp"};
            case "gate" -> new String[]{"guardian_lion","guardian_crane","dragon_guardian","sunset_banner","oath_stele","main_plaque"};
            default -> new String[]{"disciple_chair","lecture_desk","robe_shelf","tea_service","meditation_cushion","bamboo_planter","red_maple_bonsai","spirit_pine_bonsai"};
        };
        for(int i=0;i<props.length;i++) {
            int px=x+(i%2==0?-1:1)*(hx-4), pz=z-hz+4+(i/2)*3;
            prop(p,px,y,pz,face(props[i],i%2==0?Direction.EAST:Direction.WEST));
        }
        if(id.equals("alchemy"))prop(p,x-10,y,z,XiuxianBlocks.ALCHEMY_FURNACE_SPIRIT.get().defaultBlockState());
        if(id.equals("forge")) {
            prop(p,x-10,y,z,Blocks.FURNACE.defaultBlockState());prop(p,x+10,y,z,Blocks.ANVIL.defaultBlockState());
            prop(p,x+10,y,z+5,Blocks.CRAFTING_TABLE.defaultBlockState());
        }
        if(id.startsWith("disciple_")) {
            for(int side:new int[]{-1,1})prop(p,x+side*(hx-3),y,z,m("scroll_shelf"));
            for(int side:new int[]{-1,1}) {
                int bx=x+side*(hx-5);
                prop(p,bx,y,z-3,Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING,Direction.NORTH).setValue(BedBlock.PART,BedPart.FOOT));
                prop(p,bx,y,z-4,Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING,Direction.NORTH).setValue(BedBlock.PART,BedPart.HEAD));
                prop(p,bx,y,z+3,Blocks.CHEST.defaultBlockState());
            }
        }
        for(int level=1;level<b.levels;level++) {
            int fy=y+level*10;
            for(int dx=-hx+3;dx<=hx-7;dx+=4)for(int dz:new int[]{-hz+3,hz-3})prop(p,x+dx,fy,z+dz,m("scroll_shelf"));
            prop(p,x-4,fy,z,m(id.equals("bell")?"morning_bell":id.equals("drum")?"evening_drum":"lecture_desk"));
            prop(p,x,fy+4,z,m("library_lantern"));
        }
        if(id.equals("gate")||id.equals("main"))for(int side:new int[]{-1,1}) {
            prop(p,x+side*14,y,z+hz+6,m("guardian_lion"));
            prop(p,x+side*22,y,z+hz+6,m("ceremony_lamp"));
        }
        if(id.contains("view")||id.equals("waterside")||id.equals("garden")) {
            prop(p,x-6,y,z,m("watercourt_bench"));prop(p,x+6,y,z,m("qin_table"));
            prop(p,x+6,y,z+5,m("waterside_marker"));prop(p,x-6,y,z+5,m("water_clock"));
        }
        if(id.equals("tea")||id.startsWith("elder_"))for(int side:new int[]{-1,1})tree(p,x+side*(hx+7),b.floor,z+hz+4,variant);
    }

    private static void prop(Plan p,int x,int y,int z,BlockState state) {
        for(var route:p.routes)if(y>route.floor&&y<=route.floor+3
                &&(route.north?z==route.z&&Math.abs(x-route.x)<=route.halfWidth:x==route.x&&Math.abs(z-route.z)<=route.halfWidth))return;
        for(var b:p.buildings)for(int level=0;level<b.levels-1;level++)for(int step=0;step<=10;step++) {
            int sy=b.floor+level*10+step;
            if(Math.abs(x-(b.x+b.halfX-4))<=1&&z==b.z+b.halfZ-5-step&&y>=sy&&y<=sy+3)return;
        }
        set(p,x,y,z,state);
    }

    private static void tree(Plan p,int x,int y,int z,int variant) {
        fill(p,x,y+1,z,x,y+6,z,Blocks.DARK_OAK_LOG.defaultBlockState());
        var leaf=variant%2==0?Blocks.AZALEA_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true):Blocks.RED_WOOL.defaultBlockState();
        fill(p,x-3,y+5,z-3,x+3,y+7,z+3,leaf);
        fill(p,x-2,y+8,z-2,x+2,y+8,z+2,leaf);
    }

    private static void courtGardens(Plan p) {
        int[][] plantings={{26,230,4},{31,168,12},{30,102,24},{35,-60,46},{24,-124,68},
                {55,-200,70},{168,78,30},{165,-91,70},{120,-173,70},{147,-187,84}};
        for(int i=0;i<plantings.length;i++)for(int side:new int[]{-1,1}) {
            var a=plantings[i];int x=side*a[0],z=a[1],y=a[2];
            disk(p,x,y-3,z,5,3,STONE);disk(p,x,y,z,5,1,Blocks.MOSS_BLOCK.defaultBlockState());
            tree(p,x,y,z,i);
            for(int dz:new int[]{-4,4})set(p,x,y+1,z+dz,m("red_maple_bonsai"));
        }
        for(int radius:new int[]{26,32})for(int dz=-radius;dz<=radius;dz++) {
            int dx=(int)Math.sqrt(radius*radius-dz*dz);
            set(p,-dx,46,-34+dz,m("gilded_panel"));set(p,dx,46,-34+dz,m("gilded_panel"));
        }
        for(int side:new int[]{-1,1}) {
            int x=side*21,z=-5,y=46;
            fill(p,x-3,y+1,z-3,x+3,y+2,z+3,m("white_jade"));
            fill(p,x-2,y+3,z-2,x+2,y+5,z+2,m("bronze_panel"));
            fill(p,x-3,y+6,z-3,x+3,y+6,z+3,m("gilded_panel"));
            set(p,x,y+7,z,m("grand_incense_burner"));
            for(int zz=68;zz<=111;zz+=14) {
                fill(p,side*45-4,24,zz-3,side*45+4,24,zz+3,m("herb_border"));
                for(int dx=-2;dx<=2;dx+=2)set(p,side*45+dx,25,zz,m("bamboo_planter"));
                set(p,side*39,25,zz,m("herb_lamp"));
            }
        }
    }

    private static void disk(Plan p,int x,int y,int z,int radius,int h,BlockState state) {
        for(int dz=-radius;dz<=radius;dz++) {
            if(z+dz<MIN_Z||z+dz>MAX_Z)continue;
            int e=(int)Math.sqrt(radius*radius-dz*dz);
            fill(p,Math.max(MIN_X,x-e),y,z+dz,Math.min(MAX_X,x+e),y+h-1,z+dz,state);
        }
    }
    private static BlockState m(String id){return SectBlocks.state("sect_"+id);}
    private static BlockState face(String id,Direction facing){var state=m(id);return state.hasProperty(HorizontalDirectionalBlock.FACING)?state.setValue(HorizontalDirectionalBlock.FACING,facing):state;}
    private static void set(Plan p,int x,int y,int z,BlockState s){fill(p,x,y,z,x,y,z,s);}
    private static void fill(Plan p,int x1,int y1,int z1,int x2,int y2,int z2,BlockState s){p.add(x1,y1,z1,x2,y2,z2,s);}
}
