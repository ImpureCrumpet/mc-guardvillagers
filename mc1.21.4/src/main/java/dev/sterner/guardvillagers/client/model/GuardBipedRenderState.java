package dev.sterner.guardvillagers.client.model;

import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class GuardBipedRenderState extends BipedEntityRenderState {
    public int kickTicks;
    public boolean hasRangedWeapon;
    public int guardVariant;
    public ItemStack mainHandStack = ItemStack.EMPTY;
    public ItemStack offHandStack = ItemStack.EMPTY;
    public boolean isEating;
    public int itemUseTimeLeft;
    public Hand activeHand;

    public ItemStack getStackInHand(Hand hand) {
        return hand == Hand.MAIN_HAND ? this.mainHandStack : this.offHandStack;
    }
}
