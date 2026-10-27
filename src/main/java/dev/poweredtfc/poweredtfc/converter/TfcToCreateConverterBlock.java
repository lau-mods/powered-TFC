package dev.poweredtfc.poweredtfc.converter;

import com.simibubi.create.foundation.block.IBE;
import dev.poweredtfc.poweredtfc.registry.PoweredTFCRegistry;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class TfcToCreateConverterBlock extends MechanicalConverterBlock
    implements IBE<TfcToCreateConverterBlockEntity> {
    public TfcToCreateConverterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<TfcToCreateConverterBlockEntity> getBlockEntityClass() {
        return TfcToCreateConverterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TfcToCreateConverterBlockEntity> getBlockEntityType() {
        return PoweredTFCRegistry.TFC_TO_CREATE_CONVERTER_BLOCK_ENTITY.get();
    }
}
