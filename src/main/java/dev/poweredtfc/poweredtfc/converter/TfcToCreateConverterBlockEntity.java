package dev.poweredtfc.poweredtfc.converter;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import dev.poweredtfc.poweredtfc.config.PoweredTFCConfig;
import dev.poweredtfc.poweredtfc.registry.PoweredTFCRegistry;
import net.dries007.tfc.common.blockentities.rotation.RotatingBlockEntity;
import net.dries007.tfc.util.rotation.Node;
import net.dries007.tfc.util.rotation.Rotation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class TfcToCreateConverterBlockEntity extends GeneratingKineticBlockEntity {
    private float generatedSpeed;
    private int configuredCapacity = -1;

    public TfcToCreateConverterBlockEntity(BlockPos pos, BlockState state) {
        this(PoweredTFCRegistry.TFC_TO_CREATE_CONVERTER_BLOCK_ENTITY.get(), pos, state);
    }

    public TfcToCreateConverterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        float nextSpeed = readTfcInputSpeed();
        int nextCapacity = PoweredTFCConfig.TFC_TO_CREATE_STRESS_CAPACITY.get();
        if (Float.compare(nextSpeed, generatedSpeed) != 0 || nextCapacity != configuredCapacity) {
            generatedSpeed = nextSpeed;
            configuredCapacity = nextCapacity;
            updateGeneratedRotation();
            setChanged();
        }
    }

    private float readTfcInputSpeed() {
        if (getBlockState().getValue(MechanicalConverterBlock.POWERED)) {
            return 0;
        }
        Direction createOutput = getBlockState().getValue(MechanicalConverterBlock.FACING);
        Direction tfcInput = createOutput.getOpposite();
        BlockEntity input = level.getBlockEntity(worldPosition.relative(tfcInput));
        if (!(input instanceof RotatingBlockEntity rotating) || input instanceof CreateToTfcConverterBlockEntity) {
            return 0;
        }
        Node node = rotating.getRotationNode();
        Direction fromInputToConverter = createOutput;
        if (!node.connections().contains(fromInputToConverter)) {
            return 0;
        }
        Rotation rotation = node.rotation(fromInputToConverter);
        if (rotation == null) {
            return 0;
        }
        return -MechanicalPowerConversion.radiansPerTickToRpm(rotation.speed())
            * rotation.direction().getAxisDirection().getStep();
    }

    @Override
    public float getGeneratedSpeed() {
        return generatedSpeed;
    }

    @Override
    public float calculateAddedStressCapacity() {
        float capacity = generatedSpeed == 0 ? 0 : PoweredTFCConfig.TFC_TO_CREATE_STRESS_CAPACITY.get();
        lastCapacityProvided = capacity;
        return capacity;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putFloat("GeneratedSpeed", generatedSpeed);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        generatedSpeed = tag.getFloat("GeneratedSpeed");
    }
}
