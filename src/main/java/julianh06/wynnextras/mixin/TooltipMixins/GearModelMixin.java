package julianh06.wynnextras.mixin.TooltipMixins;

import com.wynntils.models.gear.GearModel;
import com.wynntils.models.gear.type.GearType;
import com.wynntils.models.items.items.game.CraftedGearItem;
import com.wynntils.models.stats.type.StatPossibleValues;
import com.wynntils.models.stats.type.StatType;
import com.wynntils.models.wynnitem.parsing.CraftedItemParseResults;
import com.wynntils.models.wynnitem.parsing.WynnItemParseResult;
import com.wynntils.models.wynnitem.parsing.WynnItemParser;
import com.wynntils.utils.type.CappedValue;
import com.wynntils.utils.type.RangedValue;
import julianh06.wynnextras.features.crafting.CraftingResult;
import julianh06.wynnextras.features.crafting.Reverser;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(value = GearModel.class, remap = false)
public class GearModelMixin {

    // move the values of the crafted at 100% effectiveness from high to base
    // store the items recipe data to possible values
    @Inject(method = "parseCraftedGearItem", at = @At("HEAD"), cancellable = true)
    private static void parseCraftedGearItem(ItemStack itemStack, CallbackInfoReturnable<CraftedGearItem> cir) {

        // We pass this down to the parser, so it can populate it
        // (gears don't have to parse possible values on the fly, since the api provides them)
        Map<StatType, StatPossibleValues> possibleValuesMap = new HashMap<>();
        WynnItemParseResult result = WynnItemParser.parseItemStack(itemStack, possibleValuesMap);

        CraftedItemParseResults craftedResults = WynnItemParser.parseCraftedItem(itemStack);

        if (craftedResults == null) cir.setReturnValue(null);

        // If the item doesn't have an effect strength, it's not a crafted gear item
        if (craftedResults.effectStrength() == -1) cir.setReturnValue(null);

        CappedValue durability = new CappedValue(result.durabilityCurrent(), result.durabilityMax());
        GearType gearType;
        // If it is crafted, and has a skin, then we cannot determine weapon type from item stack
        // Maybe it is possible to find in the string type, e.g. "Crafted Wand"
        gearType = GearType.fromString(result.itemType());
        if (gearType == null && result.requirements().classType().isPresent()) {
            // If the item is signed, we can find the class type from the requirements
            gearType = GearType.fromClassType(result.requirements().classType().get());
        }

        // If we still failed to find the gear type, try to find it from the item stack
        if (gearType == null) {
            gearType = GearType.fromItemStack(itemStack, true);

            if (gearType == null) {
                // If we failed to find the gear type, assume it is a weapon
                gearType = GearType.WEAPON;
            }
        }

        CraftedGearItem target = new CraftedGearItem(
                craftedResults.name(),
                craftedResults.effectStrength(),
                gearType,
                result.attackSpeed(),
                result.health(),
                result.damages(),
                result.defences(),
                result.requirements(),
                possibleValuesMap.values().stream().map(s -> new StatPossibleValues(
                        s.statType(),
                        new RangedValue(0, 0),
                        s.range().high(),
                        s.isPreIdentified()
                )).toList(),
                result.identifications(),
                result.powders(),
                result.powderSlots(),
                result.allRequirementsMet(),
                durability);

        Set<CraftingResult> recipe = new Reverser(target).getRecipe();

        if (recipe.isEmpty()) cir.setReturnValue(target);
        else {
            CraftingResult recipeResult = null;
            for (CraftingResult craftingResult : recipe) {
                if (recipeResult != null) continue;
                recipeResult = craftingResult;
            }

            List<StatPossibleValues> possibleValues = recipeResult.possibleValues();
            List<StatPossibleValues> possibleValuesModified = new ArrayList<>();
            for (StatPossibleValues s : possibleValues) {
                StatPossibleValues sM = new StatPossibleValues(s.statType(), s.range(), possibleValuesMap.get(s.statType()).range().high(), s.isPreIdentified());
                possibleValuesModified.add(sM);
            }

            CraftedGearItem craftedGearItem = new CraftedGearItem(
                    craftedResults.name(),
                    craftedResults.effectStrength(),
                    gearType,
                    result.attackSpeed(),
                    result.health(),
                    result.damages(),
                    result.defences(),
                    result.requirements(),
                    possibleValuesModified,
                    result.identifications(),
                    result.powders(),
                    result.powderSlots(),
                    result.allRequirementsMet(),
                    durability);

            cir.setReturnValue(craftedGearItem);
        }

        cir.cancel();
    }
}
