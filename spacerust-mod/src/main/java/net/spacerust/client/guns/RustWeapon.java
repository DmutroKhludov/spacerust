package net.spacerust.client.guns;

import net.minecraft.util.Identifier;

public enum RustWeapon {
    AK47("АК-47", "ak47", "tool_ak", "held_ak", "assault_rifle.png", "5.56 мм", 30, 133, 40.0f, true, 0.45f, "ak47_gunshot_body_01", "arsenal_glock_reload_start_01"),
    THOMPSON("Томпсон", "thompson", "tool_thompson", "held_thompson", "thompson.png", "9 мм", 20, 120, 30.0f, true, 0.55f, "thompson_gunshot_tail_outdoor_01", "arsenal_glock_reload_start_01"),
    CUSTOM_SMG("Кастом SMG", "custom_smg", "tool_custom_smg", "held_custom_smg", "custom_smg.png", "9 мм", 24, 100, 24.0f, true, 0.60f, "arsenal_custom_smg_gunshot_tail_outdoor_01", "arsenal_glock_reload_start_01"),
    MP5("MP5A4", "mp5", "tool_mp5", "held_mp5", "mp5.png", "9 мм", 30, 100, 35.0f, true, 0.50f, "mp5_gunshot_tail_outdoor_01", "arsenal_glock_reload_start_01"),
    SAR("Винтовка SAR", "sar", "tool_sar", "held_sar", "sar.png", "5.56 мм", 16, 170, 40.0f, false, 0.40f, "semi_auto_rifle_gunshot_tail_outdoor_01", "arsenal_glock_reload_start_01"),
    BOLT("Винтовка Bolt", "bolt", "tool_bolt", "held_bolt", "bolt.png", "5.56 мм", 4, 1300, 85.0f, false, 0.25f, "arsenal_bolt_rifle_gunshot_tail_outdoor_01", "arsenal_glock_reload_start_01"),
    L96("Снайперская L96", "l96", "tool_l96", "held_l96", "l96.png", "5.56 мм", 5, 1200, 85.0f, false, 0.20f, "arsenal_l96_gunshot_tail_outdoor_01", "arsenal_glock_reload_start_01"),
    M249("Пулемет M249", "m249", "tool_m249", "held_m249", "m249.png", "5.56 мм", 100, 120, 65.0f, true, 0.50f, "arsenal_m249_gunshot_tail_outdoor_01", "arsenal_glock_reload_start_01"),
    REVOLVER("Револьвер", "revolver", "tool_revolver", "held_revolver", "revolver.png", "9 мм", 8, 240, 35.0f, false, 0.60f, "revolver_gunshot_tail_outdoor_01", "arsenal_glock_reload_start_01"),
    PYTHON("Питон", "python", "tool_python", "held_python", "python.png", "9 мм", 6, 270, 55.0f, false, 0.55f, "python_gunshot_tail_outdoor_01", "arsenal_glock_reload_start_01"),
    SEMI_PISTOL("Пистолет SAP", "semipistol", "tool_semipistol", "held_semipistol", "semipistol.png", "9 мм", 10, 150, 38.0f, false, 0.65f, "semi_auto_pistol_gunshot_tail_outdoor_01", "arsenal_glock_reload_start_01"),
    PUMP_SHOTGUN("Помповый дробовик", "pump_shotgun", "tool_pump_shotgun", "held_pump_shotgun", "pump_shotgun.png", "12 калибр", 6, 1000, 12.0f, false, 0.70f, "arsenal_m4_shotgun_gunshot_tail_outdoor_01", "arsenal_glock_reload_start_01"),
    DOUBLE_BARREL("Двустволка", "doublebarrel", "tool_doublebarrel", "held_doublebarrel", "doublebarrel.png", "12 калибр", 2, 200, 12.0f, false, 0.75f, "double_shotgun_gunshot_tail_outdoor_01", "arsenal_glock_reload_start_01"),
    WATERPIPE("Водотруб", "waterpipe", "tool_waterpipe", "held_waterpipe", "waterpipe.png", "12 калибр", 1, 1800, 12.0f, false, 0.80f, "arsenal_waterpipe_shotgun_gunshot_tail_outdoor_01", "arsenal_glock_reload_start_01"),
    EOKA("Эока", "eoka", "tool_eoka", "held_eoka", "eoka.png", "Дробь", 1, 2500, 12.0f, false, 0.90f, "arsenal_eoka_pistol_gunshot_tail_outdoor_01", "arsenal_glock_reload_start_01");

    public final String title;
    public final String profileName;
    public final String toolModelName;
    public final String heldModelName;
    public final Identifier icon;
    public final String ammoName;
    public final int maxAmmo;
    public final int fireDelayMs;
    public final float baseDamage;
    public final boolean isAutomatic;
    public final float adsFovMultiplier;
    public final String shootSound;
    public final String reloadSound;

    RustWeapon(String title, String profileName, String toolModelName, String heldModelName, String iconName,
               String ammoName, int maxAmmo, int fireDelayMs, float baseDamage, boolean isAutomatic,
               float adsFovMultiplier, String shootSound, String reloadSound) {
        this.title = title;
        this.profileName = profileName;
        this.toolModelName = toolModelName;
        this.heldModelName = heldModelName;
        this.icon = new Identifier("spacerust", "textures/gui/rust_icons/" + iconName);
        this.ammoName = ammoName;
        this.maxAmmo = maxAmmo;
        this.fireDelayMs = fireDelayMs;
        this.baseDamage = baseDamage;
        this.isAutomatic = isAutomatic;
        this.adsFovMultiplier = adsFovMultiplier;
        this.shootSound = shootSound;
        this.reloadSound = reloadSound;
    }

    public boolean isShotgun() {
        return this == PUMP_SHOTGUN || this == DOUBLE_BARREL || this == WATERPIPE || this == EOKA;
    }
}
