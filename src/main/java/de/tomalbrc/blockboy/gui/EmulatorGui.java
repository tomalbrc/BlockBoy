package de.tomalbrc.blockboy.gui;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.RootCommandNode;
import de.tomalbrc.blockboy.BlockBoy;
import eu.pb4.mapcanvas.api.core.CanvasColor;
import eu.pb4.mapcanvas.api.core.CanvasImage;
import eu.pb4.mapcanvas.api.font.DefaultFonts;
import eu.pb4.mapcanvas.api.utils.CanvasUtils;
import eu.rekawek.coffeegb.CartridgeOptions;
import eu.rekawek.coffeegb.controller.ButtonListener;
import eu.rekawek.coffeegb.emulator.BlockBoyDisplay;
import eu.rekawek.coffeegb.emulator.EmulationController;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class EmulatorGui extends MapGui {
    private static final CommandDispatcher<EmulatorGui> COMMANDS = new CommandDispatcher<>();

    private int width;
    private int height;
    private int xPos;
    private int yPos;

    private double scale = 1;

    @Nullable
    private EmulationController controller;

    private ServerPlayer player;

    private boolean customTime = false;

    public EmulatorGui(ServerPlayer player, int width, int height) {
        super(player, Mth.ceil(width / 128d) + 2, Mth.ceil(height / 128d) + 2);

        this.width = width;
        this.height = height;

        this.player = player;

        player.connection.send(new ClientboundCommandsPacket((RootCommandNode) COMMANDS.getRoot()));

        // stop rain
        player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 1));

        BlockBoy.activeSessions.put(player, this);
    }

    public EmulationController getController() {
        return controller;
    }

    public boolean hasCustomTime() {
        return customTime;
    }

    public void playRom(File rom) {
        this.controller = new EmulationController(new CartridgeOptions(), rom, player);
        this.controller.startEmulation();
    }

    protected void updateImage() {
        this.setDistance(-2);
        this.drawLoading();
    }

    @Override
    public void onTick() {
        this.xPos = (this.canvas.getWidth() - (int)(160*2*scale)) / 2;
        this.yPos = (this.canvas.getHeight() - (int)(144*2*scale)) / 2;

        this.draw();
    }

    @Override
    public void onClose() {
        controller.stopEmulation();
        BlockBoy.activeSessions.remove(player);
        super.onClose();
    }

    boolean wasJumping = false;
    boolean wasSneaking = false;
    Direction zdirection = Direction.UP;
    Direction xdirection = Direction.UP;

    @Override
    public void onPlayerInput(float deltaX, float deltaZ, boolean jumping, boolean shiftKeyDown) {
        if (jumping) {
            controller.pressed(ButtonListener.Button.A);
            wasJumping = true;
        } else if (wasJumping) {
            controller.released(ButtonListener.Button.A);
            wasJumping = false;
        }

        if (shiftKeyDown) {
            wasSneaking = true;
            controller.pressed(ButtonListener.Button.B);
        } else if (wasJumping) {
            controller.released(ButtonListener.Button.B);
            wasSneaking = false;
        }

        if (deltaZ > 0) {
            zdirection = Direction.NORTH;
            controller.pressed(ButtonListener.Button.UP);
        } else if (zdirection == Direction.NORTH) {
            controller.released(ButtonListener.Button.UP);
            zdirection = Direction.UP; // abuse up as noop, only use n,e,s,w
        }

        if (deltaZ < 0) {
            zdirection = Direction.SOUTH;
            controller.pressed(ButtonListener.Button.DOWN);
        } else if (zdirection == Direction.SOUTH) {
            controller.released(ButtonListener.Button.DOWN);
            zdirection = Direction.UP; // abuse up as noop, only use n,e,s,w
        }

        if (deltaX < 0) {
            xdirection = Direction.EAST;
            controller.pressed(ButtonListener.Button.RIGHT);
        } else if (xdirection == Direction.EAST) {
            controller.released(ButtonListener.Button.RIGHT);
            xdirection = Direction.UP; // abuse up as noop, only use n,e,s,w
        }

        if (deltaX > 0) {
            xdirection = Direction.WEST;
            controller.pressed(ButtonListener.Button.LEFT);
        } else if (xdirection == Direction.WEST) {
            controller.released(ButtonListener.Button.LEFT);
            xdirection = Direction.UP; // abuse up as noop, only use n,e,s,w
        }
    }

    @Override
    public boolean onClickEntity(int entityId, EntityInteraction type, boolean isSneaking, @Nullable Vec3 interactionPos) {
        controller.pressed(type == EntityInteraction.ATTACK ? ButtonListener.Button.START : ButtonListener.Button.SELECT);
        new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                controller.released(type == EntityInteraction.ATTACK ? ButtonListener.Button.START : ButtonListener.Button.SELECT);
            }
        }, 50);
        return super.onClickEntity(entityId, type, isSneaking, interactionPos);
    }

    private void drawLoading() {
        var text = "Loading...";
        var size = (int) Math.min(this.height / 128d, this.width / 128d) * 16;
        var width = DefaultFonts.VANILLA.getTextWidth(text, size);

        CanvasUtils.fill(this.canvas,
                (this.width - width) / 2 - size + 128,
                (this.height - size) / 2 - size + 128,
                (this.width - width) / 2 + size + width + 128,
                (this.height - size) / 2 + size * 2 + 128, CanvasColor.BLACK_LOW);

        DefaultFonts.VANILLA.drawText(this.canvas, text, (this.width - width) / 2 + 128, (this.height - size) / 2 + 128, size, CanvasColor.WHITE_HIGH);

        this.canvas.sendUpdates();
    }

    private void draw() {
        CanvasImage image = null;
        if (controller == null) {
            image = new CanvasImage(this.canvas.getWidth(), this.canvas.getHeight());
            CanvasUtils.clear(image, CanvasColor.YELLOW_HIGH);
        }
        else {
            image = controller.getDisplay().render((int)(BlockBoyDisplay.DISPLAY_WIDTH*2.0*scale), (int)(BlockBoyDisplay.DISPLAY_HEIGHT*2.0*scale));
        }

        CanvasUtils.draw(this.canvas, xPos, yPos, image);

        this.canvas.sendUpdates();
    }

    public void setSize(int width, int height) {
        if (
                this.canvas.getWidth() < width + 256 || this.canvas.getHeight() < height + 256
                        || this.canvas.getWidth() > width * 2 || this.canvas.getHeight() > height * 2
        ) {
            this.resizeCanvas(Mth.ceil(width / 128d) + 2, Mth.ceil(height / 128d) + 2);
        }

        this.scale = width / 256.0;
        this.width = width;
        this.height = height;
        CanvasUtils.clear(this.canvas);
    }

    @Override
    public void executeCommand(String command) {
        try {
            COMMANDS.execute(command, this);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static LiteralArgumentBuilder<EmulatorGui> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    private static <T> RequiredArgumentBuilder<EmulatorGui, T> argument(String name, ArgumentType<T> argumentType) {
        return RequiredArgumentBuilder.argument(name, argumentType);
    }

    static {
        COMMANDS.register(literal("exit").executes(x -> {
            x.getSource().close();

            var player = x.getSource().getPlayer();
            if (player != null) {
                player.connection.send(new ClientboundSetTimePacket(player.level().getGameTime(), player.level().getDayTime(), player.level().getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)));
                if (player.level().isRaining())
                    player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.STOP_RAINING, 0));
            }

            return 0;
        }));

        COMMANDS.register(literal("link").then(argument("friend", StringArgumentType.word()).suggests(new PlayingPlayerSuggestionProvider()).executes(x -> {
            var player = x.getSource().getPlayer();
            var name = StringArgumentType.getString(x, "friend");
            var friend = player.server.getPlayerList().getPlayerByName(name);

            if (player != null && friend != null) {
                var s1 = BlockBoy.activeSessions.get(player);
                var s2 = BlockBoy.activeSessions.get(friend);
                if (s2 == null) {
                    player.sendSystemMessage(Component.literal("Could not connect with " + name));
                    return 1;
                }

                try {
                    s1.getController().link(s2.getController());
                } catch (IOException e) {
                    player.sendSystemMessage(Component.literal("Could not connect with " + name));
                    e.printStackTrace();
                }
            }

            return 0;
        })));

        COMMANDS.register(literal("unlink").then(argument("friend", StringArgumentType.word()).suggests(new PlayingPlayerSuggestionProvider()).executes(x -> {
            var player = x.getSource().getPlayer();
            var name = StringArgumentType.getString(x, "friend");
            var friend = player.server.getPlayerList().getPlayerByName(name);

            if (player != null && friend != null) {
                var s1 = BlockBoy.activeSessions.get(player);

                try {
                    s1.getController().unlink();
                } catch (IOException e) {
                    player.sendSystemMessage(Component.literal("Could not unlink, are you linked with someone?"));
                    e.printStackTrace();
                }
            }

            return 0;
        })));

        COMMANDS.register(
                literal("theme")
                        .then(literal("day").executes(x -> EmulatorGui.setTime(x, 1000)))
                        .then(literal("noon").executes(x -> EmulatorGui.setTime(x, 6000)))
                        .then(literal("night").executes(x -> EmulatorGui.setTime(x, 13000)))
                        .then(literal("midnight").executes(x -> EmulatorGui.setTime(x, 18000)))
        );

        COMMANDS.register(literal("scale").then(argument("scale", IntegerArgumentType.integer(1,4)).executes(x -> {
            var scale = IntegerArgumentType.getInteger(x, "scale");
            double scale2 = 1.0+(scale-1)/2.0;
            x.getSource().setSize((int)(256.0*scale2), (int)(256.0*scale2));
            return 0;
        })));
    }

    private static int setTime(CommandContext<EmulatorGui> x, int time) {
        var player = x.getSource().getPlayer();
        if (player != null) {
            BlockBoy.activeSessions.get(player).customTime = false;
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0));
            player.connection.send(new ClientboundSetTimePacket(18000, 18000, false));
            BlockBoy.activeSessions.get(player).customTime = true;
        }
        return 0;
    }


    static class PlayingPlayerSuggestionProvider implements SuggestionProvider<EmulatorGui> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<EmulatorGui> context,
                                                             SuggestionsBuilder builder) {

            for (Player p : BlockBoy.activeSessions.keySet()) {
                if (p != context.getSource().player) builder.suggest(p.getDisplayName().getString());
            }

            return builder.buildFuture();
        }
    }
}
