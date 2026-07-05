package dev.sterner.guardvillagers.client.renderer;

import dev.sterner.guardvillagers.GuardVillagers;
import dev.sterner.guardvillagers.GuardVillagersClient;
import dev.sterner.guardvillagers.GuardVillagersConfig;
import dev.sterner.guardvillagers.client.model.GuardArmorModel;
import dev.sterner.guardvillagers.client.model.GuardBipedRenderState;
import dev.sterner.guardvillagers.client.model.GuardVillagerModel;
import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

public class GuardRenderer extends BipedEntityRenderer<GuardEntity, GuardBipedRenderState, BipedEntityModel<GuardBipedRenderState>> {

    private final BipedEntityModel<GuardBipedRenderState> steve;
    private final BipedEntityModel<GuardBipedRenderState> normal;

    public GuardRenderer(EntityRendererFactory.Context context) {
        super(context, new GuardVillagerModel(context.getPart(GuardVillagersClient.GUARD)), 0.5F);
        this.normal = this.getModel();
        this.steve = new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER));
        if (GuardVillagersConfig.useSteveModel) {
            this.model = this.steve;
        } else {
            this.model = this.normal;
        }
        this.addFeature(new ArmorFeatureRenderer<>(
                this,
                !GuardVillagersConfig.useSteveModel
                        ? new GuardArmorModel(context.getPart(GuardVillagersClient.GUARD_ARMOR_INNER))
                        : new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER_INNER_ARMOR)),
                !GuardVillagersConfig.useSteveModel
                        ? new GuardArmorModel(context.getPart(GuardVillagersClient.GUARD_ARMOR_OUTER))
                        : new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER_OUTER_ARMOR)),
                context.getEquipmentRenderer()));
    }

    @Override
    public GuardBipedRenderState createRenderState() {
        return new GuardBipedRenderState();
    }

    @Override
    public void updateRenderState(GuardEntity entity, GuardBipedRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.guardVariant = entity.getGuardVariant();
        state.kickTicks = entity.getKickTicks();
        state.isEating = entity.isEating();
        state.itemUseTimeLeft = entity.getItemUseTimeLeft();
        state.activeHand = entity.getActiveHand();
        state.mainHandStack = entity.getMainHandStack();
        state.offHandStack = entity.getOffHandStack();
        state.hasRangedWeapon = isRanged(state.mainHandStack) || isRanged(state.offHandStack);

        BipedEntityModel.ArmPose mainPose = getArmPose(entity, state.mainHandStack, state.offHandStack, Hand.MAIN_HAND);
        BipedEntityModel.ArmPose offPose = getArmPose(entity, state.mainHandStack, state.offHandStack, Hand.OFF_HAND);
        if (entity.getMainArm() == Arm.RIGHT) {
            state.rightArmPose = mainPose;
            state.leftArmPose = offPose;
        } else {
            state.rightArmPose = offPose;
            state.leftArmPose = mainPose;
        }
    }

    private static boolean isRanged(ItemStack stack) {
        if (!stack.isEmpty()) {
            Item item = stack.getItem();
            return item instanceof BowItem || item instanceof CrossbowItem;
        }
        return false;
    }

    private BipedEntityModel.ArmPose getArmPose(GuardEntity entity, ItemStack mainHand, ItemStack offHand, Hand hand) {
        BipedEntityModel.ArmPose pose = BipedEntityModel.ArmPose.EMPTY;
        ItemStack held = hand == Hand.MAIN_HAND ? mainHand : offHand;
        if (!held.isEmpty()) {
            pose = BipedEntityModel.ArmPose.ITEM;
            if (entity.getItemUseTimeLeft() > 0) {
                UseAction useAction = held.getUseAction();
                switch (useAction) {
                    case BLOCK -> pose = BipedEntityModel.ArmPose.BLOCK;
                    case BOW -> pose = BipedEntityModel.ArmPose.BOW_AND_ARROW;
                    case SPEAR -> pose = BipedEntityModel.ArmPose.THROW_SPEAR;
                    case CROSSBOW -> {
                        if (hand == entity.getActiveHand()) {
                            pose = BipedEntityModel.ArmPose.CROSSBOW_CHARGE;
                        }
                    }
                    default -> pose = BipedEntityModel.ArmPose.EMPTY;
                }
            } else {
                boolean mainCrossbow = mainHand.getItem() instanceof CrossbowItem;
                boolean offCrossbow = offHand.getItem() instanceof CrossbowItem;
                if (mainCrossbow && entity.isAttacking()) {
                    pose = BipedEntityModel.ArmPose.CROSSBOW_HOLD;
                }
                if (offCrossbow && mainHand.getItem().getUseAction(mainHand) == UseAction.NONE && entity.isAttacking()) {
                    pose = BipedEntityModel.ArmPose.CROSSBOW_HOLD;
                }
            }
        }
        return pose;
    }

    @Override
    protected void scale(GuardBipedRenderState state, MatrixStack matrices) {
        matrices.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    public Identifier getTexture(GuardBipedRenderState state) {
        return !GuardVillagersConfig.useSteveModel
                ? GuardVillagers.id("textures/entity/guard/guard_" + state.guardVariant + ".png")
                : GuardVillagers.id("textures/entity/guard/guard_steve_" + state.guardVariant + ".png");
    }
}
