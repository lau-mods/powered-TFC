package dev.poweredtfc.poweredtfc.converter;

public final class MechanicalPowerConversion {
    private static final double RADIANS_PER_REVOLUTION = Math.PI * 2.0;
    private static final double TICKS_PER_MINUTE = 1200.0;

    public static float rpmToRadiansPerTick(float rpm) {
        return (float) (rpm * RADIANS_PER_REVOLUTION / TICKS_PER_MINUTE);
    }

    public static float radiansPerTickToRpm(float radiansPerTick) {
        return (float) (radiansPerTick * TICKS_PER_MINUTE / RADIANS_PER_REVOLUTION);
    }

    private MechanicalPowerConversion() {
    }
}
