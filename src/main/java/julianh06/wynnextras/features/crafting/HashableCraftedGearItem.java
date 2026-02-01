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
import com.wynntils.models.stats.type.StatType;
import com.wynntils.utils.type.CappedValue;
import com.wynntils.utils.type.Pair;
import com.wynntils.utils.type.RangedValue;

import java.util.*;
import java.util.stream.Collectors;

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

        Set<Pair<StatType, Integer>> thisPossibleSet = getPossibleValues().stream().map(v -> new Pair<>(v.statType(), v.baseValue())).collect(Collectors.toSet());
        Set<Pair<StatType, Integer>> thatPossibleSet = that.getPossibleValues().stream().map(v -> new Pair<>(v.statType(), v.baseValue())).collect(Collectors.toSet());

        return getGearType() == that.getGearType() &&
                getHealth() == that.getHealth() &&
                Objects.equals(getDamages(), that.getDamages()) &&
                Objects.equals(getRequirements(), that.getRequirements()) &&
                Objects.equals(thisPossibleSet, thatPossibleSet) &&
                getPowderSlots() == that.getPowderSlots() &&
                getDurability().max() == that.getDurability().max();
    }

    @Override
    public int hashCode() {
        Set<Pair<StatType, Integer>> thisPossibleSet = getPossibleValues().stream().map(v -> new Pair<>(v.statType(), v.baseValue())).collect(Collectors.toSet());
        return Objects.hash(
                getGearType(),
                getHealth(),
                getDamages(),
                getRequirements(),
                thisPossibleSet,
                getPowderSlots(),
                getDurability().max()
        );
    }
}
