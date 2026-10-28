package net.claustra01.poweredtfc.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class PoweredTFCConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue SMOULDERING_HEAT;
    public static final ModConfigSpec.IntValue FADING_HEAT;
    public static final ModConfigSpec.IntValue KINDLED_HEAT;
    public static final ModConfigSpec.IntValue SEETHING_HEAT;
    public static final ModConfigSpec.IntValue BASE_AIR;
    public static final ModConfigSpec.DoubleValue SPEED_MULTIPLIER;
    public static final ModConfigSpec.IntValue CREATE_TO_TFC_STRESS_IMPACT;
    public static final ModConfigSpec.IntValue TFC_TO_CREATE_STRESS_CAPACITY;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("blaze_burner");
        SMOULDERING_HEAT = builder.defineInRange("smoulderingHeat", 80, 0, 10000);
        FADING_HEAT = builder.defineInRange("fadingHeat", 1100, 0, 10000);
        KINDLED_HEAT = builder.defineInRange("kindledHeat", 1300, 0, 10000);
        SEETHING_HEAT = builder.defineInRange("seethingHeat", 1800, 0, 10000);
        builder.pop();
        builder.push("encased_fan");
        BASE_AIR = builder.defineInRange("baseAir", 100, 0, 10000);
        SPEED_MULTIPLIER = builder.defineInRange("speedMultiplier", 1.0, 0.0, 100.0);
        builder.pop();
        builder.push("mechanical_converters");
        CREATE_TO_TFC_STRESS_IMPACT = builder.defineInRange("createToTfcStressImpact", 64, 0, 16384);
        TFC_TO_CREATE_STRESS_CAPACITY = builder.defineInRange("tfcToCreateStressCapacity", 64, 0, 16384);
        builder.pop();
        SPEC = builder.build();
    }

    private PoweredTFCConfig() {
    }
}
