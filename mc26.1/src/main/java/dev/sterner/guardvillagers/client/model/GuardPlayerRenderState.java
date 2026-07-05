package dev.sterner.guardvillagers.client.model;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class GuardPlayerRenderState extends AvatarRenderState {
    public int kickTicks;
    public ItemStack mainHandStack;
    public ItemStack offHandStack;
    public boolean isEating;
    public int itemUseTimeLeft;
    public InteractionHand useItemHand;

    public GuardPlayerRenderState() {
        this.mainHandStack = ItemStack.EMPTY;
        this.offHandStack = ItemStack.EMPTY;
    }

    public ItemStack getStackInHand(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? this.mainHandStack : this.offHandStack;
    }
}
