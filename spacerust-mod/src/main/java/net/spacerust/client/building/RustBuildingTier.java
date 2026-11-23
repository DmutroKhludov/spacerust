package net.spacerust.client.building;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

public enum RustBuildingTier {
    TWIG("Солома", "twig", 10, 20, Items.OAK_PLANKS, "wood_dark.png"),
    WOOD("Дерево", "wood", 250, 100, Items.OAK_WOOD, "wood_light.png"),
    STONE("Камень", "stone", 500, 150, Items.STONE, "stone_light.png"),
    METAL("Металл", "metal", 1000, 100, Items.IRON_INGOT, "metal_light.png"),
    ARMORED("МВК", "armored", 2000, 25, Items.NETHERITE_INGOT, "armored_light.png");

    public final String title;
    public final String prefix;
    public final int maxHealth;
    public final int upgradeCost;
    public final Item materialItem;
    public final Identifier radialIcon;

    RustBuildingTier(String title, String prefix, int maxHealth, int upgradeCost, Item materialItem, String iconName) {
        this.title = title;
        this.prefix = prefix;
        this.maxHealth = maxHealth;
        this.upgradeCost = upgradeCost;
        this.materialItem = materialItem;
        this.radialIcon = new Identifier("spacerust", "textures/gui/radial/" + iconName);
    }

    public RustBuildingTier next() {
        switch (this) {
            case TWIG: return WOOD;
            case WOOD: return STONE;
            case STONE: return METAL;
            case METAL: return ARMORED;
            default: return null;
        }
    }
}
