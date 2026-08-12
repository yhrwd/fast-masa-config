package fastui.yure.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuickMessageTemplateTest {
    @Test
    void resolvesPlayerCoordinatesAndDimension() {
        QuickMessageTemplate.Context context = new QuickMessageTemplate.Context("Yure", 12, 64, -3,
                12.25, 64.0, -3.75, "minecraft:overworld");

        assertEquals("Yure 12 64 -3 12.25 64.00 -3.75 minecraft:overworld",
                QuickMessageTemplate.resolve(
                        "${player} ${x} ${y} ${z} ${px} ${py} ${pz} ${dimension}", context));
    }

    @Test
    void convertsNetherCoordinatesUsingFloorForNegativeValues() {
        QuickMessageTemplate.Context context = new QuickMessageTemplate.Context("Yure", -1, 70, -9,
                -1.2, 70.0, -9.1, "minecraft:the_nether");

        assertEquals("-8 -72 -1 -9", QuickMessageTemplate.resolve("${overworld_x} ${overworld_z} ${nether_x} ${nether_z}",
                context));
    }

    @Test
    void keepsUnknownVariablesAndSupportsAliases() {
        QuickMessageTemplate.Context context = new QuickMessageTemplate.Context("Yure", 8, 64, 16,
                8.0, 64.0, 16.0, "minecraft:overworld");

        assertEquals("${missing} 8 16 16 overworld", QuickMessageTemplate.resolve(
                "${missing} ${ow_x} ${owz} ${z} ${world}", context));
    }
}
