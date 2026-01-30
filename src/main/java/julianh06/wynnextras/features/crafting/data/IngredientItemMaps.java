package julianh06.wynnextras.features.crafting.data;

import com.wynntils.models.elements.type.Skill;
import com.wynntils.models.ingredients.type.IngredientInfo;
import com.wynntils.models.items.items.game.IngredientItem;
import com.wynntils.models.stats.type.StatType;
import com.wynntils.utils.type.Pair;
import com.wynntils.utils.type.RangedValue;

import java.util.Map;
import java.util.stream.Collectors;

public class IngredientItemMaps extends IngredientItem {

    private final Map<Skill, Integer> skillRequirementsMap;
    private final Map<StatType, RangedValue> idsMap;

    public IngredientItemMaps(IngredientInfo ingredientInfo) {
        super(ingredientInfo);
        skillRequirementsMap = ingredientInfo.skillRequirements().stream()
                .collect(Collectors.toMap(Pair::a, Pair::b));
        idsMap = ingredientInfo.variableStats().stream()
                .collect(Collectors.toMap(Pair::a, Pair::b));
    }

    public Integer getSkillRequirement(Skill skill) {
        return skillRequirementsMap.getOrDefault(skill, 0);
    }

    public RangedValue getIdentification(StatType stat) {
        return idsMap.get(stat);
    }

    @Override
    public String toString() {
        return this.getName();
    }
}
