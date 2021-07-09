package de.theholyexception.naturcore;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ForceLoadCommand;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class ChunkLoaderBlock extends Block {

    private static ChunkLoaderBlock instance;
    private static final String storagePath = Paths.get("").toAbsolutePath()+"//config//ChunkLoaderStorage.txt";

    private static final HashMap<ServerWorld, ArrayList<BlockPos>> knownStates = new HashMap<>();
    private HashMap<ServerWorld, HashMap<Long, Long>> chunkCache = new HashMap<>();

    private static HashMap<String, ServerWorld> worldList;

    private static final int CHUNK_MAX_LIFETIME = 15;

    public ChunkLoaderBlock(Settings settings) {
        super(settings);
        instance = this;
        //this.setDefaultState(this.getStateManager().getDefaultState().with(POWERED, false));
        {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            long currTime = System.currentTimeMillis();
                            HashMap<ServerWorld, HashMap<Long, Long>> chunkCache_copy = new HashMap<>();
//copy world list
                            synchronized (chunkCache) {
                                chunkCache_copy.putAll(chunkCache);
                            }
//update timings
                            synchronized (knownStates) {
                                for (ServerWorld w : chunkCache_copy.keySet()) {
                                    ArrayList<BlockPos> l = knownStates.get(w);
                                    //synchronized (l) {
                                        for (BlockPos p : l) updateChunkAges(p.getX(), p.getZ(), w);
                                    //}
                                }
                            }
//remove out-timed chunks
                            for (ServerWorld w : chunkCache_copy.keySet()) {
                                HashMap<Long, Long> chunks = chunkCache_copy.get(w);
                                List<Long> queryToDelete = new ArrayList<>();
                                synchronized (chunks) {
                                    for (Long pos : chunks.keySet()) {
                                        long age = currTime - chunks.get(pos);
                                        //System.out.println("age = " + age);
                                        //System.out.println("chunks.get(pos) = " + chunks.get(pos));
                                        if (age / 1000 > CHUNK_MAX_LIFETIME) {
                                            queryToDelete.add(pos);
                                        }
                                    }
                                    for (Long p : queryToDelete) chunks.remove(p);
                                    if (chunks.size() == 0) {
                                        synchronized (chunkCache) {
                                            chunkCache.remove(chunks);
                                        }
                                    }
                                }
                                for (Long p : queryToDelete) {
                                    loadChunks(getChunklocation(p), w, false);
                                }
                            }
                            Thread.sleep(1000);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }, "ChunkLoader_Thread");
            t.setDaemon(true);
            t.start();
        }
        /*
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(20000);
                    //
                    for (Object r : listThreadRunnables()) {
                        System.out.println("found thread: " + r.getClass().getName());
                        if (r instanceof MinecraftServer) {
                            System.out.println("MinecraftServer wurde gefunden!");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            }).start();

         */
    }
/*
    @Override
    protected void appendProperties(final StateManager.Builder<Block, BlockState> builder)
    {
        builder.add(POWERED);
    }
*/

    public static void onLoad(MinecraftServer server) {
        try {
            File file = new File(storagePath);
            worldList = instance.mapWorlds(server);
            if (file.exists()) instance.loadData(file);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void onSave() {
        //System.out.println("Saving Chunkloader data");
        try {
            File file = new File(storagePath);
            instance.saveData(file);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
/*
    public static Object[] listThreadRunnables(){
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        Object[] out = new Object[threadSet.size()];
        int i=0;
        for(Thread tr : threadSet){
            try {
                //System.out.println(tr.getClass().getName());
                out[i] = getField(tr, "target");
                //listFields(tr);
                //System.out.println("->");
                //listFields(out[i]);
                if(out[i] == null) throw new Exception();
                //out[i] = getFieldByType(tr, "java.lang.Runnable");
                i++;
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
        if(i != out.length){
            Object[] o = new Object[i];
            System.arraycopy(out, 0, o, 0, i);
            return o;
        }
        return out;
    }

    public static Object getField(Object obj, String fieldName) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException{
        Field f = obj.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(obj);
    }
*/

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        //System.out.println("onPlaced()");
        //knownStates.put(pos, false);
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        synchronized (knownStates) {
            ArrayList<BlockPos> list = knownStates.get(world);
            if(list != null) {
                synchronized (list) {
                    list.remove(pos);
                }
                if(list.size() == 0){
                    knownStates.remove(world);
                }
            }

        }
        //Boolean a = knownStates.remove(pos);
        //System.out.println("onBreak()");
    }

    @Override
    public void neighborUpdate(final BlockState state, final World world, final BlockPos pos, final Block block, final BlockPos neighborPos, final boolean moved) {
        //System.out.println("neighborUpdate()");

        if (!world.isClient && block != this) {
            ArrayList<BlockPos> blocksInWorld;
            synchronized (knownStates) {
                blocksInWorld = knownStates.get(world);
            }

            if(blocksInWorld == null) {
                blocksInWorld = new ArrayList<>();
            }

            boolean gotPower = world.isReceivingRedstonePower(pos);

            boolean isPowered;
            synchronized (blocksInWorld){
                isPowered = blocksInWorld.contains(pos);
            }



           // Boolean lastState = knownStates.get(pos);
            //System.out.println("lastState: " + (lastState != null));

            if (gotPower != isPowered) {

                synchronized (knownStates) {
                    synchronized (blocksInWorld){
                        if(!gotPower) {
                            boolean result = blocksInWorld.remove(pos);
                            //System.out.println("remove ok ? " + result);
                        } else {
                            blocksInWorld.add(pos);
                        }
                        knownStates.put((ServerWorld)world, blocksInWorld);
                    }
                }

                //world.setBlockState(pos, state.with(ChunkActivatorBlock.POWERED, gotPower && !isPowered), 4);
                //world.getBlockTickScheduler().schedule(pos, this, this.getTickRate(world));
                //System.out.println("gotPower: " + gotPower + ", isPowered: " + isPowered);
                //knownStates.put(pos, gotPower);
                //synchronized (blocksInWorld){
                //    if(isPowered) blocksInWorld.remove(pos); else blocksInWorld.add(pos);
                //}
                //System.out.println(world.getChunkManager().setChunkForced().toString());


                if(gotPower)  loadChunks(pos, (ServerWorld)world, gotPower);
                //System.out.println(world.getRegistryKey());
                //System.out.println(world.getChunkManager().getWorldChunk(1,2).);
            }
        }
    }


    private final static int chunkRadius = 2;

    public void loadChunks(BlockPos pos, ServerWorld world, boolean load) {
        loadChunks(pos.getX(), pos.getZ(), world, load);
    }

    //TODO FIX THIS MALAKA

    public void loadChunks(int posX, int posZ, ServerWorld world, boolean load) {
        //System.out.println((load ? "" : "un") + "Load some chunks");
        if(!load && load){
            ((ServerWorld)world).setChunkForced((posX >> 4) , (posZ >> 4) , load);
            //System.out.println("setChunkForced(" + ((posX >> 4) ) + ", " + ((posZ >> 4)) + ", " + load + ");");
            return;
        }
        for (int x = -chunkRadius; x <= chunkRadius; x ++) {
            for (int z = -chunkRadius; z <= chunkRadius; z ++) {
                ((ServerWorld)world).setChunkForced((posX >> 4) + x, (posZ >> 4) + z, load);
                //System.out.println("setChunkForced(" + ((posX >> 4) + x) + ", " + ((posZ >> 4) + z) + ", " + load + ");");
            }
        }
        if(load) updateChunkAges(posX, posZ, world);
    }

    public void updateChunkAges(int posX, int posZ, ServerWorld world){
        synchronized (chunkCache){
            long t = System.currentTimeMillis();
            for (int x = -chunkRadius; x <= chunkRadius; x ++) {
                for (int z = -chunkRadius; z <= chunkRadius; z ++) {
                    HashMap<Long, Long> a = chunkCache.get(world);
                    if(a == null){
                        a = new HashMap<>();
                        chunkCache.put(world, a);
                    }
                    synchronized (a){
                        a.put(makeChunklocation((posX >> 4) + x, (posZ >> 4) + z), t);
                    }
                }
            }
        }
        /*
        synchronized (knownStates){
            ArrayList<BlockPos> a = knownStates.get(world);
            if(a == null){
                a = new ArrayList<BlockPos>();
                knownStates.put(world, a);
            }
            synchronized (a){
                BlockPos b = new BlockPos(posX, 0, posZ);
                if(!a.contains(b)) a.add(b);
            }
        }
*/
        /*synchronized (knownStates) {
            System.out.println(knownStates);
        }
        synchronized (chunkCache) {
            System.out.println(chunkCache);
        }*/
    }

    private static long makeChunklocation(int x, int z){
        return ((z & 0xFFFFFFFFL) << 32) | (x & 0xFFFFFFFFL);
    }

    private static BlockPos getChunklocation(long l){
        return new BlockPos((int)((l & 0xFFFFFFFFL) << 4), 0, (int)(((l >> 32) & 0xFFFFFFFFL) << 4));
    }



    private void saveData(File file) throws Exception {
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file), 32768));
        synchronized (knownStates) {
            dos.writeInt(worldList.size());
            //System.out.println("Saving blocks for " + knownStates.size() + " worlds...");
            for (String worldName : worldList.keySet()) {
                ServerWorld world = worldList.get(worldName);
                ArrayList<BlockPos> positions = knownStates.get(world);
                byte[] a = worldName.getBytes(StandardCharsets.UTF_8);
                dos.write(a.length);
                dos.write(a);
                if(positions == null || positions.size() == 0) {
                    dos.writeInt(0);
                    continue;
                }
                synchronized (positions) {
                    dos.writeInt(positions.size());
                    for (BlockPos p : positions) {
                        dos.writeInt(p.getX());
                        dos.writeInt(p.getY());
                        dos.writeInt(p.getZ());
                    }
                }
            }
        }
        synchronized (chunkCache) {
            dos.writeInt(worldList.size());
            //System.out.println("Saving chunk-locations for " + chunkCache.size() + " worlds...");

            for (String worldName : worldList.keySet()) {
                HashMap<Long, Long> chunks = chunkCache.get(worldList.get(worldName));
                byte[] a = worldName.getBytes(StandardCharsets.UTF_8);
                dos.write(a.length);
                dos.write(a);
                if(chunks == null || chunks.size() == 0) {
                    dos.writeInt(0);
                    continue;
                }
                synchronized (chunks) {
                    dos.writeInt(chunks.size());
                    for(Long k : chunks.keySet()){
                        dos.writeLong(k);
                        dos.writeLong(chunks.get(k));
                    }
                }
            }
        }
        //private static final HashMap<ServerWorld, ArrayList<BlockPos>> knownStates = new HashMap<>();
        //private HashMap<ServerWorld, HashMap<Long, Long>> chunkCache = new HashMap<>();
        dos.close();
    }

    private void loadData(File file) throws Exception {
        DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file), 32768));
        int worldCount = dis.readInt();
        //System.out.println("loading blocks for " + worldCount + " worlds...");
        for(int i=0; i<worldCount; i++){
            synchronized (knownStates) {
                byte[] name = new byte[dis.read()];
                dis.readFully(name);
                int size = dis.readInt();
                ArrayList<BlockPos> positions = new ArrayList<BlockPos>(size);
                for(int ii=0; ii<size; ii++){
                    int x = dis.readInt();
                    int y = dis.readInt();
                    int z = dis.readInt();
                    positions.add(new BlockPos(x, y, z));
                }
                knownStates.put(worldList.get(new String(name)), positions);
            }
        }
        worldCount = dis.readInt();
        //System.out.println("loading chunk-locations for " + worldCount + " worlds...");
        for(int i=0; i<worldCount; i++) {
            synchronized (chunkCache) {
                byte[] name = new byte[dis.read()];
                dis.readFully(name);
                int size = dis.readInt();
                HashMap<Long, Long> chunks = new HashMap<Long, Long>(size);
                for(int ii=0; ii<size; ii++){
                    long k = dis.readLong();
                    long v = dis.readLong();
                    chunks.put(k, v);
                }
                chunkCache.put(worldList.get(new String(name)), chunks);
            }
        }
        dis.close();
    }

    public HashMap<String, ServerWorld> mapWorlds(MinecraftServer server) throws Exception {
        HashMap<String, ServerWorld> map = new HashMap<String, ServerWorld>();
        for(RegistryKey<World> out : listWorldDimensions(server)){
            String k = out.getValue().getPath();
            ServerWorld w = server.getWorld(out);
            map.put(k, w);
            //System.out.println("mapWorlds() : " + k);
        }
        return map;
    }

    public static  Set<RegistryKey<World>> listWorldDimensions(MinecraftServer server){
        for(ServerWorld w : server.getWorlds()){
            return w.getServer().getWorldRegistryKeys();
        }
        return null;
    }
}
