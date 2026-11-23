package net.spacerust.client.deployables;

import net.minecraft.util.Identifier;

public enum RustDeployableType {
    DOOR_WOOD("Деревянная дверь", "door_wood_0", "door_wood.png", 200.0f, true, false, "door_wood_open", "door_wood_close"),
    DOOR_METAL("Металлическая дверь", "door_metal_0", "door_metal.png", 250.0f, true, false, "door_metal_open", "door_metal_close"),
    DOOR_ARMORED("Бронированная дверь", "door_armored_0", "door_armored.png", 800.0f, true, false, "door_armored_open", "door_armored_close"),
    DOOR_DOUBLE_WOOD("Двойная деревянная дверь", "door_double_wood_0", "door_double_wood.png", 200.0f, true, true, "door_wood_open", "door_wood_close"),
    DOOR_DOUBLE_METAL("Двойная металлическая дверь", "door_double_metal_0", "door_double_metal.png", 250.0f, true, true, "door_metal_open", "door_metal_close"),
    DOOR_DOUBLE_ARMORED("Двойная бронированная дверь", "door_double_armored_0", "door_double_armored.png", 800.0f, true, true, "door_armored_open", "door_armored_close"),
    GARAGE_DOOR("Гаражная дверь", "garage_door_0", "garage_door.png", 600.0f, true, true, "garagedoor_begin_movement", "garagedoor_close_finish"),

    TOOL_CUPBOARD("Шкаф с инструментами", "deploy_tool_cupboard", "tool_cupboard.png", 500.0f, false, false, "door_knock", null),
    SMALL_BOX("Маленький ящик", "deploy_small_box", "small_box.png", 100.0f, false, false, "salvage_large_wood_box_open_001", "salvage_large_wood_box_close_001"),
    LARGE_BOX("Большой деревянный ящик", "deploy_large_box", "large_box.png", 250.0f, false, false, "salvage_large_wood_box_open_001", "salvage_large_wood_box_close_001"),

    KEY_LOCK("Соло замок", "lock_key_locked", "key_lock.png", 100.0f, false, false, "keylock_unlock", "keylock_lock"),
    CODE_LOCK("Кодовый замок", "lock_code_locked", "code_lock.png", 200.0f, false, false, "keylock_unlock", "keylock_lock");

    public final String title;
    public final String modelName;
    public final Identifier icon;
    public final float maxHealth;
    public final boolean isDoor;
    public final boolean isDoubleDoor;
    public final String openSound;
    public final String closeSound;

    RustDeployableType(String title, String modelName, String iconName, float maxHealth,
                       boolean isDoor, boolean isDoubleDoor, String openSound, String closeSound) {
        this.title = title;
        this.modelName = modelName;
        this.icon = new Identifier("spacerust", "textures/gui/rust_icons/" + iconName);
        this.maxHealth = maxHealth;
        this.isDoor = isDoor;
        this.isDoubleDoor = isDoubleDoor;
        this.openSound = openSound;
        this.closeSound = closeSound;
    }
}
