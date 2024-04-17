package eu.rekawek.coffeegb.emulator;

import eu.rekawek.coffeegb.CartridgeOptions;
import eu.rekawek.coffeegb.Gameboy;
import eu.rekawek.coffeegb.controller.ButtonListener;
import eu.rekawek.coffeegb.memory.cart.Cartridge;
import eu.rekawek.coffeegb.serial.SerialEndpoint;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.io.IOException;

public class EmulationController {

    private final BlockBoyDisplay display;

    private final CartridgeOptions options;

    private File currentRom;

    private Cartridge cart;

    private Gameboy gameboy;

    private boolean isRunning;

    private Cartridge.GameboyType type;

    private ServerPlayer player;

    public EmulationController(CartridgeOptions options, File initialRom, ServerPlayer player) {
        this.options = options;
        this.currentRom = initialRom;

        this.type = Cartridge.GameboyType.AUTOMATIC;
        this.display = new BlockBoyDisplay(1, false);
        this.player = player;
    }

    public BlockBoyDisplay getDisplay() {
        return this.display;
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
        gameboy.init(display, null, SerialEndpoint.NULL_ENDPOINT);
        gameboy.registerTickListener(new TimingTicker());

        new Thread(display).start();
        //new Thread(sound).start(); // TODO: add sounds? using noteblocks? doesn't seem feasible
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

        display.stop();
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