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

        // Explode in non-overworld dimensions
        if (!level.dimensionType().bedWorks()) {
            level.removeBlock(pos, false);
            BlockPos otherPos = getOtherPartPos(state, pos);
            if (level.getBlockState(otherPos).is(this)) {
                level.removeBlock(otherPos, false);
            }
            Vec3 center = pos.getCenter();
            level.explode(null, level.damageSources().badRespawnPointExplosion(center), null,
                    center, 5.0f, true, Level.ExplosionInteraction.BLOCK);
            return InteractionResult.SUCCESS;
        }

        // Find the head position for sleeping
        BlockPos headPos = state.getValue(PART) == BedPart.HEAD ? pos : getOtherPartPos(state, pos);
        BlockState headState = level.getBlockState(headPos);

        if (!headState.is(this) || headState.getValue(PART) != BedPart.HEAD) {
            return InteractionResult.CONSUME;
        }

        if (headState.getValue(OCCUPIED)) {
            player.displayClientMessage(Component.translatable("block.minecraft.bed.occupied"), true);
            return InteractionResult.SUCCESS;
        }

        Either<Player.BedSleepingProblem, net.minecraft.util.Unit> result = player.startSleepInBed(headPos);
        result.ifLeft(problem -> {
            Component message = problem.getMessage();
            if (message != null) {
                player.displayClientMessage(message, true);
            }
        });

        result.ifRight(unit -> {
            // Mark as occupied
            BlockState currentHeadState = level.getBlockState(headPos);
            if (currentHeadState.is(this)) {
                level.setBlock(headPos, currentHeadState.setValue(OCCUPIED, true), 3);
            }
            BlockPos bedFootPos = getOtherPartPos(currentHeadState, headPos);
            BlockState currentFootState = level.getBlockState(bedFootPos);
            if (currentFootState.is(this)) {
                level.setBlock(bedFootPos, currentFootState.setValue(OCCUPIED, true), 3);
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

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockPos otherPos = getOtherPartPos(state, pos);
            BlockState otherState = level.getBlockState(otherPos);
            if (otherState.is(this) && otherState.getValue(PART) != state.getValue(PART)) {
                level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), 35);
                level.levelEvent(player, 2001, otherPos, Block.getId(otherState));
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
        level.setBlock(pos, state.setValue(OCCUPIED, occupied), 3);
    }

    @Override
    public Optional<Vec3> getRespawnPosition(BlockState state, EntityType<?> type, LevelReader level, BlockPos pos,
                                              float orientation, @Nullable LivingEntity entity) {
        // Find the foot position
        Direction facing = state.getValue(FACING);
        BlockPos footPos = state.getValue(PART) == BedPart.FOOT ? pos : pos.relative(facing.getOpposite());
        BlockPos headPos = state.getValue(PART) == BedPart.HEAD ? pos : pos.relative(facing);

        // Check positions around the bed for a safe spawn
        // Priority: sides of foot, sides of head, then around the perimeter
        Direction left = facing.getCounterClockWise();
        Direction right = facing.getClockWise();

        BlockPos[] candidates = {
                footPos.relative(left), footPos.relative(right),
                headPos.relative(left), headPos.relative(right),
                footPos.relative(facing.getOpposite()),
                headPos.relative(facing),
                footPos.relative(left).relative(facing.getOpposite()),
                footPos.relative(right).relative(facing.getOpposite()),
                headPos.relative(left).relative(facing),
                headPos.relative(right).relative(facing)
        };

        for (BlockPos candidate : candidates) {
            Optional<Vec3> respawn = findSafeRespawn(type, level, candidate);
            if (respawn.isPresent()) {
                return respawn;
            }
        }

        return Optional.empty();
    }

    private static Optional<Vec3> findSafeRespawn(EntityType<?> type, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (!below.isFaceSturdy(level, pos.below(), Direction.UP)) {
            return Optional.empty();
        }

        VoxelShape shape1 = level.getBlockState(pos).getCollisionShape(level, pos);
        VoxelShape shape2 = level.getBlockState(pos.above()).getCollisionShape(level, pos.above());

        if (shape1.isEmpty() && shape2.isEmpty()) {
            return Optional.of(Vec3.upFromBottomCenterOf(pos, 0.0));
        }

        return Optional.empty();
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

    private static BlockPos getOtherPartPos(BlockState state, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        return state.getValue(PART) == BedPart.FOOT ? pos.relative(facing) : pos.relative(facing.getOpposite());
    }
}
