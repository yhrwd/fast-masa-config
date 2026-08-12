package fastui.yure.config;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Pure matching logic for the client-side entity rendering filter. */
public final class EntityRenderFilter {
    private EntityRenderFilter() {
    }

    public static boolean shouldRender(boolean enabled, boolean whitelist, List<String> configuredIds, String entityId) {
        return State.from(enabled, whitelist, configuredIds).shouldRender(entityId);
    }

    public static Set<String> normalizedIds(List<String> ids) {
        Set<String> result = new HashSet<>();
        if (ids == null) {
            return result;
        }
        for (String id : ids) {
            String normalized = normalize(id);
            if (normalized.contains(":")) {
                result.add(normalized);
            }
        }
        return result;
    }

    public static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }

    /** Immutable configuration snapshot used for one entity-render decision. */
    public record State(boolean enabled, boolean whitelist, Set<String> ids) {
        public State {
            ids = Set.copyOf(ids);
        }

        public static State from(boolean enabled, boolean whitelist, List<String> configuredIds) {
            return new State(enabled, whitelist, enabled ? normalizedIds(configuredIds) : Set.of());
        }

        public boolean shouldRender(String entityId) {
            if (!this.enabled) {
                return true;
            }
            boolean matched = this.ids.contains(normalize(entityId));
            // 启用白名单时只渲染列表实体；关闭时隐藏列表实体。
            return this.whitelist ? matched : !matched;
        }
    }
}
