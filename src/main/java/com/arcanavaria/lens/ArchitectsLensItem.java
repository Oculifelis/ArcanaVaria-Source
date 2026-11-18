package com.arcanavaria.lens;

import static com.arcanavaria.ArcanaVaria.MODID;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * The Architect's Lens (0.7.0): a building tool spending Ordo vis from a wand (Thaumaturge's foci are spell graphs built
 * in the focal manipulator, with no hook for a block-placing focus of our own, so it is a tool; the vis still comes from
 * the wand). Sneak and scroll to change mode.
 * <ul>
 * <li>Mirror and radial modes: right-click a block to set the anchor; then, with the Lens in either hand, every block you
 * place is copied about it (turned or mirrored to match).</li>
 * <li>Line, wall, fill, replace, copy: right-click two blocks for the corners, then right-click the air to build with the
 * block in your other hand (copy writes a blank Blueprint Scroll in your other hand instead; replace swaps every block
 * like the first corner).</li>
 * <li>Paste: hold a written scroll in the other hand, right-click where its corner goes, then the air to build it.</li>
 * <li>Sneak and right-click the air: undo the latest operation in this dimension, up to 20 steps. Sneak and right-click
 * a block: clear the selection.</li>
 * </ul>
 * The client previews all of it with particles (LensClient).
 */
public class ArchitectsLensItem extends Item {
    public ArchitectsLensItem(Properties props) {
        super(props);
    }

    public static LensState state(ItemStack stack) {
        return stack.getOrDefault(LensRegistry.STATE.get(), LensState.DEFAULT);
    }

    public static void setMode(ItemStack stack, LensMode mode) {
        stack.set(LensRegistry.STATE.get(), state(stack).withMode(mode));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player p = ctx.getPlayer();
        if (p == null) {
            return InteractionResult.PASS;
        }
        ItemStack stack = ctx.getItemInHand();
        LensState s = state(stack);
        if (ctx.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (s.mode() == LensMode.BIND) {
            if (p.isSecondaryUseActive()) {
                stack.remove(LensRegistry.SUPPLY.get());
                tell(p, "unbound");
            } else if (ctx.getLevel().mayInteract(p, ctx.getClickedPos())
                    && ctx.getLevel().getCapability(Capabilities.Item.BLOCK, ctx.getClickedPos(), ctx.getClickedFace()) != null) {
                stack.set(LensRegistry.SUPPLY.get(), new LensSupply(GlobalPos.of(ctx.getLevel().dimension(), ctx.getClickedPos()), ctx.getClickedFace()));
                tell(p, "bound", ctx.getClickedPos().toShortString());
            }
            return InteractionResult.SUCCESS;
        }
        if (s.mode().gadgetShape() || s.mode() == LensMode.DESTROY) {
            if (p.isSecondaryUseActive()) {
                BlockState selected = ctx.getLevel().getBlockState(ctx.getClickedPos());
                if (!selected.hasBlockEntity() && selected.getBlock().asItem() != net.minecraft.world.item.Items.AIR) {
                    stack.set(LensRegistry.SELECTED.get(), com.arcanavaria.lens.bg2.BlockStates.cleanBlockState(selected));
                    tell(p, "selected", selected.getBlock().getName());
                }
            } else {
                gadget((ServerLevel) ctx.getLevel(), (ServerPlayer) p, stack, ctx.getClickedPos(), ctx.getClickedFace());
            }
            return InteractionResult.SUCCESS;
        }
        if (p.isSecondaryUseActive()) {
            stack.set(LensRegistry.STATE.get(), s.cleared());
            tell(p, "cleared");
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = ctx.getClickedPos();
        if (s.mode().symmetry()) {
            stack.set(LensRegistry.STATE.get(), s.withAnchor(pos));
            tell(p, "anchor", pos.toShortString());
        } else if (s.mode() == LensMode.PASTE) {
            stack.set(LensRegistry.STATE.get(), s.click(pos.relative(ctx.getClickedFace())));
            tell(p, "paste_at");
        } else {
            LensState next = s.click(pos);
            stack.set(LensRegistry.STATE.get(), next);
            if (next.ready()) {
                tell(p, "corner_b", LensGeometry.shape(next.mode(), next.a().get(), next.b().get()).size());
            } else {
                tell(p, "corner_a");
            }
        }
        ctx.getLevel().playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 1.6F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player p, InteractionHand hand) {
        if (!(level instanceof ServerLevel server) || !(p instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = p.getItemInHand(hand);
        if (p.isSecondaryUseActive()) {
            int n = LensBuilder.undo(server, sp);
            tell(p, n > 0 ? "undone" : "nothing_to_undo", n);
            return InteractionResult.SUCCESS;
        }
        LensState s = state(stack);
        if (s.mode().gadgetShape() || s.mode() == LensMode.DESTROY || s.mode() == LensMode.BIND) {
            return InteractionResult.SUCCESS;
        }
        if (!s.ready()) {
            tell(p, "howto." + (s.mode().symmetry() ? "symmetry" : s.mode() == LensMode.PASTE ? "paste" : "shape"));
            return InteractionResult.SUCCESS;
        }
        ItemStack other = p.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        switch (s.mode()) {
            case COPY -> copy(server, p, s, other);
            case PASTE -> paste(server, sp, s, other);
            default -> build(server, sp, s, other);
        }
        return InteractionResult.SUCCESS;
    }

    public static BlockState selected(Player player, ItemStack lens) {
        ItemStack other = player.getMainHandItem() == lens ? player.getOffhandItem() : player.getMainHandItem();
        return other.getItem() instanceof BlockItem block ? block.getBlock().defaultBlockState() : lens.get(LensRegistry.SELECTED.get());
    }

    private static void gadget(ServerLevel level, ServerPlayer player, ItemStack lens, BlockPos start, net.minecraft.core.Direction face) {
        LensSettings settings = lens.getOrDefault(LensRegistry.SETTINGS.get(), LensSettings.DEFAULT);
        LensMode mode = state(lens).mode();
        BlockState block = selected(player, lens);
        if (mode != LensMode.DESTROY && block == null) {
            tell(player, "select_block");
            return;
        }
        LensBuilder.begin(player);
        int changed = 0;
        if (mode == LensMode.DESTROY) {
            for (BlockPos at : LensShapes.destruction(player, start, face, settings)) {
                BlockState before = level.getBlockState(at);
                if (LensBuilder.destroy(level, player, at) == LensBuilder.Result.NO_VIS) {
                    tell(player, "no_vis", changed);
                    return;
                }
                changed += before != level.getBlockState(at) ? 1 : 0;
            }
        } else {
            for (var entry : LensShapes.collect(player, mode, settings, start, face, block)) {
                BlockState before = level.getBlockState(entry.pos());
                LensBuilder.Result result = LensBuilder.place(level, player, entry.pos(), entry.state(), settings.exchange());
                if (result != LensBuilder.Result.DONE) {
                    tell(player, result == LensBuilder.Result.NO_VIS ? "no_vis" : "no_items", changed);
                    return;
                }
                changed += before != level.getBlockState(entry.pos()) ? 1 : 0;
            }
        }
        tell(player, "built", changed);
    }

    private static void build(ServerLevel level, ServerPlayer p, LensState s, ItemStack other) {
        if (!(other.getItem() instanceof BlockItem bi)) {
            tell(p, "need_block");
            return;
        }
        BlockState state = bi.getBlock().defaultBlockState();
        BlockState target = level.getBlockState(s.a().get());
        boolean replace = s.mode() == LensMode.REPLACE;
        LensBuilder.begin(p);
        int placed = 0;
        for (BlockPos at : LensGeometry.shape(s.mode(), s.a().get(), s.b().get())) {
            if (replace && level.getBlockState(at).getBlock() != target.getBlock()) {
                continue;
            }
            LensBuilder.Result r = LensBuilder.place(level, p, at, state, replace);
            if (r != LensBuilder.Result.DONE) {
                tell(p, r == LensBuilder.Result.NO_VIS ? "no_vis" : "no_items", placed);
                return;
            }
            placed++;
        }
        level.playSound(null, s.a().get(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8F, 1.2F);
        tell(p, "built", placed);
    }

    private static void copy(ServerLevel level, Player p, LensState s, ItemStack scroll) {
        if (!scroll.is(LensRegistry.BLUEPRINT_SCROLL.get()) || scroll.has(LensRegistry.BLUEPRINT.get())) {
            tell(p, "need_blank");
            return;
        }
        BlockPos a = s.a().get(), b = s.b().get();
        BlockPos min = BlockPos.min(a, b);
        List<BlueprintData.Entry> blocks = new ArrayList<>();
        for (BlockPos at : LensGeometry.box(a, b)) {
            if (!level.isLoaded(at)) {
                continue;
            }
            BlockState st = level.getBlockState(at);
            if (!st.isAir() && level.getBlockEntity(at) == null) {
                blocks.add(new BlueprintData.Entry(at.subtract(min), com.arcanavaria.lens.bg2.BlockStates.cleanBlockState(st)));
            }
        }
        ItemStack written = scroll.split(1);
        written.set(LensRegistry.BLUEPRINT.get(), new BlueprintData("", blocks));
        if (!p.getInventory().add(written)) {
            p.drop(written, false);
        }
        tell(p, "copied", blocks.size());
    }

    private static void paste(ServerLevel level, ServerPlayer p, LensState s, ItemStack scroll) {
        BlueprintData data = scroll.get(LensRegistry.BLUEPRINT.get());
        if (data == null) {
            tell(p, "need_written");
            return;
        }
        LensSettings settings = LensBuilder.settings(p);
        data = data.transformed(settings.rotation(), settings.reflection());
        LensBuilder.begin(p);
        int placed = 0;
        for (BlueprintData.Entry e : data.blocks()) {
            LensBuilder.Result r = LensBuilder.place(level, p, s.a().get().offset(e.at()), e.state(), false);
            if (r != LensBuilder.Result.DONE) {
                tell(p, r == LensBuilder.Result.NO_VIS ? "no_vis" : "no_items", placed);
                return;
            }
            placed++;
        }
        tell(p, "built", placed);
    }

    static void tell(Player p, String key, Object... args) {
        p.sendOverlayMessage(Component.translatable("message.%s.lens.%s".formatted(MODID, key), args));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        LensState s = state(stack);
        out.accept(Component.translatable("tooltip.%s.lens.mode".formatted(MODID),
                Component.translatable("lens.%s.mode.%s".formatted(MODID, s.mode().getSerializedName()))).withStyle(ChatFormatting.AQUA));
        out.accept(Component.translatable("lens.%s.help.%s".formatted(MODID, s.mode().getSerializedName())).withStyle(ChatFormatting.GRAY));
        out.accept(Component.translatable("tooltip.%s.lens.howto".formatted(MODID)).withStyle(ChatFormatting.DARK_GRAY));
    }
}
