package net.backupcup.mcde.util;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.backupcup.mcde.MCDEnchantments;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.random.LocalRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.registry.Registry;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.backupcup.mcde.util.Slots.*;
import static net.minecraft.util.registry.Registry.ENCHANTMENT;

public class EnchantmentUtils {
    public static Stream<Identifier> getEnchantmentStream() {
        return ENCHANTMENT.getIds().stream();
    }

    public static Stream<Identifier> getEnchantmentsForItem(ItemStack itemStack) {
        var existing = EnchantmentHelper.get(itemStack).keySet().stream()
            .map(e -> ENCHANTMENT.getId(e))
            .collect(Collectors.toSet());
        return getAllEnchantmentsForItem(itemStack)
            .filter(id -> !existing.contains(id));
    }

    public static Stream<Identifier> getAllEnchantmentsForItem(ItemStack itemStack) {
        Predicate<Enchantment> target = itemStack.isIn(ModTags.Items.WEAPONS) ?
            e -> e.type.equals(EnchantmentTarget.WEAPON) :
            e -> e.isAcceptableItem(itemStack);

        return ENCHANTMENT.stream()
            .filter(MCDEnchantments.getConfig()::isEnchantmentAllowed)
            .filter(e -> e.isAvailableForRandomSelection() || !MCDEnchantments.getConfig().isAvailabilityForRandomSelectionRespected())
            .filter(e -> !e.isTreasure() || MCDEnchantments.getConfig().isTreasureAllowed())
            .filter(e -> !e.isCursed() || MCDEnchantments.getConfig().areCursedAllowed())
            .filter(target)
            .map(Registry.ENCHANTMENT::getId);
    }

    public static List<EnchantmentTarget> getEnchantmentTargets(Item item) {
        return Arrays.stream(EnchantmentTarget.values())
            .filter(target -> target.isAcceptableItem(item)).toList();
    }

    public static Identifier getEnchantmentId(Enchantment enchantment) {
        return ENCHANTMENT.getId(enchantment);
    }

    public static EnchantmentSlots generateEnchantments(ItemStack itemStack) {
        return generateEnchantments(itemStack, new LocalRandom(System.nanoTime()));
    }

    public static EnchantmentSlots generateEnchantments(ItemStack itemStack, Random random) {
        var builder = EnchantmentSlots.builder();
        var enchantments = getEnchantmentsForItem(itemStack).collect(ObjectArrayList.toList());
        Util.shuffle(enchantments, random);
        boolean isTwoChoiceGenerated = false;
        boolean isSecondSlotGenerated = false;
        float threeChoiceChance = 0.5f;
        float secondSlotChance = 0.5f;
        float thirdSlotChance = 0.25f;

        if (enchantments.isEmpty()) {
            return EnchantmentSlots.EMPTY;
        }

        if (random.nextFloat() < threeChoiceChance && enchantments.size() >= 3) {
            builder.withSlot(FIRST, enchantments.pop(), enchantments.pop(), enchantments.pop());
        }
        else if (enchantments.size() >= 2) {
            builder.withSlot(FIRST, enchantments.pop(), enchantments.pop());
            isTwoChoiceGenerated = true;
        }
        else {
            builder.withSlot(FIRST, enchantments.pop());
        }

        if (enchantments.isEmpty()) {
            return builder.build();
        }

        if (random.nextFloat() < secondSlotChance) {
            if (!isTwoChoiceGenerated && random.nextFloat() < threeChoiceChance && enchantments.size() >= 3) {
                builder.withSlot(SECOND, enchantments.pop(), enchantments.pop(), enchantments.pop());
            }
            else if (enchantments.size() >= 2) {
                builder.withSlot(SECOND, enchantments.pop(), enchantments.pop());
                isTwoChoiceGenerated = true;
            }
            else {
                builder.withSlot(SECOND, enchantments.pop());
            }
            isSecondSlotGenerated = true;
        }

        if (enchantments.isEmpty()) {
            return builder.build();
        }

        if (isSecondSlotGenerated && random.nextFloat() < thirdSlotChance) {
            if (!isTwoChoiceGenerated && random.nextFloat() < threeChoiceChance && enchantments.size() >= 3) {
                builder.withSlot(THIRD, enchantments.pop(), enchantments.pop(), enchantments.pop());
            }
            else if (enchantments.size() >= 2) {
                builder.withSlot(THIRD, enchantments.pop(), enchantments.pop());
                isTwoChoiceGenerated = true;
            }
            else {
                builder.withSlot(THIRD, enchantments.pop());
            }
        }

        return builder.build();
    }

    public static boolean canGenerateEnchantment(ItemStack itemStack) {
        return !getEnchantmentsNotInItem(itemStack).isEmpty();
    }

    public static Optional<Identifier> generateEnchantment(ItemStack itemStack) {
        return generateEnchantment(itemStack, new LocalRandom(System.nanoTime()));
    }

    public static Optional<Identifier> generateEnchantment(ItemStack itemStack, Random random) {
        var newEnchantments = getEnchantmentsNotInItem(itemStack);
        if (newEnchantments.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(newEnchantments.get(random.nextInt(newEnchantments.size())));
    }

    public static Set<Identifier> getAllEnchantmentsInItem(ItemStack itemStack) {
        var present = EnchantmentHelper.get(itemStack).keySet().stream()
            .map(key -> Registry.ENCHANTMENT.getId(key))
            .collect(Collectors.toSet());
        var slots = EnchantmentSlots.fromItemStack(itemStack);
        if (slots == null) {
            return present;
        }
        slots.stream()
            .flatMap(s -> s.choices().stream())
            .map(c -> c.getEnchantmentId()).forEach(id -> present.add(id));
        return present;
    }

    private static List<Identifier> getEnchantmentsNotInItem(ItemStack itemStack) {
        var present = getAllEnchantmentsInItem(itemStack);
         return getAllEnchantmentsForItem(itemStack)
            .filter(id -> !present.contains(id)).toList();
    }
}
