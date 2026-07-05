package dev.sterner.guardvillagers.client.screen;

import dev.sterner.guardvillagers.GuardVillagers;
import dev.sterner.guardvillagers.GuardVillagersConfig;
import dev.sterner.guardvillagers.common.entity.GuardEntity;
import dev.sterner.guardvillagers.common.network.GuardFollowPacket;
import dev.sterner.guardvillagers.common.network.GuardPatrolPacket;
import dev.sterner.guardvillagers.common.screenhandler.GuardVillagerScreenHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

public class GuardVillagerScreen extends AbstractContainerScreen<GuardVillagerScreenHandler> {
    private static final Identifier GUARD_GUI_TEXTURES = GuardVillagers.id("textures/gui/inventory.png");

    private static final WidgetSprites GUARD_FOLLOWING_ICONS = new WidgetSprites(GuardVillagers.id("following/following"), GuardVillagers.id("following/following_highlighted"));
    private static final WidgetSprites GUARD_NOT_FOLLOWING_ICONS = new WidgetSprites(GuardVillagers.id("following/not_following"), GuardVillagers.id("following/not_following_highlighted"));
    private static final WidgetSprites GUARD_PATROLLING_ICONS = new WidgetSprites(GuardVillagers.id("patrolling/patrolling1"), GuardVillagers.id("patrolling/patrolling2"));
    private static final WidgetSprites GUARD_NOT_PATROLLING_ICONS = new WidgetSprites(GuardVillagers.id("patrolling/notpatrolling1"), GuardVillagers.id("patrolling/notpatrolling2"));

    private static final Identifier ARMOR_EMPTY_TEXTURE = Identifier.withDefaultNamespace("hud/armor_empty");
    private static final Identifier ARMOR_HALF_TEXTURE = Identifier.withDefaultNamespace("hud/armor_half");
    private static final Identifier ARMOR_FULL_TEXTURE = Identifier.withDefaultNamespace("hud/armor_full");

    private final Player player;
    private final GuardEntity guardEntity;
    private float mousePosX;
    private float mousePosY;
    private boolean buttonPressed;

    public GuardVillagerScreen(GuardVillagerScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, handler.guardEntity.getDisplayName());
        this.titleLabelX = 80;
        this.inventoryLabelX = 100;
        this.player = inventory.player;
        this.guardEntity = handler.guardEntity;
    }

    @Override
    protected void init() {
        super.init();
        if (!GuardVillagersConfig.followHero || player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE)) {
            this.addRenderableWidget(new GuardGuiButton(
                    this.leftPos + 100, this.height / 2 - 40, 20, 18,
                    GUARD_FOLLOWING_ICONS, GUARD_NOT_FOLLOWING_ICONS, true,
                    (button) -> ClientPlayNetworking.send(new GuardFollowPacket(guardEntity.getId()))
            ));
        }
        if (!GuardVillagersConfig.setGuardPatrolHotv || player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE)) {
            this.addRenderableWidget(new GuardGuiButton(
                    this.leftPos + 120, this.height / 2 - 40, 20, 18,
                    GUARD_PATROLLING_ICONS, GUARD_NOT_PATROLLING_ICONS, false,
                    (button) -> {
                        buttonPressed = !buttonPressed;
                        ClientPlayNetworking.send(new GuardPatrolPacket(guardEntity.getId(), buttonPressed));
                    }
            ));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.mousePosX = mouseX;
        this.mousePosY = mouseY;
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int i = this.leftPos;
        int j = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, GUARD_GUI_TEXTURES, i, j, 0f, 0f, this.imageWidth, this.imageHeight, 256, 256);
        InventoryScreen.extractEntityInInventoryFollowsMouse(
                graphics,
                i + 26,
                j + 8,
                i + 76,
                j + 80,
                30,
                0.0625f,
                this.mousePosX,
                this.mousePosY,
                this.guardEntity
        );
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);

        int health = Mth.ceil(guardEntity.getHealth());
        int armor = guardEntity.getArmorValue();

        boolean statusU = guardEntity.hasEffect(MobEffects.POISON);
        boolean statusW = guardEntity.hasEffect(MobEffects.WITHER);
        HeartType heart = statusU ? HeartType.POISONED : statusW ? HeartType.WITHERED : guardEntity.isFullyFrozen() ? HeartType.FROZEN : HeartType.NORMAL;

        // Health row
        for (int i = 0; i < 10; i++) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HeartType.CONTAINER.getTexture(false), (i * 8) + 80, 20, 9, 9);
        }
        for (int i = 0; i < health / 2; i++) {
            if (health % 2 != 0 && health / 2 == i + 1) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, heart.getTexture(false), (i * 8) + 80, 20, 9, 9);
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, heart.getTexture(true), ((i + 1) * 8) + 80, 20, 9, 9);
            } else {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, heart.getTexture(false), (i * 8) + 80, 20, 9, 9);
            }
        }

        // Armor row
        for (int i = 0; i < 10; i++) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARMOR_EMPTY_TEXTURE, (i * 8) + 80, 30, 9, 9);
        }
        for (int i = 0; i < armor / 2; i++) {
            if (armor % 2 != 0 && armor / 2 == i + 1) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARMOR_FULL_TEXTURE, (i * 8) + 80, 30, 9, 9);
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARMOR_HALF_TEXTURE, ((i + 1) * 8) + 80, 30, 9, 9);
            } else {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARMOR_FULL_TEXTURE, (i * 8) + 80, 30, 9, 9);
            }
        }
    }

    class GuardGuiButton extends ImageButton {
        private final WidgetSprites texture;
        private final WidgetSprites newTexture;
        private final boolean isFollowButton;

        public GuardGuiButton(int xIn, int yIn, int widthIn, int heightIn,
                              WidgetSprites resourceLocationIn, WidgetSprites newTexture,
                              boolean isFollowButton, Button.OnPress onPressIn) {
            super(xIn, yIn, widthIn, heightIn, resourceLocationIn, onPressIn);
            this.texture = resourceLocationIn;
            this.newTexture = newTexture;
            this.isFollowButton = isFollowButton;
        }

        public boolean requirementsForTexture() {
            boolean following = GuardVillagerScreen.this.guardEntity.isFollowing();
            boolean patrol = GuardVillagerScreen.this.guardEntity.isPatrolling();
            return this.isFollowButton ? following : patrol;
        }

        @Override
        public void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
            WidgetSprites icon = this.requirementsForTexture() ? this.texture : this.newTexture;
            Identifier resourcelocation = icon.get(this.isFocused(), this.isHoveredOrFocused());
            context.blitSprite(RenderPipelines.GUI_TEXTURED, resourcelocation, this.getX(), this.getY(), this.width, this.height);
        }
    }

    @Environment(value = EnvType.CLIENT)
    enum HeartType {
        CONTAINER(Identifier.withDefaultNamespace("hud/heart/container"), Identifier.withDefaultNamespace("hud/heart/container")),
        NORMAL(Identifier.withDefaultNamespace("hud/heart/full"), Identifier.withDefaultNamespace("hud/heart/half")),
        POISONED(Identifier.withDefaultNamespace("hud/heart/poisoned_full"), Identifier.withDefaultNamespace("hud/heart/poisoned_half")),
        WITHERED(Identifier.withDefaultNamespace("hud/heart/withered_full"), Identifier.withDefaultNamespace("hud/heart/withered_half")),
        FROZEN(Identifier.withDefaultNamespace("hud/heart/frozen_full"), Identifier.withDefaultNamespace("hud/heart/frozen_half"));

        private final Identifier fullTexture;
        private final Identifier halfTexture;

        HeartType(Identifier fullTexture, Identifier halfTexture) {
            this.fullTexture = fullTexture;
            this.halfTexture = halfTexture;
        }

        public Identifier getTexture(boolean half) {
            return half ? this.halfTexture : this.fullTexture;
        }
    }
}