package dev.sterner.guardvillagers.client.model;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

public class GuardSteveModel extends BipedEntityModel<GuardBipedRenderState> {
    public GuardSteveModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setAngles(GuardBipedRenderState state) {
        super.setAngles(state);
        if (state.kickTicks > 0) {
            float f1 = 1.0F - (float) MathHelper.abs(10 - 2 * state.kickTicks) / 10.0F;
            this.rightLeg.pitch = MathHelper.lerp(f1, this.rightLeg.pitch, -1.40F);
        }
        float ageInTicks = state.age;
        if (state.preferredArm == Arm.RIGHT) {
            this.eatingAnimationRightHand(Hand.MAIN_HAND, state, ageInTicks);
            this.eatingAnimationLeftHand(Hand.OFF_HAND, state, ageInTicks);
        } else {
            this.eatingAnimationRightHand(Hand.OFF_HAND, state, ageInTicks);
            this.eatingAnimationLeftHand(Hand.MAIN_HAND, state, ageInTicks);
        }
    }

    public static TexturedModelData createMesh() {
        ModelData meshdefinition = BipedEntityModel.getModelData(Dilation.NONE, 0.0F);
        return TexturedModelData.of(meshdefinition, 64, 64);
    }

    public void eatingAnimationRightHand(Hand hand, GuardBipedRenderState state, float ageInTicks) {
        ItemStack itemstack = state.getStackInHand(hand);
        boolean drinkingOrEating = itemstack.getUseAction() == UseAction.EAT || itemstack.getUseAction() == UseAction.DRINK;
        if (state.isEating && drinkingOrEating
                || state.itemUseTimeLeft > 0 && drinkingOrEating && state.activeHand == hand) {
            this.rightArm.yaw = -0.5F;
            this.rightArm.pitch = -1.3F;
            this.rightArm.roll = MathHelper.cos(ageInTicks) * 0.1F;
            this.head.pitch = MathHelper.cos(ageInTicks) * 0.2F;
            this.head.yaw = 0.0F;
            this.hat.copyTransform(head);
        }
    }

    public void eatingAnimationLeftHand(Hand hand, GuardBipedRenderState state, float ageInTicks) {
        ItemStack itemstack = state.getStackInHand(hand);
        boolean drinkingOrEating = itemstack.getUseAction() == UseAction.EAT || itemstack.getUseAction() == UseAction.DRINK;
        if (state.isEating && drinkingOrEating
                || state.itemUseTimeLeft > 0 && drinkingOrEating && state.activeHand == hand) {
            this.leftArm.yaw = 0.5F;
            this.leftArm.pitch = -1.3F;
            this.leftArm.roll = MathHelper.cos(ageInTicks) * 0.1F;
            this.head.pitch = MathHelper.cos(ageInTicks) * 0.2F;
            this.head.yaw = 0.0F;
            this.hat.copyTransform(head);
        }
    }
}
