package me.shedaniel.rei.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GlStateManager;
import me.shedaniel.cloth.api.ClientUtils;
import me.shedaniel.rei.RoughlyEnoughItemsCore;
import me.shedaniel.rei.api.DisplayHelper;
import me.shedaniel.rei.client.ClientHelper;
import me.shedaniel.rei.client.ScreenHelper;
import me.shedaniel.rei.client.Weather;
import me.shedaniel.rei.gui.widget.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.audio.PositionedSoundInstance;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.*;
import net.minecraft.client.render.GuiLighting;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.TranslatableTextComponent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ContainerScreenOverlay extends AbstractParentElement implements Drawable {
    
    private static final Identifier CHEST_GUI_TEXTURE = new Identifier("roughlyenoughitems", "textures/gui/recipecontainer.png");
    private static final List<QueuedTooltip> QUEUED_TOOLTIPS = Lists.newArrayList();
    public static String searchTerm = "";
    private static int page = 0;
    private static ItemListOverlay itemListOverlay;
    private final List<Widget> widgets = Lists.newLinkedList();
    private Rectangle rectangle;
    private Window window;
    private CraftableToggleButtonWidget toggleButtonWidget;
    private ButtonWidget buttonLeft, buttonRight;
    private int lastLeft;
    
    public static ItemListOverlay getItemListOverlay() {
        return itemListOverlay;
    }
    
    public void init() {
        init(false);
    }
    
    public void init(boolean setPage) {
        //Update Variables
        this.children().clear();
        this.window = MinecraftClient.getInstance().window;
        DisplayHelper.DisplayBoundsHandler boundsHandler = RoughlyEnoughItemsCore.getDisplayHelper().getResponsibleBoundsHandler(MinecraftClient.getInstance().currentScreen.getClass());
        this.rectangle = RoughlyEnoughItemsCore.getConfigManager().getConfig().mirrorItemPanel ? boundsHandler.getLeftBounds(MinecraftClient.getInstance().currentScreen) : boundsHandler.getRightBounds(MinecraftClient.getInstance().currentScreen);
        this.lastLeft = getLeft();
        widgets.add(itemListOverlay = new ItemListOverlay(page));
        itemListOverlay.updateList(boundsHandler, boundsHandler.getItemListArea(rectangle), page, searchTerm, false);
        
        widgets.add(buttonLeft = new ButtonWidget(rectangle.x, rectangle.y + 5, 16, 16, new TranslatableTextComponent("text.rei.left_arrow")) {
            @Override
            public void onPressed() {
                page--;
                if (page < 0)
                    page = getTotalPage();
                itemListOverlay.updateList(boundsHandler, boundsHandler.getItemListArea(rectangle), page, searchTerm, false);
            }
            
            @Override
            public Optional<String> getTooltips() {
                return Optional.ofNullable(I18n.translate("text.rei.previous_page"));
            }
            
            @Override
            public boolean changeFocus(boolean boolean_1) {
                return false;
            }
        });
        widgets.add(buttonRight = new ButtonWidget(rectangle.x + rectangle.width - 18, rectangle.y + 5, 16, 16, new TranslatableTextComponent("text.rei.right_arrow")) {
            @Override
            public void onPressed() {
                page++;
                if (page > getTotalPage())
                    page = 0;
                itemListOverlay.updateList(boundsHandler, boundsHandler.getItemListArea(rectangle), page, searchTerm, false);
            }
            
            @Override
            public Optional<String> getTooltips() {
                return Optional.ofNullable(I18n.translate("text.rei.next_page"));
            }
            
            @Override
            public boolean changeFocus(boolean boolean_1) {
                return false;
            }
        });
        
        if (setPage)
            page = MathHelper.clamp(page, 0, getTotalPage());
        
        widgets.add(new ButtonWidget(RoughlyEnoughItemsCore.getConfigManager().getConfig().mirrorItemPanel ? window.getScaledWidth() - 30 : 10, 10, 20, 20, "") {
            @Override
            public void onPressed() {
                if (Screen.hasShiftDown()) {
                    ClientHelper.setCheating(!ClientHelper.isCheating());
                    return;
                }
                RoughlyEnoughItemsCore.getConfigManager().openConfigScreen(ScreenHelper.getLastContainerScreen());
            }
            
            @Override
            public void render(int mouseX, int mouseY, float delta) {
                super.render(mouseX, mouseY, delta);
                GuiLighting.disable();
                if (ClientHelper.isCheating() && RoughlyEnoughItemsCore.hasOperatorPermission()) {
                    if (RoughlyEnoughItemsCore.hasPermissionToUsePackets())
                        fill(getBounds().x, getBounds().y, getBounds().x + 20, getBounds().y + 20, 721354752);
                    else
                        fill(getBounds().x, getBounds().y, getBounds().x + 20, getBounds().y + 20, 1476440063);
                }
                MinecraftClient.getInstance().getTextureManager().bindTexture(CHEST_GUI_TEXTURE);
                GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                blit(getBounds().x + 3, getBounds().y + 3, 0, 0, 14, 14);
            }
            
            @Override
            public Optional<String> getTooltips() {
                String tooltips = I18n.translate("text.rei.config_tooltip");
                tooltips += "\n  ";
                if (!ClientHelper.isCheating())
                    tooltips += "\n" + I18n.translate("text.rei.cheating_disabled");
                else if (!RoughlyEnoughItemsCore.hasOperatorPermission())
                    tooltips += "\n" + I18n.translate("text.rei.cheating_enabled_no_perms");
                else if (RoughlyEnoughItemsCore.hasPermissionToUsePackets())
                    tooltips += "\n" + I18n.translate("text.rei.cheating_enabled");
                else
                    tooltips += "\n" + I18n.translate("text.rei.cheating_limited_enabled");
                return Optional.ofNullable(tooltips);
            }
            
            @Override
            public boolean changeFocus(boolean boolean_1) {
                return false;
            }
        });
        if (RoughlyEnoughItemsCore.getConfigManager().getConfig().showUtilsButtons) {
            widgets.add(new ButtonWidget(RoughlyEnoughItemsCore.getConfigManager().getConfig().mirrorItemPanel ? window.getScaledWidth() - 55 : 35, 10, 20, 20, "") {
                @Override
                public void onPressed() {
                    MinecraftClient.getInstance().player.sendChatMessage(RoughlyEnoughItemsCore.getConfigManager().getConfig().gamemodeCommand.replaceAll("\\{gamemode}", getNextGameMode().getName()));
                }
                
                @Override
                public void render(int mouseX, int mouseY, float delta) {
                    text = getGameModeShortText(getCurrentGameMode());
                    super.render(mouseX, mouseY, delta);
                }
                
                @Override
                public Optional<String> getTooltips() {
                    return Optional.ofNullable(I18n.translate("text.rei.gamemode_button.tooltip", getGameModeText(getNextGameMode())));
                }
                
                @Override
                public boolean changeFocus(boolean boolean_1) {
                    return false;
                }
            });
            widgets.add(new ButtonWidget(RoughlyEnoughItemsCore.getConfigManager().getConfig().mirrorItemPanel ? window.getScaledWidth() - 80 : 60, 10, 20, 20, "") {
                @Override
                public void onPressed() {
                    MinecraftClient.getInstance().player.sendChatMessage(RoughlyEnoughItemsCore.getConfigManager().getConfig().weatherCommand.replaceAll("\\{weather}", getNextWeather().name().toLowerCase()));
                }
                
                @Override
                public void render(int mouseX, int mouseY, float delta) {
                    super.render(mouseX, mouseY, delta);
                    GuiLighting.disable();
                    MinecraftClient.getInstance().getTextureManager().bindTexture(CHEST_GUI_TEXTURE);
                    GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                    blit(getBounds().x + 3, getBounds().y + 3, getCurrentWeather().getId() * 14, 14, 14, 14);
                }
                
                @Override
                public Optional<String> getTooltips() {
                    return Optional.ofNullable(I18n.translate("text.rei.weather_button.tooltip", I18n.translate(getNextWeather().getTranslateKey())));
                }
                
                @Override
                public boolean changeFocus(boolean boolean_1) {
                    return false;
                }
            });
        }
        widgets.add(new ClickableLabelWidget(rectangle.x + (rectangle.width / 2), rectangle.y + 10, "", getTotalPage() > 0) {
            @Override
            public void render(int mouseX, int mouseY, float delta) {
                page = MathHelper.clamp(page, 0, getTotalPage());
                this.text = String.format("%s/%s", page + 1, getTotalPage() + 1);
                super.render(mouseX, mouseY, delta);
            }
            
            @Override
            public Optional<String> getTooltips() {
                return Optional.ofNullable(I18n.translate("text.rei.go_back_first_page"));
            }
            
            @Override
            public void onLabelClicked() {
                MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                page = 0;
                itemListOverlay.updateList(boundsHandler, boundsHandler.getItemListArea(rectangle), page, searchTerm, false);
            }
            
            @Override
            public boolean changeFocus(boolean boolean_1) {
                return false;
            }
        });
        buttonLeft.enabled = buttonRight.enabled = getTotalPage() > 0;
        if (ScreenHelper.searchField == null)
            ScreenHelper.searchField = new SearchFieldWidget(0, 0, 0, 0);
        ScreenHelper.searchField.getBounds().setBounds(getTextFieldArea());
        this.widgets.add(ScreenHelper.searchField);
        ScreenHelper.searchField.setText(searchTerm);
        ScreenHelper.searchField.setChangedListener(s -> {
            searchTerm = s;
            itemListOverlay.updateList(boundsHandler, boundsHandler.getItemListArea(rectangle), page, searchTerm, true);
        });
        if (RoughlyEnoughItemsCore.getConfigManager().getConfig().enableCraftableOnlyButton)
            this.widgets.add(toggleButtonWidget = new CraftableToggleButtonWidget(getCraftableToggleArea()) {
                @Override
                public void onPressed() {
                    RoughlyEnoughItemsCore.getConfigManager().toggleCraftableOnly();
                    itemListOverlay.updateList(boundsHandler, boundsHandler.getItemListArea(rectangle), page, searchTerm, true);
                }
                
                @Override
                public void lateRender(int mouseX, int mouseY, float delta) {
                    blitOffset = 300;
                    super.lateRender(mouseX, mouseY, delta);
                }
            });
        else
            toggleButtonWidget = null;
        this.itemListOverlay.updateList(boundsHandler, boundsHandler.getItemListArea(rectangle), page, searchTerm, false);
    }
    
    private Weather getNextWeather() {
        try {
            Weather current = getCurrentWeather();
            int next = current.getId() + 1;
            if (next >= 3)
                next = 0;
            return Weather.byId(next);
        } catch (Exception e) {
            return Weather.CLEAR;
        }
    }
    
    private Weather getCurrentWeather() {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world.isThundering())
            return Weather.THUNDER;
        if (world.getLevelProperties().isRaining())
            return Weather.RAIN;
        return Weather.CLEAR;
    }
    
    private String getGameModeShortText(GameMode gameMode) {
        return I18n.translate("text.rei.short_gamemode." + gameMode.getName());
    }
    
    private String getGameModeText(GameMode gameMode) {
        return I18n.translate("selectWorld.gameMode." + gameMode.getName());
    }
    
    private GameMode getNextGameMode() {
        try {
            GameMode current = getCurrentGameMode();
            int next = current.getId() + 1;
            if (next > 3)
                next = 0;
            return GameMode.byId(next);
        } catch (Exception e) {
            return GameMode.INVALID;
        }
    }
    
    private GameMode getCurrentGameMode() {
        return MinecraftClient.getInstance().getNetworkHandler().getPlayerListEntry(MinecraftClient.getInstance().player.getGameProfile().getId()).getGameMode();
    }
    
    private Rectangle getTextFieldArea() {
        int widthRemoved = RoughlyEnoughItemsCore.getConfigManager().getConfig().enableCraftableOnlyButton ? 22 : 2;
        if (RoughlyEnoughItemsCore.getConfigManager().getConfig().sideSearchField)
            return new Rectangle(rectangle.x + 2, window.getScaledHeight() - 22, rectangle.width - 6 - widthRemoved, 18);
        if (MinecraftClient.getInstance().currentScreen instanceof RecipeViewingScreen) {
            RecipeViewingScreen widget = (RecipeViewingScreen) MinecraftClient.getInstance().currentScreen;
            return new Rectangle(widget.getBounds().x, window.getScaledHeight() - 22, widget.getBounds().width - widthRemoved, 18);
        }
        return new Rectangle(ScreenHelper.getLastContainerScreenHooks().rei_getContainerLeft(), window.getScaledHeight() - 22, ScreenHelper.getLastContainerScreenHooks().rei_getContainerWidth() - widthRemoved, 18);
    }
    
    private Rectangle getCraftableToggleArea() {
        Rectangle searchBoxArea = getTextFieldArea();
        searchBoxArea.setLocation(searchBoxArea.x + searchBoxArea.width + 4, searchBoxArea.y - 1);
        searchBoxArea.setSize(20, 20);
        return searchBoxArea;
    }
    
    private String getCheatModeText() {
        return I18n.translate(String.format("%s%s", "text.rei.", ClientHelper.isCheating() ? "cheat" : "nocheat"));
    }
    
    public Rectangle getRectangle() {
        return rectangle;
    }
    
    @Override
    public void render(int mouseX, int mouseY, float delta) {
        List<ItemStack> currentStacks = ClientHelper.getInventoryItemsTypes();
        if (RoughlyEnoughItemsCore.getDisplayHelper().getBaseBoundsHandler() != null && RoughlyEnoughItemsCore.getDisplayHelper().getBaseBoundsHandler().shouldRecalculateArea(!RoughlyEnoughItemsCore.getConfigManager().getConfig().mirrorItemPanel, rectangle))
            init(true);
        else if (RoughlyEnoughItemsCore.getConfigManager().isCraftableOnlyEnabled() && (!hasSameListContent(new LinkedList<>(ScreenHelper.inventoryStacks), currentStacks) || (currentStacks.size() != ScreenHelper.inventoryStacks.size()))) {
            ScreenHelper.inventoryStacks = ClientHelper.getInventoryItemsTypes();
            DisplayHelper.DisplayBoundsHandler boundsHandler = RoughlyEnoughItemsCore.getDisplayHelper().getResponsibleBoundsHandler(MinecraftClient.getInstance().currentScreen.getClass());
            itemListOverlay.updateList(boundsHandler, boundsHandler.getItemListArea(rectangle), page, searchTerm, true);
        }
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        GuiLighting.disable();
        this.renderWidgets(mouseX, mouseY, delta);
    }
    
    public void lateRender(int mouseX, int mouseY, float delta) {
        ScreenHelper.searchField.laterRender(mouseX, mouseY, delta);
        if (toggleButtonWidget != null)
            toggleButtonWidget.lateRender(mouseX, mouseY, delta);
        Screen currentScreen = MinecraftClient.getInstance().currentScreen;
        if (!(currentScreen instanceof RecipeViewingScreen) || !((RecipeViewingScreen) currentScreen).choosePageActivated)
            QUEUED_TOOLTIPS.stream().filter(Objects::nonNull).forEach(this::renderTooltip);
        QUEUED_TOOLTIPS.clear();
    }
    
    public void renderTooltip(QueuedTooltip tooltip) {
        renderTooltip(tooltip.getText(), tooltip.getX(), tooltip.getY());
    }
    
    public void renderTooltip(List<String> lines, int mouseX, int mouseY) {
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        if (!lines.isEmpty()) {
            GlStateManager.disableRescaleNormal();
            GuiLighting.disable();
            GlStateManager.disableLighting();
            int width = 0;
            for(String line : lines)
                if (font.getStringWidth(line) > width)
                    width = font.getStringWidth(line);
            int height = lines.size() <= 1 ? 8 : lines.size() * 10;
            int x = Math.max(mouseX + 12, 6);
            int y = Math.min(mouseY - 12, window.getScaledHeight() - height - 6);
            if (x + width > window.getScaledWidth())
                x -= 24 + width;
            if (y < 6)
                y += 24;
            
            this.blitOffset = 1000;
            this.fillGradient(x - 3, y - 4, x + width + 3, y - 3, -267386864, -267386864);
            this.fillGradient(x - 3, y + height + 3, x + width + 3, y + height + 4, -267386864, -267386864);
            this.fillGradient(x - 3, y - 3, x + width + 3, y + height + 3, -267386864, -267386864);
            this.fillGradient(x - 4, y - 3, x - 3, y + height + 3, -267386864, -267386864);
            this.fillGradient(x + width + 3, y - 3, x + width + 4, y + height + 3, -267386864, -267386864);
            this.fillGradient(x - 3, y - 3 + 1, x - 3 + 1, y + height + 3 - 1, 1347420415, 1344798847);
            this.fillGradient(x + width + 2, y - 3 + 1, x + width + 3, y + height + 3 - 1, 1347420415, 1344798847);
            this.fillGradient(x - 3, y - 3, x + width + 3, y - 3 + 1, 1347420415, 1347420415);
            this.fillGradient(x - 3, y + height + 2, x + width + 3, y + height + 3, 1344798847, 1344798847);
            
            int currentY = y;
            for(int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
                GlStateManager.disableDepthTest();
                font.drawWithShadow(lines.get(lineIndex), x, currentY, -1);
                GlStateManager.enableDepthTest();
                currentY += lineIndex == 0 ? 12 : 10;
            }
            this.blitOffset = 0;
            GlStateManager.enableLighting();
            GuiLighting.enable();
            GlStateManager.enableRescaleNormal();
        }
    }
    
    private boolean hasSameListContent(List<ItemStack> list1, List<ItemStack> list2) {
        list1.sort((itemStack, t1) -> ItemListOverlay.tryGetItemStackName(itemStack).compareToIgnoreCase(ItemListOverlay.tryGetItemStackName(t1)));
        list2.sort((itemStack, t1) -> ItemListOverlay.tryGetItemStackName(itemStack).compareToIgnoreCase(ItemListOverlay.tryGetItemStackName(t1)));
        
        return list1.stream().map(ItemListOverlay::tryGetItemStackName).collect(Collectors.joining("")).equals(list2.stream().map(ItemListOverlay::tryGetItemStackName).collect(Collectors.joining("")));
    }
    
    public void addTooltip(QueuedTooltip queuedTooltip) {
        QUEUED_TOOLTIPS.add(queuedTooltip);
    }
    
    public void renderWidgets(int int_1, int int_2, float float_1) {
        if (!ScreenHelper.isOverlayVisible())
            return;
        buttonLeft.enabled = buttonRight.enabled = getTotalPage() > 0;
        widgets.forEach(widget -> {
            GuiLighting.disable();
            widget.render(int_1, int_2, float_1);
        });
        GuiLighting.disable();
    }
    
    private int getLeft() {
        if (MinecraftClient.getInstance().currentScreen instanceof RecipeViewingScreen) {
            RecipeViewingScreen widget = (RecipeViewingScreen) MinecraftClient.getInstance().currentScreen;
            return widget.getBounds().x;
        }
        if (MinecraftClient.getInstance().player.getRecipeBook().isGuiOpen())
            return ScreenHelper.getLastContainerScreenHooks().rei_getContainerLeft() - 147 - 30;
        return ScreenHelper.getLastContainerScreenHooks().rei_getContainerLeft();
    }
    
    private int getTotalPage() {
        return itemListOverlay.getTotalPage();
    }
    
    @Override
    public boolean mouseScrolled(double i, double j, double amount) {
        if (!ScreenHelper.isOverlayVisible())
            return false;
        if (rectangle.contains(ClientUtils.getMouseLocation())) {
            if (amount > 0 && buttonLeft.enabled)
                buttonLeft.onPressed();
            else if (amount < 0 && buttonRight.enabled)
                buttonRight.onPressed();
            else
                return false;
            return true;
        }
        for(Widget widget : widgets)
            if (widget.mouseScrolled(i, j, amount))
                return true;
        return false;
    }
    
    @Override
    public boolean keyPressed(int int_1, int int_2, int int_3) {
        if (ScreenHelper.isOverlayVisible())
            for(Element listener : widgets)
                if (listener.keyPressed(int_1, int_2, int_3))
                    return true;
        if (ClientHelper.HIDE.matchesKey(int_1, int_2)) {
            ScreenHelper.toggleOverlayVisible();
            return true;
        }
        if (!ScreenHelper.isOverlayVisible())
            return false;
        ItemStack itemStack = null;
        if (MinecraftClient.getInstance().currentScreen instanceof ContainerScreen)
            if (ScreenHelper.getLastContainerScreenHooks().rei_getHoveredSlot() != null && !ScreenHelper.getLastContainerScreenHooks().rei_getHoveredSlot().getStack().isEmpty())
                itemStack = ScreenHelper.getLastContainerScreenHooks().rei_getHoveredSlot().getStack();
        if (itemStack != null && !itemStack.isEmpty()) {
            if (ClientHelper.RECIPE.matchesKey(int_1, int_2))
                return ClientHelper.executeRecipeKeyBind(itemStack);
            else if (ClientHelper.USAGE.matchesKey(int_1, int_2))
                return ClientHelper.executeUsageKeyBind(itemStack);
        }
        return false;
    }
    
    @Override
    public boolean charTyped(char char_1, int int_1) {
        if (!ScreenHelper.isOverlayVisible())
            return false;
        for(Element listener : widgets)
            if (listener.charTyped(char_1, int_1))
                return true;
        return false;
    }
    
    @Override
    public List<? extends Element> children() {
        return widgets;
    }
    
    @Override
    public boolean mouseClicked(double double_1, double double_2, int int_1) {
        if (!ScreenHelper.isOverlayVisible())
            return false;
        for(Element element : widgets)
            if (element.mouseClicked(double_1, double_2, int_1)) {
                this.setFocused(element);
                if (int_1 == 0)
                    this.setDragging(true);
                return true;
            }
        return false;
    }
    
    public static class SearchFieldWidget extends TextFieldWidget {
        public SearchFieldWidget(int x, int y, int width, int height) {
            super(x, y, width, height);
        }
        
        public void laterRender(int int_1, int int_2, float float_1) {
            GuiLighting.disable();
            GlStateManager.disableDepthTest();
            super.render(int_1, int_2, float_1);
            GlStateManager.enableDepthTest();
        }
        
        @Override
        public boolean mouseClicked(double double_1, double double_2, int int_1) {
            if (isVisible() && getBounds().contains(double_1, double_2) && int_1 == 1)
                setText("");
            return super.mouseClicked(double_1, double_2, int_1);
        }
        
        @Override
        public void render(int int_1, int int_2, float float_1) {
        }
    }
    
}
