package xiuxian.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public record VeinProcessingRecipe(ResourceLocation id, String station, Ingredient input, int inputCount,
        Ingredient reagent, int reagentCount, ItemStack output, ItemStack residue, int ticks) implements Recipe<Container> {
    public VeinProcessingRecipe {
        if (!java.util.Set.of("crusher", "washer", "condenser", "forge", "distiller", "charger").contains(station)
                || inputCount < 1 || inputCount > 64 || reagentCount < 1 || reagentCount > 64 || ticks < 1 || ticks > 72000
                || input.isEmpty() || reagent.isEmpty() || output.isEmpty() || output.getCount() > output.getMaxStackSize()
                || (!residue.isEmpty() && residue.getCount() > residue.getMaxStackSize()))
            throw new IllegalArgumentException("Invalid vein processing recipe: " + id);
        output = output.copy(); residue = residue.copy();
    }
    @Override public boolean matches(Container container, Level level) {
        return container.getContainerSize() >= 5 && input.test(container.getItem(0)) && container.getItem(0).getCount() >= inputCount
                && reagent.test(container.getItem(1)) && container.getItem(1).getCount() >= reagentCount;
    }
    @Override public ItemStack assemble(Container c, RegistryAccess access) { return output.copy(); }
    @Override public ItemStack getResultItem(RegistryAccess access) { return output.copy(); }
    @Override public boolean canCraftInDimensions(int w, int h) { return false; }
    @Override public boolean isSpecial() { return true; }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return XiuxianRecipes.VEIN_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return XiuxianRecipes.VEIN_TYPE; }
    @Override public NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY, input, reagent); }
    public static final class Serializer implements RecipeSerializer<VeinProcessingRecipe> {
        @Override public VeinProcessingRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new VeinProcessingRecipe(id, GsonHelper.getAsString(json,"station"), Ingredient.fromJson(json.get("ingredient")),
                    GsonHelper.getAsInt(json,"input_count",1), Ingredient.fromJson(json.get("reagent")), GsonHelper.getAsInt(json,"reagent_count",1),
                    ShapedRecipe.itemStackFromJson(json.getAsJsonObject("result")), json.has("residue") ? ShapedRecipe.itemStackFromJson(json.getAsJsonObject("residue")) : ItemStack.EMPTY,
                    GsonHelper.getAsInt(json,"ticks",100));
        }
        @Override public VeinProcessingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf b) {
            return new VeinProcessingRecipe(id,b.readUtf(),Ingredient.fromNetwork(b),b.readVarInt(),Ingredient.fromNetwork(b),b.readVarInt(),b.readItem(),b.readItem(),b.readVarInt());
        }
        @Override public void toNetwork(FriendlyByteBuf b, VeinProcessingRecipe r) {
            b.writeUtf(r.station); r.input.toNetwork(b); b.writeVarInt(r.inputCount); r.reagent.toNetwork(b); b.writeVarInt(r.reagentCount);
            b.writeItem(r.output); b.writeItem(r.residue); b.writeVarInt(r.ticks);
        }
    }
}
