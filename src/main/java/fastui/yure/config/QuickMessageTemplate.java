package fastui.yure.config;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 在发送时解析快捷消息中的动态变量，不修改持久化的原始内容。 */
public final class QuickMessageTemplate {
    private static final Pattern VARIABLE = Pattern.compile("\\$\\{([A-Za-z0-9_.-]+)}");

    private QuickMessageTemplate() {
    }

    public static String resolve(String content, Context context) {
        if (content == null || content.isEmpty() || context == null) {
            return content;
        }

        Map<String, String> values = values(context);
        Matcher matcher = VARIABLE.matcher(content);
        StringBuffer result = new StringBuffer(content.length());
        while (matcher.find()) {
            String replacement = values.get(matcher.group(1).toLowerCase(Locale.ROOT));
            if (replacement != null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            } else {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static Map<String, String> values(Context context) {
        int x = context.x();
        int y = context.y();
        int z = context.z();
        boolean nether = context.dimensionId().endsWith("the_nether");
        boolean overworld = context.dimensionId().endsWith("overworld");
        int overworldX = nether ? scale(x, 8) : x;
        int overworldZ = nether ? scale(z, 8) : z;
        int netherX = overworld ? floorDivide(x, 8) : x;
        int netherZ = overworld ? floorDivide(z, 8) : z;

        Map<String, String> result = new HashMap<>();
        put(result, context.playerName(), "player", "player_name", "name");
        put(result, Integer.toString(x), "x", "current_x", "block_x");
        put(result, Integer.toString(y), "y", "current_y", "block_y");
        put(result, Integer.toString(z), "z", "current_z", "block_z");
        put(result, format(context.preciseX()), "px", "precise_x");
        put(result, format(context.preciseY()), "py", "precise_y");
        put(result, format(context.preciseZ()), "pz", "precise_z");
        String dimensionName = shortDimensionName(context.dimensionId());
        put(result, context.dimensionId(), "dimension", "dimension_id");
        put(result, dimensionName, "dimension_name", "world");
        put(result, Integer.toString(overworldX), "overworld_x", "ow_x", "owx");
        put(result, Integer.toString(overworldZ), "overworld_z", "ow_z", "owz");
        put(result, Integer.toString(netherX), "nether_x", "n_x", "nx");
        put(result, Integer.toString(netherZ), "nether_z", "n_z", "nz");
        return result;
    }

    private static String shortDimensionName(String dimensionId) {
        int separator = dimensionId.indexOf(':');
        return separator >= 0 && separator + 1 < dimensionId.length()
                ? dimensionId.substring(separator + 1) : dimensionId;
    }

    private static void put(Map<String, String> values, String value, String... names) {
        String safeValue = value == null ? "" : value;
        for (String name : names) {
            values.put(name, safeValue);
        }
    }

    private static int scale(int value, int factor) {
        long scaled = (long) value * factor;
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, scaled));
    }

    private static int floorDivide(int value, int divisor) {
        return Math.floorDiv(value, divisor);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    /** 发送前由客户端根据当前世界状态构造的变量上下文。 */
    public record Context(String playerName, int x, int y, int z, double preciseX, double preciseY, double preciseZ,
            String dimensionId) {
        public Context {
            playerName = playerName == null ? "" : playerName;
            dimensionId = dimensionId == null ? "" : dimensionId;
        }
    }
}
