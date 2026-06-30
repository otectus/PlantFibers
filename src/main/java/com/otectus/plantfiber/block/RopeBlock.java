package com.otectus.plantfiber.block;

import com.otectus.plantfiber.config.PlantFiberConfig;
import com.otectus.plantfiber.entity.RopeKnotEntity;
import com.otectus.plantfiber.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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
    private boolean isValidNormalAnchor(BlockState state, LevelReader level, BlockPos pos) {
        return state.is(ModTags.ROPE_ANCHOR_BLOCKS) || state.isFaceSturdy(level, pos, Direction.DOWN);
    }

    /** A fence (by tag) can support an outward, side-tied rope. */
    private boolean isValidFenceAnchor(BlockState state) {
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

    private boolean canReplaceWithRope(LevelReader level, BlockPos pos) {
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
     * Entry point for placing/extending rope with the rope item. Delegated from
     * {@code RopeBlockItem.useOn}. Runs the placement priority chain:
     * existing rope &rarr; fence anchor &rarr; underside anchor &rarr; fail.
     */
    public InteractionResult tryPlaceFromContext(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        ItemStack held = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        BlockState clickedState = level.getBlockState(clickedPos);

        // 1. Extend an existing rope column downward.
        if (clickedState.is(this)) {
            return tryExtendExistingRope(level, clickedPos, player, held);
        }
        // 2. Tie to a fence: create/realize a rope-knot entity on the post (the wrap), then the
        //    hanging column extends outward then down from it.
        if (clickedFace.getAxis().isHorizontal()
                && PlantFiberConfig.COMMON.enableFenceRopeAnchors.get()
                && isValidFenceAnchor(clickedState)) {
            return tryAttachKnot(level, clickedPos, clickedFace, player, held);
        }
        // 3. Hang below a valid anchor block, whether the bottom or a side face was clicked.
        if (isValidNormalAnchor(clickedState, level, clickedPos)) {
            return tryPlaceUnderBlock(level, clickedPos, player, held);
        }
        // 4. Never side-mount rope on ordinary blocks.
        return InteractionResult.PASS;
    }

    private InteractionResult tryExtendExistingRope(Level level, BlockPos clickedRopePos, Player player, ItemStack held) {
        // Inherit the column's anchoring so a fence-hung column keeps hugging the fence the whole way
        // down (every segment stays FENCE_ANCHORED with the same facing); a normal column stays centered.
        BlockState columnState = level.getBlockState(clickedRopePos);
        boolean fenceAnchored = columnState.getValue(FENCE_ANCHORED);
        Direction facing = fenceAnchored ? columnState.getValue(HORIZONTAL_FACING) : Direction.NORTH;
        // Walk down through the connected rope column to the lowest rope.
        BlockPos.MutableBlockPos cursor = clickedRopePos.mutable();
        int minY = level.getMinBuildHeight();
        while (cursor.getY() - 1 >= minY && level.getBlockState(cursor.below()).is(this)) {
            cursor.move(Direction.DOWN);
        }
        BlockPos target = cursor.below();
        if (!isWithinPlaceableBounds(level, target) || !canReplaceWithRope(level, target)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        placeRopeBlock(level, target, player, held, fenceAnchored, facing);
        return InteractionResult.CONSUME;
    }

    /**
     * Side-clicking a fence creates a {@link RopeKnotEntity} on the post (if none yet), consuming
     * one rope. A subsequent use (on the knot or the fence) extends the hanging rope outward, then
     * down. The knot is the wrap visual + the anchor's lifecycle owner; the rope column itself is
     * ordinary fence-anchored {@link RopeBlock}s starting one cell outward.
     */
    private InteractionResult tryAttachKnot(Level level, BlockPos fencePos, Direction clickedFace,
                                            Player player, ItemStack held) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        RopeKnotEntity knot = findKnot(level, fencePos);
        if (knot != null) {
            return extendFromKnot(level, fencePos, knot.getExitDirection(), player, held);
        }
        RopeKnotEntity created = new RopeKnotEntity(level, fencePos, clickedFace);
        level.addFreshEntity(created);
        created.playPlacementSound();
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Nullable
    private static RopeKnotEntity findKnot(Level level, BlockPos fencePos) {
        for (RopeKnotEntity knot : level.getEntitiesOfClass(RopeKnotEntity.class, new AABB(fencePos))) {
            if (knot.getPos().equals(fencePos)) {
                return knot;
            }
        }
        return null;
    }

    /**
     * Place the outward fence-anchored top segment if missing, otherwise extend the existing column
     * downward. Called by {@link RopeKnotEntity#interact} and the fence-block path. Server-side.
     */
    public InteractionResult extendFromKnot(Level level, BlockPos fencePos, Direction exitDirection,
                                            Player player, ItemStack held) {
        BlockPos outward = fencePos.relative(exitDirection);
        if (level.getBlockState(outward).is(this)) {
            return tryExtendExistingRope(level, outward, player, held);
        }
        return placeOutwardRope(level, fencePos, exitDirection, player, held);
    }

    private InteractionResult placeOutwardRope(Level level, BlockPos fencePos, Direction exitDirection,
                                               Player player, ItemStack held) {
        BlockPos target = fencePos.relative(exitDirection);
        if (!isWithinPlaceableBounds(level, target) || !canReplaceWithRope(level, target)) {
            return InteractionResult.PASS;
        }
        // HORIZONTAL_FACING points back toward the supporting fence.
        placeRopeBlock(level, target, player, held, true, exitDirection.getOpposite());
        return InteractionResult.CONSUME;
    }

    private InteractionResult tryPlaceUnderBlock(Level level, BlockPos clickedPos, Player player, ItemStack held) {
        BlockPos target = clickedPos.below();
        if (!isWithinPlaceableBounds(level, target) || !canReplaceWithRope(level, target)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        placeRopeBlock(level, target, player, held, false, Direction.NORTH);
        return InteractionResult.CONSUME;
    }

    private boolean isWithinPlaceableBounds(Level level, BlockPos pos) {
        return level.getWorldBorder().isWithinBounds(pos)
                && pos.getY() >= level.getMinBuildHeight()
                && pos.getY() < level.getMaxBuildHeight();
    }

    private void placeRopeBlock(Level level, BlockPos target, Player player, ItemStack held,
                                boolean fenceAnchored, Direction facing) {
        FluidState fluid = level.getFluidState(target);
        boolean waterlogged = PlantFiberConfig.COMMON.ropeCanBeWaterlogged.get()
                && fluid.getType() == Fluids.WATER;
        BlockState newState = this.defaultBlockState()
                .setValue(WATERLOGGED, waterlogged)
                .setValue(FENCE_ANCHORED, fenceAnchored)
                .setValue(HORIZONTAL_FACING, facing);
        level.setBlock(target, newState, Block.UPDATE_ALL);

        SoundType sound = newState.getSoundType(level, target, player);
        level.playSound(null, target, sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);

        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
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
