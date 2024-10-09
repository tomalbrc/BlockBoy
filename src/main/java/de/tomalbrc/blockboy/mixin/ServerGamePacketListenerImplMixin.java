package de.tomalbrc.blockboy.mixin;

import de.tomalbrc.blockboy.BlockBoy;
import de.tomalbrc.blockboy.gui.MapGui;
import eu.pb4.sgui.api.gui.HotbarGui;
import eu.pb4.sgui.virtual.VirtualScreenHandlerInterface;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void blockboy$handleDisconnect(DisconnectionDetails disconnectionDetails, CallbackInfo ci) {
        if (BlockBoy.activeSessions.containsKey(this.player)) {
            Objects.requireNonNull(BlockBoy.activeSessions.get(player).getController()).stopEmulation();
            Objects.requireNonNull(BlockBoy.activeSessions.get(player)).close();
            BlockBoy.activeSessions.remove(player);
        }
    }

    @Inject(method = "handleCustomCommandSuggestions", at = @At("HEAD"), cancellable = true)
    private void blockboy$handleCustomCommandSuggestions(ServerboundCommandSuggestionPacket packet, CallbackInfo ci) {
        if (this.player.containerMenu instanceof VirtualScreenHandlerInterface handler && handler.getGui() instanceof MapGui computerGui) {
            this.server.execute(() -> {
                computerGui.onCommandSuggestion(packet.getId(), packet.getCommand());
            });
            ci.cancel();
        }
    }

    @Inject(method = "performChatCommand", at = @At("HEAD"), cancellable = true)
    private void blockboy$onCommandExecution(ServerboundChatCommandPacket packet, LastSeenMessages lastSeenMessages, CallbackInfo ci) {
        if (this.player.containerMenu instanceof VirtualScreenHandlerInterface handler && handler.getGui() instanceof MapGui computerGui) {
            computerGui.executeCommand(packet.command());
            ci.cancel();
        }
    }

    @Inject(method = "handlePlayerInput", at = @At("HEAD"), cancellable = true)
    private void blockboy$onPlayerInput(ServerboundPlayerInputPacket packet, CallbackInfo ci) {
        if (this.player.containerMenu instanceof VirtualScreenHandlerInterface handler && handler.getGui() instanceof MapGui computerGui) {
            this.server.execute(() -> {
                computerGui.onPlayerInput(packet.getXxa(), packet.getZza(), packet.isJumping(), packet.isShiftKeyDown());
            });
            ci.cancel();
        }
    }

    @Inject(method = "handlePlayerCommand", at = @At("HEAD"), cancellable = true)
    private void blockboy$onClientCommand(ServerboundPlayerCommandPacket packet, CallbackInfo ci) {
        if (this.player.containerMenu instanceof VirtualScreenHandlerInterface handler && handler.getGui() instanceof MapGui computerGui) {
            this.server.execute(() -> {
                computerGui.onPlayerCommand(packet.getId(), packet.getAction(), packet.getData());
            });
            ci.cancel();
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void blockboy$onSendPacket(Packet<?> packet, CallbackInfo ci) {
        if (BlockBoy.activeSessions.containsKey(this.player) && BlockBoy.activeSessions.get(this.player).hasCustomTime() && packet instanceof ClientboundSetTimePacket)
            ci.cancel();
    }

    @Inject(method = "handleInteract", at = @At("HEAD"), cancellable = true)
    private void blockboy$onInteract(ServerboundInteractPacket serverboundInteractPacket, CallbackInfo ci) {
        if (BlockBoy.activeSessions.containsKey(this.player)) {
            serverboundInteractPacket.dispatch(new ServerboundInteractPacket.Handler() {
                @Override
                public void onInteraction(InteractionHand interactionHand) {
                    BlockBoy.activeSessions.get(player).onClickEntity(0, HotbarGui.EntityInteraction.INTERACT, false, Vec3.ZERO);
                }

                @Override
                public void onInteraction(InteractionHand interactionHand, Vec3 vec3) {
                    BlockBoy.activeSessions.get(player).onClickEntity(0, HotbarGui.EntityInteraction.INTERACT, false, Vec3.ZERO);

                }

                @Override
                public void onAttack() {
                    BlockBoy.activeSessions.get(player).onClickEntity(0, HotbarGui.EntityInteraction.ATTACK, false, Vec3.ZERO);
                }
            });

            ci.cancel();
        }
    }

    @Inject(method = "handleAnimate", at = @At("HEAD"))
    private void blockboy$handleAnimate(ServerboundSwingPacket serverboundSwingPacket, CallbackInfo ci) {
        if (BlockBoy.activeSessions.containsKey(this.player))
            BlockBoy.activeSessions.get(player).onClickEntity(0, HotbarGui.EntityInteraction.ATTACK, false, Vec3.ZERO);
    }
}