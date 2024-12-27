package de.tomalbrc.blockboy;

import de.tomalbrc.blockboy.gui.EmulatorGui;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class BlockBoy implements ModInitializer {
    public static Map<Player, EmulatorGui> activeSessions = new Reference2ObjectArrayMap<>();

    public static final Logger LOGGER = LogManager.getLogger("blockboy");

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> BlockBoyCommand.register(dispatcher));
    }
}
