package de.theholyexception.naturcore.mixin;

import de.theholyexception.naturcore.ChunkLoaderBlock;
import net.fabricmc.fabric.api.event.server.ServerStartCallback;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public class MinecraftServerInject {
    //@Inject(at = @At("HEAD"), method = "register()V")
    /*@Inject(at = @At("RETURN"), method = "<init>(" +
            "Lnet/minecraft/server/MinecraftServer;" +
            "Ljava/util/concurrent/Executor;" +
            "Lnet/minecraft/world/level/storage/LevelStorage$Session;" +
            "Lnet/minecraft/world/level/ServerWorldProperties;" +
            "Lnet/minecraft/util/registry/RegistryKey;" +
            "Lnet/minecraft/world/dimension/DimensionType;" +
            "Lnet/minecraft/server/WorldGenerationProgressListener;" +
            "Lnet/minecraft/world/gen/chunk/ChunkGenerator;" +
            "Z" +
            "J" +
            "Ljava/util/List;" +
            "Z" +
            ")V", cancellable = true)
    public void init(        MinecraftServer server,
                             Executor workerExecutor,
                             LevelStorage.Session session,
                             ServerWorldProperties properties,
                             RegistryKey<World> registryKey,
                             DimensionType dimensionType,
                             WorldGenerationProgressListener worldGenerationProgressListener,
                             ChunkGenerator chunkGenerator,
                             boolean debugWorld,
                             long l,
                             List<Spawner> list,
                             boolean bl,
                             CallbackInfoReturnable info) {*/

  //  @Inject(at = @At("HEAD"), method = "startServer(Ljava/util/function;)Ljava/lang/Object;", remap = false)

    @Inject(at = @At(value = "TAIL", ordinal = 0), method = "loadWorld")
    public void afterSetupServer(CallbackInfo info) {
        System.out.println("[NaturCore] afterSetupServerInject");
        MinecraftServer server = (MinecraftServer) (Object) this;
        ChunkLoaderBlock.onLoad(server);
    }

    @Inject(at = @At(value = "HEAD", ordinal = 0), method = "shutdown")
    public void shutdown(CallbackInfo info) {
        System.out.println("[NaturCore] shutdownInject");
        ChunkLoaderBlock.onSave();
    }

    @Inject(at = @At(value = "HEAD", ordinal = 0), method = "save")
    public void save(boolean suppressLogs, boolean bl, boolean bl2, CallbackInfoReturnable info) {
        System.out.println("[NaturCore] saveInject");
        ChunkLoaderBlock.onSave();
    }
}

