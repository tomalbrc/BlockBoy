package eu.rekawek.coffeegb.emulator;

import de.tomalbrc.blockboy.BlockBoy;
import de.tomalbrc.blockboy.BlockBoySoundOutput;
import de.tomalbrc.blockboy.ModConfig;
import eu.rekawek.coffeegb.CartridgeOptions;
import eu.rekawek.coffeegb.Gameboy;
import eu.rekawek.coffeegb.controller.ButtonListener;
import eu.rekawek.coffeegb.memory.cart.Cartridge;
import eu.rekawek.coffeegb.sound.SoundOutput;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.io.File;
import java.io.IOException;

public class EmulationController {


    private final BlockBoyDisplay display;
    private final SoundOutput sound;

    private final CartridgeOptions options;

    private final File currentRom;

    private Cartridge cart;

    private Gameboy gameboy;

    private boolean isRunning;

    private final Cartridge.GameboyType type;

    private final ServerPlayer player;

    private final StreamSerialEndpoint streamSerial = new StreamSerialEndpoint();

    private Thread serialThread;

    private Player linkedPlayer = null;

    public EmulationController(CartridgeOptions options, File initialRom, ServerPlayer player) {
        this.options = options;
        this.currentRom = initialRom;

        this.type = Cartridge.GameboyType.AUTOMATIC;
        this.display = new BlockBoyDisplay(1, false);
        this.sound = FabricLoader.getInstance().isModLoaded("voicechat") && ModConfig.getInstance().sound ? new BlockBoySoundOutput(player) : SoundOutput.NULL_OUTPUT;
        this.player = player;
    }

    public BlockBoyDisplay getDisplay() {
        return this.display;
    }

    public void unlink() throws IOException {
        boolean wasAlive = serialThread.isAlive();
        if (wasAlive) {
            serialThread.interrupt();
            streamSerial.stop();

            if (linkedPlayer != null && BlockBoy.activeSessions.containsKey(linkedPlayer)) {
                var controller = BlockBoy.activeSessions.get(linkedPlayer).getController();
                if (controller != null) controller.unlink();
            }
        }
    }

    public void link(EmulationController friend) throws IOException {
        this.streamSerial.getInputStream().connect(friend.streamSerial.getOutputStream());
        this.streamSerial.getOutputStream().connect(friend.streamSerial.getInputStream());

        this.linkedPlayer = friend.player;

        friend.serialThread = new Thread(this.streamSerial);
        this.serialThread = new Thread(friend.streamSerial);

        friend.serialThread.start();
        this.serialThread.start();
    }


    public void startEmulation() {
        Cartridge newCart;
        try {
            newCart = loadRom(currentRom);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        stopEmulation();
        cart = newCart;
        gameboy = new Gameboy(cart);
        gameboy.init(display, sound, streamSerial);
        gameboy.registerTickListener(new TimingTicker());

        new Thread(display).start();
        if (sound != SoundOutput.NULL_OUTPUT) new Thread((BlockBoySoundOutput)sound).start(); // TODO: add sounds? using noteblocks? doesn't seem feasible
        new Thread(gameboy).start();
        isRunning = true;
    }

    public void stopEmulation() {
        if (!isRunning) {
            return;
        }
        isRunning = false;
        if (gameboy != null) {
            gameboy.stop();
            gameboy = null;
        }
        if (cart != null) {
            cart.flushBattery();
            cart = null;
        }

        streamSerial.stop();
        display.stop();
        sound.stop();
    }

    private Cartridge loadRom(File rom) throws IOException {
        return new Cartridge(rom, player, options.isSupportBatterySaves(), type, options.isUsingBootstrap());
    }

    public void pressed(ButtonListener.Button button) {
        this.gameboy.pressedButton(button);
    }

    public void released(ButtonListener.Button button) {
        this.gameboy.releasedButton(button);
    }
}