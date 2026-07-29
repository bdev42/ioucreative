package com.github.bdev42.ioucreative.mixin;

import com.github.bdev42.ioucreative.IOUCreative;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {

    @Shadow
    @Final
    protected ServerPlayer player;

    @Inject(method = "setGameModeForPlayer", at = @At("RETURN"))
    private void iouc_setGameModeForPlayer(GameType gameModeForPlayer, GameType previousGameModeForPlayer, CallbackInfo ci) {
        if (player.connection == null) return;
        IOUCreative.onGameModeChanged(player, gameModeForPlayer, previousGameModeForPlayer);
    }

}
