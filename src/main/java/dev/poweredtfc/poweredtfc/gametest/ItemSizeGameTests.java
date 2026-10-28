package dev.poweredtfc.poweredtfc.gametest;

import java.util.List;

import dev.poweredtfc.poweredtfc.PoweredTFC;
import dev.poweredtfc.poweredtfc.registry.PoweredTFCRegistry;
import net.dries007.tfc.common.component.mold.Vessel;
import net.dries007.tfc.common.component.size.ItemSizeManager;
import net.dries007.tfc.common.component.size.Size;
import net.dries007.tfc.common.component.size.Weight;
import net.dries007.tfc.common.items.TFCItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(PoweredTFC.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ItemSizeGameTests {
    private static final List<Classification> CLASSIFICATIONS = List.of(
        new Classification("huge_heavy", Size.HUGE, Weight.HEAVY),
        new Classification("large_heavy", Size.LARGE, Weight.HEAVY),
        new Classification("large_light", Size.LARGE, Weight.LIGHT),
        new Classification("large_medium", Size.LARGE, Weight.MEDIUM),
        new Classification("large_very_heavy", Size.LARGE, Weight.VERY_HEAVY),
        new Classification("large_very_light", Size.LARGE, Weight.VERY_LIGHT),
        new Classification("normal_light", Size.NORMAL, Weight.LIGHT),
        new Classification("normal_medium", Size.NORMAL, Weight.MEDIUM),
        new Classification("normal_very_heavy", Size.NORMAL, Weight.VERY_HEAVY),
        new Classification("small_light", Size.SMALL, Weight.LIGHT),
        new Classification("small_medium", Size.SMALL, Weight.MEDIUM),
        new Classification("small_very_light", Size.SMALL, Weight.VERY_LIGHT),
        new Classification("tiny_light", Size.TINY, Weight.LIGHT),
        new Classification("tiny_very_light", Size.TINY, Weight.VERY_LIGHT),
        new Classification("very_large_heavy", Size.VERY_LARGE, Weight.HEAVY),
        new Classification("very_large_medium", Size.VERY_LARGE, Weight.MEDIUM),
        new Classification("very_large_very_heavy", Size.VERY_LARGE, Weight.VERY_HEAVY),
        new Classification("very_small_medium", Size.VERY_SMALL, Weight.MEDIUM),
        new Classification("very_small_very_light", Size.VERY_SMALL, Weight.VERY_LIGHT)
    );

    @GameTest(template = "empty", templateNamespace = "minecraft", batch = PoweredTFC.MOD_ID)
    public static void allCreateItemsAndConvertersHaveOneClassification(GameTestHelper helper) {
        int createItems = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id.getNamespace().equals("create")) {
                createItems++;
                assertOneClassification(helper, item, id);
            }
        }

        helper.assertTrue(createItems == 699,
            "Create 6.0.10 must register exactly 699 items, got " + createItems);
        assertOneClassification(helper, PoweredTFCRegistry.CREATE_TO_TFC_CONVERTER_ITEM.get(),
            ResourceLocation.fromNamespaceAndPath(PoweredTFC.MOD_ID, "create_to_tfc_converter"));
        assertOneClassification(helper, PoweredTFCRegistry.TFC_TO_CREATE_CONVERTER_ITEM.get(),
            ResourceLocation.fromNamespaceAndPath(PoweredTFC.MOD_ID, "tfc_to_create_converter"));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "minecraft", batch = PoweredTFC.MOD_ID)
    public static void classificationsControlTfcSizeWeightAndVesselStorage(GameTestHelper helper) {
        ItemStack flour = stack("create:wheat_flour");
        ItemStack basin = stack("create:basin");
        var flourSize = ItemSizeManager.get(flour);
        var basinSize = ItemSizeManager.get(basin);

        helper.assertTrue(flourSize.getSize(flour) == Size.TINY,
            "Wheat flour must use the tiny classification");
        helper.assertTrue(flourSize.getWeight(flour) == Weight.VERY_LIGHT,
            "Wheat flour must use the very light classification");
        helper.assertTrue(basinSize.getSize(basin) == Size.HUGE,
            "A Basin must use the huge classification");
        helper.assertTrue(basinSize.getWeight(basin) == Weight.HEAVY,
            "A Basin must use the heavy classification");

        Vessel flourVessel = Vessel.get(new ItemStack(TFCItems.VESSEL.asItem()));
        Vessel basinVessel = Vessel.get(new ItemStack(TFCItems.VESSEL.asItem()));
        helper.assertTrue(flourVessel != null && flourVessel.insertItem(0, flour.copy(), false).isEmpty(),
            "A small TFC Vessel must store the tiny Create item");
        ItemStack extractedFlour = flourVessel.extractItem(0, 1, false);
        helper.assertTrue(extractedFlour.is(flour.getItem()) && extractedFlour.getCount() == 1,
            "The stored tiny Create item must be removable from a small TFC Vessel");
        helper.assertTrue(flourVessel.getStackInSlot(0).isEmpty(),
            "The TFC Vessel slot must be empty after removing the tiny Create item");
        helper.assertTrue(basinVessel != null && basinVessel.insertItem(0, basin.copy(), false).getCount() == 1,
            "A small TFC Vessel must refuse the huge Create item");
        helper.assertTrue(basinVessel.getStackInSlot(0).isEmpty(),
            "A refused huge Create item must not occupy the TFC Vessel slot");
        helper.succeed();
    }

    private static void assertOneClassification(GameTestHelper helper, Item item, ResourceLocation id) {
        Classification match = null;
        for (Classification classification : CLASSIFICATIONS) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(
                PoweredTFC.MOD_ID, "item_size/create_" + classification.name()));
            if (item.builtInRegistryHolder().is(tag)) {
                helper.assertTrue(match == null,
                    id + " must have exactly one size and weight classification");
                match = classification;
            }
        }
        helper.assertTrue(match != null, id + " must have exactly one size and weight classification, got 0");

        ItemStack stack = new ItemStack(item);
        var itemSize = ItemSizeManager.get(stack);
        helper.assertTrue(itemSize.getSize(stack) == match.size(),
            id + " must resolve TFC size " + match.size() + ", got " + itemSize.getSize(stack));
        helper.assertTrue(itemSize.getWeight(stack) == match.weight(),
            id + " must resolve TFC weight " + match.weight() + ", got " + itemSize.getWeight(stack));
    }

    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private record Classification(String name, Size size, Weight weight) {
    }

    private ItemSizeGameTests() {
    }
}
