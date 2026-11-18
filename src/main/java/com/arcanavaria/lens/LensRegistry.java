package com.arcanavaria.lens;

import static com.arcanavaria.ArcanaVaria.MODID;
import static com.arcanavaria.ArcanaVaria.id;

import com.arcanavaria.AVRegistry;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The Architect's Lens (0.7.0): items, components, the mode packet and the symmetry hook on block placing. */
public final class LensRegistry {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<LensState>> STATE = COMPONENTS.registerComponentType(
            "lens_state", b -> b.persistent(LensState.CODEC).networkSynchronized(LensState.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlueprintData>> BLUEPRINT = COMPONENTS.registerComponentType(
            "blueprint", b -> b.persistent(BlueprintData.CODEC).networkSynchronized(BlueprintData.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<LensSettings>> SETTINGS = COMPONENTS.registerComponentType(
            "lens_settings", b -> b.persistent(LensSettings.CODEC).networkSynchronized(LensSettings.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<LensSupply>> SUPPLY = COMPONENTS.registerComponentType(
            "lens_supply", b -> b.persistent(LensSupply.CODEC).networkSynchronized(LensSupply.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockState>> SELECTED = COMPONENTS.registerComponentType(
            "lens_block", b -> b.persistent(BlockState.CODEC).networkSynchronized(ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY)));

    public static final DeferredItem<ArchitectsLensItem> ARCHITECTS_LENS = AVRegistry.tab(AVRegistry.ITEMS.registerItem("architects_lens",
            ArchitectsLensItem::new, p -> p.stacksTo(1).rarity(Rarity.UNCOMMON).component(STATE.get(), LensState.DEFAULT)));
    public static final DeferredItem<BlueprintScrollItem> BLUEPRINT_SCROLL = AVRegistry.tab(AVRegistry.ITEMS.registerItem("blueprint_scroll",
            BlueprintScrollItem::new, p -> p.stacksTo(16)));

    /** Sneak + scroll on the client: step the held Lens's mode by {@code delta}. */
    public record ModePayload(int delta) implements CustomPacketPayload {
        public static final Type<ModePayload> TYPE = new Type<>(id("lens_mode"));
        public static final StreamCodec<ByteBuf, ModePayload> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(ModePayload::new, ModePayload::delta);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SettingsPayload(LensMode mode, LensSettings settings) implements CustomPacketPayload {
        public static final Type<SettingsPayload> TYPE = new Type<>(id("lens_settings"));
        public static final StreamCodec<ByteBuf, SettingsPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.fromCodec(LensMode.CODEC), SettingsPayload::mode,
                LensSettings.STREAM_CODEC, SettingsPayload::settings, SettingsPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ActionPayload(int action) implements CustomPacketPayload {
        public static final Type<ActionPayload> TYPE = new Type<>(id("lens_action"));
        public static final StreamCodec<ByteBuf, ActionPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(ActionPayload::new, ActionPayload::action);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
        bus.addListener(LensRegistry::payloads);
        NeoForge.EVENT_BUS.addListener(LensRegistry::placed);
    }

    private static void payloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(ModePayload.TYPE, ModePayload.STREAM_CODEC, LensRegistry::onMode);
        event.registrar("1").playToServer(SettingsPayload.TYPE, SettingsPayload.STREAM_CODEC, (message, context) -> {
            ItemStack lens = held(context.player());
            if (lens != null && message.mode() != null) {
                if (ArchitectsLensItem.state(lens).mode() != message.mode()) {
                    ArchitectsLensItem.setMode(lens, message.mode());
                }
                lens.set(SETTINGS.get(), message.settings());
            }
        });
        event.registrar("1").playToServer(ActionPayload.TYPE, ActionPayload.STREAM_CODEC, (message, context) -> {
            if (context.player() instanceof ServerPlayer player && held(player) != null) {
                if (message.action() == 0) {
                    int count = LensBuilder.undo((ServerLevel) player.level(), player);
                    ArchitectsLensItem.tell(player, count > 0 ? "undone" : "nothing_to_undo", count);
                } else if (message.action() > 0) {
                    BlueprintData blueprint = player.getOffhandItem().get(BLUEPRINT.get());
                    if (blueprint == null) {
                        blueprint = player.getMainHandItem().get(BLUEPRINT.get());
                    }
                    if (blueprint == null) {
                        ArchitectsLensItem.tell(player, "need_written");
                        return;
                    }
                    var materials = List.copyOf(blueprint.materials().entrySet());
                    int pages = Math.max(1, (materials.size() + 31) / 32);
                    int page = message.action() - 1;
                    if (page >= pages) {
                        return;
                    }
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.arcanavaria.lens.materials", page + 1, pages));
                    for (var entry : materials.subList(page * 32, Math.min(materials.size(), (page + 1) * 32))) {
                        int available = LensInventory.count(player, entry.getKey());
                        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.arcanavaria.lens.material",
                                entry.getKey().getDefaultInstance().getHoverName(), entry.getValue(), Math.max(0, entry.getValue() - available)));
                    }
                }
            }
        });
    }

    private static void onMode(ModePayload msg, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer p)) {
            return;
        }
        ItemStack lens = held(p);
        if (lens != null) {
            LensMode m = ArchitectsLensItem.state(lens).mode().step(Integer.signum(msg.delta()));
            ArchitectsLensItem.setMode(lens, m);
            ArchitectsLensItem.tell(p, "mode", net.minecraft.network.chat.Component.translatable("lens.%s.mode.%s".formatted(MODID, m.getSerializedName())));
        }
    }

    public static ItemStack held(net.minecraft.world.entity.player.Player p) {
        for (InteractionHand h : InteractionHand.values()) {
            if (p.getItemInHand(h).is(ARCHITECTS_LENS.get())) {
                return p.getItemInHand(h);
            }
        }
        return null;
    }

    /** Symmetry: a block placed by someone holding a Lens in a mirror or radial mode is copied about its anchor. */
    private static void placed(BlockEvent.EntityPlaceEvent event) {
        if (LensBuilder.checkingPlacement()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer p) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ItemStack lens = held(p);
        if (lens == null) {
            return;
        }
        LensState s = ArchitectsLensItem.state(lens);
        BlockPos at = event.getPos();
        if (!s.mode().symmetry() || s.anchor().isEmpty() || s.anchor().get().distManhattan(at) > 96) {
            return;
        }
        List<LensGeometry.Copy> copies = LensGeometry.copies(s.mode(), s.anchor().get(), at, event.getPlacedBlock());
        LensBuilder.begin(p);
        for (LensGeometry.Copy c : copies) {
            LensBuilder.Result r = LensBuilder.place(level, p, c.at(), c.state(), false);
            if (r != LensBuilder.Result.DONE) {
                ArchitectsLensItem.tell(p, r == LensBuilder.Result.NO_VIS ? "no_vis" : "no_items", 0);
                break;
            }
        }
    }

    private LensRegistry() {
    }
}
