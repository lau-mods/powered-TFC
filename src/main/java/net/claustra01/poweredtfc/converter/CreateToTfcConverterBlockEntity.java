package net.claustra01.poweredtfc.converter;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.claustra01.poweredtfc.registry.PoweredTFCRegistry;
import java.util.EnumSet;
import net.dries007.tfc.common.blockentities.rotation.RotatingBlockEntity;
import net.dries007.tfc.util.rotation.NetworkAction;
import net.dries007.tfc.util.rotation.Node;
import net.dries007.tfc.util.rotation.Rotation;
import net.dries007.tfc.util.rotation.SourceNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class CreateToTfcConverterBlockEntity extends KineticBlockEntity
    implements RotatingBlockEntity {
    private final SourceNode node;
    private boolean invalidInNetwork;

    public CreateToTfcConverterBlockEntity(BlockPos pos, BlockState state) {
        this(PoweredTFCRegistry.CREATE_TO_TFC_CONVERTER_BLOCK_ENTITY.get(), pos, state);
    }

    public CreateToTfcConverterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        Direction createInput = state.getValue(MechanicalConverterBlock.FACING);
        Direction tfcOutput = createInput.getOpposite();
        node = new SourceNode(pos, EnumSet.of(tfcOutput), createInput, 0) {
            @Override
            public String toString() {
                return "Create-to-TFC converter at " + pos();
            }
        };
    }

    @Override
    public void tick() {
        super.tick();
        boolean powered = getBlockState().getValue(MechanicalConverterBlock.POWERED);
        float previousImpact = lastStressApplied;
        float configuredImpact = calculateStressApplied(powered);
        if (configuredImpact != previousImpact && level != null && !level.isClientSide && hasNetwork()) {
            getOrCreateNetwork().updateStressFor(this, configuredImpact);
            getOrCreateNetwork().updateStress();
            sendData();
        }
        Rotation.Tickable rotation = node.rotation();
        rotation.tick();
        float outputSpeed = powered || hasTfcToCreateSource()
            ? 0 : MechanicalPowerConversion.rpmToRadiansPerTick(getSpeed());
        Direction createInput = getBlockState().getValue(MechanicalConverterBlock.FACING);
        outputSpeed *= -createInput.getAxisDirection().getStep();
        rotation.setSpeed(outputSpeed);
    }

    @Override
    public float calculateStressApplied() {
        return calculateStressApplied(getBlockState().getValue(MechanicalConverterBlock.POWERED));
    }

    private float calculateStressApplied(boolean powered) {
        if (powered) {
            lastStressApplied = 0;
            return 0;
        }
        return super.calculateStressApplied();
    }

    private boolean hasTfcToCreateSource() {
        return level != null && !level.isClientSide && hasNetwork()
            && getOrCreateNetwork().sources.keySet().stream()
                .anyMatch(TfcToCreateConverterBlockEntity.class::isInstance);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        node.rotation().saveToTag(tag);
        tag.putBoolean("InvalidInTfcNetwork", invalidInNetwork);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        node.rotation().loadFromTag(tag);
        invalidInNetwork = tag.getBoolean("InvalidInTfcNetwork");
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) {
            performNetworkAction(NetworkAction.ADD_SOURCE);
        }
    }

    @Override
    public void remove() {
        if (level != null) {
            performNetworkAction(NetworkAction.REMOVE);
        }
        super.remove();
    }

    @Override
    public void onChunkUnloaded() {
        if (level != null) {
            performNetworkAction(NetworkAction.REMOVE);
        }
        super.onChunkUnloaded();
    }

    @Override
    public void markAsInvalidInNetwork() {
        invalidInNetwork = true;
    }

    @Override
    public boolean isInvalidInNetwork() {
        return invalidInNetwork;
    }

    @Override
    public Node getRotationNode() {
        return node;
    }
}
