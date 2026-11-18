package com.arcanavaria.lab;

import static com.arcanavaria.ArcanaVaria.MODID;
import static com.arcanavaria.ArcanaVaria.id;

import com.arcanavaria.AVRegistry;
import com.arcanavaria.lab.boss.BossData;
import com.arcanavaria.lab.boss.GuardianAltarBlock;
import com.arcanavaria.lab.boss.GuardianAltarBlockEntity;
import com.arcanavaria.lab.boss.LabArchetype;
import com.arcanavaria.lab.boss.LabBosses;
import com.arcanavaria.lab.boss.LabPool;
import com.arcanavaria.lab.boss.LabTrait;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaCapabilities;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The Forsaken Laboratories (0.6.0), code side: the ward and its anti-cheese, the puzzle blocks, the guardian altar and
 * data-driven mini-boss pool, and the journals. The structures themselves come from Worldforge as data. Every lab block
 * is unbreakable outside creative: the labs are built of them, nobody crafts them.
 */
public final class LabRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);

    /** Blocks a sealed player may still place and break inside a ward (the lattice for routing puzzles, torches). */
    public static final TagKey<Block> MUTABLE = TagKey.create(Registries.BLOCK, id("lab_mutable"));

    /** data/&lt;ns&gt;/arcanavaria/lab_archetype, lab_trait and lab_pool */
    public static final ResourceKey<Registry<LabArchetype>> ARCHETYPE_KEY = ResourceKey.createRegistryKey(id("lab_archetype"));
    public static final ResourceKey<Registry<LabTrait>> TRAIT_KEY = ResourceKey.createRegistryKey(id("lab_trait"));
    public static final ResourceKey<Registry<LabPool>> POOL_KEY = ResourceKey.createRegistryKey(id("lab_pool"));

    public static final DeferredBlock<WardCoreBlock> WARD_CORE = block("ward_core", WardCoreBlock::new, SoundType.LODESTONE,
            s -> s.getValue(WardCoreBlock.WARD) == WardState.SEALED ? 12 : s.getValue(WardCoreBlock.WARD) == WardState.DORMANT ? 5 : 2, false);
    public static final DeferredBlock<GuardianAltarBlock> GUARDIAN_ALTAR = block("guardian_altar", GuardianAltarBlock::new, SoundType.DEEPSLATE_BRICKS,
            s -> s.getValue(GuardianAltarBlock.AWAKE) ? 10 : 0, true);
    public static final DeferredBlock<AspectLockBlock> ASPECT_LOCK = block("aspect_lock", AspectLockBlock::new, SoundType.COPPER,
            s -> s.getValue(BlockStateProperties.LIT) ? 9 : 3, false);
    public static final DeferredBlock<RuneStoneBlock> RUNE_STONE = block("rune_stone", RuneStoneBlock::new, SoundType.DEEPSLATE_BRICKS,
            s -> s.getValue(BlockStateProperties.LIT) ? 11 : 4, false);
    public static final DeferredBlock<RuneLockBlock> RUNE_LOCK = block("rune_lock", RuneLockBlock::new, SoundType.DEEPSLATE_BRICKS,
            s -> s.getValue(BlockStateProperties.LIT) ? 10 : 2 + s.getValue(RuneLockBlock.PIPS), false);
    public static final DeferredBlock<VisLiftBlock> VIS_LIFT = block("vis_lift", VisLiftBlock::new, SoundType.COPPER,
            s -> VisLiftBlock.active(s) ? 10 : 2, false);
    public static final DeferredBlock<PhaseBlock> PHASE_BLOCK = block("phase_block", PhaseBlock::new, SoundType.AMETHYST,
            s -> s.getValue(PhaseBlock.SOLID) ? 6 : 3, true);
    public static final DeferredBlock<BeamBlocks.Emitter> BEAM_EMITTER = block("beam_emitter", BeamBlocks.Emitter::new, SoundType.COPPER,
            s -> 8, false);
    public static final DeferredBlock<BeamBlocks.Mirror> BEAM_MIRROR = block("beam_mirror", BeamBlocks.Mirror::new, SoundType.GLASS,
            s -> 0, true);
    public static final DeferredBlock<BeamBlocks.Receptor> BEAM_RECEPTOR = block("beam_receptor", BeamBlocks.Receptor::new, SoundType.COPPER,
            s -> s.getValue(BlockStateProperties.LIT) ? 12 : 2, false);
    public static final DeferredBlock<FloorGateBlock> FLOOR_GATE = block("floor_gate", FloorGateBlock::new, SoundType.GLASS,
            s -> s.getValue(FloorGateBlock.OPEN) ? 0 : 4, true);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WardCoreBlockEntity>> WARD_CORE_BE =
            BLOCK_ENTITIES.register("ward_core", () -> new BlockEntityType<>(WardCoreBlockEntity::new, WARD_CORE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GuardianAltarBlockEntity>> GUARDIAN_ALTAR_BE =
            BLOCK_ENTITIES.register("guardian_altar", () -> new BlockEntityType<>(GuardianAltarBlockEntity::new, GUARDIAN_ALTAR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AspectLockBlockEntity>> ASPECT_LOCK_BE =
            BLOCK_ENTITIES.register("aspect_lock", () -> new BlockEntityType<>(AspectLockBlockEntity::new, ASPECT_LOCK.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RuneLockBlockEntity>> RUNE_LOCK_BE =
            BLOCK_ENTITIES.register("rune_lock", () -> new BlockEntityType<>(RuneLockBlockEntity::new, RUNE_LOCK.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VisLiftBlock.Tick>> VIS_LIFT_BE =
            BLOCK_ENTITIES.register("vis_lift", () -> new BlockEntityType<>(VisLiftBlock.Tick::new, VIS_LIFT.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PhaseBlock.Tick>> PHASE_BLOCK_BE =
            BLOCK_ENTITIES.register("phase_block", () -> new BlockEntityType<>(PhaseBlock.Tick::new, PHASE_BLOCK.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BeamBlocks.EmitterTick>> BEAM_EMITTER_BE =
            BLOCK_ENTITIES.register("beam_emitter", () -> new BlockEntityType<>(BeamBlocks.EmitterTick::new, BEAM_EMITTER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BeamBlocks.ReceptorTick>> BEAM_RECEPTOR_BE =
            BLOCK_ENTITIES.register("beam_receptor", () -> new BlockEntityType<>(BeamBlocks.ReceptorTick::new, BEAM_RECEPTOR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FloorGateBlock.Tick>> FLOOR_GATE_BE =
            BLOCK_ENTITIES.register("floor_gate", () -> new BlockEntityType<>(FloorGateBlock.Tick::new, FLOOR_GATE.get()));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<LabJournalItem.Journal>> JOURNAL = COMPONENTS.registerComponentType(
            "journal", b -> b.persistent(LabJournalItem.Journal.CODEC).networkSynchronized(LabJournalItem.Journal.STREAM_CODEC));

    public static final DeferredItem<LabJournalItem> LAB_JOURNAL = AVRegistry.tab(AVRegistry.ITEMS.registerItem("lab_journal",
            LabJournalItem::new, p -> p.stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<WardLink>> WARD_LINK = ATTACHMENTS.register("ward_link",
            () -> AttachmentType.<WardLink>builder(() -> null).serialize(WardLink.CODEC).copyOnDeath().build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<BossData>> BOSS = ATTACHMENTS.register("lab_boss",
            () -> AttachmentType.<BossData>builder(() -> null).serialize(BossData.CODEC).build());

    private LabRegistry() {
    }

    private static <B extends Block> DeferredBlock<B> block(String name, Function<BlockBehaviour.Properties, B> factory, SoundType sound,
                                                            ToIntFunction<BlockState> light, boolean seeThrough) {
        DeferredBlock<B> block = BLOCKS.registerBlock(name, factory, p -> {
            p.mapColor(MapColor.COLOR_PURPLE).strength(-1.0F, 3600000.0F).noLootTable().sound(sound).lightLevel(light);
            return seeThrough ? p.noOcclusion().isViewBlocking((s, l, pos) -> false).isSuffocating((s, l, pos) -> false) : p;
        });
        AVRegistry.tab(AVRegistry.ITEMS.registerSimpleBlockItem(block, p -> p.rarity(Rarity.EPIC)));
        return block;
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        COMPONENTS.register(bus);
        ATTACHMENTS.register(bus);
        bus.addListener(LabRegistry::registries);
        bus.addListener(LabRegistry::capabilities);
        WardEvents.register();
        LabBosses.register();
    }

    private static void registries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(ARCHETYPE_KEY, LabArchetype.CODEC);
        event.dataPackRegistry(TRAIT_KEY, LabTrait.CODEC);
        event.dataPackRegistry(POOL_KEY, LabPool.CODEC);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(EssentiaCapabilities.STORAGE, ASPECT_LOCK_BE.get(), (be, side) -> be.storage);
    }
}
