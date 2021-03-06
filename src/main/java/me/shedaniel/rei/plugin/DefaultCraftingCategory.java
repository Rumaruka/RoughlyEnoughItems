package me.shedaniel.rei.plugin;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GlStateManager;
import me.shedaniel.rei.api.RecipeCategory;
import me.shedaniel.rei.gui.widget.ItemSlotWidget;
import me.shedaniel.rei.gui.widget.RecipeBaseWidget;
import me.shedaniel.rei.gui.widget.Widget;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GuiLighting;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

public class DefaultCraftingCategory implements RecipeCategory<DefaultCraftingDisplay> {
    
    private static final Identifier DISPLAY_TEXTURE = new Identifier("roughlyenoughitems", "textures/gui/display.png");
    
    @Override
    public Identifier getIdentifier() {
        return DefaultPlugin.CRAFTING;
    }
    
    @Override
    public ItemStack getCategoryIcon() {
        return new ItemStack(Blocks.CRAFTING_TABLE.getItem());
    }
    
    @Override
    public String getCategoryName() {
        return I18n.translate("category.rei.crafting");
    }
    
    @Override
    public List<Widget> setupDisplay(Supplier<DefaultCraftingDisplay> recipeDisplaySupplier, Rectangle bounds) {
        Point startPoint = new Point((int) bounds.getCenterX() - 58, (int) bounds.getCenterY() - 27);
        List<Widget> widgets = new LinkedList<>(Arrays.asList(new RecipeBaseWidget(bounds) {
            @Override
            public void render(int mouseX, int mouseY, float delta) {
                super.render(mouseX, mouseY, delta);
                GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                GuiLighting.disable();
                MinecraftClient.getInstance().getTextureManager().bindTexture(DISPLAY_TEXTURE);
                blit(startPoint.x, startPoint.y, 0, 0, 116, 54);
            }
        }));
        List<List<ItemStack>> input = recipeDisplaySupplier.get().getInput();
        List<ItemSlotWidget> slots = Lists.newArrayList();
        for(int y = 0; y < 3; y++)
            for(int x = 0; x < 3; x++)
                slots.add(new ItemSlotWidget(startPoint.x + 1 + x * 18, startPoint.y + 1 + y * 18, Lists.newArrayList(), true, true, true));
        for(int i = 0; i < input.size(); i++) {
            if (recipeDisplaySupplier.get() instanceof DefaultShapedDisplay) {
                if (!input.get(i).isEmpty())
                    slots.get(getSlotWithSize(recipeDisplaySupplier.get(), i)).setItemList(input.get(i));
            } else if (!input.get(i).isEmpty())
                slots.get(i).setItemList(input.get(i));
        }
        widgets.addAll(slots);
        widgets.add(new ItemSlotWidget(startPoint.x + 95, startPoint.y + 19, recipeDisplaySupplier.get().getOutput(), false, true, true) {
            @Override
            protected String getItemCountOverlay(ItemStack currentStack) {
                if (currentStack.getAmount() == 1)
                    return "";
                if (currentStack.getAmount() < 1)
                    return "§c" + currentStack.getAmount();
                return currentStack.getAmount() + "";
            }
        });
        return widgets;
    }
    
    private int getSlotWithSize(DefaultCraftingDisplay recipeDisplay, int num) {
        if (recipeDisplay.getWidth() == 1) {
            if (num == 1)
                return 3;
            if (num == 2)
                return 6;
        }
        
        if (recipeDisplay.getWidth() == 2) {
            if (num == 2)
                return 3;
            if (num == 3)
                return 4;
            if (num == 4)
                return 6;
            if (num == 5)
                return 7;
        }
        return num;
    }
    
}
