package de.tomalbrc.blockboy.gui;

import eu.pb4.mapcanvas.api.core.CombinedPlayerCanvas;
import eu.pb4.mapcanvas.api.core.DrawableCanvas;
import eu.pb4.mapcanvas.api.utils.VirtualDisplay;
import eu.pb4.sgui.api.gui.HotbarGui;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class MapGui extends HotbarGui {
    public final Entity entity;
    public CombinedPlayerCanvas canvas;
    public VirtualDisplay virtualDisplay;
    public final BlockPos pos;

    public final IntList additionalEntities = new IntArrayList();

    public MapGui(ServerPlayer player, int width, int height) {
        super(player);
        var pos = player.getOnPos().atY(2048);
        this.pos = pos;

        this.entity = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, player.level());
        this.entity.setYRot(0);
        this.entity.setYHeadRot(0);
        this.entity.setNoGravity(true);
        this.entity.setXRot(0);
        this.entity.setInvisible(true);
        this.initialize(width, height);

        player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.CHANGE_GAME_MODE, GameType.ADVENTURE.getId()));

        var p = new ClientboundAddEntityPacket(entity.getId(), entity.getUUID(), entity.position().x(), entity.position().y(), entity.position().z(), entity.getXRot(), entity.getYRot(), entity.getType(), 0, entity.getDeltaMovement(), entity.getYHeadRot());
        player.connection.send(p);

        player.connection.send(new ClientboundSetEntityDataPacket(this.entity.getId(), this.entity.getEntityData().getNonDefaultValues()));
        player.connection.send(new ClientboundMoveEntityPacket.Rot(player.getId(), (byte) 0, (byte) 0, player.onGround()));

        var buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(this.entity.getId());
        buf.writeVarIntArray(new int[]{player.getId()});

        player.connection.send(new ClientboundSetPassengersPacket(buf));
        //player.connection.send(new ClientboundSetCameraPacket(this.entity));


        for (int i = 0; i < 9; i++) {
            this.setSlot(i, new ItemStack(Items.AIR));
        }

        this.open();
    }

    protected void resizeCanvas(int width, int height) {
        this.destroy();
        this.initialize(width, height);
        this.player.connection.send(ClientboundTeleportEntityPacket.teleport(this.entity.getId(), PositionMoveRotation.of(this.entity), Set.of(), false));
    }

    protected void initialize(int width, int height) {
        this.canvas = DrawableCanvas.create(width, height);
        this.virtualDisplay = VirtualDisplay.of(this.canvas, pos.south(), Direction.NORTH, 0, true);

        this.canvas.addPlayer(player);
        this.virtualDisplay.addPlayer(player);

        this.entity.setPos(pos.getX() - width / 2d + 1, pos.getY() - height / 2d, pos.getZ()+0.3);
    }

    protected void destroy() {
        this.virtualDisplay.removePlayer(this.player);
        this.virtualDisplay.destroy();
        this.canvas.removePlayer(this.player);
        this.canvas.destroy();
    }


    @Override
    public void onClose() {
        this.destroy();
        this.player.server.getCommands().sendCommands(this.player);
        this.player.connection.send(new ClientboundSetCameraPacket(this.player));
        this.player.connection.send(new ClientboundRemoveEntitiesPacket(this.entity.getId()));
        if (!this.additionalEntities.isEmpty()) {
            this.player.connection.send(new ClientboundRemoveEntitiesPacket(this.additionalEntities));
        }
        this.player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.CHANGE_GAME_MODE, this.player.gameMode.getGameModeForPlayer().getId()));
        this.player.connection.send(new ClientboundPlayerPositionPacket(this.player.getId(), PositionMoveRotation.of(this.player), Set.of()));

        super.onClose();
    }

    public void onCommandSuggestion(int id, String fullCommand) {

    }

    @Override
    public boolean canPlayerClose() {
        return false;
    }

    @Override
    public boolean onClickEntity(int entityId, EntityInteraction type, boolean isSneaking, @Nullable Vec3 interactionPos) {
        return super.onClickEntity(entityId, type, isSneaking, interactionPos);
    }

    public void setDistance(double i) {
        this.entity.setPos(this.entity.getX(), this.entity.getY(), this.pos.getZ() - i);
        this.player.connection.send(new ClientboundTeleportEntityPacket(this.entity.getId(), new PositionMoveRotation(this.entity.position(), Vec3.ZERO, this.player.getYRot(), this.player.getXRot()), Set.of(), false));
    }

    @Override
    public boolean onPlayerAction(ServerboundPlayerActionPacket.Action action, Direction direction) {
        if (action == ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS) {
            this.close();
        }
        return false;
    }

    // deltaX/Z is currently useless while in camera mode, as it is always 0
    public void onPlayerInput(float deltaX, float deltaZ, boolean jumping, boolean shiftKeyDown) {

    }

    public void onPlayerCommand(int id, ServerboundPlayerCommandPacket.Action command, int data) {
    }

    public void executeCommand(String command) {
    }
}