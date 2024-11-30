package de.tomalbrc.blockboy;

import de.tomalbrc.blockboy.gui.EmulatorGui;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.Objects;

public class BlockBoy implements ModInitializer {
    public static Map<Player, EmulatorGui> activeSessions = new Reference2ObjectArrayMap<>();

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> BlockBoyCommand.register(dispatcher));
        ServerPlayConnectionEvents.DISCONNECT.register((serverGamePacketListener, server) -> {
            if (BlockBoy.activeSessions.containsKey(serverGamePacketListener.player)) {
                Objects.requireNonNull(BlockBoy.activeSessions.get(serverGamePacketListener.player).getController()).stopEmulation();
                Objects.requireNonNull(BlockBoy.activeSessions.get(serverGamePacketListener.player)).close();
                BlockBoy.activeSessions.remove(serverGamePacketListener.player);
            }
        });
    }
}
