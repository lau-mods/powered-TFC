package net.claustra01.poweredtfc.gametest;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipeBuilder;
import net.claustra01.poweredtfc.PoweredTFC;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import net.dries007.tfc.common.component.TFCComponents;
import net.dries007.tfc.common.component.heat.HeatCapability;
import net.dries007.tfc.util.calendar.CalendarTransaction;
import net.dries007.tfc.util.calendar.Calendars;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PoweredTFC.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SequencedAssemblyGameTests {
    private static final float MINIMUM_TEMPERATURE = 921.00006f;

    @GameTest(template = "empty", templateNamespace = "minecraft", batch = PoweredTFC.MOD_ID)
    public static void temperatureGate(GameTestHelper helper) {
        for (String inputId : new String[] {"tfc:raw_iron_bloom", "tfc:refined_iron_bloom"}) {
            ItemStack hot = stack(inputId);
            HeatCapability.setTemperature(hot, MINIMUM_TEMPERATURE);
            helper.assertTrue(findPressing(helper, hot).isPresent(),
                inputId + " at the minimum temperature must start its assembly recipe");
            ItemStack cold = stack(inputId);
            HeatCapability.setTemperature(cold, 920);
            helper.assertTrue(findPressing(helper, cold).isEmpty(),
                inputId + " at 920 degrees must not start its assembly recipe");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft", batch = PoweredTFC.MOD_ID)
    public static void rawBloomRecipe(GameTestHelper helper) {
        assertRecipeShape(helper, "raw_iron_bloom", "tfc:refined_iron_bloom");
        ItemStack result = complete(helper, "tfc:raw_iron_bloom");
        helper.assertTrue(result.is(stack("tfc:refined_iron_bloom").getItem())
                && result.getCount() == 1,
            "Nine pressing operations must produce one refined iron bloom");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft", batch = PoweredTFC.MOD_ID)
    public static void wroughtIronRecipe(GameTestHelper helper) {
        assertRecipeShape(helper, "refined_iron_bloom", "tfc:metal/ingot/wrought_iron");
        ItemStack result = complete(helper, "tfc:refined_iron_bloom");
        helper.assertTrue(result.is(stack("tfc:metal/ingot/wrought_iron").getItem())
                && result.getCount() == 1,
            "Nine pressing operations must produce one wrought iron ingot");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft", batch = PoweredTFC.MOD_ID)
    public static void intermediateCoolsContinuesAndReloads(GameTestHelper helper) {
        ItemStack input = stack("tfc:raw_iron_bloom");
        HeatCapability.setTemperature(input, 1000);
        ItemStack intermediate = press(helper, input);
        float heatCapacity = HeatCapability.view(intermediate).getHeatCapacity();
        ItemStack loaded = ItemStack.parse(helper.getLevel().registryAccess(),
            intermediate.save(helper.getLevel().registryAccess())).orElseThrow();
        helper.assertTrue(loaded.has(AllDataComponents.SEQUENCED_ASSEMBLY),
            "The assembly state must survive an item save and load");
        helper.assertTrue(HeatCapability.has(loaded)
                && HeatCapability.view(loaded).getHeatCapacity() == heatCapacity,
            "The intermediate heat and heat capacity must survive an item save and load");
        helper.assertTrue(Math.abs(HeatCapability.getTemperature(loaded)
                - HeatCapability.getTemperature(intermediate)) < 1,
            "The intermediate temperature must survive an item save and load");

        try (CalendarTransaction transaction = Calendars.SERVER.transaction()) {
            transaction.add(24000);
            float cooledTemperature = HeatCapability.getTemperature(loaded);
            helper.assertTrue(cooledTemperature < MINIMUM_TEMPERATURE,
                "The intermediate item must cool below the starting temperature over time");
            ItemStack advanced = press(helper, loaded);
            helper.assertTrue(advanced.has(AllDataComponents.SEQUENCED_ASSEMBLY),
                "Cooling below the start temperature must not interrupt an assembly in progress");
            helper.assertTrue(Math.abs(HeatCapability.getTemperature(advanced) - cooledTemperature) < 1,
                "The advanced intermediate must retain its cooled temperature");
            helper.assertTrue(HeatCapability.view(advanced).getHeatCapacity() == heatCapacity,
                "The advanced intermediate must retain the starting heat capacity");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft", batch = PoweredTFC.MOD_ID)
    public static void completionCarriesCurrentTemperature(GameTestHelper helper) {
        ItemStack current = stack("tfc:raw_iron_bloom");
        HeatCapability.setTemperature(current, 1000);
        for (int step = 0; step < 8; step++) {
            current = press(helper, current);
        }
        HeatCapability.setTemperature(current, 700);
        ItemStack result = press(helper, current);
        helper.assertTrue(result.is(stack("tfc:refined_iron_bloom").getItem()),
            "The final pressing operation must produce the heatable recipe result");
        helper.assertTrue(Math.abs(HeatCapability.getTemperature(result) - 700) < 1,
            "The result must inherit the intermediate temperature at completion");
        helper.assertTrue(HeatCapability.view(result).getHeatCapacity()
                == HeatCapability.getDefinition(result).heatCapacity(),
            "The result must use its own heat definition after completion");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft", batch = PoweredTFC.MOD_ID)
    public static void nonHeatableResultHasNoHeat(GameTestHelper helper) {
        helper.assertTrue(HeatCapability.getDefinition(new ItemStack(Items.DIAMOND)) == null,
            "The test result must not have a TFC heat definition");
        RecipeManager manager = helper.getLevel().getRecipeManager();
        Collection<RecipeHolder<?>> recipes = manager.getRecipes();
        RecipeHolder<SequencedAssemblyRecipe> testRecipe = new SequencedAssemblyRecipeBuilder(
            ResourceLocation.fromNamespaceAndPath(PoweredTFC.MOD_ID, "non_heatable_result_test"))
            .require(stack("tfc:metal/ingot/wrought_iron").getItem())
            .transitionTo(stack("tfc:metal/ingot/wrought_iron").getItem())
            .addOutput(Items.DIAMOND, 1)
            .loops(1)
            .addStep(PressingRecipe::new, builder -> builder)
            .addStep(PressingRecipe::new, builder -> builder)
            .addStep(PressingRecipe::new, builder -> builder)
            .build();
        ArrayList<RecipeHolder<?>> recipesWithTest = new ArrayList<>(recipes);
        recipesWithTest.add(testRecipe);
        manager.replaceRecipes(recipesWithTest);
        try {
            ItemStack result = stack("tfc:metal/ingot/wrought_iron");
            HeatCapability.setTemperature(result, 1000);
            for (int step = 0; step < 3; step++) {
                result = press(helper, result);
            }
            helper.assertTrue(result.is(Items.DIAMOND),
                "The completed assembly recipe must produce its non-heatable result");
            helper.assertTrue(!result.has(TFCComponents.HEAT),
                "A non-heatable result must not receive an invalid heat component");
        } finally {
            manager.replaceRecipes(recipes);
        }
        helper.succeed();
    }

    private static ItemStack complete(GameTestHelper helper, String inputId) {
        ItemStack current = stack(inputId);
        HeatCapability.setTemperature(current, 1000);
        for (int step = 0; step < 9; step++) {
            current = press(helper, current);
            if (step < 8) {
                helper.assertTrue(current.has(AllDataComponents.SEQUENCED_ASSEMBLY)
                        && HeatCapability.has(current),
                    "Each of the first eight operations must produce a heated assembly intermediate");
            }
        }
        return current;
    }

    private static ItemStack press(GameTestHelper helper, ItemStack input) {
        Optional<RecipeHolder<PressingRecipe>> recipe = findPressing(helper, input);
        helper.assertTrue(recipe.isPresent(), "A pressing step must be available for " + input);
        return recipe.orElseThrow().value().rollResults(helper.getLevel().random).getFirst();
    }

    private static Optional<RecipeHolder<PressingRecipe>> findPressing(GameTestHelper helper,
        ItemStack input) {
        return SequencedAssemblyRecipe.getRecipe(helper.getLevel(), input,
            AllRecipeTypes.PRESSING.getType(), PressingRecipe.class);
    }

    private static void assertRecipeShape(GameTestHelper helper, String recipePath, String resultId) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(PoweredTFC.MOD_ID,
            "sequenced_assembly/" + recipePath);
        Optional<RecipeHolder<?>> holder = helper.getLevel().getRecipeManager().byKey(id);
        helper.assertTrue(holder.isPresent()
                && holder.orElseThrow().value() instanceof SequencedAssemblyRecipe,
            id + " must load as a sequenced assembly recipe");
        SequencedAssemblyRecipe recipe = (SequencedAssemblyRecipe) holder.orElseThrow().value();
        helper.assertTrue(recipe.getLoops() == 3 && recipe.getSequence().size() == 3,
            id + " must contain three pressing steps repeated for three loops");
        helper.assertTrue(recipe.getResultItem(helper.getLevel().registryAccess())
                .is(stack(resultId).getItem()),
            id + " must have the required result");
    }

    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private SequencedAssemblyGameTests() {
    }
}
