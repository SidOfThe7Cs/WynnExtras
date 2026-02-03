package julianh06.wynnextras.features.crafting;

import com.wynntils.models.elements.type.Skill;
import com.wynntils.models.gear.type.GearRequirements;
import com.wynntils.models.ingredients.type.IngredientInfo;
import com.wynntils.models.items.items.game.CraftedGearItem;
import com.wynntils.models.profession.type.ProfessionType;
import com.wynntils.models.stats.type.StatPossibleValues;
import com.wynntils.models.stats.type.StatType;
import com.wynntils.utils.type.Pair;
import com.wynntils.utils.type.RangedValue;
import julianh06.wynnextras.features.crafting.data.CraftableType;
import julianh06.wynnextras.features.crafting.data.IngredientItemMaps;
import julianh06.wynnextras.features.crafting.data.recipes.RecipeLoader;
import julianh06.wynnextras.utils.CraftingUtils;
import org.joml.Vector2i;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Reverser {
    private static final Map<HashableCraftedGearItem, Set<CraftingResult>> knownRecipes = new HashMap<>();

    private final CraftedGearItem target;
    private final CraftableType targetType;
    private Set<List<IngredientItemMaps>> recipeOptions = new HashSet<>();
    private final HashableCraftedGearItem hashable;

    private final Recipe mutableRecipe;

    private final List<Pair<RecipeLoader.RecipeData, Recipe.Materials>> possibleMaterialData = new ArrayList<>();
    private final Set<RecipeLoader.RecipeData> possibleConstantData;

    public Reverser(CraftedGearItem target) throws RuntimeException {
        this.target = target;
        this.hashable = HashableCraftedGearItem.from(target);
        this.targetType = CraftableType.fromGearType(target.getGearType());
        if (targetType == null) {
            throw new RuntimeException("CraftableType not found " + target.getGearType());
        } else {
            mutableRecipe = new Recipe(targetType);
            possibleConstantData = RecipeLoader.getPossibleRecipes(targetType, target.getRequirements().level());
        }
    }

    public Set<CraftingResult> getKnownRecipe() {
        return knownRecipes.get(hashable);
    }

    public Set<CraftingResult> getRecipe() {
        Set<CraftingResult> knowResult = knownRecipes.get(hashable);
        if (knowResult != null) return knowResult;

        //if (!target.getName().contains("Helmet")) return Collections.emptySet(); // TODO remove

        // TODO check if we have -skill point reqs and 0 value ids (we have -req but not 0value ids) what about 0 reqs?
        List<StatPossibleValues> targetPossibleValues = target.getPossibleValues();
        GearRequirements targetRequirements = target.getRequirements();
        Set<Skill> skillPointTypes = targetRequirements.skills().stream().map(Pair::a).collect(Collectors.toSet());
        int targetHealth = target.getHealth();
        Set<StatType> targetStatTypes = targetPossibleValues.stream().map(StatPossibleValues::statType).collect(Collectors.toSet());


        Map<String, IngredientInfo> allIngs = CraftingUtils.getAllIngs();

        Set<IngredientInfo> allValid = new HashSet<>();

        Map<StatType, Set<IngredientInfo>> validStats = new HashMap<>();

        // only used if we have a slot with a multiplier of 0
        Set<IngredientInfo> allDura = new HashSet<>();

        Set<IngredientInfo> validDura = new HashSet<>();

        Map<Skill, Set<IngredientInfo>> validReqs = new HashMap<>();

        Set<IngredientItemMaps> validMetaIngs = new HashSet<>();

        // add all ings to different maps
        int maxLvl = possibleConstantData.stream().map(RecipeLoader.RecipeData::lvl).mapToInt(Vector2i::y).max().orElse(0);
        for (IngredientInfo info : allIngs.values()) {
            // check used profession
            List<ProfessionType> professions = info.professions();
            if (!professions.contains(targetType.getStation())) continue;

            // check level
            if (info.level() > maxLvl) continue;

            allDura.add(info);

            // TODO includes all meta ings need to check special case of -100 effectivness
            if (!info.positionModifiers().isEmpty()) {
                validMetaIngs.add(new IngredientItemMaps(info));
            }

            // TODO somehow handle when an ing rolls 0 and an id that can not be 0
            // filter out ings that contain skill reqs not on the item
            List<Pair<Skill, Integer>> ingReqs = info.skillRequirements();
            if (!ingReqs.isEmpty() && !skillPointTypes.containsAll(ingReqs.stream().map(Pair::a).collect(Collectors.toSet())))
                continue;
            // filter out ings that contain ids not on the item
            List<Pair<StatType, RangedValue>> stats = info.variableStats();
            if (!targetStatTypes.containsAll(stats.stream().map(Pair::a).collect(Collectors.toSet()))) continue;

            // map by stat/skill
            for (Pair<StatType, RangedValue> stat : stats) {
                Set<IngredientInfo> statSet = validStats.computeIfAbsent(stat.a(), k -> new HashSet<>());
                statSet.add(info);
            }
            for (Pair<Skill, Integer> skillReq : ingReqs) {
                Set<IngredientInfo> reqSet = validReqs.computeIfAbsent(skillReq.a(), k -> new HashSet<>());
                reqSet.add(info);
            }

            if (info.positionModifiers().isEmpty()) allValid.add(info);

            if (info.durabilityModifier() > 0) validDura.add(info);
        }

        // TODO move to after found ings and test dura this only matter at the end whatever
        for (RecipeLoader.RecipeData data : possibleConstantData) {
            mutableRecipe.setConstants(data);
            for (int l = 1; l <= 3; l++) {
                for (int s = 1; s <= 3; s++) {
                    mutableRecipe.getMaterials().setMat1Tier(l);
                    mutableRecipe.getMaterials().setMat2Tier(s);
                    Vector2i recipeHealth = mutableRecipe.getHealth();

                    if (targetType.isArmour()) {
                        if (recipeHealth.x <= targetHealth && targetHealth <= recipeHealth.y) {
                            possibleMaterialData.add(new Pair<>(data, new Recipe.Materials(mutableRecipe.getMaterials())));
                        }
                    } else possibleMaterialData.add(new Pair<>(data, new Recipe.Materials(mutableRecipe.getMaterials())));
                }
            }
        }

        if (possibleMaterialData.isEmpty()) {
            System.err.println("No Recipe data found!");
            return Collections.emptySet();
        }

        System.out.println("Found " + possibleMaterialData.size() + " Recipe data found");
        for (Map.Entry<StatType, Set<IngredientInfo>> Stat : validStats.entrySet()) {
            //System.out.println("Found " + Stat.getValue().size() + " ings with " + Stat.getKey().getDisplayName());
        }
        for (Map.Entry<Skill, Set<IngredientInfo>> Stat : validReqs.entrySet()) {
            //System.out.println("Found " + Stat.getValue().size() + " ings with " + Stat.getKey().getDisplayName());
        }
        //System.out.println(validDura.size() + " validDura ings");
        //System.out.println(validMetaIngs.size() + " validMetaIngs ings");

        // add at least one of every req
        for (Pair<Skill, Integer> req : targetRequirements.skills()) {
            if (req.b() == 0) continue;
            Set<IngredientInfo> ingOptions = validReqs.get(req.a());
            addNext(ingOptions, (existingIng) -> existingIng.getSkillRequirement(req.a()) != 0);
            //System.out.println("recipe option is now " + recipeOptions.size() + " after " + req.a().getDisplayName());
        }

        // add at least one of every stat
        for (StatPossibleValues id : targetPossibleValues) {
            Set<IngredientInfo> ingOptions = validStats.get(id.statType());
            if (id.baseValue() == 0) continue;
            addNext(ingOptions, (existingIng) -> existingIng.getIdentification(id.statType()) != null);
            //System.out.println("recipe option is now " + recipeOptions.size() + " after " + id.statType().getDisplayName());
        }

        // fill every existing possible valid recipe with every combination of valid ings (outputs a couple thousand not many)
        fillWith(allValid);
        System.out.println("recipe option is now " + recipeOptions.size() + " after filling");

        Set<List<IngredientItemMaps>> allMetaCombinations = new HashSet<>();
        for (List<IngredientItemMaps> recipeOption : recipeOptions) {
            //TODO this is where it blows up need to smart add meta ings ie:
            // calculate if each slot need + or - effectivness to reach target
            // then check empty slots and find meta ings that are usefull toward reaching target
            // can check against the max dura from possible recipe data to see if dura is < target dura
            allMetaCombinations.addAll(findAllMetaCombinations(recipeOption, new ArrayList<>(validMetaIngs)));
            //if (recipeOption.size() == 6) System.out.println(recipeOption);
        }

        recipeOptions = allMetaCombinations;
        System.out.println("recipe option is now " + recipeOptions.size() + " after permuting with meta ings");

        // set filters out recipes that are identical statwise but are a different order of ings
        Set<CraftingResult> possibleRecipes = new HashSet<>();

        int duplicateCounter = 0;
        Set<String> seen = new HashSet<>();
        for (List<IngredientItemMaps> ingsList : recipeOptions) {
            String key = CraftingUtils.getIngListKey(ingsList);
            if (!seen.add(key)) {
                duplicateCounter++;
                continue;
            }

            mutableRecipe.setIngredients(ingsList);
            mutableRecipe.updateMultipliers();

            CraftingResult result = mutableRecipe.craft();
            if (result == null) continue;
            if (result.equalsIgnoreDurability(target, false)) {
                possibleRecipes.add(result);
            }

        }

        System.out.println("found " + duplicateCounter + " duplicates");
        System.out.println("found " + possibleRecipes.size() + " recipe options for " + target.getName() + "\n");

        // add to know even if it fails so we dont continually retry on failures
        knownRecipes.put(hashable, possibleRecipes);
        return possibleRecipes;
    }

    // mutates recipeOptions and guarantess ever recipe has atleast one of the specified req/id
    private void addNext(Set<IngredientInfo> ingOptions, Function<IngredientItemMaps, Boolean> checker) {
        if (recipeOptions.isEmpty()) {
            for (IngredientInfo ing : ingOptions) {
                recipeOptions.add(new ArrayList<>(Collections.singleton(new IngredientItemMaps(ing))));
            }
        } else {
            Set<List<IngredientItemMaps>> toRemove = new HashSet<>();
            Set<List<IngredientItemMaps>> toAdd = new HashSet<>();
            for (List<IngredientItemMaps> currentRecipe : recipeOptions) {

                // if recipe already contains do ntohing
                boolean alreadyContains = false;
                for (IngredientItemMaps existingIng : currentRecipe) {
                    if (checker.apply(existingIng)) {
                        alreadyContains = true;
                        break;
                    }
                }

                // if it does not contain remove it and add a new recipe for current+every ing that has that stat
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

    // mutates recipe options
    private void fillWith(Set<IngredientInfo> ingOptions) {
        Queue<List<IngredientItemMaps>> queue = new ArrayDeque<>(recipeOptions);
        Set<List<IngredientItemMaps>> results = new HashSet<>();
        Set<String> seen = new HashSet<>();

        while (!queue.isEmpty()) {
            List<IngredientItemMaps> current = queue.poll();

            // avoid duplicates
            String key = current.stream()
                    .map(ing -> ing == null ? "null" : ing.getIngredientInfo().name())
                    .sorted()
                    .collect(Collectors.joining(","));
            if (!seen.add(key)) continue;

            // if its already full add it to results and go next (since we swap with recipeOptions at end not add too)
            if (current.size() == 6) {
                if (!isIncorrect(current)) {
                    results.add(current);
                }
                continue;
            } else if (current.size() > 6) { // why is this even here
                System.err.println("5 + 1 = 7 ???");
                return;
            }

            // if its less than six then add it to results (everything in recipeOptions is valid we are filling)
            results.add(current);

            // also add a new recipe for current + every ing to the queue (will be added to results when iterated over)
            for (IngredientInfo ing : ingOptions) {
                ArrayList<IngredientItemMaps> extended = new ArrayList<>(current);
                extended.add(new IngredientItemMaps(ing));
                queue.add(extended);
            }
        }

        recipeOptions = results;
    }

    // does not imply the opposite
    private boolean isIncorrect(List<IngredientItemMaps> ingredients) {
        if (ingredients.size() != 6) return false;
        Map<StatType, RangedValue> totalIds = new HashMap<>();

        for (IngredientItemMaps ing : ingredients) {
            // instantly return that it might be correct if it has ing effectiveness this is just a quick scan
            if (!ing.getIngredientInfo().positionModifiers().isEmpty()) return false;

            // get every id and add it to running total
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

        // check if target is within range
        for (StatPossibleValues targetId : target.getPossibleValues()) {
            RangedValue givenId = totalIds.get(targetId.statType());
            if (givenId == null) return true;
            if (!givenId.inRange(targetId.baseValue())) return true;
        }

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
        CraftingUtils.generateCombinations(paddedIngredients, false, true, 0, 6, new ArrayList<>(), allOrdersForProvided, new HashSet<>(), CraftingUtils::getIngListKey, new HashSet<>());

        Set<List<IngredientItemMaps>> results = new HashSet<>();

        Map<Integer, Set<List<IngredientItemMaps>>> metaCombinations = new HashMap<>();
        for (List<IngredientItemMaps> ordered : allOrdersForProvided) {
            int nullCount = (int) (6 - ordered.stream().filter(Objects::nonNull).count());

            Set<List<IngredientItemMaps>> metaCombination = metaCombinations.computeIfAbsent(
                    nullCount,
                    k -> {
                        Set<List<IngredientItemMaps>> result = new HashSet<>();
                        CraftingUtils.generateCombinations(validMetaIngs, true, true, 0, k, new ArrayList<>(), result, new HashSet<>(), CraftingUtils::getIngListKey, new HashSet<>());
                        return result;
                    }
            );

            for (List<IngredientItemMaps> metaIngs : metaCombination) {
                if (metaIngs.size() != nullCount) {
                    System.out.println("i messed up down here");
                    return Collections.emptySet();
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

}
