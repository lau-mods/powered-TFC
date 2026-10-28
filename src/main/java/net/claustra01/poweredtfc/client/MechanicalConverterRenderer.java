package net.claustra01.poweredtfc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.claustra01.poweredtfc.converter.MechanicalConverterBlock;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

public final class MechanicalConverterRenderer<T extends KineticBlockEntity>
    extends KineticBlockEntityRenderer<T> {
    public MechanicalConverterRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(T blockEntity, float partialTicks, PoseStack poseStack,
        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Direction createSide = blockEntity.getBlockState().getValue(MechanicalConverterBlock.FACING);
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutoutMipped());
        SuperByteBuffer shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF,
            blockEntity.getBlockState(), createSide);
        int light = blockEntity.getLevel() == null ? packedLight
            : LevelRenderer.getLightColor(blockEntity.getLevel(), blockEntity.getBlockPos().relative(createSide));
        standardKineticRotationTransform(shaft, blockEntity, light).renderInto(poseStack, buffer);
    }
}
