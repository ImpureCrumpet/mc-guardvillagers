package dev.sterner.guardvillagers.common.screenhandler;

import dev.sterner.guardvillagers.GuardVillagers;
import dev.sterner.guardvillagers.common.entity.GuardEntity;
import dev.sterner.guardvillagers.common.network.GuardData;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class GuardVillagerScreenHandler extends AbstractContainerMenu {

    private final Player player;
    public final GuardEntity guardEntity;
    public final Container guardInventory;
    private static final EquipmentSlot[] EQUIPMENT_SLOT_ORDER = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public GuardVillagerScreenHandler(int syncId, Inventory playerInventory, GuardData buf) {
        this(syncId, playerInventory, playerInventory.player.level().getEntity(buf.guardId()) instanceof GuardEntity guard ? guard : null);
    }

    public GuardVillagerScreenHandler(int syncId, Inventory playerInventory, GuardEntity guardEntity) {
        this(syncId, playerInventory, guardEntity.guardInventory, guardEntity);
    }

    public GuardVillagerScreenHandler(int id, Inventory playerInventory, Container inventory, GuardEntity guardEntity) {
        super(GuardVillagers.GUARD_SCREEN_HANDLER, id);
        this.guardInventory = inventory;
        this.player = playerInventory.player;
        this.guardEntity = guardEntity;
        inventory.startOpen(playerInventory.player);
        this.addSlot(new Slot(guardInventory, 0, 8, 9) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return EQUIPMENT_SLOT_ORDER[0] == guardEntity.getEquipmentSlotForItem(stack) && GuardVillagers.hotvChecker(player, guardEntity);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void setByPlayer(ItemStack stack) {
                super.setByPlayer(stack);
                guardEntity.setItemSlot(EquipmentSlot.HEAD, stack);
            }

            @Override
            public boolean mayPickup(Player playerIn) {
                return GuardVillagers.hotvChecker(playerInventory.player, guardEntity);
            }

            @Override
            public Identifier getNoItemIcon() {
                return InventoryMenu.EMPTY_ARMOR_SLOT_HELMET;
            }
        });
        this.addSlot(new Slot(guardInventory, 1, 8, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return EQUIPMENT_SLOT_ORDER[1] == guardEntity.getEquipmentSlotForItem(stack) && GuardVillagers.hotvChecker(player, guardEntity);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void setByPlayer(ItemStack stack) {
                super.setByPlayer(stack);
                guardEntity.setItemSlot(EquipmentSlot.CHEST, stack);
            }

            @Override
            public boolean mayPickup(Player playerIn) {
                return GuardVillagers.hotvChecker(playerInventory.player, guardEntity);
            }

            @Override
            public Identifier getNoItemIcon() {
                return InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE;
            }
        });
        this.addSlot(new Slot(guardInventory, 2, 8, 44) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return EQUIPMENT_SLOT_ORDER[2] == guardEntity.getEquipmentSlotForItem(stack) && GuardVillagers.hotvChecker(player, guardEntity);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void setByPlayer(ItemStack stack) {
                super.setByPlayer(stack);
                guardEntity.setItemSlot(EquipmentSlot.LEGS, stack);
            }

            @Override
            public boolean mayPickup(Player playerIn) {
                return GuardVillagers.hotvChecker(playerInventory.player, guardEntity);
            }

            @Override
            public Identifier getNoItemIcon() {
                return InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS;
            }
        });
        this.addSlot(new Slot(guardInventory, 3, 8, 62) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return EQUIPMENT_SLOT_ORDER[3] == guardEntity.getEquipmentSlotForItem(stack) && GuardVillagers.hotvChecker(playerInventory.player, guardEntity);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void setByPlayer(ItemStack stack) {
                super.setByPlayer(stack);
                guardEntity.setItemSlot(EquipmentSlot.FEET, stack);
            }

            @Override
            public boolean mayPickup(Player playerIn) {
                return GuardVillagers.hotvChecker(playerInventory.player, guardEntity);
            }

            @Override
            public Identifier getNoItemIcon() {
                return InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS;
            }
        });
        this.addSlot(new Slot(guardInventory, 4, 77, 62) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return GuardVillagers.hotvChecker(playerInventory.player, guardEntity);
            }

            @Override
            public void setByPlayer(ItemStack stack) {
                super.setByPlayer(stack);
                guardEntity.setItemSlot(EquipmentSlot.OFFHAND, stack);
            }

            @Override
            public boolean mayPickup(Player playerIn) {
                return GuardVillagers.hotvChecker(playerInventory.player, guardEntity);
            }

            @Override
            public Identifier getNoItemIcon() {
                return InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD;
            }
        });

        this.addSlot(new Slot(guardInventory, 5, 77, 44) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return GuardVillagers.hotvChecker(playerInventory.player, guardEntity);
            }

            @Override
            public boolean mayPickup(Player playerIn) {
                return GuardVillagers.hotvChecker(playerIn, guardEntity);
            }

            @Override
            public void setByPlayer(ItemStack stack) {
                super.setByPlayer(stack);
                guardEntity.setItemSlot(EquipmentSlot.MAINHAND, stack);
            }
        });
        for (int l = 0; l < 3; ++l) {
            for (int j1 = 0; j1 < 9; ++j1) {
                this.addSlot(new Slot(playerInventory, j1 + (l + 1) * 9, 8 + j1 * 18, 84 + l * 18));
            }
        }

        for (int i1 = 0; i1 < 9; ++i1) {
            this.addSlot(new Slot(playerInventory, i1, 8 + i1 * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            int i = this.guardInventory.getContainerSize();
            if (index < i) {
                if (!this.moveItemStackTo(itemstack1, i, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.getSlot(1).mayPlace(itemstack1) && !this.getSlot(1).hasItem()) {
                if (!this.moveItemStackTo(itemstack1, 1, 2, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.getSlot(0).mayPlace(itemstack1)) {
                if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (i <= 2 || !this.moveItemStackTo(itemstack1, 2, i, false)) {
                int j = i + 27;
                int k = j + 9;
                if (index >= j && index < k) {
                    if (!this.moveItemStackTo(itemstack1, i, j, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= i && index < j) {
                    if (!this.moveItemStackTo(itemstack1, j, k, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(itemstack1, j, j, false)) {
                    return ItemStack.EMPTY;
                }

                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.guardInventory.stillValid(player) && this.guardEntity.isAlive() && this.guardEntity.distanceTo(player) < 8.0F;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.guardInventory.stopOpen(player);
        this.guardEntity.interacting = false;
    }
}
