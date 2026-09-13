package com.otectus.plantfiber.block;

import com.otectus.plantfiber.config.PlantFiberConfig;
import com.otectus.plantfiber.entity.RopeKnotEntity;
import com.otectus.plantfiber.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A climbable, self-supporting hanging rope. Unlike a ladder it does not need a solid block
 * behind every segment: a rope is valid as long as it hangs from a valid anchor above or from
 * another rope that ultimately connects to one. Climbing is provided by the
 * {@code #minecraft:climbable} block tag, not by code here.
 *
 * <p>Rope can be anchored two ways. Normally it hangs below a valid anchor block (clicking any
 * face of that block starts it in the cell directly below). It may also be tied to the side of a
 * fence, in which case it starts one block outward from the fence and {@link #FENCE_ANCHORED} is
 * set on that top segment, with {@link #HORIZONTAL_FACING} pointing back toward the fence.
 */
public class RopeBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty FENCE_ANCHORED = BooleanProperty.create("fence_anchored");
    public static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.box(6.0, 0.0, 6.0, 10.0, 16.0, 10.0);
    // Fence-anchored ropes hug the fence's outer face: the 4×4 column is pushed flush against the
    // post side it is tied to (HORIZONTAL_FACING points at that fence), so the whole column runs
    // down the outside of the fence instead of floating one cell out.
    private static final VoxelShape SHAPE_HUG_NORTH = Block.box(6.0, 0.0, 0.0, 10.0, 16.0, 4.0);
    private static final VoxelShape SHAPE_HUG_SOUTH = Block.box(6.0, 0.0, 12.0, 10.0, 16.0, 16.0);
    private static final VoxelShape SHAPE_HUG_WEST = Block.box(0.0, 0.0, 6.0, 4.0, 16.0, 10.0);
    private static final VoxelShape SHAPE_HUG_EAST = Block.box(12.0, 0.0, 6.0, 16.0, 16.0, 10.0);

    private static VoxelShape hugShape(Direction facing) {
        return switch (facing) {
            case SOUTH -> SHAPE_HUG_SOUTH;
            case WEST -> SHAPE_HUG_WEST;
            case EAST -> SHAPE_HUG_EAST;
            default -> SHAPE_HUG_NORTH;
        };
    }

    public RopeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(WATERLOGGED, Boolean.FALSE)
                .setValue(FENCE_ANCHORED, Boolean.FALSE)
                .setValue(HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED, FENCE_ANCHORED, HORIZONTAL_FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FENCE_ANCHORED) ? hugShape(state.getValue(HORIZONTAL_FACING)) : SHAPE;
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return false;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    // --- Support / anchoring ---

    /** A normal block can support a rope hanging below it if it is tagged or presents a sturdy down face. */
    boolean isValidNormalAnchor(BlockState state, LevelReader level, BlockPos pos) {
        return state.is(ModTags.ROPE_ANCHOR_BLOCKS) || state.isFaceSturdy(level, pos, Direction.DOWN);
    }

    /** A fence (by tag) can support an outward, side-tied rope. */
    boolean isValidFenceAnchor(BlockState state) {
        return state.is(ModTags.ROPE_FENCE_ANCHORS);
    }

    private boolean isSupportedFrom(LevelReader level, BlockPos abovePos) {
        BlockState above = level.getBlockState(abovePos);
        return above.is(this) || isValidNormalAnchor(above, level, abovePos);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!PlantFiberConfig.COMMON.ropeBreaksWhenUnsupported.get()) {
            return true;
        }
        if (state.getValue(FENCE_ANCHORED)) {
            // The whole fence-hung column is FENCE_ANCHORED so it hugs the fence side uniformly.
            // The top segment is held by the fence beside it; the segments below hang from the rope
            // above them, exactly like a normal column.
            BlockPos fencePos = pos.relative(state.getValue(HORIZONTAL_FACING));
            return isValidFenceAnchor(level.getBlockState(fencePos)) || isSupportedFrom(level, pos.above());
        }
        return isSupportedFrom(level, pos.above());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        // A rope draws support from a single neighbour: the fence to its HORIZONTAL_FACING side when
        // fence-anchored, otherwise the block directly above. Schedule a check when that neighbour
        // changes rather than destroying synchronously inside a neighbour-update sweep.
        if (PlantFiberConfig.COMMON.ropeBreaksWhenUnsupported.get()) {
            // A fence-hung segment can draw support from the fence to its side OR the rope above, so
            // re-check when either neighbour changes; an ordinary rope only cares about the block above.
            boolean watch = state.getValue(FENCE_ANCHORED)
                    ? (direction == state.getValue(HORIZONTAL_FACING) || direction == Direction.UP)
                    : direction == Direction.UP;
            if (watch) {
                level.scheduleTick(pos, this, 1);
            }
        }
        return state;
    }

    @Override
    public void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canSurvive(state, level, pos)) {
            // Removing this rope fires neighbour updates to the rope below, propagating the break
            // strictly downward one block per tick.
            level.destroyBlock(pos, PlantFiberConfig.COMMON.unsupportedRopeDropsItems.get());
        }
    }

    // --- Replaceability ---

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Placement is fully driven through RopeBlockItem -> tryPlaceFromContext; this remains as a
        // sane fallback for any vanilla placement path and respects the enable flag.
        if (!PlantFiberConfig.COMMON.enableRope.get()) {
            return null;
        }
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        boolean waterlogged = PlantFiberConfig.COMMON.ropeCanBeWaterlogged.get()
                && fluid.getType() == Fluids.WATER;
        return this.defaultBlockState().setValue(WATERLOGGED, waterlogged);
    }

    @Override
    public boolean canPlaceLiquid(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return PlantFiberConfig.COMMON.ropeCanBeWaterlogged.get()
                && SimpleWaterloggedBlock.super.canPlaceLiquid(level, pos, state, fluid);
    }

    boolean canReplaceWithRope(LevelReader level, BlockPos pos) {
        return isRopeReplaceable(level.getBlockState(pos));
    }

    private boolean isRopeReplaceable(BlockState state) {
        if (state.is(this)) {
            return false;
        }
        if (state.is(ModTags.ROPE_REPLACEABLE_BLOCKS)) {
            return true;
        }
        if (PlantFiberConfig.COMMON.ropeCanReplacePlants.get() && state.is(ModTags.VEGETATION)) {
            return true;
        }
        return state.canBeReplaced();
    }

    // --- Placement orchestration ---

    /**
     * Entry point for placing/extending rope with the rope item, delegated from
     * {@code RopeBlockItem.useOn} and therefore already inside Forge's {@code ItemStack.useOn}
     * placement transaction. The knot route reaches the same code by invoking the held stack's
     * {@code useOn} with a fence hit, so both routes share one resolver, one authorization pass and
     * one Forge wrapper.
     */
    public InteractionResult tryPlaceFromContext(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        RopePlacement.Resolved resolved =
                RopePlacement.resolve(this, level, context.getClickedPos(), context.getClickedFace());
        if (resolved == null) {
            // Not a rope placement here: PASS so an off-hand item still gets its turn.
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ItemStack held = context.getItemInHand();
        if (!RopePlacement.isAuthorized(level, player, held, resolved)) {
            return InteractionResult.FAIL;
        }
        return switch (resolved.kind()) {
            case KNOT -> placeKnot(level, player, held, resolved);
            case SEGMENT -> placeSegment(level, player, held, resolved);
        };
    }

    /**
     * Side-clicking a fence creates a {@link RopeKnotEntity} on the post, consuming one rope. A knot
     * has no block snapshot for Forge to roll back, so insertion must succeed (it is cancellable
     * through the entity-join event) before anything is charged or sounded. A refused insertion leaves
     * the world untouched — in particular it never runs the knot's rope-breaking removal path.
     */
    private InteractionResult placeKnot(Level level, Player player, ItemStack held, RopePlacement.Resolved resolved) {
        BlockPos fencePos = resolved.target();
        if (!isValidFenceAnchor(level.getBlockState(fencePos)) || findKnot(level, fencePos) != null) {
            return InteractionResult.FAIL;
        }
        RopeKnotEntity created = new RopeKnotEntity(level, fencePos, resolved.supportFace());
        if (!level.addFreshEntity(created)) {
            return InteractionResult.FAIL;
        }
        created.playPlacementSound();
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    /**
     * Place one rope block. The target is revalidated immediately before mutation, failure of the
     * mutation itself is propagated, and the stack is only charged once the block actually changed —
     * Forge owns the rollback if a placement event later cancels this transaction.
     */
    private InteractionResult placeSegment(Level level, Player player, ItemStack held, RopePlacement.Resolved resolved) {
        BlockPos target = resolved.target();
        if (!isWithinPlaceableBounds(level, target) || !canReplaceWithRope(level, target)
                || !RopePlacement.isAuthorized(level, player, held, resolved)) {
            return InteractionResult.FAIL;
        }
        FluidState fluid = level.getFluidState(target);
        boolean waterlogged = PlantFiberConfig.COMMON.ropeCanBeWaterlogged.get()
                && fluid.getType() == Fluids.WATER;
        BlockState newState = this.defaultBlockState()
                .setValue(WATERLOGGED, waterlogged)
                .setValue(FENCE_ANCHORED, resolved.fenceAnchored())
                .setValue(HORIZONTAL_FACING, resolved.facing());

        RopePlacement.armPlacementSound(level, target);
        if (!level.setBlock(target, newState, Block.UPDATE_ALL)) {
            RopePlacement.clearPlacementSound();
            return InteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    /**
     * Extend the rope hanging from a knot: the outward top segment if it is missing, otherwise the
     * bottom of the existing column. Routed through the held stack's {@code useOn} so this shares the
     * item route's Forge transaction. Server-side.
     */
    public InteractionResult extendFromKnot(BlockPos fencePos, Direction exitDirection,
                                            Player player, InteractionHand hand) {
        Vec3 hitLocation = Vec3.atCenterOf(fencePos)
                .add(exitDirection.getStepX() * 0.5D, exitDirection.getStepY() * 0.5D, exitDirection.getStepZ() * 0.5D);
        BlockHitResult hit = new BlockHitResult(hitLocation, exitDirection, fencePos, false);
        return player.getItemInHand(hand).useOn(new UseOnContext(player, hand, hit));
    }

    /**
     * Forge defers this callback until a captured placement is accepted, so it is the first point at
     * which the placement is genuinely committed — and therefore where the placement sound belongs.
     */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && RopePlacement.consumePlacementSound(level, pos)) {
            SoundType sound = state.getSoundType(level, pos, null);
            level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS,
                    (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        }
    }

    @Nullable
    static RopeKnotEntity findKnot(Level level, BlockPos fencePos) {
        for (RopeKnotEntity knot : level.getEntitiesOfClass(RopeKnotEntity.class, new AABB(fencePos))) {
            if (knot.getPos().equals(fencePos)) {
                return knot;
            }
        }
        return null;
    }

    boolean isWithinPlaceableBounds(Level level, BlockPos pos) {
        return level.getWorldBorder().isWithinBounds(pos)
                && pos.getY() >= level.getMinBuildHeight()
                && pos.getY() < level.getMaxBuildHeight();
    }

    // --- Fire ---

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 60;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 30;
    }
}
