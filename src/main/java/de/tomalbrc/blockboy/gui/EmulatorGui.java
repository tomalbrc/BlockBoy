package de.tomalbrc.blockboy.gui;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.RootCommandNode;
import eu.pb4.mapcanvas.api.core.CanvasColor;
import eu.pb4.mapcanvas.api.core.CanvasImage;
import eu.pb4.mapcanvas.api.font.DefaultFonts;
import eu.pb4.mapcanvas.api.utils.CanvasUtils;
import eu.rekawek.coffeegb.CartridgeOptions;
import eu.rekawek.coffeegb.Gameboy;
import eu.rekawek.coffeegb.controller.ButtonListener;
import eu.rekawek.coffeegb.emulator.EmulationController;
import eu.rekawek.coffeegb.memory.cart.Cartridge;
import eu.rekawek.coffeegb.emulator.BlockBoyDisplay;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class EmulatorGui extends MapGui {
    private static final CommandDispatcher<EmulatorGui> COMMANDS = new CommandDispatcher<>();

    private int width;
    private int height;
    private int xPos;
    private int yPos;

    @Nullable
    private EmulationController controller;

    private ServerPlayer player;



    public EmulatorGui(ServerPlayer player, int width, int height) {
        super(player, Mth.ceil(width / 128d) + 2, Mth.ceil(height / 128d) + 2);

        this.width = width;
        this.height = height;

        this.player = player;

        player.connection.send(new ClientboundCommandsPacket((RootCommandNode) COMMANDS.getRoot()));
        this.updateImage();
    }

    public void playRom(File rom) {
        this.controller = new EmulationController(new CartridgeOptions(), rom, player);
        this.controller.startEmulation();
    }

    protected void updateImage() {
        this.setDistance(0.1);
        this.drawLoading();
    }

    @Override
    public void onTick() {
        this.xPos = (this.canvas.getWidth() - 160*2) / 2;
        this.yPos = (this.canvas.getHeight() - 144*2) / 2;

        this.draw();
    }

    @Override
    public void onClose() {
        controller.stopEmulation();
        super.onClose();
    }



    boolean wasJumping = false;
    boolean wasSneaking = false;
    Direction zdirection = Direction.UP;
    Direction xdirection = Direction.UP;
    boolean wasInteract = false;

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
            image = controller.getDisplay().render(BlockBoyDisplay.DISPLAY_WIDTH*2, BlockBoyDisplay.DISPLAY_HEIGHT*2);
        }

        CanvasUtils.draw(this.canvas, xPos, yPos, image);

        this.player.connection.send(new ClientboundTeleportEntityPacket(this.entity));
        this.player.connection.send(new ClientboundMoveEntityPacket.Rot(player.getId(), (byte) 0, (byte) 0, player.isOnGround()));
        this.canvas.sendUpdates();
    }

    public void setSize(int width, int height) {
        if (
                this.canvas.getWidth() < width + 256 || this.canvas.getHeight() < height + 256
                        || this.canvas.getWidth() > width * 2 || this.canvas.getHeight() > height * 2
        ) {
            this.resizeCanvas(Mth.ceil(width / 128d) + 2, Mth.ceil(height / 128d) + 2);
        }

        this.width = width;
        this.height = height;
        this.updateImage();
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
            return 0;
        }));

        COMMANDS.register(literal("save").executes(x -> {
            // TODO; save game?
            return 0;
        }));

        COMMANDS.register(literal("size")
               // todo: change emu size?
        );
    }
}
