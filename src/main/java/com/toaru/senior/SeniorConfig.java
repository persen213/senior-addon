package com.toaru.senior;

import net.minecraftforge.common.ForgeConfigSpec;

/** Balance values, saved in config/senior-common.toml. The server's file decides. */
final class SeniorConfig {
    private SeniorConfig() { }

    static final ForgeConfigSpec SPEC;

    static final ForgeConfigSpec.DoubleValue FOLD_RANGE;
    static final ForgeConfigSpec.DoubleValue FOLD_COOLDOWN_SECONDS;
    static final ForgeConfigSpec.DoubleValue FOLD_CP;
    static final ForgeConfigSpec.DoubleValue FOLD_OVERLOAD;

    static final ForgeConfigSpec.DoubleValue SHEAR_RANGE;
    static final ForgeConfigSpec.DoubleValue SHEAR_HEARTS;
    static final ForgeConfigSpec.DoubleValue SHEAR_COOLDOWN_SECONDS;
    static final ForgeConfigSpec.DoubleValue SHEAR_CP;
    static final ForgeConfigSpec.DoubleValue SHEAR_OVERLOAD;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("foldstep");
        FOLD_RANGE = b.comment("Maximum distance in blocks.").defineInRange("range", 16.0, 2.0, 64.0);
        FOLD_COOLDOWN_SECONDS = b.comment("Cooldown in seconds.").defineInRange("cooldownSeconds", 9.0, 0.0, 3600.0);
        FOLD_CP = b.comment("CP cost.").defineInRange("cp", 150.0, 0.0, 100000.0);
        FOLD_OVERLOAD = b.comment("Overload cost.").defineInRange("overload", 40.0, 0.0, 100000.0);
        b.pop();

        b.push("spatial_shear");
        SHEAR_RANGE = b.comment("Maximum distance in blocks.").defineInRange("range", 12.0, 2.0, 64.0);
        SHEAR_HEARTS = b.comment("Damage in hearts (1 heart = 2 health points).").defineInRange("damageHearts", 4.0, 0.0, 100.0);
        SHEAR_COOLDOWN_SECONDS = b.comment("Cooldown in seconds.").defineInRange("cooldownSeconds", 8.0, 0.0, 3600.0);
        SHEAR_CP = b.comment("CP cost.").defineInRange("cp", 120.0, 0.0, 100000.0);
        SHEAR_OVERLOAD = b.comment("Overload cost.").defineInRange("overload", 35.0, 0.0, 100000.0);
        b.pop();

        SPEC = b.build();
    }

    static float f(ForgeConfigSpec.DoubleValue v) { return v.get().floatValue(); }
    static int ticks(ForgeConfigSpec.DoubleValue seconds) { return (int) Math.round(seconds.get() * 20.0); }
}
