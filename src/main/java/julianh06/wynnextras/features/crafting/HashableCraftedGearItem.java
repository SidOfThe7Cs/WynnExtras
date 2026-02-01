package julianh06.wynnextras.features.crafting;

import com.wynntils.models.elements.type.Element;
import com.wynntils.models.elements.type.Powder;
import com.wynntils.models.gear.type.GearAttackSpeed;
import com.wynntils.models.gear.type.GearRequirements;
import com.wynntils.models.gear.type.GearType;
import com.wynntils.models.items.items.game.CraftedGearItem;
import com.wynntils.models.stats.type.DamageType;
import com.wynntils.models.stats.type.StatActualValue;
import com.wynntils.models.stats.type.StatPossibleValues;
import com.wynntils.utils.type.CappedValue;
import com.wynntils.utils.type.Pair;
import com.wynntils.utils.type.RangedValue;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class HashableCraftedGearItem extends CraftedGearItem {

    private HashableCraftedGearItem(String name, int effectStrength, GearType gearType, GearAttackSpeed attackSpeed, int health, List<Pair<DamageType, RangedValue>> damages, List<Pair<Element, Integer>> defences, GearRequirements requirements, List<StatPossibleValues> possibleValues, List<StatActualValue> identifications, List<Powder> powders, int powderSlots, boolean requirementsMet, CappedValue durability) {
        super(name, effectStrength, gearType, attackSpeed, health, damages, defences, requirements, possibleValues, identifications, powders, powderSlots, requirementsMet, durability);
    }

    public static HashableCraftedGearItem from(CraftedGearItem gearItem) {
        return new HashableCraftedGearItem(
                gearItem.getName(),
                gearItem.getEffectStrength(),
                gearItem.getGearType(),
                gearItem.getAttackSpeed().orElse(null),
                gearItem.getHealth(),
                gearItem.getDamages(),
                gearItem.getDefences(),
                gearItem.getRequirements(),
                gearItem.getPossibleValues(),
                gearItem.getIdentifications(),
                gearItem.getPowders(),
                gearItem.getPowderSlots(),
                gearItem.meetsActualRequirements(),
                gearItem.getDurability()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HashableCraftedGearItem that = (HashableCraftedGearItem) o;

        return getGearType() == that.getGearType() &&
                getHealth() == that.getHealth() &&
                Objects.equals(getDamages(), that.getDamages()) &&
                Objects.equals(getRequirements(), that.getRequirements()) &&
                Arrays.equals(
                        getPossibleValues().stream().map(StatPossibleValues::baseValue).toArray(),
                        that.getPossibleValues().stream().map(StatPossibleValues::baseValue).toArray()
                ) &&
                getPowderSlots() == that.getPowderSlots() &&
                getDurability().max() == that.getDurability().max();
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getGearType(),
                getHealth(),
                getDamages(),
                getRequirements(),
                Arrays.hashCode(getPossibleValues().stream().map(StatPossibleValues::baseValue).toArray()),
                getPowderSlots(),
                getDurability().max()
        );
    }
}
