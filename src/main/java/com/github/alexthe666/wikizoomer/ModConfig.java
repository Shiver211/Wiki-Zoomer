package com.github.alexthe666.wikizoomer;

import net.minecraftforge.common.config.Config;

@Config(modid = "wikizoomer")
public class ModConfig {

    @Config.Comment("If true, batch export will only include EntityLivingBase subclasses (mobs, animals, etc.), excluding items, projectiles, vehicles, and other non-living entities.")
    @Config.Name("Only Living Entities")
    public static boolean onlyLivingEntities = false;
}
