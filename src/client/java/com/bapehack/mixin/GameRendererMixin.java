package com.bapehack.mixin;

import com.bapehack.BapeHackClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.util.hit.HitResult;
import net.minecraft.entity.Entity;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "findCrosshairTarget", at = @At("HEAD"))
    private void bh$swingStart(Entity camera, double blockRange, double entityRange, float tickDelta,
                               CallbackInfoReturnable<HitResult> cir) {
        BapeHackClient.updateSwingScan();
    }

    @Inject(method = "findCrosshairTarget", at = @At("RETURN"))
    private void bh$swingEnd(Entity camera, double blockRange, double entityRange, float tickDelta,
                             CallbackInfoReturnable<HitResult> cir) {
        BapeHackClient.swingScan = false;
    }
}
