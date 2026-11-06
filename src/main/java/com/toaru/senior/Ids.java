package com.toaru.senior;

import io.github.pinchan4273.reacademycraft.api.AbilityId;

/** All ability and category ids of this add-on in one place. */
final class Ids {
    private Ids() { }

    static AbilityId id(String path) { return AbilityId.parse(SeniorMod.MOD_ID + ":" + path); }

    static final AbilityId CATEGORY = id("senior");
    static final AbilityId FACTOR = id("senior_factor"); // registry id of the factor item

    static final AbilityId FOLDSTEP = id("foldstep");
    static final AbilityId SHEAR = id("spatial_shear");
}
