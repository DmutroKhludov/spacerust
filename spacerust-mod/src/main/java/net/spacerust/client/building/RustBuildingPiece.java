package net.spacerust.client.building;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;

public enum RustBuildingPiece {
    FOUNDATION("Фундамент", "foundation", "foundation.png", 3.0, 0.5, 3.0, true, false),
    WALL("Стена", "wall", "wall.png", 3.0, 3.0, 0.25, false, true),
    DOORWAY("Дверной проем", "doorway", "wall_doorway.png", 3.0, 3.0, 0.25, false, true),
    WINDOW("Оконный проем", "window", "wall_window.png", 3.0, 3.0, 0.25, false, true),
    FLOOR("Пол / Потолок", "floor", "floor.png", 3.0, 0.2, 3.0, false, false),
    FLOOR_TRIANGLE("Треугольный пол", "floor_triangle", "floor_triangle.png", 3.0, 0.2, 2.6, false, false),
    ROOF("Крыша", "roof", "roof.png", 3.0, 2.12, 3.0, false, false),
    HALF_WALL("Полустена", "half_wall", "wall_half.png", 3.0, 1.5, 0.25, false, true),
    LOW_WALL("Низкая стена", "low_wall", "wall_low.png", 3.0, 1.0, 0.25, false, true),
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

    public List<Box> getCollisionBoxes(double x, double y, double z, float yaw) {
        List<Box> list = new ArrayList<>();
        switch (this) {
            case DOORWAY:
                // Single Doorway: 1.3m opening in center (from -0.65 to +0.65, height 2.2m)
                list.add(makeRotatedBox(x, y, z, yaw, -1.5, 0.0, -0.15, -0.65, 3.0, 0.15));
                list.add(makeRotatedBox(x, y, z, yaw, 0.65, 0.0, -0.15, 1.5, 3.0, 0.15));
                list.add(makeRotatedBox(x, y, z, yaw, -0.65, 2.2, -0.15, 0.65, 3.0, 0.15));
                break;

            case FRAME:
                // Double Door Frame: 2.7m opening in center (from -1.35 to +1.35, height 2.7m)
                list.add(makeRotatedBox(x, y, z, yaw, -1.5, 0.0, -0.15, -1.35, 3.0, 0.15));
                list.add(makeRotatedBox(x, y, z, yaw, 1.35, 0.0, -0.15, 1.5, 3.0, 0.15));
                list.add(makeRotatedBox(x, y, z, yaw, -1.35, 2.7, -0.15, 1.35, 3.0, 0.15));
                break;

            case WINDOW:
                // Window: opening in center
                list.add(makeRotatedBox(x, y, z, yaw, -1.5, 0.0, -0.15, -0.65, 3.0, 0.15));
                list.add(makeRotatedBox(x, y, z, yaw, 0.65, 0.0, -0.15, 1.5, 3.0, 0.15));
                list.add(makeRotatedBox(x, y, z, yaw, -0.65, 0.0, -0.15, 0.65, 0.9, 0.15));
                list.add(makeRotatedBox(x, y, z, yaw, -0.65, 2.1, -0.15, 0.65, 3.0, 0.15));
                break;

            case HALF_WALL:
                list.add(makeRotatedBox(x, y, z, yaw, -1.5, 0.0, -0.15, 1.5, 1.5, 0.15));
                break;

            case LOW_WALL:
                list.add(makeRotatedBox(x, y, z, yaw, -1.5, 0.0, -0.15, 1.5, 1.0, 0.15));
                break;

            case FLOOR:
            case FLOOR_TRIANGLE:
                list.add(new Box(x - 1.5, y - 0.12, z - 1.5, x + 1.5, y + 0.05, z + 1.5));
                break;

            case FOUNDATION:
                list.add(new Box(x - 1.5, y, z - 1.5, x + 1.5, y + 0.5, z + 1.5));
                break;

            default:
                list.add(createBoundingBox(x, y, z, yaw));
                break;
        }
        return list;
    }

    public static Box makeRotatedBox(double bx, double by, double bz, float yaw, double lx1, double ly1, double lz1, double lx2, double ly2, double lz2) {
        float normYaw = (yaw % 360f + 360f) % 360f;
        double rx1, rz1, rx2, rz2;
        if (normYaw >= 45 && normYaw < 135) { // 90 deg
            rx1 = -lz2; rx2 = -lz1;
            rz1 = lx1;  rz2 = lx2;
        } else if (normYaw >= 135 && normYaw < 225) { // 180 deg
            rx1 = -lx2; rx2 = -lx1;
            rz1 = -lz2; rz2 = -lz1;
        } else if (normYaw >= 225 && normYaw < 315) { // 270 deg
            rx1 = lz1;  rx2 = lz2;
            rz1 = -lx2; rz2 = -lx1;
        } else { // 0 deg
            rx1 = lx1;  rx2 = lx2;
            rz1 = lz1;  rz2 = lz2;
        }
        double minX = bx + Math.min(rx1, rx2);
        double maxX = bx + Math.max(rx1, rx2);
        double minZ = bz + Math.min(rz1, rz2);
        double maxZ = bz + Math.max(rz1, rz2);
        return new Box(minX, by + ly1, minZ, maxX, by + ly2, maxZ);
    }
}
