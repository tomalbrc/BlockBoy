package de.tomalbrc.blockboy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.tomalbrc.blockboy.gui.EmulatorGui;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.argument;

public class BlockBoyCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> blockboy = Commands.literal("blockboy").requires(Permissions.require("blockboy.command", 1));

        blockboy.then(argument("rom", StringArgumentType.word()).suggests(new RomSuggestionProvider()).executes(command -> {
            EmulatorGui gui = new EmulatorGui(command.getSource().getPlayer(), 256, 256);
            gui.playRom(RomSuggestionProvider.resolve(StringArgumentType.getString(command, "rom")));
            return 0;
        }));

        LiteralCommandNode<CommandSourceStack> gestureNode = blockboy.build();

        dispatcher.getRoot().addChild(gestureNode);
    }

    static class RomSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context,
                                                             SuggestionsBuilder builder) {

            Path path = FabricLoader.getInstance().getGameDir().resolve("roms/");

            try {
                if (!path.toFile().exists())
                    Files.createDirectories(path); // Create parent directories if they don't exist

                var files = Files.list(path);
                for (Path filepath : files.toList()) {
                    var str = filepath.getFileName().toString().toLowerCase();
                    if (str.endsWith(".gbc") || str.endsWith(".gb"))
                        builder.suggest(filepath.getFileName().toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return builder.buildFuture();
        }

        public static File resolve(String filename) {
            Path path = FabricLoader.getInstance().getGameDir().resolve("roms/");
            return path.resolve(filename).toFile();
        }
    }
}
