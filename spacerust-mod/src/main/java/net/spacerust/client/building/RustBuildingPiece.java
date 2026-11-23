package net.spacerust.client.building;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

public enum RustBuildingPiece {
    FOUNDATION("Фундамент", "foundation", "foundation.png", 3.0, 0.5, 3.0, true, false),
    WALL("Стена", "wall", "wall.png", 3.0, 3.0, 0.25, false, true),
    DOORWAY("Дверной проем", "doorway", "wall_doorway.png", 3.0, 3.0, 0.25, false, true),
    WINDOW("Оконный проем", "window", "wall_window.png", 3.0, 3.0, 0.25, false, true),
    FLOOR("Пол / Потолок", "floor", "floor.png", 3.0, 0.2, 3.0, false, false),
    FLOOR_TRIANGLE("Треугольный пол", "floor_triangle", "floor_triangle.png", 3.0, 0.2, 2.6, false, false),
    ROOF("Крыша", "roof", "roof.png", 3.0, 2.12, 3.0, false, false),
    HALF_WALL("Полустена", "half_wall", "wall_half.png", 3.0, 1.5, 0.25, false, true),
    LOW_WALL("Низкая стена", "low_wall", "wall_low.png", 3.0, 0.75, 0.25, false, true),
    FRAME("Двойной проем", "frame", "wall_frame.png", 3.0, 3.0, 0.25, false, true),
    STAIRS_L("L-Лестница", "stairs_l", "stairs_l.png", 3.0, 3.0, 3.0, false, false),
    FOUNDATION_STEPS("Ступени", "foundation_steps", "foundation_steps.png", 3.0, 1.0, 1.5, false, false),
    RAMP("Рампа / Пандус", "ramp", "ramp.png", 3.0, 1.5, 3.0, false, false);

    public final String title;
    public final String modelPrefix;
    public final Identifier icon;
    public final double width;
    public final double height;
    public final double depth;
    public final boolean isFoundation;
    public final boolean isWall;

    RustBuildingPiece(String title, String modelPrefix, String iconName, double width, double height, double depth, boolean isFoundation, boolean isWall) {
        this.title = title;
        this.modelPrefix = modelPrefix;
        this.icon = new Identifier("spacerust", "textures/gui/rust_icons/" + iconName);
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.isFoundation = isFoundation;
        this.isWall = isWall;
    }

    public String getModelName(RustBuildingTier tier) {
        return modelPrefix + "_" + tier.prefix;
    }

    public Box createBoundingBox(double x, double y, double z) {
        return createBoundingBox(x, y, z, 0f);
    }

    public Box createBoundingBox(double x, double y, double z, float yaw) {
        double w = width;
        double d = depth;
        float normYaw = (yaw % 360f + 360f) % 360f;
        if ((normYaw > 45 && normYaw < 135) || (normYaw > 225 && normYaw < 315)) {
            double temp = w;
            w = d;
            d = temp;
        }
        double hw = w / 2.0;
        double hd = d / 2.0;
        return new Box(x - hw, y, z - hd, x + hw, y + height, z + hd);
    }
}
