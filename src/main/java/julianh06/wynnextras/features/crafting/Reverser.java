package julianh06.wynnextras.features.crafting;

import com.wynntils.models.elements.type.Skill;
import com.wynntils.models.gear.type.GearRequirements;
import com.wynntils.models.ingredients.type.IngredientInfo;
import com.wynntils.models.items.items.game.CraftedGearItem;
import com.wynntils.models.profession.type.ProfessionType;
import com.wynntils.models.stats.type.StatActualValue;
import com.wynntils.models.stats.type.StatType;
import com.wynntils.utils.type.Pair;
import com.wynntils.utils.type.RangedValue;
import julianh06.wynnextras.features.crafting.data.Constants;
import julianh06.wynnextras.features.crafting.data.CraftableType;
import julianh06.wynnextras.features.crafting.data.IngredientItemMaps;
import julianh06.wynnextras.utils.CraftingUtils;
import net.fabricmc.loader.impl.lib.sat4j.pb.tools.INegator;
import net.minecraft.text.Text;
import org.apache.commons.lang3.tuple.Triple;
import org.joml.Vector2i;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Reverser {

    private final CraftedGearItem target;
    private final CraftableType targetType;
    private final Set<List<IngredientItemMaps>> recipeOptions = new HashSet<>();

    private Recipe mutableRecipe;

    // constants, lMatTier, sMatTier
    private final List<Triple<Constants.RecipeRange, Integer, Integer>> possibleMaterialData = new ArrayList<>();
    private final List<Constants.RecipeRange> possibleConstantData;

    public Reverser(CraftedGearItem target) throws RuntimeException {
        this.target = target;
        this.targetType = CraftableType.fromGearType(target.getGearType());
        if (targetType == null) {
            throw new RuntimeException("CraftableType not found " + target.getGearType());
        } else {
            mutableRecipe = new Recipe(targetType);
            possibleConstantData = Constants.getByLevel(target.getRequirements().level());
        }
    }

    public void getRecipe() {
        // TODO check if we have -skill point reqs and 0 value ids
        List<StatActualValue> targetIdentifications = target.getIdentifications();
        GearRequirements targetRequirements = target.getRequirements();
        Set<Skill> skillPointTypes = targetRequirements.skills().stream().map(Pair::a).collect(Collectors.toSet());
        int targetHealth = target.getHealth();
        Set<StatType> targetStatTypes = targetIdentifications.stream().map(StatActualValue::statType).collect(Collectors.toSet());


        Map<String, IngredientInfo> allIngs = CraftingUtils.getAllIngs();

        Set<IngredientInfo> allValid = new HashSet<>();

        Map<StatType, Set<IngredientInfo>> validStats = new HashMap<>();

        // only used if we have a slot with a multiplier of 0
        Set<IngredientInfo> allDura = new HashSet<>();

        Set<IngredientInfo> validDura = new HashSet<>();

        Map<Skill, Set<IngredientInfo>> validReqs = new HashMap<>();

        Set<IngredientItemMaps> validMetaIngs = new HashSet<>();

        for (IngredientInfo info : allIngs.values()) {
            // filter out ingredient without the used profession
            List<ProfessionType> professions = info.professions();
            if (!professions.contains(targetType.getStation())) continue;

            allDura.add(info);

            if (!info.positionModifiers().isEmpty()) {
                validMetaIngs.add(new IngredientItemMaps(info));
            }

            // filter out ings that contain skill reqs not on the item
            List<Pair<Skill, Integer>> ingReqs = info.skillRequirements();
            if (!ingReqs.isEmpty() && !skillPointTypes.containsAll(ingReqs.stream().map(Pair::a).collect(Collectors.toSet()))) continue;

            // filter out ings that contain ids not on the item
            List<Pair<StatType, RangedValue>> stats = info.variableStats();
            if (!targetStatTypes.containsAll(stats.stream().map(Pair::a).collect(Collectors.toSet()))) continue;

            for (Pair<StatType, RangedValue> stat : stats) {
                Set<IngredientInfo> statSet = validStats.computeIfAbsent(stat.a(), k -> new HashSet<>());
                statSet.add(info);
            }

            for (Pair<Skill, Integer> skillReq : ingReqs) {
                Set<IngredientInfo> reqSet = validReqs.computeIfAbsent(skillReq.a(), k -> new HashSet<>());
                reqSet.add(info);
            }

            if (info.positionModifiers().isEmpty()) allValid.add(info);

            validDura.add(info);
        }

        for (Constants.RecipeRange data : possibleConstantData) {
            mutableRecipe.setConstants(data);
            for (int l = 1; l <= 3; l++) {
                for (int s = 1; s <= 3; s++) {
                    mutableRecipe.setlMaterialTier(l);
                    mutableRecipe.setsMaterialTier(s);
                    Vector2i recipeHealth = mutableRecipe.getHealth();
                    if (recipeHealth.x <= targetHealth && targetHealth <= recipeHealth.y) {
                        possibleMaterialData.add(Triple.of(data, l, s));
                    }
                }
            }
        }

        if (possibleMaterialData.isEmpty()) {
            System.err.println("No Recipe data found!");
            return;
        }

        System.out.println("Found " + possibleMaterialData.size() + " Recipe data found");
        for (Map.Entry<StatType, Set<IngredientInfo>> Stat : validStats.entrySet()) {
            System.out.println("Found " + Stat.getValue().size() + " ings with " + Stat.getKey().getDisplayName());
        }
        for (Map.Entry<Skill, Set<IngredientInfo>> Stat : validReqs.entrySet()) {
            System.out.println("Found " + Stat.getValue().size() + " ings with " + Stat.getKey().getDisplayName());
        }
        System.out.println(validDura.size() + " validDura ings");
        System.out.println(validMetaIngs.size() + " validMetaIngs ings");

        for (Pair<Skill, Integer> req : targetRequirements.skills()) {
            if (req.b() == 0) continue;
            Set<IngredientInfo> ingOptions = validReqs.get(req.a());
            addNext(ingOptions, (existingIng) -> existingIng.getSkillRequirement(req.a()) != 0);
            System.out.println("recipe option is now " + recipeOptions.size() + " after " + req.a().getDisplayName());
        }

        for (StatActualValue id : targetIdentifications) {
            Set<IngredientInfo> ingOptions = validStats.get(id.statType());
            if (id.value() == 0) continue;
            addNext(ingOptions, (existingIng) -> existingIng.getIdentification(id.statType()) != null);
            System.out.println("recipe option is now " + recipeOptions.size() + " after " + id.statType().getDisplayName());
        }

        fillWith(allValid); // TODO contains duplicates?

        List<String> testIngs = new ArrayList<>(List.of("Elephelk Trunk", "Cursed Wings", "Cursed Wings", "Enhanced Behaviour Microchip"));
        Collections.sort(testIngs);
        for (List<IngredientItemMaps> option : recipeOptions) {
            if (option.size() != 4) continue;
            List<String> names = new ArrayList<>();
            for (IngredientItemMaps item : option) {
                String name = item.getName();
                names.add(name);
            }
            Collections.sort(names);
            if (testIngs.equals(names)) {
                System.out.println("found correct " + testIngs);
            }
        }
        for (IngredientItemMaps metaIng : validMetaIngs) {
            if (metaIng.getName().equals("Negative Rafflesia")) System.out.println("found Rafflesia " + metaIng);
            if (metaIng.getName().equals("Decaying Heart")) System.out.println("found Heart " + metaIng);
        }

        //checkAll();

        //System.out.println("recipe option is now " + recipeOptions.size() + " after check");

        //recipe option is now 6246 after filling

        System.out.println("recipe option is now " + recipeOptions.size() + " after filling");
        Set<List<IngredientItemMaps>> allMetaCombinations = new HashSet<>();
        for (List<IngredientItemMaps> recipeOption : recipeOptions) {
            allMetaCombinations.addAll(findAllMetaCombinations(recipeOption, new ArrayList<>(validMetaIngs)));
            //if (recipeOption.size() == 6) System.out.println(recipeOption);
        }

        recipeOptions.clear();
        recipeOptions.addAll(allMetaCombinations);
        System.out.println("recipe option is now " + recipeOptions.size() + " after permuting with meta ings");

        List<CraftingResult> possibleRecipes = new ArrayList<>();
        int duplicateCounter = 0;
        Set<String> seen = new HashSet<>();
        for (List<IngredientItemMaps> ingsList : recipeOptions) {
            String key = getIngListKey(ingsList);
            if (!seen.add(key)) {
                duplicateCounter++;
                continue;
            }

            mutableRecipe.setIngredients(ingsList);
            mutableRecipe.updateMultipliers();


            CraftingResult result = mutableRecipe.craft();
            if (result.equalsIgnoreDurability(target, false)) {
                for (IngredientInfo ing : mutableRecipe.getIngredients()) {
                    System.out.println(ing.name());
                }
                System.out.println();
                possibleRecipes.add(result);
            }

        }

        for (CraftingResult result : possibleRecipes) {
            for (Text line : result.getTooltip()) {
                System.out.println(line.getString());
            }
            System.out.println("\n");
        }

        System.out.println("found " + duplicateCounter + " duplicates");
        System.out.println("found " +  possibleRecipes.size() + " recipe options");
    }

    private void addNext(Set<IngredientInfo> ingOptions, Function<IngredientItemMaps, Boolean> checker) {
        if (recipeOptions.isEmpty()) {
            for (IngredientInfo ing : ingOptions) {
                recipeOptions.add(new ArrayList<>(Collections.singleton(new IngredientItemMaps(ing))));
            }
        } else {
            Set<List<IngredientItemMaps>> toRemove = new HashSet<>();
            Set<List<IngredientItemMaps>> toAdd = new HashSet<>();
            for (List<IngredientItemMaps> currentRecipe : recipeOptions) {
                boolean alreadyContains = false;
                for (IngredientItemMaps existingIng : currentRecipe) {
                    if (checker.apply(existingIng)) {
                        alreadyContains = true;
                        break;
                    }
                }
                if (!alreadyContains) {
                    toRemove.add(currentRecipe);
                    if (currentRecipe.size() < 6) {
                        for (IngredientInfo newIng : ingOptions) {
                            List<IngredientItemMaps> copy = new ArrayList<>(currentRecipe);
                            copy.add(new IngredientItemMaps(newIng));
                            toAdd.add(copy);
                        }
                    }
                }
            }
            recipeOptions.removeAll(toRemove);
            recipeOptions.addAll(toAdd);
        }
    }

    private void fillWith(Set<IngredientInfo> ingOptions) {
        Queue<List<IngredientItemMaps>> queue = new ArrayDeque<>(recipeOptions);
        Set<List<IngredientItemMaps>> results = new HashSet<>();
        Set<String> seen = new HashSet<>();

        while (!queue.isEmpty()) {
            List<IngredientItemMaps> current = queue.poll();

            String key = current.stream()
                    .map(ing -> ing == null ? "null" : ing.getIngredientInfo().name())
                    .sorted()
                    .collect(Collectors.joining(","));
            if (!seen.add(key)) continue;

            if (current.size() == 6) {
                if (!isIncorrect(current)) {
                    results.add(current);
                }
                continue;
            } else if (current.size() > 6) {
                System.err.println("5 + 1 = 7 ???");
                return;
            }

            results.add(current);

            for (IngredientInfo ing : ingOptions) {
                ArrayList<IngredientItemMaps> extended = new ArrayList<>(current);
                extended.add(new IngredientItemMaps(ing));
                queue.add(extended);
            }
        }

        recipeOptions.clear();
        recipeOptions.addAll(results);
    }


    private void checkAll() {
        //IngredientItemMaps bestDura = new IngredientItemMaps(CraftingUtils.getIng("Elephelk Trunk")); // TODO method

        Set<List<IngredientItemMaps>> toRemove = new HashSet<>();
        for (Triple<Constants.RecipeRange, Integer, Integer> possibility : possibleMaterialData) {
            mutableRecipe.setFromTriple(possibility);
            for (List<IngredientItemMaps> currentRecipe : recipeOptions) {
                List<IngredientItemMaps> copy = new ArrayList<>(currentRecipe);

                mutableRecipe.setIngredients(copy);
                CraftingResult craft = mutableRecipe.craft();

                //GearRequirements requirements = craft.requirements();
                RangedValue durability = craft.durability();
                if (durability.high() < target.getDurability().max()) {
                    toRemove.add(currentRecipe);
                    continue;
                }
            }
        }
        recipeOptions.removeAll(toRemove);
    }

    // does not imply the opposite
    private boolean isIncorrect(List<IngredientItemMaps> ingredients) {
        Map<StatType, RangedValue> totalIds = new HashMap<>();

        for (IngredientItemMaps ing : ingredients) {
            // instantly return that it might be correct if it has ing effectiveness this is just a quick scan
            if (!ing.getIngredientInfo().positionModifiers().isEmpty()) return false;
            for (Pair<StatType, RangedValue> id : ing.getIngredientInfo().variableStats()) {
                RangedValue existing = totalIds.get(id.a());
                if (existing == null) {
                    totalIds.put(id.a(), id.b());
                } else {
                    RangedValue newValue = CraftingUtils.addRanges(existing, id.b());
                    totalIds.put(id.a(), newValue);
                }
            }
        }

        for (StatActualValue targetId : target.getIdentifications()) {
            RangedValue givenId = totalIds.get(targetId.statType());
            if (givenId == null) return true;
            if (!givenId.inRange(targetId.value())) return true;
        }

        return false;
    }

    //TODO change it so that it gets the ing with the highest high for each stat
    // and then finds the multiplier to reach target
    // and then condenses so that if a single ing has the highest value for multiple stats its multiplier is only counted once
    // and then have variables for the highest multiplier and the total multiplier (after condencing)

    // TODO flip for negative
    private boolean shouldTrim(List<IngredientItemMaps> ingredients) {
        // map stattype -> best ingredient for that stat (highest high value)
        Map<StatType, Pair<IngredientItemMaps, Double>> bestIngForStat = new HashMap<>();

        for (IngredientItemMaps ing : ingredients) {
            if (ing == null) continue;

            for (Pair<StatType, RangedValue> stat : ing.getIngredientInfo().variableStats()) {
                StatType statType = stat.a();
                double currentHigh = stat.b().high();

                // Check if this ingredient is better for this stat
                bestIngForStat.merge(
                        statType,
                        new Pair<>(ing, currentHigh),
                        (existing, candidate) -> candidate.b() > existing.b() ? candidate : existing
                );
            }
        }

        // calculate multiplier for each stat based on its best ingredient
        Map<StatType, Double> statMultipliers = new HashMap<>();
        Map<IngredientItemMaps, List<StatType>> ingToStats = new HashMap<>();

        for (StatActualValue targetId : target.getIdentifications()) {
            StatType statType = targetId.statType();
            double targetValue = targetId.value();

            Pair<IngredientItemMaps, Double> bestPair = bestIngForStat.get(statType);
            if (bestPair == null) return true;

            IngredientItemMaps bestIng = bestPair.a();
            double bestHigh = bestPair.b();

            if (bestHigh <= 0) return true;

            // multiplier needed for this stat
            double multiplier = targetValue / bestHigh;
            statMultipliers.put(statType, multiplier);

            // track which stats each ingredient is best for
            ingToStats.computeIfAbsent(bestIng, k -> new ArrayList<>()).add(statType);
        }

        // for each ingredient take the max multiplier of all stats its best for
        for (Map.Entry<IngredientItemMaps, List<StatType>> entry : ingToStats.entrySet()) {
            IngredientItemMaps ing = entry.getKey();
            List<StatType> stats = entry.getValue();

            OptionalDouble maxMultiplier = stats.stream()
                    .mapToDouble(statMultipliers::get)
                    .max();

            double totalWeight = stats.stream()
                    .mapToDouble(stat -> {
                        // Normalize by how much this stat contributes vs others
                        double statValue = ing.getIdentification(stat).high();
                        double allValuesSum = stats.stream()
                                .mapToDouble(s -> ing.getIdentification(s).high())
                                .sum();
                        return statValue / allValuesSum;
                    })
                    .sum();

        }

        //TODO empty slots and count of max for stats

        return false;
    }

    private Set<List<IngredientItemMaps>> findAllMetaCombinations(List<IngredientItemMaps> ingredients, List<IngredientItemMaps> validMetaIngs) {
        List<IngredientItemMaps> paddedIngredients = new ArrayList<>(ingredients);
        if (paddedIngredients.size() >= 6) return Collections.singleton(paddedIngredients);
        for (int i = paddedIngredients.size(); i < 6; i++) {
            paddedIngredients.add(null);
        }
        if (paddedIngredients.size() != 6) {
            System.err.println("i messed up here");
            return Collections.emptySet();
        }

        Set<List<IngredientItemMaps>> allOrdersForProvided = new HashSet<>();
        generateCombinations(paddedIngredients, false, true, 0, 6, new ArrayList<>(), allOrdersForProvided, new HashSet<>(), Reverser::getIngListKey, new HashSet<>());

        Set<List<IngredientItemMaps>> results = new HashSet<>();

        Map<Integer, Set<List<IngredientItemMaps>>> metaCombinations = new HashMap<>();
        for (List<IngredientItemMaps> ordered : allOrdersForProvided) {
            int nullCount = (int) (6 - ordered.stream().filter(Objects::nonNull).count());

            Set<List<IngredientItemMaps>> metaCombination = metaCombinations.computeIfAbsent(
                    nullCount,
                    k -> {
                        Set<List<IngredientItemMaps>> result = new HashSet<>();
                        generateCombinations(validMetaIngs, true, true, 0, k, new ArrayList<>(), result, new HashSet<>(), Reverser::getIngListKey, new HashSet<>());
                        return result;
                    }
            );

            for (List<IngredientItemMaps> metaIngs : metaCombination) {
                if (metaIngs.size() != nullCount){
                    System.out.println("i messed up down here");
                    return  Collections.emptySet();
                }

                List<IngredientItemMaps> result = new ArrayList<>(ordered);
                Iterator<IngredientItemMaps> fillerIter = metaIngs.iterator();

                for (int i = 0; i < result.size(); i++) {
                    if (result.get(i) == null && fillerIter.hasNext()) {
                        result.set(i, fillerIter.next());
                    }
                }

                if (result.size() != 6) {
                    System.err.println("i messed up here at the end");
                    return Collections.emptySet();
                }

                results.add(result);
            }
        }

        return results;
    }

    private static <T> void generateCombinations(
            List<T> ings,
            boolean duplicates,
            boolean permute,
            int startIdx,
            int slots,
            List<T> current,
            Set<List<T>> result,
            Set<Integer> usedIndices,
            Function<List<T>, String> toKey,
            Set<String> seen
    ) {
        String key = toKey.apply(current);
        if (!seen.add(key)) return;

        if (current.size() == slots) {
            result.add(new ArrayList<>(current));
            return;
        }

        int start = permute ? 0 : startIdx;
        for (int i = start; i < ings.size(); i++) {
            if (!duplicates && usedIndices.contains(i)) {
                continue;
            }

            current.add(ings.get(i));
            usedIndices.add(i);
            int newStart = permute ? 0 : duplicates ? i : i + 1;
            generateCombinations(ings, duplicates, permute, newStart, slots, current, result, usedIndices, toKey, seen);
            current.removeLast();
            usedIndices.remove(i);
        }
    }

    private static String getIngListKey(List<IngredientItemMaps> recipe) {
        return recipe.stream()
                .map(ing -> ing == null ? "null" : ing.getIngredientInfo().name())
                .collect(Collectors.joining(","));
    }
}
