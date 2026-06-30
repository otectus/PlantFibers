package com.otectus.plantfiber.entity;

import com.otectus.plantfiber.block.RopeBlock;
import com.otectus.plantfiber.config.PlantFiberConfig;
import com.otectus.plantfiber.registry.ModBlocks;
import com.otectus.plantfiber.registry.ModEntities;
import com.otectus.plantfiber.registry.ModItems;
import com.otectus.plantfiber.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * A small rope knot tied around a fence post. It is anchored at the fence's block position (the
 * fence block stays untouched, so any modded fence works) and renders the rope band around the
 * central post plus a side-exit strand toward {@link #getExitDirection()} — the direction in which
 * the hanging rope column begins. Modelled on the vanilla leash knot.
 *
 * <p>The hanging rope itself is still a normal {@link RopeBlock} column starting one cell outward;
 * this entity is the visual + the anchor's lifecycle owner. It survives only while a valid fence is
 * present, and when removed it breaks the rope column that depends on it.
 */
public class RopeKnotEntity extends HangingEntity {
    public static final double OFFSET_Y = 0.375D;

    private static final EntityDataAccessor<Direction> EXIT_DIRECTION =
            SynchedEntityData.defineId(RopeKnotEntity.class, EntityDataSerializers.DIRECTION);

    public RopeKnotEntity(EntityType<? extends RopeKnotEntity> type, Level level) {
        super(type, level);
    }

    public RopeKnotEntity(Level level, BlockPos pos, Direction exitDirection) {
        super(ModEntities.ROPE_KNOT.get(), level, pos);
        this.setExitDirection(exitDirection);
        this.recalculateBoundingBox();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(EXIT_DIRECTION, Direction.NORTH);
    }

    public Direction getExitDirection() {
        return this.entityData.get(EXIT_DIRECTION);
    }

    public void setExitDirection(Direction direction) {
        this.entityData.set(EXIT_DIRECTION, direction);
    }

    /** Centre the knot on the fence post regardless of the (painting-style) facing the base uses. */
    @Override
    protected void recalculateBoundingBox() {
        if (this.pos == null) {
            return;
        }
        double x = this.pos.getX() + 0.5D;
        double y = this.pos.getY() + 0.5D;
        double z = this.pos.getZ() + 0.5D;
        this.setPosRaw(x, y, z);
        double w = 0.25D;
        double h = 0.25D;
        this.setBoundingBox(new AABB(x - w, y - h, z - w, x + w, y + h, z + w));
    }

    @Override
    public boolean survives() {
        return this.level().getBlockState(this.pos).is(ModTags.ROPE_FENCE_ANCHORS);
    }

    /**
     * Check support every tick (cheap block lookup) so removing the fence clears the knot promptly,
     * rather than the vanilla 100-tick cadence. Static decoration: no base physics tick needed.
     */
    @Override
    public void tick() {
        if (this.level().isClientSide) {
            return;
        }
        if (!this.survives()) {
            if (PlantFiberConfig.COMMON.unsupportedRopeDropsItems.get()) {
                this.dropItem((Entity) null);
            }
            this.discard();
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.ROPE_ITEM.get())
                && PlantFiberConfig.COMMON.enableRope.get()
                && PlantFiberConfig.COMMON.enableFenceRopeAnchors.get()
                && ModBlocks.ROPE.get() instanceof RopeBlock rope) {
            if (this.level().isClientSide) {
                return InteractionResult.SUCCESS;
            }
            return rope.extendFromKnot(this.level(), this.pos, this.getExitDirection(), player, held);
        }
        return InteractionResult.PASS;
    }

    /** When the knot is genuinely removed (broken / fence gone), break the rope column it anchors. */
    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && reason.shouldDestroy()) {
            breakDependentRope();
        }
        super.remove(reason);
    }

    private void breakDependentRope() {
        BlockPos ropePos = this.pos.relative(this.getExitDirection());
        BlockState state = this.level().getBlockState(ropePos);
        if (state.getBlock() instanceof RopeBlock
                && state.getValue(RopeBlock.FENCE_ANCHORED)
                && state.getValue(RopeBlock.HORIZONTAL_FACING) == this.getExitDirection().getOpposite()) {
            // destroyBlock fires neighbour updates, cascading the unsupported column downward.
            this.level().destroyBlock(ropePos, PlantFiberConfig.COMMON.unsupportedRopeDropsItems.get());
        }
    }

    @Override
    public void dropItem(@Nullable Entity breaker) {
        if (!this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            return;
        }
        if (breaker instanceof Player player && player.getAbilities().instabuild) {
            return;
        }
        this.spawnAtLocation(new ItemStack(ModItems.ROPE_ITEM.get()), 0.0F);
    }

    @Override
    public void playPlacementSound() {
        SoundType sound = SoundType.VINE;
        this.playSound(sound.getPlaceSound(), (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
    }

    @Override
    public int getWidth() {
        return 9;
    }

    @Override
    public int getHeight() {
        return 9;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("ExitDir", (byte) this.getExitDirection().get3DDataValue());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setExitDirection(Direction.from3DDataValue(tag.getByte("ExitDir")));
        this.recalculateBoundingBox();
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this, 0, this.getPos());
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.ROPE_ITEM.get());
    }
}
