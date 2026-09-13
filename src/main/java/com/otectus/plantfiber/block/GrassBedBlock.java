package com.otectus.plantfiber.block;

import com.otectus.plantfiber.config.PlantFiberConfig;
import com.mojang.datafixers.util.Either;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GrassBedBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;
    public static final BooleanProperty OCCUPIED = BlockStateProperties.OCCUPIED;

    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 6.0, 16.0);

    public GrassBedBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, BedPart.FOOT)
                .setValue(OCCUPIED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, OCCUPIED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // --- Bed interaction ---

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.CONSUME;
        }

        if (!PlantFiberConfig.COMMON.enableGrassBed.get()) {
            return InteractionResult.FAIL;
        }

        BedPair pair = resolvePair(level, state, pos);

        // Explode in non-overworld dimensions
        if (!level.dimensionType().bedWorks()) {
            level.removeBlock(pos, false);
            if (pair != null) {
                BlockPos otherPos = pos.equals(pair.headPos()) ? pair.footPos() : pair.headPos();
                level.removeBlock(otherPos, false);
            }
            Vec3 center = pos.getCenter();
            level.explode(null, level.damageSources().badRespawnPointExplosion(center), null,
                    center, 5.0f, true, Level.ExplosionInteraction.BLOCK);
            return InteractionResult.SUCCESS;
        }

        if (pair == null) {
            return InteractionResult.CONSUME;
        }

        // The head owns occupancy; fix a foot that drifted out of step before reading it.
        syncOccupancyFromHead(level, pair);

        if (pair.headState().getValue(OCCUPIED)) {
            player.displayClientMessage(Component.translatable("block.minecraft.bed.occupied"), true);
            return InteractionResult.SUCCESS;
        }

        Either<Player.BedSleepingProblem, net.minecraft.util.Unit> result = player.startSleepInBed(pair.headPos());
        result.ifLeft(problem -> {
            Component message = problem.getMessage();
            if (message != null) {
                player.displayClientMessage(message, true);
            }
        });

        result.ifRight(unit -> {
            // Re-resolve: starting sleep can have changed the world underneath us.
            BedPair current = resolvePair(level, level.getBlockState(pair.headPos()), pair.headPos());
            if (current != null) {
                setPairOccupied(level, current, true);
            }
        });

        return InteractionResult.SUCCESS;
    }

    // --- Two-part placement ---

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        BlockPos headPos = context.getClickedPos().relative(facing);
        Level level = context.getLevel();

        if (!level.getBlockState(headPos).canBeReplaced(context) || !level.getWorldBorder().isWithinBounds(headPos)) {
            return null;
        }

        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(PART, BedPart.FOOT);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            Direction facing = state.getValue(FACING);
            BlockPos headPos = pos.relative(facing);
            level.setBlock(headPos, state.setValue(PART, BedPart.HEAD), 3);
            level.blockUpdated(pos, Blocks.AIR);
            state.updateNeighbourShapes(level, pos, 3);
        }
    }

    // --- Breaking logic ---

    /**
     * Vanilla destruction ordering: only a creative player breaking the <em>foot</em> silently removes
     * the head, because the head is this bed's single loot owner and creative must not drop anything.
     * Every survival case is left to {@link #updateShape}, so the counterpart is destroyed through the
     * normal neighbour update and resolves its loot table exactly once.
     */
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && state.getValue(PART) == BedPart.FOOT) {
            BedPair pair = resolvePair(level, state, pos);
            if (pair != null) {
                level.setBlock(pair.headPos(), Blocks.AIR.defaultBlockState(), 35);
                level.levelEvent(player, 2001, pair.headPos(), Block.getId(pair.headState()));
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction facing = state.getValue(FACING);
        Direction toOther = state.getValue(PART) == BedPart.FOOT ? facing : facing.getOpposite();

        if (direction == toOther) {
            if (!neighborState.is(this) || neighborState.getValue(PART) == state.getValue(PART)) {
                return Blocks.AIR.defaultBlockState();
            }
        }

        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    // --- Block entity ---

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GrassBedBlockEntity(pos, state);
    }

    // --- IForgeBlock bed overrides ---

    @Override
    public boolean isBed(BlockState state, BlockGetter level, BlockPos pos, @Nullable Entity player) {
        return true;
    }

    @Override
    public Direction getBedDirection(BlockState state, LevelReader level, BlockPos pos) {
        return state.getValue(FACING);
    }

    @Override
    public void setBedOccupied(BlockState state, Level level, BlockPos pos, LivingEntity sleeper, boolean occupied) {
        BedPair pair = resolvePair(level, state, pos);
        if (pair == null) {
            // Half a bed: still keep this block's own state honest.
            setOccupiedIfChanged(level, pos, state, occupied);
            return;
        }
        setPairOccupied(level, pair, occupied);
    }

    /**
     * Delegated to {@link GrassBedRespawn}, which uses vanilla's stand-up candidate ordering with an
     * added hazard veto. Deliberately not gated on {@code enableGrassBed}: disabling placement/use must
     * not strand a player who already set their spawn at a valid bed.
     */
    @Override
    public Optional<Vec3> getRespawnPosition(BlockState state, EntityType<?> type, LevelReader level, BlockPos pos,
                                              float orientation, @Nullable LivingEntity entity) {
        return GrassBedRespawn.findRespawnPosition(this, state, type, level, pos, orientation, entity);
    }

    // --- Pathfinding ---

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return false;
    }

    // --- Seed for model randomization ---

    @Override
    public long getSeed(BlockState state, BlockPos pos) {
        BlockPos footPos = state.getValue(PART) == BedPart.FOOT ? pos : pos.relative(state.getValue(FACING).getOpposite());
        return Mth.getSeed(footPos.getX(), pos.getY(), footPos.getZ());
    }

    // --- Helpers ---

    /** A validated head/foot pair: same block, opposite parts, matching facing, reciprocal positions. */
    public record BedPair(BlockPos headPos, BlockState headState, BlockPos footPos, BlockState footState,
                          Direction facing) {
    }

    /**
     * Resolve the bed {@code state}/{@code pos} belongs to, or {@code null} when the two halves do not
     * form a valid pair. Bounded to the two cells involved: it never scans, recurses, or adopts an
     * unrelated bed block that merely happens to be adjacent.
     */
    @Nullable
    public BedPair resolvePair(BlockGetter level, BlockState state, BlockPos pos) {
        if (!state.is(this)) {
            return null;
        }
        Direction facing = state.getValue(FACING);
        BedPart part = state.getValue(PART);
        BlockPos otherPos = getOtherPartPos(state, pos);
        BlockState otherState = level.getBlockState(otherPos);
        if (!otherState.is(this)
                || otherState.getValue(PART) == part
                || otherState.getValue(FACING) != facing
                || !getOtherPartPos(otherState, otherPos).equals(pos)) {
            return null;
        }
        return part == BedPart.HEAD
                ? new BedPair(pos, state, otherPos, otherState, facing)
                : new BedPair(otherPos, otherState, pos, state, facing);
    }

    /** Write {@code occupied} to both halves, skipping any half that already matches. */
    private void setPairOccupied(Level level, BedPair pair, boolean occupied) {
        setOccupiedIfChanged(level, pair.headPos(), pair.headState(), occupied);
        setOccupiedIfChanged(level, pair.footPos(), pair.footState(), occupied);
    }

    /** Bring a foot that drifted out of step back to the head's occupancy. */
    private void syncOccupancyFromHead(Level level, BedPair pair) {
        setOccupiedIfChanged(level, pair.footPos(), pair.footState(), pair.headState().getValue(OCCUPIED));
    }

    private static void setOccupiedIfChanged(Level level, BlockPos pos, BlockState state, boolean occupied) {
        if (state.getValue(OCCUPIED) != occupied) {
            level.setBlock(pos, state.setValue(OCCUPIED, occupied), 3);
        }
    }

    static BlockPos getOtherPartPos(BlockState state, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        return state.getValue(PART) == BedPart.FOOT ? pos.relative(facing) : pos.relative(facing.getOpposite());
    }
}
