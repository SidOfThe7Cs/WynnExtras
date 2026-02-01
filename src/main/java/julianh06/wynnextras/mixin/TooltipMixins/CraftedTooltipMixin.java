package julianh06.wynnextras.mixin.TooltipMixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.wynntils.core.WynntilsMod;
import com.wynntils.handlers.tooltip.impl.crafted.CraftedTooltipIdentifications;
import com.wynntils.handlers.tooltip.type.TooltipIdentificationDecorator;
import com.wynntils.handlers.tooltip.type.TooltipStyle;
import com.wynntils.models.character.type.ClassType;
import com.wynntils.models.items.properties.CraftedItemProperty;
import com.wynntils.models.stats.type.StatActualValue;
import com.wynntils.models.stats.type.StatPossibleValues;
import com.wynntils.models.stats.type.StatType;
import com.wynntils.utils.type.RangedValue;
import julianh06.wynnextras.utils.CraftingUtils;
import net.minecraft.text.MutableText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CraftedTooltipIdentifications.class, remap = false)
public class CraftedTooltipMixin {

    // add decorator as needed
    @Inject(method = "getStatLine", at = @At("RETURN"), cancellable = true)
    private static void getStatLine(StatType statType,
                                    CraftedItemProperty craftedItem,
                                    ClassType currentClass,
                                    TooltipStyle style,
                                    CallbackInfoReturnable<MutableText> cir,
                                    @Local(name = "statActualValue") StatActualValue statActualValue
    ) {
        MutableText line = cir.getReturnValue();

        StatPossibleValues possibleValues = craftedItem.getPossibleValues().stream()
                .filter(stat -> stat.statType() == statType)
                .findFirst()
                .orElse(null);

        if (possibleValues == null) {
            WynntilsMod.warn("(crafted) Missing stat type in item " + craftedItem.getName() + " for stat: " + statType
                    + " which has value: " + statActualValue.value());
            cir.setReturnValue(line);
            return;
        }

        if (possibleValues.range().isFixed()) {
            cir.setReturnValue(line);
            return;
        }

        // Append decorations
        TooltipIdentificationDecorator decorator = CraftingUtils.getDecorator();
        // the "actual" value includes changes from durability dmg we have stored the value at 100% dura in baseValue
        if (decorator != null) {
            StatActualValue valueAtFullDura = new StatActualValue(possibleValues.statType(), possibleValues.baseValue(), 0, new RangedValue(0, 0));
            line.append(decorator.getSuffix(valueAtFullDura, possibleValues, style));
        }

        cir.setReturnValue(line);
    }
}
