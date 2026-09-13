package com.otectus.plantfiber.block;

import com.otectus.plantfiber.config.PlantFiberConfig;
import com.otectus.plantfiber.entity.RopeKnotEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The single place that decides <em>what</em> a rope use would change, shared by the item route
 * ({@link com.otectus.plantfiber.item.RopeBlockItem}) and the knot route
 * ({@link RopeKnotEntity#interact}). Resolution never mutates the world, so the same answer can be
 * authorized, revalidated and then committed by {@link RopeBlock}.
 *
 * <p>It also carries the transient "this position was just placed by a rope use" token that lets the
 * placement sound wait for Forge to actually accept the placement (see
 * {@link RopeBlock#onPlace}). The token lives only between the {@code setBlock} call and the accepted
 * {@code onPlace} callback within one interaction; it is never persisted.
 */
public final class RopePlacement {
    /** What a use would create: the knot entity on a fence post, or one rope block. */
    public enum Kind {
        KNOT,
        SEGMENT
    }

    /**
     * @param target        the block position that would change (the fence post for a knot)
     * @param authorizePos  the position handed to {@link Player#mayUseItemAt} so the adventure-mode
     *                      {@code CanPlaceOn} check sees the actual supporting block
     * @param supportFace   the face of the supporting block the placement leans on
     * @param fenceAnchored the {@link RopeBlock#FENCE_ANCHORED} value the new segment inherits
     * @param facing        the {@link RopeBlock#HORIZONTAL_FACING} value the new segment inherits
     */
    public record Resolved(Kind kind, BlockPos target, BlockPos authorizePos, Direction supportFace,
                           boolean fenceAnchored, Direction facing) {
    }

    @Nullable
    private static Level pendingSoundLevel;
    @Nullable
    private static BlockPos pendingSoundPos;

    private RopePlacement() {
    }

    /**
     * Work out the intended placement for a use against {@code clickedPos}/{@code clickedFace}, or
     * {@code null} when this interaction is not a rope placement at all (the caller then returns
     * {@code PASS} so the off-hand item is still tried). Placement priority and the two-click fence
     * behaviour are unchanged: existing column &rarr; fence knot, then fence rope &rarr; underside anchor.
     */
    @Nullable
    public static Resolved resolve(RopeBlock rope, Level level, BlockPos clickedPos, Direction clickedFace) {
        BlockState clickedState = level.getBlockState(clickedPos);

        if (clickedState.is(rope)) {
            return extendColumn(rope, level, clickedPos);
        }

        if (clickedFace.getAxis().isHorizontal()
                && PlantFiberConfig.COMMON.enableFenceRopeAnchors.get()
                && rope.isValidFenceAnchor(clickedState)) {
            RopeKnotEntity knot = RopeBlock.findKnot(level, clickedPos);
            if (knot == null) {
                // First click: the wrap around the post. HORIZONTAL_FACING is unused for the knot itself.
                return new Resolved(Kind.KNOT, clickedPos, clickedPos.relative(clickedFace), clickedFace,
                        true, clickedFace.getOpposite());
            }
            Direction exit = knot.getExitDirection();
            BlockPos outward = clickedPos.relative(exit);
            if (level.getBlockState(outward).is(rope)) {
                return extendColumn(rope, level, outward);
            }
            // HORIZONTAL_FACING points back toward the supporting fence.
            return segment(rope, level, outward, exit, true, exit.getOpposite());
        }

        if (rope.isValidNormalAnchor(clickedState, level, clickedPos)) {
            return segment(rope, level, clickedPos.below(), Direction.DOWN, false, Direction.NORTH);
        }

        return null;
    }

    /** Walk down the connected column and target the cell below its lowest rope. */
    @Nullable
    private static Resolved extendColumn(RopeBlock rope, Level level, BlockPos columnPos) {
        BlockState columnState = level.getBlockState(columnPos);
        boolean fenceAnchored = columnState.getValue(RopeBlock.FENCE_ANCHORED);
        Direction facing = fenceAnchored ? columnState.getValue(RopeBlock.HORIZONTAL_FACING) : Direction.NORTH;
        BlockPos.MutableBlockPos cursor = columnPos.mutable();
        int minY = level.getMinBuildHeight();
        while (cursor.getY() - 1 >= minY && level.getBlockState(cursor.below()).is(rope)) {
            cursor.move(Direction.DOWN);
        }
        return segment(rope, level, cursor.below(), Direction.DOWN, fenceAnchored, facing);
    }

    @Nullable
    private static Resolved segment(RopeBlock rope, Level level, BlockPos target, Direction supportFace,
                                    boolean fenceAnchored, Direction facing) {
        if (!rope.isWithinPlaceableBounds(level, target) || !rope.canReplaceWithRope(level, target)) {
            return null;
        }
        return new Resolved(Kind.SEGMENT, target, target, supportFace, fenceAnchored, facing);
    }

    /**
     * Whether this player may actually change the resolved target. Checked once after resolution and
     * again immediately before mutation, because the authorization answer can change in between.
     */
    public static boolean isAuthorized(Level level, Player player, ItemStack stack, Resolved resolved) {
        BlockPos target = resolved.target();
        if (player.isSpectator()) {
            return false;
        }
        if (!level.isInWorldBounds(target) || !level.isLoaded(target)
                || !level.getWorldBorder().isWithinBounds(target)) {
            return false;
        }
        if (level instanceof ServerLevel serverLevel && !serverLevel.mayInteract(player, target)) {
            return false;
        }
        // Build/adventure semantics against the real supporting face, not the clicked one.
        return player.mayUseItemAt(resolved.authorizePos(), resolved.supportFace(), stack);
    }

    /**
     * Mark a position as awaiting its placement sound. Any earlier token is dropped: a placement whose
     * event was cancelled never reaches {@code onPlace}, so its token must not outlive the next use.
     */
    static void armPlacementSound(Level level, BlockPos pos) {
        pendingSoundLevel = level;
        pendingSoundPos = pos.immutable();
    }

    static void clearPlacementSound() {
        pendingSoundLevel = null;
        pendingSoundPos = null;
    }

    /** Consume the token for {@code pos}, if this is the placement it was armed for. */
    static boolean consumePlacementSound(Level level, BlockPos pos) {
        if (pendingSoundLevel != level || !pos.equals(pendingSoundPos)) {
            return false;
        }
        clearPlacementSound();
        return true;
    }
}
