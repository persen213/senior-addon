package com.toaru.senior;

import com.toaru.senior.net.SeniorNet;
import io.github.pinchan4273.reacademycraft.api.AbilityId;
import io.github.pinchan4273.reacademycraft.api.ActivationKind;
import io.github.pinchan4273.reacademycraft.api.CategoryDefinition;
import io.github.pinchan4273.reacademycraft.api.Presentation;
import io.github.pinchan4273.reacademycraft.api.SkillDefinition;
import io.github.pinchan4273.reacademycraft.api.SkillRequirement;
import io.github.pinchan4273.reacademycraft.api.TreePosition;
import io.github.pinchan4273.reacademycraft.api.forge.RegisterAbilitiesEvent;
import java.util.List;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Senior: 11th-Dimensional Manipulation, a Re:AcademyCraft add-on. Phase 3: Foldstep and Spatial Shear. */
@Mod(SeniorMod.MOD_ID)
public final class SeniorMod {
    public static final String MOD_ID = "senior";

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    /** Gives the Senior category when you acquire an ability (registry id senior:senior_factor). */
    public static final RegistryObject<Item> SENIOR_FACTOR =
            ITEMS.register("senior_factor", () -> new Item(new Item.Properties().stacksTo(16)));

    public SeniorMod() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        bus.addListener(this::registerAbilities);
        bus.addListener(this::creativeTab);
        SeniorNet.register();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SeniorConfig.SPEC);
    }

    private void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) event.accept(SENIOR_FACTOR);
    }

    private static SkillDefinition skill(AbilityId id, int level, float x, float y, AbilityId treeParent, List<SkillRequirement> prerequisites) {
        return new SkillDefinition(id, Ids.CATEGORY, level, prerequisites, ActivationKind.INSTANT,
                new Presentation(MOD_ID + ".skill." + id.path(), MOD_ID + ".skill." + id.path() + ".desc", null),
                new TreePosition(x, y, treeParent));
    }

    /** Re:AcademyCraft fires this once during startup. Registration is only possible here. */
    private void registerAbilities(RegisterAbilitiesEvent event) {
        var abilities = event.registrar(MOD_ID);
        // Final structure (all 8 skills, planned): controllable skills per level 2, 2, 2, 1, 1.
        abilities.category(new CategoryDefinition(Ids.CATEGORY, Ids.FACTOR, List.of(2, 2, 2, 1, 1),
                new Presentation(MOD_ID + ".ability.senior", null, null)));

        abilities.skill(skill(Ids.FOLDSTEP, 1, 15, 15, null, List.of()));
        abilities.skill(skill(Ids.SHEAR, 1, 15, 90, null, List.of()));

        var behaviors = event.behaviors(MOD_ID);
        behaviors.behavior(Ids.FOLDSTEP, SeniorSkills::foldstep);
        behaviors.behavior(Ids.SHEAR, SeniorSkills::spatialShear);
    }
}
