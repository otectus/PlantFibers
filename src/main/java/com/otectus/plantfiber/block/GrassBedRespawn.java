package com.otectus.plantfiber.block;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Respawn placement for the Grass Bed.
 *
 * <p>Candidate ordering is vanilla's: {@link BedBlock#findStandUpPosition} is asked for a stand-up spot
 * at the validated head. Vanilla's second pass deliberately relaxes its danger checks, so the level it
 * searches is wrapped in a read-only view that reports a collision for any fire/lava cell. That keeps
 * hazards out of every candidate instead of merely rejecting the first result, so a safe candidate
 * further down the list is still found. The accepted position is then re-checked against the real
 * world for hazards and the world border.
 */
public final class GrassBedRespawn {
    private GrassBedRespawn() {
    }

    public static Optional<Vec3> findRespawnPosition(GrassBedBlock block, BlockState state, EntityType<?> type,
                                                     LevelReader level, BlockPos pos, float orientation,
                                                     @Nullable LivingEntity entity) {
        // Beds only establish a respawn point where they work at all.
        if (!level.dimensionType().bedWorks()) {
            return Optional.empty();
        }
        GrassBedBlock.BedPair pair = block.resolvePair(level, state, pos);
        if (pair == null) {
            return Optional.empty();
        }
        EntityType<?> respawnType = entity != null ? entity.getType() : type;
        if (respawnType == null) {
            return Optional.empty();
        }

        Optional<Vec3> candidate = BedBlock.findStandUpPosition(respawnType, new HazardVetoView(level),
                pair.headPos(), pair.facing(), orientation);
        if (candidate.isEmpty()) {
            return Optional.empty();
        }

        AABB box = respawnType.getDimensions().makeBoundingBox(candidate.get());
        if (!level.getWorldBorder().isWithinBounds(box) || containsHazard(level, box)) {
            return Optional.empty();
        }
        return candidate;
    }

    /** True when any block the given box occupies is fire or lava. */
    private static boolean containsHazard(BlockGetter level, AABB box) {
        BlockPos min = BlockPos.containing(box.minX + 1.0E-7D, box.minY + 1.0E-7D, box.minZ + 1.0E-7D);
        BlockPos max = BlockPos.containing(box.maxX - 1.0E-7D, box.maxY - 1.0E-7D, box.maxZ - 1.0E-7D);
        for (BlockPos cell : BlockPos.betweenClosed(min, max)) {
            if (isHazard(level, cell)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isHazard(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).is(BlockTags.FIRE) || level.getFluidState(pos).is(FluidTags.LAVA);
    }

    /**
     * A read-only collision view that adds fire/lava as an obstruction. Terrain, floor height, entity
     * collisions and the world border are all delegated untouched, so vanilla's candidate ordering and
     * its floor/ceiling rules are preserved; only the hazard veto is added.
     */
    private static final class HazardVetoView implements CollisionGetter {
        private final LevelReader delegate;

        private HazardVetoView(LevelReader delegate) {
            this.delegate = delegate;
        }

        @Override
        public Iterable<VoxelShape> getBlockCollisions(@Nullable Entity entity, AABB box) {
            if (containsHazard(this.delegate, box)) {
                return List.of(Shapes.block());
            }
            return CollisionGetter.super.getBlockCollisions(entity, box);
        }

        @Override
        public List<VoxelShape> getEntityCollisions(@Nullable Entity entity, AABB box) {
            return entity == null ? Collections.emptyList() : this.delegate.getEntityCollisions(entity, box);
        }

        @Override
        public WorldBorder getWorldBorder() {
            return this.delegate.getWorldBorder();
        }

        @Nullable
        @Override
        public BlockGetter getChunkForCollisions(int chunkX, int chunkZ) {
            return this.delegate.getChunkForCollisions(chunkX, chunkZ);
        }

        @Nullable
        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return this.delegate.getBlockEntity(pos);
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return this.delegate.getBlockState(pos);
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return this.delegate.getFluidState(pos);
        }

        @Override
        public int getHeight() {
            return this.delegate.getHeight();
        }

        @Override
        public int getMinBuildHeight() {
            return this.delegate.getMinBuildHeight();
        }
    }
}
