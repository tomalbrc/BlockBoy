package de.tomalbrc.blockboy.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import de.tomalbrc.blockboy.BlockBoy;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Inject(method = "broadcastAll(Lnet/minecraft/network/protocol/Packet;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V"))
    private void blockboy$onBroadcastAll(Packet<?> packet, CallbackInfo ci, @Local ServerPlayer serverPlayer) {
        if (BlockBoy.activeSessions.containsKey(serverPlayer)) {
            if ((BlockBoy.activeSessions.get(serverPlayer).hasCustomTime() && packet instanceof ClientboundSetTimePacket)) {
                ci.cancel();
            }
            else if (packet instanceof ClientboundGameEventPacket gameEventPacket && (gameEventPacket.getEvent() == ClientboundGameEventPacket.STOP_RAINING || gameEventPacket.getEvent() == ClientboundGameEventPacket.START_RAINING)) {
                ci.cancel();
            }
        }
    }
}
