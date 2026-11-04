package dev.fiststyle;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;

/** One of the ten fist-style items; damage and speed come from {@link StyleStats}. */
public class StyleItem extends Item {
    public final int styleNumber;
    private final Multimap<Attribute, AttributeModifier> mainHandModifiers;

    public StyleItem(int styleNumber) {
        super(new Item.Properties().stacksTo(1));
        this.styleNumber = styleNumber;
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                BASE_ATTACK_DAMAGE_UUID, "Weapon modifier",
                StyleStats.DAMAGE[styleNumber - 1] - 1.0, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                BASE_ATTACK_SPEED_UUID, "Weapon modifier",
                StyleStats.SPEED[styleNumber - 1] - 4.0, AttributeModifier.Operation.ADDITION));
        this.mainHandModifiers = builder.build();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? mainHandModifiers : super.getDefaultAttributeModifiers(slot);
    }
}
