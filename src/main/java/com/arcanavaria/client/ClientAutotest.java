package com.arcanavaria.client;

import com.arcanavaria.AVRegistry;
import com.arcanavaria.ArcanaVaria;
import com.arcanavaria.warden.GreatwoodWarden;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import com.mojang.logging.LogUtils;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Development-only automated scythe check, inert unless the JVM runs with -Darcanavaria.autotest (only the Gradle dev
 * run passes it with -Pautotest, see build.gradle and tools/run_client_test.sh). It makes a fresh creative superflat
 * world, holds the thaumium scythe, drops the void and elemental scythes on the ground in front, takes a first-person
 * and a third-person screenshot, then plants a Heartseed and watches the Greatwood Warden fight (survival, Resistance IV):
 * state log every second, screenshots, halves its health for phase 2, uses the Amber Heart twice, kills the Warden and
 * lists the loot; then builds an Alembic Lattice that joins ignis and aer into their compound (client/LatticeAutotest,
 * alone with -Pautotest=lattice); with Fundamentum it then swings each whip at a frozen zombie (client/WhipAutotest). Logs "AVTEST" lines and stops the client.
 */
@Mod(value = ArcanaVaria.MODID, dist = Dist.CLIENT)
public final class ClientAutotest {
    private static final String MODE = System.getProperty("arcanavaria.autotest");
    private static final Logger LOG = LogUtils.getLogger();
    private static final String WORLD = "avtest";
    private static final long TIMEOUT_NANOS = 6L * 60L * 1_000_000_000L;

    private static boolean titleSeen;
    private static boolean creating;
    private static long titleAt;
    private static int idleTicks;
    private static int worldTicks;
    private static boolean finished;
    private static int whipSeen;
    private static int whipSeenAt;
    private static int climbTicks;
    private static boolean offScreen;

    public ClientAutotest(IEventBus modBus) {
        if (MODE == null) {
            return;
        }
        LOG.info("AVTEST armed ({})", MODE);
        NeoForge.EVENT_BUS.addListener(ClientAutotest::tick);
    }

    private static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || finished) {
            return;
        }
        if (!offScreen) {
            // test clients stay out of sight: the window lives off-screen (shots read the framebuffer, not the screen)
            offScreen = true;
            org.lwjgl.glfw.GLFW.glfwSetWindowPos(mc.getWindow().handle(), -4000, -4000);
        }
        if (titleSeen && System.nanoTime() - titleAt > TIMEOUT_NANOS) {
            LOG.info("AVTEST timeout");
            finish(mc);
            return;
        }
        try {
            step(mc);
        } catch (Throwable t) {
            LOG.error("AVTEST step failed", t);
        }
    }

    private static void step(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            if (!creating && mc.screen != null && !(mc.screen instanceof TitleScreen) && mc.getOverlay() == null && ++idleTicks > 200) {
                LOG.info("AVTEST dismissing {}", mc.screen.getClass().getName());
                mc.setScreen(new TitleScreen());
                idleTicks = 0;
            }
            if (!creating && mc.screen instanceof TitleScreen title && mc.getOverlay() == null) {
                creating = true;
                titleSeen = true;
                titleAt = System.nanoTime();
                LOG.info("AVTEST title");
                mc.options.pauseOnLostFocus = false;
                mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
                mc.options.framerateLimit().set(30);
                mc.options.renderDistance().set(4);
                mc.options.simulationDistance().set(5);
                deleteOldWorld(mc);
                LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, LevelSettings.DifficultySettings.DEFAULT, true, WorldDataConfiguration.DEFAULT);
                LOG.info("AVTEST creating world {}", WORLD);
                // -Pautotest=structures needs real terrain and worldgen; every other stage builds on a superflat floor
                mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(20261101L, "structures".equals(MODE), false),
                        "structures".equals(MODE) ? WorldPresets::createNormalWorldDimensions : WorldPresets::createFlatWorldDimensions, title);
            }
            return;
        }
        if (mc.screen instanceof PauseScreen) {
            mc.setScreen(null);
        }
        worldTicks++;
        if ("scythes".equals(MODE)) {
            if (worldTicks == 40) {
                ScytheClientAutotest.check(mc);
                onServer(mc, ScytheClientAutotest::checkKubeJs);
            } else if (worldTicks == 60) {
                LOG.info("AVTEST done (scythes)");
                finish(mc);
            }
            return;
        }
        if (worldTicks == 1) {
            LOG.info("AVTEST joined");
        } else if (worldTicks == 60) {
            setUp(mc);
        } else if (worldTicks == 260) {
            shot(mc, "first");
        } else if (worldTicks == 280) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        } else if (worldTicks == 340) {
            shot(mc, "third");
            if ("lattice".equals(MODE)) {
                // -Pautotest=lattice: straight on to the lattice stage, no Warden
                worldTicks = LATTICE_AT - 1;
            } else if ("thurible".equals(MODE)) {
                worldTicks = THURIBLE_AT - 1;
            } else if ("lens".equals(MODE)) {
                worldTicks = LENS_AT - 1;
            } else if ("lab".equals(MODE)) {
                worldTicks = LAB_AT - 1;
            } else if ("structures".equals(MODE)) {
                worldTicks = STRUCT_AT - 1;
            } else if ("exanimator".equals(MODE)) {
                // -Pautotest=exanimator: the Exanimator boss fight only
                worldTicks = EXAN_AT - 1;
            } else if ("whips".equals(MODE)) {
                // -Pautotest=whips: the scythe swings, the whips, the six primals and the summon marks
                worldTicks = SCYTHES_AT - 1;
            }
        } else if (worldTicks == 360) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            onServer(mc, ClientAutotest::summonWarden);
        } else if (worldTicks > 360 && worldTicks < 1400 && worldTicks % 20 == 0) {
            onServer(mc, ClientAutotest::logWarden);
            if (worldTicks == 420 || worldTicks == 560 || worldTicks == 680 || worldTicks == 840 || worldTicks == 1000 || worldTicks == 1160) {
                shot(mc, "warden" + worldTicks);
            }
            if (worldTicks == 760) {
                onServer(mc, ClientAutotest::halveWarden);
            } else if (worldTicks == 1240) {
                onServer(mc, ClientAutotest::amberHeart);
            } else if (worldTicks == 1300) {
                onServer(mc, ClientAutotest::killWarden);
            } else if (worldTicks == 1380) {
                onServer(mc, ClientAutotest::logLoot);
            }
        } else if (worldTicks == 1390) {
            shot(mc, "loot");
        } else if (worldTicks >= EXAN_AT) {
            exanimatorStage(mc, worldTicks - EXAN_AT);
        } else if (worldTicks >= STRUCT_AT) {
            structureStage(mc, worldTicks - STRUCT_AT);
        } else if (worldTicks >= LATTICE_AT && worldTicks < THURIBLE_AT) {
            latticeStage(mc, worldTicks - LATTICE_AT);
        } else if (worldTicks == THURIBLE_AT && "lattice".equals(MODE)) {
            LOG.info("AVTEST done (lattice only)");
            finish(mc);
        } else if (worldTicks >= THURIBLE_AT && worldTicks < LENS_AT) {
            thuribleStage(mc, worldTicks - THURIBLE_AT);
        } else if (worldTicks == LENS_AT && "thurible".equals(MODE)) {
            LOG.info("AVTEST done (thurible only)");
            finish(mc);
        } else if (worldTicks >= LENS_AT && worldTicks < LAB_AT) {
            lensStage(mc, worldTicks - LENS_AT);
        } else if (worldTicks == LAB_AT && "lens".equals(MODE)) {
            LOG.info("AVTEST done (lens only)");
            finish(mc);
        } else if (worldTicks >= LAB_AT && worldTicks < SCYTHES_AT) {
            labStage(mc, worldTicks - LAB_AT);
        } else if (worldTicks == SCYTHES_AT && "lab".equals(MODE)) {
            LOG.info("AVTEST done (lab only)");
            finish(mc);
        } else if (worldTicks >= SCYTHES_AT && worldTicks < WHIPS_AT) {
            scytheStage(mc, worldTicks - SCYTHES_AT);
        } else if (worldTicks == WHIPS_AT && !AVRegistry.FUNDAMENTUM) {
            LOG.info("AVTEST done (no whips)");
            finish(mc);
        } else if (worldTicks >= WHIPS_AT && AVRegistry.FUNDAMENTUM) {
            whipStage(mc, worldTicks - WHIPS_AT);
        }
    }

    private static final int LATTICE_AT = 1420;
    private static final int THURIBLE_AT = LATTICE_AT + 400;
    private static final int LENS_AT = THURIBLE_AT + 310;
    private static final int LAB_AT = LENS_AT + 120;
    private static final int SCYTHES_AT = LAB_AT + 460;
    private static final int WHIPS_AT = SCYTHES_AT + 3 * 30;
    /** Far past every other stage; only -Pautotest=structures jumps here. */
    private static final int STRUCT_AT = 100000;
    private static final int EXAN_AT = 200000;

    /** The Exanimator: rises (START), fights a player it can't hurt, Eclipse at half health, killed, drop logged. */
    private static void exanimatorStage(Minecraft mc, int t) {
        if (t == 0) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            onServer(mc, p -> ExanimatorAutotest.setUp(p, LOG));
        } else if (t == 1700) {
            onServer(mc, p -> ExanimatorAutotest.summary(p, LOG));
        } else if (t == 1710) {
            LOG.info("AVTEST done (exanimator)");
            finish(mc);
        } else {
            if (t % 10 == 0 && t < 1500) {
                onServer(mc, p -> ExanimatorAutotest.report(p, LOG, "t=" + t));
            }
            if (t == 900) {
                onServer(mc, p -> ExanimatorAutotest.halve(p, LOG));
            } else if (t == 1400) {
                onServer(mc, p -> ExanimatorAutotest.kill(p, LOG));
            } else if (t == 1480) {
                onServer(mc, p -> ExanimatorAutotest.loot(p, LOG));
            }
            int[] shots = {30, 70, 200, 340, 480, 620, 760, 880, 930, 980, 1100, 1250, 1380, 1500};
            for (int s : shots) {
                if (t == s - 4) {
                    onServer(mc, ExanimatorAutotest::face);
                } else if (t == s) {
                    shot(mc, "exanimator" + t);
                }
            }
        }
    }

    /**
     * The Wizard Tower and a Forsaken Laboratory as the world created them (client/StructureAutotest): located, visited,
     * walked into; the ward must seal and the markers must have become lab blocks. Shots outside and inside each.
     */
    private static void structureStage(Minecraft mc, int t) {
        if (t == 0) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            onServer(mc, p -> StructureAutotest.locate(p, LOG));
        } else if (t == 40) {
            onServer(mc, p -> StructureAutotest.approach(p, LOG, true));
        } else if (t == 160) {
            shot(mc, "tower_outside");
        } else if (t == 180) {
            onServer(mc, p -> StructureAutotest.enter(p, LOG, true));
        } else if (t == 260) {
            shot(mc, "tower_inside");
        } else if (t == 270) {
            onServer(mc, p -> StructureAutotest.report(p, LOG, true));
        } else if (t == 280 && !StructureAutotest.climbDone) {
            // the climb: hold here, one step a second, until every floor is cleared and the guardian is dead
            if (climbTicks % 20 == 0) {
                onServer(mc, p -> StructureAutotest.climb(p, LOG));
            }
            if (climbTicks % 160 == 150) {
                shot(mc, "tower_climb" + climbTicks);
            }
            if (++climbTicks > 4000) {
                LOG.error("AVTEST climb FAIL out of time");
                StructureAutotest.climbDone = true;
            }
            worldTicks--;
        } else if (t == 300) {
            onServer(mc, p -> StructureAutotest.approach(p, LOG, false));
        } else if (t == 700) {
            shot(mc, "lab_above");
        } else if (t == 720) {
            onServer(mc, p -> StructureAutotest.enter(p, LOG, false));
        } else if (t == 820) {
            shot(mc, "lab_inside");
        } else if (t == 830) {
            onServer(mc, p -> StructureAutotest.report(p, LOG, false));
        } else if (t == 860) {
            LOG.info("AVTEST done (structures)");
            finish(mc);
        }
    }

    /**
     * The Alembic Lattice (client/LatticeAutotest): ignis and aer drawn from two jars meet at a junction and their compound
     * fills a third jar. A report every second, shots of motes in flight and of the result.
     */
    private static void latticeStage(Minecraft mc, int t) {
        if (t == 0) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            onServer(mc, p -> {
                p.getInventory().clearContent();
                LatticeAutotest.build(p, LOG);
            });
        } else if (t % 20 == 0 && t < 380) {
            onServer(mc, p -> LatticeAutotest.report(p, LOG, "t=" + t));
        }
        if (t == 37 || t == 44 || t == 90 || t == 300) {
            shot(mc, "lattice" + t);
        } else if (t == 390) {
            onServer(mc, p -> LatticeAutotest.report(p, LOG, "final"));
        }
    }

    /**
     * The Thurible (client/ThuribleAutotest): filled from a jar and from a phial, then swung (use held on the client) at a
     * zombie, and swung into it once. The shots prove the model has no gaps: first person held and through the swing,
     * third person from behind and in front, looking level, up and down, swinging and still.
     */
    private static void thuribleStage(Minecraft mc, int t) {
        if (t == 0) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            onServer(mc, p -> ThuribleAutotest.setUp(p, LOG));
        } else if (t == 12) {
            shot(mc, "thurible_fp_held");
        } else if (t == 20) {
            mc.options.keyUse.setDown(true);
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        } else if (t >= 24 && t <= 44 && t % 4 == 0) {
            shot(mc, "thurible_fp_swing" + t);
        } else if (t == 50) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        } else if (t == 60 || t == 66) {
            shot(mc, "thurible_3p_back" + t);
        } else if (t == 70) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        } else if (t == 80 || t == 86) {
            shot(mc, "thurible_3p_front" + t);
        } else if (t == 90) {
            mc.player.setXRot(-35);
        } else if (t == 98) {
            shot(mc, "thurible_3p_front_up");
        } else if (t == 100) {
            mc.player.setXRot(45);
        } else if (t == 108) {
            shot(mc, "thurible_3p_front_down");
        } else if (t == 110) {
            mc.player.setXRot(0);
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            mc.player.setYRot(mc.player.getYRot() + 60);
        } else if (t == 118) {
            shot(mc, "thurible_3p_back_turned");
        } else if (t == 120) {
            mc.options.keyUse.setDown(false);
            mc.player.setYRot(mc.player.getYRot() - 60);
            onServer(mc, p -> ThuribleAutotest.report(p, LOG, "after swing"));
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        } else if (t == 132) {
            shot(mc, "thurible_3p_still");
        } else if (t == 136) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        } else if (t == 145) {
            onServer(mc, p -> ThuribleAutotest.hit(p, LOG));
        } else if (t == 190) {
            onServer(mc, p -> ThuribleAutotest.report(p, LOG, "final"));
        } else if (t == 200) {
            shot(mc, "thurible_idle");
        } else if (t >= 210) {
            thuribleLook(mc, t - 210);
        }
    }

    /**
     * The no-gaps pass: the zombie, the clouds and the particles out of the way and a narrow field of view, so
     * the censer fills the shots. First person held and through a swing, then third person front and back, level, from
     * above and from below, swinging and still.
     */
    private static void thuribleLook(Minecraft mc, int u) {
        if (u <= 80) {
            onServer(mc, p -> p.level().getEntitiesOfClass(net.minecraft.world.entity.Entity.class, p.getBoundingBox().inflate(32),
                    e -> e instanceof com.arcanavaria.thurible.ThuribleCloud || e instanceof net.minecraft.world.entity.monster.zombie.Zombie)
                    .forEach(net.minecraft.world.entity.Entity::discard));
            mc.particleEngine.clearParticles();
        }
        if (u == 0) {
            mc.options.fov().set(50);            // the HUD stays: hiding it also hides the first-person hand
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.player.setXRot(10);
        } else if (u == 8) {
            shot(mc, "thurible_look_fp_held");
        } else if (u == 10) {
            mc.options.keyUse.setDown(true);
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        } else if (u >= 13 && u <= 29 && u % 4 == 1) {
            shot(mc, "thurible_look_fp_swing" + u);
        } else if (u == 32) {
            mc.options.fov().set(55);
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            mc.player.setXRot(12);                  // the camera a little above, so the hanging censer is in frame
        } else if (u == 38 || u == 41) {
            shot(mc, "thurible_look_3p_front" + u);
        } else if (u == 43) {
            mc.player.setXRot(25);
        } else if (u == 47) {
            shot(mc, "thurible_look_3p_front_high");
        } else if (u == 49) {
            mc.player.setXRot(-20);
        } else if (u == 53) {
            shot(mc, "thurible_look_3p_front_low");
        } else if (u == 55) {
            mc.player.setXRot(20);
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        } else if (u == 61 || u == 64) {
            shot(mc, "thurible_look_3p_back" + u);
        } else if (u == 66) {
            mc.options.keyUse.setDown(false);
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            mc.player.setXRot(12);
        } else if (u == 74) {
            shot(mc, "thurible_look_3p_front_still");
        } else if (u == 76) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        } else if (u == 80) {
            shot(mc, "thurible_look_3p_back_still");
        } else if (u == 82) {
            mc.options.fov().set(70);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
    }

    /** The Architect's Lens (client/LensAutotest): fill, undo, radial copies, copy and paste, then a shot of a wall's ghost. */
    private static void lensStage(Minecraft mc, int t) {
        mc.getToastManager().clear();
        if (t == 0) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            onServer(mc, p -> LensAutotest.run(p, LOG));
        } else if (t == 50 || t == 90) {
            shot(mc, "lens" + t);
        } else if (t == 62) {
            net.minecraft.client.KeyMapping.click(com.arcanavaria.lens.client.LensKeys.MENU.getKey());
        } else if (t == 63) {
            LOG.info("AVTEST lens wheel {}", mc.screen instanceof com.arcanavaria.lens.client.LensWheelScreen ? "PASS" : "FAIL");
            shot(mc, "lens_wheel");
        } else if (t == 68) {
            LOG.info("AVTEST lens wheel_release {}", mc.screen == null ? "PASS" : "FAIL");
            mc.options.chatVisibility().set(net.minecraft.world.entity.player.ChatVisiblity.FULL);
            onServer(mc, player -> {
                var scroll = new net.minecraft.world.item.ItemStack(com.arcanavaria.lens.LensRegistry.BLUEPRINT_SCROLL.get());
                scroll.set(com.arcanavaria.lens.LensRegistry.BLUEPRINT.get(), new com.arcanavaria.lens.BlueprintData("", java.util.List.of(
                        new com.arcanavaria.lens.BlueprintData.Entry(BlockPos.ZERO, net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState()),
                        new com.arcanavaria.lens.BlueprintData.Entry(BlockPos.ZERO.above(), net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState()))));
                player.setItemInHand(InteractionHand.OFF_HAND, scroll);
            });
            mc.setScreen(new com.arcanavaria.lens.client.LensSettingsScreen(com.arcanavaria.lens.LensRegistry.held(mc.player)));
        } else if (t == 73) {
            shot(mc, "lens_settings");
            var screen = mc.screen;
            if (screen instanceof com.arcanavaria.lens.client.LensSettingsScreen) {
                var range = screen.children().stream().filter(child -> child instanceof net.minecraft.client.gui.components.Button button
                        && button.getMessage().getString().startsWith("Range ")).findFirst();
                if (range.isPresent()) {
                    ((net.minecraft.client.gui.components.Button) range.get()).onPress(new net.minecraft.client.input.KeyEvent(257, 0, 0));
                    LOG.info("AVTEST lens settings_button PASS");
                } else {
                    LOG.info("AVTEST lens settings_button FAIL");
                }
                var materials = screen.children().stream().filter(child -> child instanceof net.minecraft.client.gui.components.Button button
                        && button.getMessage().getString().startsWith("Materials ")).findFirst();
                materials.ifPresent(widget -> ((net.minecraft.client.gui.components.Button) widget)
                        .onPress(new net.minecraft.client.input.KeyEvent(257, 0, 0)));
                screen.onClose();
            } else {
                LOG.info("AVTEST lens settings_button FAIL screen absent");
            }
        } else if (t == 82) {
            onServer(mc, player -> {
                var lens = com.arcanavaria.lens.LensRegistry.held(player);
                var settings = lens.getOrDefault(com.arcanavaria.lens.LensRegistry.SETTINGS.get(), com.arcanavaria.lens.LensSettings.DEFAULT);
                LOG.info("AVTEST lens settings_packet {} range={}", settings.range() == 7
                        && com.arcanavaria.lens.ArchitectsLensItem.state(lens).ready() ? "PASS" : "FAIL", settings.range());
            });
        } else if (t == 95) {
            mc.options.chatVisibility().set(net.minecraft.world.entity.player.ChatVisiblity.HIDDEN);
            onServer(mc, player -> {
                com.arcanavaria.lens.ArchitectsLensItem.setMode(com.arcanavaria.lens.LensRegistry.held(player), com.arcanavaria.lens.LensMode.SURFACE);
                player.setItemInHand(InteractionHand.OFF_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE_BRICKS, 64));
                player.setXRot(65);
                player.teleportTo(-0.5, -60, 1.5);
            });
        } else if (t == 110) {
            LOG.info("AVTEST lens ghost {} blocks={}", com.arcanavaria.lens.client.LensClient.preview().isEmpty() ? "FAIL" : "PASS",
                    com.arcanavaria.lens.client.LensClient.preview().size());
            shot(mc, "lens_gadget_ghost");
        }
    }

    /**
     * The Forsaken Laboratories (client/LabAutotest): sealed in a ward, the anti-cheese checks, every puzzle, the guardian
     * rising and dying (shots of the room, the guardian and its bar), then dying in a second ward and respawning outside.
     */
    private static void labStage(Minecraft mc, int t) {
        if (t == 0) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            onServer(mc, p -> LabAutotest.setUp(p, LOG));
        } else if (t == 30) {
            onServer(mc, p -> LabAutotest.sealed(p, LOG));
        } else if (t == 60) {
            onServer(mc, p -> LabAutotest.puzzles(p, LOG, 0));
        } else if (t == 90) {
            onServer(mc, p -> LabAutotest.puzzles(p, LOG, 1));
        } else if (t == 100) {
            onServer(mc, p -> p.connection.teleport(0.5, -59, -2.5, 0, 25));
        } else if (t == 115) {
            onServer(mc, p -> LabAutotest.puzzles(p, LOG, 2));
            shot(mc, "lab_room");
        } else if (t == 120) {
            onServer(mc, p -> p.connection.teleport(-0.5, -59, -2.5, 135, 25));
        } else if (t == 135) {
            shot(mc, "lab_beam");
        } else if (t == 140) {
            onServer(mc, p -> p.connection.teleport(0.5, -59, 0.5, 0, 0));
        } else if (t == 200) {
            onServer(mc, p -> LabAutotest.boss(p, LOG, false));
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        } else if (t == 215) {
            shot(mc, "lab_guardian");
        } else if (t == 240) {
            onServer(mc, p -> LabAutotest.boss(p, LOG, true));
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        } else if (t == 270) {
            shot(mc, "lab_cleared");
            onServer(mc, p -> LabAutotest.deathSetUp(p, LOG));
        } else if (t == 300) {
            onServer(mc, p -> LabAutotest.die(p, LOG));
        } else if (t == 330 && mc.player != null) {
            mc.player.respawn();
        } else if (t == 380) {
            onServer(mc, p -> LabAutotest.afterRespawn(p, LOG));
        }
    }

    /**
     * Fundamentum's whips, one every WhipAutotest.TICKS_PER_WHIP: set up, a shot held idle (coiled under the handle) in first
     * person, one from the front in third person (coil in hand), then a swing in first person with two shots mid-swing,
     * and the result.
     */
    private static void whipStage(Minecraft mc, int t) {
        int i = t / WhipAutotest.TICKS_PER_WHIP, at = t % WhipAutotest.TICKS_PER_WHIP;
        if (i >= WhipAutotest.whips().size()) {
            afterWhips(mc, t - WhipAutotest.whips().size() * WhipAutotest.TICKS_PER_WHIP);
            return;
        }
        if (at == 0) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            onServer(mc, p -> WhipAutotest.setUp(p, i));
        } else if (at == 16) {
            shot(mc, "whipidle" + i);
        } else if (at == 17) {
            // turn away from the zombie, so the front camera sees the player and not the zombie
            onServer(mc, p -> WhipAutotest.face(p, 180));
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        } else if (at == 28) {
            shot(mc, "whipidle3p" + i);
        } else if (at == 29) {
            onServer(mc, p -> WhipAutotest.face(p, 0));
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        } else if (at == 36) {
            onServer(mc, p -> WhipAutotest.swing(p, LOG));
        } else if (at >= 37 && at <= 46 && whipSeen < i + 1 && WhipAutotest.swingingOnClient(mc)) {
            whipSeen = i + 1;
            whipSeenAt = at;
            LOG.info("AVTEST whip {} swinging on client", i);
        } else if (whipSeen == i + 1 && (at == whipSeenAt + 2 || at == whipSeenAt + 5)) {
            // two shots a few ticks into the swing, once the cord is out
            shot(mc, "whip" + i + (at == whipSeenAt + 2 ? "a" : "b"));
        } else if (at == 47 && whipSeen < i + 1) {
            LOG.info("AVTEST whip {} FAIL no swing on client", i);
        } else if (at == 66) {
            onServer(mc, p -> WhipAutotest.result(p, LOG));
        }
    }

    /** Each scythe held in first person and swung (the attack swing), with shots before and through the swing. */
    private static void scytheStage(Minecraft mc, int t) {
        String[] scythes = {"thaumium_scythe", "void_scythe", "elemental_scythe"};
        int i = t / 30, at = t % 30;
        if (at == 0) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            onServer(mc, p -> {
                p.setItemInHand(InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(com.arcanavaria.ArcanaVaria.id(scythes[i]))));
                p.setXRot(10.0F);
            });
        } else if (at == 8) {
            shot(mc, "scythe" + i + "_held");
        } else if (at == 10) {
            mc.player.swing(InteractionHand.MAIN_HAND);
        } else if (at == 11 || at == 13 || at == 15) {
            LOG.info("AVTEST scythe {} swing attackAnim={}", scythes[i], mc.player.getAttackAnim(0.0F));
            shot(mc, "scythe" + i + "_swing" + at);
        }
    }

    private static int primalSwings;
    private static boolean primalsDone;

    /**
     * After the four whips: the Elemental Whip swung every WhipAutotest.TICKS_PER_PRIMAL ticks at a rooted zombie until
     * all six primals have shown (or MAX_PRIMAL_SWINGS), then the Amberbark Lash's mark with real summons.
     */
    private static void afterWhips(Minecraft mc, int t) {
        if (!primalsDone) {
            int at = t % WhipAutotest.TICKS_PER_PRIMAL;
            if (t == 0) {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                onServer(mc, WhipAutotest::primalsSetUp);
            } else if (t < 10) {
                return;
            } else if (at == 0) {
                int n = ++primalSwings;
                onServer(mc, p -> WhipAutotest.primalSwing(p, LOG));
                if (n == 1) {
                    LOG.info("AVTEST primals start");
                }
            } else if (at < 24) {
                onServer(mc, WhipAutotest::primalSample);
                if (at == 5 && primalSwings <= 3) {
                    shot(mc, "primal" + primalSwings);
                }
            } else if (at == 24) {
                int n = primalSwings;
                onServer(mc, p -> {
                    if (WhipAutotest.primalRead(p, n, LOG) || n >= WhipAutotest.MAX_PRIMAL_SWINGS) {
                        WhipAutotest.primalsEnd(LOG);
                        primalsDone = true;
                    }
                });
            }
            marksAt = worldTicks + 20;
            return;
        }
        int m = worldTicks - marksAt;
        if (m == 0) {
            onServer(mc, p -> WhipAutotest.marksSetUp(p, LOG));
        } else if (m == 100) {
            onServer(mc, p -> WhipAutotest.marksLash(p, LOG));
        } else if (m == 112) {
            shot(mc, "marks_lash");
        } else if (m == 130) {
            onServer(mc, p -> WhipAutotest.marksCheck(p, LOG));
        } else if (m == 200) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            shot(mc, "marks_summons");
        } else if (m == 290) {
            onServer(mc, p -> WhipAutotest.marksResult(p, LOG));
        } else if (m == 300) {
            LOG.info("AVTEST done");
            finish(mc);
        }
    }

    private static int marksAt = Integer.MAX_VALUE;

    private static void deleteOldWorld(Minecraft mc) {
        LevelStorageSource source = mc.getLevelSource();
        if (!source.levelExists(WORLD)) {
            return;
        }
        try (LevelStorageSource.LevelStorageAccess access = source.createAccess(WORLD)) {
            access.deleteLevel();
            LOG.info("AVTEST deleted old world {}", WORLD);
        } catch (Exception e) {
            LOG.warn("AVTEST could not delete old world {}", WORLD, e);
        }
    }

    /** Superflat, player on y=-60 facing south and looking down a little; two scythes float on the ground in front. */
    private static void setUp(Minecraft mc) {
        mc.options.chatVisibility().set(net.minecraft.world.entity.player.ChatVisiblity.HIDDEN);
        String ground = "{PickupDelay:32767,Age:-32768,NoGravity:1b,Motion:[0.0,0.0,0.0],Item:{id:\"arcanavaria:%s\",count:1}}";
        String[] commands = {
            "time set noon",
            "weather clear",
            "gamerule advance_time false",
            "gamerule advance_weather false",
            "gamerule spawn_mobs false",
            "tp @a 0.5 -60 0.5 0 35",
            "clear @a",
            "item replace entity @a weapon.mainhand with arcanavaria:thaumium_scythe",
            "summon minecraft:item -0.3 -59.9 2.3 " + String.format(ground, "void_scythe"),
            "summon minecraft:item 1.3 -59.9 2.3 " + String.format(ground, "elemental_scythe"),
        };
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null) {
            LOG.error("AVTEST no integrated server");
            return;
        }
        server.execute(() -> {
            for (String c : commands) {
                LOG.info("AVTEST /{}", c);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
            }
            LOG.info("AVTEST set up");
        });
    }

    private static void onServer(Minecraft mc, java.util.function.Consumer<ServerPlayer> job) {
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayers().isEmpty() ? null : server.getPlayerList().getPlayers().get(0);
            if (p == null) {
                LOG.error("AVTEST no server player");
                return;
            }
            try {
                job.accept(p);
            } catch (Throwable t) {
                LOG.error("AVTEST server job failed", t);
            }
        });
    }

    private static GreatwoodWarden warden(ServerPlayer p) {
        List<GreatwoodWarden> all = p.level().getEntitiesOfClass(GreatwoodWarden.class, p.getBoundingBox().inflate(64.0));
        return all.isEmpty() ? null : all.get(0);
    }

    /** Survival with Resistance IV, regeneration and saturation; plants a Heartseed 10 blocks south through its real useOn. */
    private static void summonWarden(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        for (String c : new String[] {"difficulty normal", "gamemode survival @a", "effect give @a resistance infinite 3 true",
            "effect give @a regeneration infinite 2 true", "effect give @a saturation infinite 0 true", "clear @a",
            "item replace entity @a weapon.mainhand with arcanavaria:heartseed", "kill @e[type=minecraft:item]",
            "tp @a 0.5 -60 0.5 0 10"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        }
        ItemStack seed = p.getMainHandItem();
        BlockHitResult hit = new BlockHitResult(new Vec3(0.5, -61.0, 10.5), Direction.UP, new BlockPos(0, -61, 10), false);
        InteractionResult r = seed.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, hit));
        InteractionResult again = new ItemStack(AVRegistry.HEARTSEED.get()).useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, hit));
        LOG.info("AVTEST heartseed use={} second={} left={} warden={}", r, again, p.getMainHandItem().getCount(), warden(p) != null);
    }

    private static void logWarden(ServerPlayer p) {
        GreatwoodWarden w = warden(p);
        LOG.info("AVTEST warden {} player hp={}/{}", w == null ? "none" : "state=" + w.state() + " t=" + (int) w.stateTime(0)
            + " hp=" + (int) w.getHealth() + " p2=" + w.phaseTwo() + " dist=" + String.format("%.1f", w.distanceTo(p)) + " at=" + w.blockPosition().toShortString()
            + " player at=" + p.blockPosition().toShortString(),
            (int) p.getHealth(), (int) p.getMaxHealth());
    }

    private static void halveWarden(ServerPlayer p) {
        GreatwoodWarden w = warden(p);
        if (w != null) {
            w.setHealth(116.0F);
            boolean hurt = w.hurtServer((ServerLevel) p.level(), p.damageSources().playerAttack(p), 10.0F);
            LOG.info("AVTEST halved warden hurt={} hp={}", hurt, w.getHealth());
        }
    }

    private static void amberHeart(ServerPlayer p) {
        float before = p.getMaxHealth();
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AVRegistry.AMBER_HEART.get(), 2));
        InteractionResult first = p.getMainHandItem().use(p.level(), p, InteractionHand.MAIN_HAND);
        InteractionResult second = p.getMainHandItem().use(p.level(), p, InteractionHand.MAIN_HAND);
        LOG.info("AVTEST amber heart first={} second={} max {} -> {} left={}", first, second, before, p.getMaxHealth(),
            p.getMainHandItem().getCount());
    }

    private static void killWarden(ServerPlayer p) {
        GreatwoodWarden w = warden(p);
        if (w != null) {
            w.hurtServer((ServerLevel) p.level(), p.damageSources().playerAttack(p), 1000.0F);
            LOG.info("AVTEST killed warden dead={}", w.isDeadOrDying());
        }
    }

    private static void logLoot(ServerPlayer p) {
        List<ItemEntity> items = p.level().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(48.0));
        StringBuilder sb = new StringBuilder();
        for (ItemEntity e : items) {
            sb.append(e.getItem().getCount()).append('x').append(BuiltInRegistries.ITEM.getKey(e.getItem().getItem())).append(' ');
        }
        LOG.info("AVTEST loot [{}] warden left={}", sb.toString().trim(), warden(p) != null);
    }

    private static void shot(Minecraft mc, String view) {
        String name = "avtest_" + view + "_" + (System.currentTimeMillis() / 1000) + ".png";
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), 1, msg -> LOG.info("AVTEST screenshot message: {}", msg.getString()));
        LOG.info("AVTEST shot {} held={}", name, mc.player.getMainHandItem());
    }

    private static void finish(Minecraft mc) {
        finished = true;
        LOG.info("AVTEST stopping client");
        mc.stop();
    }
}
