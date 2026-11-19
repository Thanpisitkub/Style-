package dev.fiststyle;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client-side remembered state of the two toggles (the server only keeps the live copy). */
public final class StyleConfig {
    private StyleConfig() {}

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue AUTO_PARRY;
    public static final ForgeConfigSpec.BooleanValue AUTO_DODGE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        AUTO_PARRY = builder.comment("Automatically parry incoming melee attacks").define("autoParry", false);
        AUTO_DODGE = builder.comment("Automatically dodge incoming attacks").define("autoDodge", false);
        SPEC = builder.build();
    }
}
