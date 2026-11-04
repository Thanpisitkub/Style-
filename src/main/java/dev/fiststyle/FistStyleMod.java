package dev.fiststyle;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(FistStyleMod.MOD_ID)
public class FistStyleMod {
    public static final String MOD_ID = "fiststyle";

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    /** Index = style number - 1. */
    public static final List<RegistryObject<Item>> STYLE_ITEMS = registerStyleItems();

    public static final RegistryObject<CreativeModeTab> STYLE_TAB = TABS.register("style", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MOD_ID))
                    .icon(() -> new ItemStack(STYLE_ITEMS.get(0).get()))
                    .displayItems((parameters, output) -> {
                        for (RegistryObject<Item> item : STYLE_ITEMS) {
                            output.accept(item.get());
                        }
                    })
                    .build());

    public FistStyleMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modEventBus);
        TABS.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(new LaunchHandler());
    }

    private static List<RegistryObject<Item>> registerStyleItems() {
        List<RegistryObject<Item>> items = new ArrayList<>();
        for (int n = 1; n <= StyleStats.DAMAGE.length; n++) {
            final int styleNumber = n;
            items.add(ITEMS.register("style_" + n, () -> new StyleItem(styleNumber)));
        }
        return List.copyOf(items);
    }
}
