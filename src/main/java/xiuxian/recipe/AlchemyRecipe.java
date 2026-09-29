package xiuxian.recipe;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import xiuxian.item.XiuxianItems;

public class AlchemyRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final String group;
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;
    private final float experience;
    private final int cookingTime;
    private final RecipeType<AlchemyRecipe> recipeType;
    private final RecipeSerializer<?> serializer;

    public AlchemyRecipe(ResourceLocation id, String group, List<Ingredient> ingredients, ItemStack result,
                         float experience, int cookingTime, RecipeType<AlchemyRecipe> recipeType,
                         RecipeSerializer<?> serializer) {
        this.id = id;
        this.group = group;
        this.ingredients = NonNullList.create();
        this.ingredients.addAll(ingredients);
        this.result = result.copy();
        this.experience = experience;
        this.cookingTime = cookingTime;
        this.recipeType = recipeType;
        this.serializer = serializer;
    }

    @Override
    public boolean matches(Container container, Level level) {
        if (container.getContainerSize() < 6 || ingredients.isEmpty() || ingredients.size() > 6) return false;
        BitSet used = new BitSet(6);
        boolean matched = assignIngredient(0, used, container);
        if (!matched) return false;
        for (int slot = 0; slot < 6; slot++) {
            if (!container.getItem(slot).isEmpty() && !used.get(slot)) return false;
        }
        return true;
    }

    private boolean assignIngredient(int ingredientIndex, BitSet used, Container container) {
        if (ingredientIndex == ingredients.size()) return true;
        Ingredient ingredient = ingredients.get(ingredientIndex);
        for (int slot = 0; slot < 6; slot++) {
            if (!used.get(slot) && !container.getItem(slot).isEmpty()
                    && ingredient.test(container.getItem(slot))) {
                used.set(slot);
                if (assignIngredient(ingredientIndex + 1, used, container)) return true;
                used.clear(slot);
            }
        }
        return false;
    }

    public boolean consumeIngredients(Container container) {
        BitSet used = new BitSet(6);
        if (!assignIngredient(0, used, container)) return false;
        for (int slot = 0; slot < 6; slot++) {
            if (!container.getItem(slot).isEmpty() && !used.get(slot)) return false;
        }
        for (int slot = used.nextSetBit(0); slot >= 0; slot = used.nextSetBit(slot + 1)) {
            container.removeItem(slot, 1);
        }
        return true;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return ingredients.size() <= 6;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return result.copy();
    }

    @Override
    public ResourceLocation getId() { return id; }

    @Override
    public RecipeSerializer<?> getSerializer() { return serializer; }

    @Override
    public RecipeType<?> getType() { return recipeType; }

    @Override
    public NonNullList<Ingredient> getIngredients() { return ingredients; }

    @Override
    public String getGroup() { return group; }

    public float experience() { return experience; }

    public int cookingTime() { return cookingTime; }

    @Override
    public ItemStack getToastSymbol() {
        if (recipeType == XiuxianRecipes.SPIRIT_ALCHEMY_TYPE) {
            return new ItemStack(XiuxianItems.ALCHEMY_FURNACE_SPIRIT_ITEM.get());
        }
        if (recipeType == XiuxianRecipes.EARTH_ALCHEMY_TYPE) {
            return new ItemStack(XiuxianItems.ALCHEMY_FURNACE_EARTH_ITEM.get());
        }
        if (recipeType == XiuxianRecipes.HEAVEN_ALCHEMY_TYPE) {
            return new ItemStack(XiuxianItems.ALCHEMY_FURNACE_HEAVEN_ITEM.get());
        }
        return new ItemStack(XiuxianItems.ALCHEMY_FURNACE_ITEM.get());
    }

    public static class Serializer implements RecipeSerializer<AlchemyRecipe> {
        private final RecipeType<AlchemyRecipe> recipeType;

        public Serializer(RecipeType<AlchemyRecipe> recipeType) { this.recipeType = recipeType; }

        @Override
        public AlchemyRecipe fromJson(ResourceLocation id, JsonObject json) {
            List<Ingredient> ingredients = new ArrayList<>();
            if (json.has("ingredients")) {
                JsonArray array = GsonHelper.getAsJsonArray(json, "ingredients");
                for (int i = 0; i < array.size(); i++) ingredients.add(Ingredient.fromJson(array.get(i)));
            } else {
                ingredients.add(Ingredient.fromJson(GsonHelper.getNonNull(json, "ingredient")));
            }
            if (ingredients.isEmpty() || ingredients.size() > 6) {
                throw new IllegalArgumentException("Alchemy recipes need between one and six ingredients");
            }
            String group = GsonHelper.getAsString(json, "group", "");
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            float experience = GsonHelper.getAsFloat(json, "experience", 0.0F);
            int cookingTime = GsonHelper.getAsInt(json, "cookingtime", 200);
            return new AlchemyRecipe(id, group, ingredients, result, experience, cookingTime,
                    recipeType, this);
        }

        @Override
        public AlchemyRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            String group = buffer.readUtf();
            int count = buffer.readVarInt();
            List<Ingredient> ingredients = new ArrayList<>(count);
            for (int i = 0; i < count; i++) ingredients.add(Ingredient.fromNetwork(buffer));
            return new AlchemyRecipe(id, group, ingredients, buffer.readItem(), buffer.readFloat(),
                    buffer.readVarInt(), recipeType, this);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, AlchemyRecipe recipe) {
            buffer.writeUtf(recipe.group);
            buffer.writeVarInt(recipe.ingredients.size());
            recipe.ingredients.forEach(ingredient -> ingredient.toNetwork(buffer));
            buffer.writeItem(recipe.result);
            buffer.writeFloat(recipe.experience);
            buffer.writeVarInt(recipe.cookingTime);
        }
    }
}
