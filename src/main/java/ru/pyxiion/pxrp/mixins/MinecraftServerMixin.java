package ru.pyxiion.pxrp.mixins;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.pyxiion.pxrp.LuaCmdLoader;
import ru.pyxiion.pxrp.PxRp;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(method = "net.minecraft.server.MinecraftServer.reloadResources", at = @At("TAIL"))
    private void reloadRes(Collection<String> dataPacks, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        PxRp.instance.luaLoader.reload();
    }
}
