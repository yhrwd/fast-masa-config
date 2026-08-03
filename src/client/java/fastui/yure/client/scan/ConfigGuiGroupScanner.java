package fastui.yure.client.scan;

import fastui.yure.FastMasaConfig;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.interfaces.IConfigGui;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ConfigGuiGroupScanner {
    private static final Set<String> GROUP_NAMES = Set.of("tab", "category", "group", "page", "section");

    private ConfigGuiGroupScanner() {
    }

    public static List<Group> collectGroups(Object screen, IConfigGui configGui) {
        CandidateResult best = null;

        for (Candidate candidate : findCandidates(screen)) {
            Optional<CandidateResult> result = scanCandidate(candidate, configGui);

            if (result.isPresent() && (best == null || isBetter(result.get(), best))) {
                best = result.get();
            }
        }

        return best == null ? List.of(defaultGroup(configGui)) : best.groups();
    }

    private static List<Candidate> findCandidates(Object screen) {
        List<Candidate> candidates = new ArrayList<>();
        List<Owner> owners = collectOwners(screen);

        for (Owner owner : owners) {
            List<Field> fields = getAllFields(owner.value().getClass());

            for (Field field : fields) {
                if (field.getType().isEnum() && isMutable(field) && isGroupName(field.getName())) {
                    candidates.add(new Candidate(new FieldAccess(field, ownerFor(field, owner.value())), fieldPath(owner, field), owner.priority(), enumValues(field.getType())));
                }
            }

            addIndexedListCandidates(candidates, owner, fields);
        }

        addMethodCandidates(candidates, screen.getClass());
        return candidates;
    }

    private static List<Owner> collectOwners(Object screen) {
        List<Owner> owners = new ArrayList<>();
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        addOwner(owners, seen, screen, "", 0);

        for (Field field : getAllFields(screen.getClass())) {
            if (isNestedObject(field)) {
                readField(field, ownerFor(field, screen)).ifPresent(value -> addOwner(owners, seen, value, field.getName(), 1));
            }
        }

        return owners;
    }

    private static void addOwner(List<Owner> owners, Set<Object> seen, Object value, String path, int priority) {
        if (value != null && seen.add(value)) {
            owners.add(new Owner(value, path, priority));
        }
    }

    private static void addIndexedListCandidates(List<Candidate> candidates, Owner owner, List<Field> fields) {
        for (Field indexField : fields) {
            if (isIndexField(indexField) == false) {
                continue;
            }

            for (Field listField : fields) {
                if (isGroupListField(listField) == false) {
                    continue;
                }

                readSelectorValues(listField, ownerFor(listField, owner.value())).ifPresent(values -> candidates.add(new Candidate(
                        new FieldAccess(indexField, ownerFor(indexField, owner.value())),
                        fieldPath(owner, indexField) + ":" + fieldPath(owner, listField), owner.priority() + 2, values)));
            }
        }
    }

    private static void addMethodCandidates(List<Candidate> candidates, Class<?> screenClass) {
        for (Class<?> enumClass : screenClass.getDeclaredClasses()) {
            if (enumClass.isEnum() == false) {
                continue;
            }

            for (Class<?> owner : methodOwners(screenClass)) {
                Method getter = null;
                Method setter = null;

                for (Method method : owner.getDeclaredMethods()) {
                    if (Modifier.isStatic(method.getModifiers()) == false || isGroupName(method.getName()) == false) {
                        continue;
                    }

                    if (method.getParameterCount() == 0 && method.getReturnType() == enumClass) {
                        getter = method;
                    } else if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == enumClass) {
                        setter = method;
                    }
                }

                if (getter != null && setter != null) {
                    candidates.add(new Candidate(new MethodAccess(getter, setter), owner.getName() + "." + getter.getName() + "()", 3, enumValues(enumClass)));
                }
            }
        }
    }

    private static List<Class<?>> methodOwners(Class<?> screenClass) {
        List<Class<?>> owners = new ArrayList<>();
        owners.add(screenClass);
        String packageName = screenClass.getPackageName();
        int guiIndex = packageName.lastIndexOf(".gui");

        if (guiIndex >= 0) {
            addClassIfPresent(owners, packageName.substring(0, guiIndex) + ".data.DataManager", screenClass.getClassLoader());
        }

        addClassIfPresent(owners, packageName + ".DataManager", screenClass.getClassLoader());
        return owners;
    }

    private static void addClassIfPresent(List<Class<?>> owners, String className, ClassLoader loader) {
        try {
            owners.add(Class.forName(className, false, loader));
        } catch (ClassNotFoundException ignored) {
            // DataManager 并非所有配置界面的状态宿主。
        }
    }

    private static Optional<CandidateResult> scanCandidate(Candidate candidate, IConfigGui configGui) {
        List<GroupData> scanned = new ArrayList<>();

        try {
            Object original = candidate.access().get();

            try {
                for (SelectorValue value : candidate.values()) {
                    candidate.access().set(value.selectorValue());
                    List<GuiConfigsBase.ConfigOptionWrapper> configs = copyConfigs(configGui.getConfigs());
                    scanned.add(new GroupData(value.groupValue(), configs, configNames(configs)));
                }
            } finally {
                restoreSelector(candidate, original);
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return Optional.empty();
        }

        List<GroupData> groups = scanned.stream().filter(group -> group.configNames().isEmpty() == false).toList();
        Set<Set<String>> configSets = new LinkedHashSet<>();
        LinkedHashSet<String> allNames = new LinkedHashSet<>();

        for (GroupData group : groups) {
            configSets.add(group.configNames());
            allNames.addAll(group.configNames());
        }

        if (groups.size() <= 1 || configSets.size() <= 1) {
            return Optional.empty();
        }

        return Optional.of(new CandidateResult(candidate.priority(), allNames.size(), groups.stream()
                .map(group -> new Group(groupId(group.value()), groupName(group.value()), candidate.path() + ":" + groupId(group.value()), group.configs()))
                .toList()));
    }

    private static void restoreSelector(Candidate candidate, Object original) {
        try {
            candidate.access().set(original);
        } catch (ReflectiveOperationException | RuntimeException e) {
            // 恢复失败时不能丢弃已采集的分组，日志明确说明 selector 可能仍处于临时值。
            FastMasaConfig.LOGGER.warn("Failed to restore config group selector [{}]; selector state may have changed", candidate.path(), e);
        }
    }

    private static boolean isBetter(CandidateResult candidate, CandidateResult current) {
        if (candidate.uniqueConfigCount() != current.uniqueConfigCount()) {
            return candidate.uniqueConfigCount() > current.uniqueConfigCount();
        }

        if (candidate.groups().size() != current.groups().size()) {
            return candidate.groups().size() > current.groups().size();
        }

        return candidate.priority() < current.priority();
    }

    private static List<SelectorValue> enumValues(Class<?> enumClass) {
        Object[] values = enumClass.getEnumConstants();
        List<SelectorValue> result = new ArrayList<>();

        if (values != null) {
            for (Object value : values) {
                result.add(new SelectorValue(value, value));
            }
        }

        return result;
    }

    private static Optional<List<SelectorValue>> readSelectorValues(Field field, Object owner) {
        Optional<Object> value = readField(field, owner);

        if (value.isEmpty() || (value.get() instanceof Collection<?>) == false) {
            return Optional.empty();
        }

        Collection<?> collection = (Collection<?>) value.get();

        if (collection.size() <= 1 || collection.size() > 128) {
            return Optional.empty();
        }

        List<SelectorValue> values = new ArrayList<>();
        int index = 0;

        for (Object group : collection) {
            if (group == null) {
                return Optional.empty();
            }

            values.add(new SelectorValue(index++, group));
        }

        return Optional.of(values);
    }

    private static List<GuiConfigsBase.ConfigOptionWrapper> copyConfigs(List<GuiConfigsBase.ConfigOptionWrapper> configs) {
        return configs == null ? List.of() : List.copyOf(configs);
    }

    private static Set<String> configNames(List<GuiConfigsBase.ConfigOptionWrapper> configs) {
        LinkedHashSet<String> names = new LinkedHashSet<>();

        for (GuiConfigsBase.ConfigOptionWrapper wrapper : configs) {
            IConfigBase config = wrapper.getConfig();
            if (config != null) {
                names.add(config.getName());
            }
        }

        return names;
    }

    private static Group defaultGroup(IConfigGui configGui) {
        return new Group("default", "Default", "default", copyConfigs(configGui.getConfigs()));
    }

    private static String groupId(Object value) {
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }

        return stringValue(value, "getName").or(() -> stringValue(value, "getNameKey")).or(() -> stringValue(value, "getId")).orElse(String.valueOf(value));
    }

    private static String groupName(Object value) {
        return stringValue(value, "getTitleDisplayName").or(() -> stringValue(value, "getDisplayName"))
                .or(() -> stringValue(value, "getStringValue")).or(() -> stringValue(value, "getTranslatedName"))
                .orElseGet(() -> groupId(value));
    }

    private static Optional<String> stringValue(Object value, String methodName) {
        if (value == null) {
            return Optional.empty();
        }

        Method method = findNoArgMethod(value.getClass(), methodName);
        if (method == null) {
            return Optional.empty();
        }

        try {
            method.setAccessible(true);
            Object result = method.invoke(value);
            return result instanceof String string && string.isBlank() == false ? Optional.of(string) : Optional.empty();
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<Object> readField(Field field, Object owner) {
        try {
            field.setAccessible(true);
            return Optional.ofNullable(field.get(owner));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            fields.addAll(List.of(current.getDeclaredFields()));
        }

        return fields;
    }

    private static Method findNoArgMethod(Class<?> type, String name) {
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            try {
                return current.getDeclaredMethod(name);
            } catch (NoSuchMethodException ignored) {
                // 继续查找父类。
            }
        }

        return null;
    }

    private static Object ownerFor(Field field, Object owner) {
        return Modifier.isStatic(field.getModifiers()) ? null : owner;
    }

    private static String fieldPath(Owner owner, Field field) {
        return owner.path().isBlank() ? field.getName() : owner.path() + "." + field.getName();
    }

    private static boolean isMutable(Field field) {
        return field.isSynthetic() == false && Modifier.isFinal(field.getModifiers()) == false;
    }

    private static boolean isGroupName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return GROUP_NAMES.contains(normalized) || GROUP_NAMES.stream().anyMatch(normalized::contains);
    }

    private static boolean isIndexField(Field field) {
        return isMutable(field) && (field.getType() == int.class || field.getType() == Integer.class)
                && field.getName().toLowerCase(Locale.ROOT).endsWith("index");
    }

    private static boolean isGroupListField(Field field) {
        String name = field.getName().toLowerCase(Locale.ROOT);
        return Collection.class.isAssignableFrom(field.getType()) && (name.contains("list") || isGroupName(name));
    }

    private static boolean isNestedObject(Field field) {
        return field.getType().isPrimitive() == false && field.getType().isEnum() == false && field.getType().isArray() == false
                && Collection.class.isAssignableFrom(field.getType()) == false && Map.class.isAssignableFrom(field.getType()) == false
                && field.getType().getName().startsWith("java.") == false;
    }

    public record Group(String id, String displayName, String sourceId, List<GuiConfigsBase.ConfigOptionWrapper> configs) {
    }

    private record Owner(Object value, String path, int priority) {
    }

    private record Candidate(Access access, String path, int priority, List<SelectorValue> values) {
    }

    private record SelectorValue(Object selectorValue, Object groupValue) {
    }

    private record GroupData(Object value, List<GuiConfigsBase.ConfigOptionWrapper> configs, Set<String> configNames) {
    }

    private record CandidateResult(int priority, int uniqueConfigCount, List<Group> groups) {
    }

    private interface Access {
        Object get() throws ReflectiveOperationException;

        void set(Object value) throws ReflectiveOperationException;
    }

    private record FieldAccess(Field field, Object owner) implements Access {
        @Override
        public Object get() throws ReflectiveOperationException {
            this.field.setAccessible(true);
            return this.field.get(this.owner);
        }

        @Override
        public void set(Object value) throws ReflectiveOperationException {
            this.field.setAccessible(true);
            this.field.set(this.owner, value);
        }
    }

    private record MethodAccess(Method getter, Method setter) implements Access {
        @Override
        public Object get() throws ReflectiveOperationException {
            this.getter.setAccessible(true);
            return this.getter.invoke(null);
        }

        @Override
        public void set(Object value) throws ReflectiveOperationException {
            this.setter.setAccessible(true);
            this.setter.invoke(null, value);
        }
    }
}
