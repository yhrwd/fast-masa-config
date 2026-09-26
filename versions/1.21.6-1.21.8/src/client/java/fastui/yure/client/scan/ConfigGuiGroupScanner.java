package fastui.yure.client.scan;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.interfaces.IConfigGui;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ConfigGuiGroupScanner {
    private static final Set<String> GROUP_FIELD_NAMES = Set.of("tab", "category", "group", "page", "section");

    private ConfigGuiGroupScanner() {
    }

    public static List<Group> collectGroups(Object screen, IConfigGui configGui) {
        List<CandidateResult> candidates = findGroupCandidates(screen).stream()
                .map(candidate -> scanCandidate(candidate, configGui))
                .flatMap(optional -> optional.stream())
                .toList();

        return selectBestCandidate(candidates)
                .map(candidate -> candidate.groups())
                .orElseGet(() -> List.of(defaultGroup(configGui)));
    }

    private static List<GroupCandidate> findGroupCandidates(Object screen) {
        List<GroupCandidate> candidates = new ArrayList<>();

        for (CandidateOwner owner : collectCandidateOwners(screen)) {
            for (Field field : getAllFields(owner.value().getClass())) {
                addEnumCandidate(candidates, field, getFieldOwner(owner.value(), field), owner.priority(),
                        getFieldPath(owner, field));
            }

            addIndexedListCandidates(candidates, owner);
        }

        addMethodBackedEnumCandidates(candidates, screen.getClass());

        return candidates;
    }

    private static List<CandidateOwner> collectCandidateOwners(Object screen) {
        List<CandidateOwner> owners = new ArrayList<>();
        Set<Object> seenOwners = Collections.newSetFromMap(new IdentityHashMap<>());
        addCandidateOwner(owners, seenOwners, screen, "", 0);

        for (Field field : screen.getClass().getDeclaredFields()) {
            if (isObjectContainerField(field)) {
                readFieldValue(field, getFieldOwner(screen, field))
                        .ifPresent(value -> addCandidateOwner(owners, seenOwners, value, field.getName(), 1));
            }
        }

        return owners;
    }

    private static void addCandidateOwner(List<CandidateOwner> owners, Set<Object> seenOwners, Object value,
            String path, int priority) {
        if (value != null && seenOwners.add(value)) {
            owners.add(new CandidateOwner(value, path, priority));
        }
    }

    private static Object getFieldOwner(Object screen, Field field) {
        return Modifier.isStatic(field.getModifiers()) ? null : screen;
    }

    private static void addEnumCandidate(List<GroupCandidate> candidates, Field field, Object owner, int priority,
            String path) {
        if (field.getType().isEnum() && isMutableField(field) && isGroupFieldName(field.getName())) {
            Object[] enumValues = field.getType().getEnumConstants();

            if (enumValues != null) {
                List<SelectorValue> values = new ArrayList<>(enumValues.length);

                for (Object enumValue : enumValues) {
                    values.add(new SelectorValue(enumValue, enumValue));
                }

                candidates.add(new GroupCandidate(new FieldSelectorAccess(field, owner), path, priority, values));
            }
        }
    }

    private static void addMethodBackedEnumCandidates(List<GroupCandidate> candidates, Class<?> screenClass) {
        for (Class<?> enumClass : findNestedEnumClasses(screenClass)) {
            for (Class<?> stateHolderClass : findLikelyStateHolderClasses(screenClass)) {
                findMethodBackedCandidate(enumClass, stateHolderClass)
                        .ifPresent(candidate -> candidates.add(candidate));
            }
        }
    }

    private static List<Class<?>> findNestedEnumClasses(Class<?> screenClass) {
        List<Class<?>> enumClasses = new ArrayList<>();

        for (Class<?> declaredClass : screenClass.getDeclaredClasses()) {
            if (declaredClass.isEnum()) {
                enumClasses.add(declaredClass);
            }
        }

        return enumClasses;
    }

    private static List<Class<?>> findLikelyStateHolderClasses(Class<?> screenClass) {
        LinkedHashSet<Class<?>> classes = new LinkedHashSet<>();
        classes.add(screenClass);

        String packageName = screenClass.getPackageName();
        int guiSegment = packageName.lastIndexOf(".gui");

        if (guiSegment >= 0) {
            addClassIfPresent(classes, packageName.substring(0, guiSegment) + ".data.DataManager",
                    screenClass.getClassLoader());
        }

        addClassIfPresent(classes, packageName + ".DataManager", screenClass.getClassLoader());

        return List.copyOf(classes);
    }

    private static void addClassIfPresent(Set<Class<?>> classes, String className, ClassLoader classLoader) {
        try {
            classes.add(Class.forName(className, false, classLoader));
        } catch (ClassNotFoundException ignored) {
            // 不是所有配置界面都把状态放在 DataManager 中。
        }
    }

    private static Optional<GroupCandidate> findMethodBackedCandidate(Class<?> enumClass, Class<?> stateHolderClass) {
        Method getter = null;
        Method setter = null;

        for (Method method : stateHolderClass.getDeclaredMethods()) {
            int modifiers = method.getModifiers();

            if (Modifier.isStatic(modifiers) == false || isGroupMethodName(method.getName()) == false) {
                continue;
            }

            if (method.getParameterCount() == 0 && method.getReturnType() == enumClass) {
                getter = method;
            }

            if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == enumClass) {
                setter = method;
            }
        }

        if (getter == null || setter == null) {
            return Optional.empty();
        }

        Object[] enumValues = enumClass.getEnumConstants();

        if (enumValues == null) {
            return Optional.empty();
        }

        List<SelectorValue> values = new ArrayList<>(enumValues.length);

        for (Object enumValue : enumValues) {
            values.add(new SelectorValue(enumValue, enumValue));
        }

        String path = stateHolderClass.getName() + "." + getter.getName() + "()";
        return Optional.of(new GroupCandidate(new MethodSelectorAccess(getter, setter), path, 3, values));
    }

    private static void addIndexedListCandidates(List<GroupCandidate> candidates, CandidateOwner owner) {
        List<Field> indexFields = getAllFields(owner.value().getClass()).stream()
                .filter(ConfigGuiGroupScanner::isIndexField)
                .toList();
        List<Field> listFields = getAllFields(owner.value().getClass()).stream()
                .filter(ConfigGuiGroupScanner::isGroupListField)
                .toList();

        for (Field indexField : indexFields) {
            for (Field listField : listFields) {
                readGroupValues(listField, getFieldOwner(owner.value(), listField))
                        .ifPresent(values -> candidates.add(new GroupCandidate(
                                new FieldSelectorAccess(indexField, getFieldOwner(owner.value(), indexField)),
                                getFieldPath(owner, indexField) + ":" + getFieldPath(owner, listField),
                                owner.priority() + 2,
                                values)));
            }
        }
    }

    private static Optional<List<SelectorValue>> readGroupValues(Field listField, Object owner) {
        Optional<Object> value = readFieldValue(listField, owner);

        if (value.isEmpty()) {
            return Optional.empty();
        }

        Object rawValue = value.get();

        if ((rawValue instanceof Collection<?>) == false) {
            return Optional.empty();
        }

        Collection<?> collection = (Collection<?>) rawValue;

        if (collection.size() <= 1 || collection.size() > 128) {
            return Optional.empty();
        }

        List<SelectorValue> values = new ArrayList<>(collection.size());
        int index = 0;

        for (Object groupValue : collection) {
            if (groupValue == null) {
                return Optional.empty();
            }

            values.add(new SelectorValue(index, groupValue));
            index++;
        }

        return Optional.of(values);
    }

    private static Optional<Object> readFieldValue(Field field, Object owner) {
        try {
            field.setAccessible(true);
            return Optional.ofNullable(field.get(owner));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static boolean isObjectContainerField(Field field) {
        if (field.getType().isPrimitive() || field.getType().isEnum() || field.getType().isArray()) {
            return false;
        }

        if (Collection.class.isAssignableFrom(field.getType()) || Map.class.isAssignableFrom(field.getType())) {
            return false;
        }

        return field.getType().getName().startsWith("java.") == false;
    }

    private static boolean isMutableField(Field field) {
        return field.isSynthetic() == false && Modifier.isFinal(field.getModifiers()) == false;
    }

    private static boolean isGroupFieldName(String fieldName) {
        return GROUP_FIELD_NAMES.contains(fieldName.toLowerCase(Locale.ROOT));
    }

    private static boolean isGroupMethodName(String methodName) {
        String normalizedName = methodName.toLowerCase(Locale.ROOT);

        for (String groupFieldName : GROUP_FIELD_NAMES) {
            if (normalizedName.contains(groupFieldName)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isIndexField(Field field) {
        String fieldName = field.getName().toLowerCase(Locale.ROOT);
        return isMutableField(field)
                && (field.getType() == int.class || field.getType() == Integer.class)
                && fieldName.endsWith("index");
    }

    private static boolean isGroupListField(Field field) {
        String fieldName = field.getName().toLowerCase(Locale.ROOT);
        return Collection.class.isAssignableFrom(field.getType())
                && (fieldName.contains("list") || fieldName.contains("group") || fieldName.contains("tab")
                        || fieldName.contains("categor") || fieldName.contains("page")
                        || fieldName.contains("section"));
    }

    private static String getFieldPath(CandidateOwner owner, Field field) {
        return owner.path().isBlank() ? field.getName() : owner.path() + "." + field.getName();
    }

    private static Optional<CandidateResult> scanCandidate(GroupCandidate candidate, IConfigGui configGui) {
        List<GroupData> groups = new ArrayList<>();

        try {
            Object originalValue = candidate.access().get();

            try {
                for (SelectorValue value : candidate.values()) {
                    candidate.access().set(value.selectorValue());
                    List<GuiConfigsBase.ConfigOptionWrapper> configs = copyConfigs(configGui.getConfigs());
                    groups.add(new GroupData(value.groupValue(), configs, configNames(configs)));
                }
            } finally {
                candidate.access().set(originalValue);
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return Optional.empty();
        }

        List<GroupData> usefulGroups = removeEmptyAndAggregateGroups(groups);
        Set<Set<String>> distinctConfigSets = new HashSet<>();
        LinkedHashSet<String> uniqueConfigNames = new LinkedHashSet<>();

        for (GroupData group : usefulGroups) {
            distinctConfigSets.add(group.configNames());
            uniqueConfigNames.addAll(group.configNames());
        }

        if (usefulGroups.size() <= 1 || distinctConfigSets.size() <= 1) {
            return Optional.empty();
        }

        return Optional.of(new CandidateResult(candidate, toGroups(candidate, usefulGroups), uniqueConfigNames));
    }

    private static List<GuiConfigsBase.ConfigOptionWrapper> copyConfigs(
            List<GuiConfigsBase.ConfigOptionWrapper> configs) {
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

    private static List<GroupData> removeEmptyAndAggregateGroups(List<GroupData> groups) {
        List<GroupData> nonEmptyGroups = groups.stream()
                .filter(group -> group.configNames().isEmpty() == false)
                .toList();

        return nonEmptyGroups.stream()
                .filter(group -> isAggregateGroup(group, nonEmptyGroups) == false)
                .toList();
    }

    private static boolean isAggregateGroup(GroupData group, List<GroupData> allGroups) {
        LinkedHashSet<String> otherNames = new LinkedHashSet<>();
        int otherGroupCount = 0;
        int largestOtherGroupSize = 0;

        for (GroupData otherGroup : allGroups) {
            if (otherGroup == group) {
                continue;
            }

            otherGroupCount++;
            largestOtherGroupSize = Math.max(largestOtherGroupSize, otherGroup.configNames().size());
            otherNames.addAll(otherGroup.configNames());
        }

        return otherGroupCount > 1
                && group.configNames().size() > largestOtherGroupSize
                && group.configNames().containsAll(otherNames);
    }

    private static List<Group> toGroups(GroupCandidate candidate, List<GroupData> groups) {
        return groups.stream()
                .map(group -> new Group(getGroupId(group.groupValue()), getGroupDisplayName(group.groupValue()),
                        candidate.path() + ":" + getGroupId(group.groupValue()), group.configs()))
                .toList();
    }

    private static Optional<CandidateResult> selectBestCandidate(List<CandidateResult> candidates) {
        CandidateResult best = null;

        for (CandidateResult candidate : candidates) {
            if (best == null || isBetterCandidate(candidate, best)) {
                best = candidate;
            }
        }

        return Optional.ofNullable(best);
    }

    private static boolean isBetterCandidate(CandidateResult candidate, CandidateResult best) {
        if (candidate.uniqueConfigNames().size() != best.uniqueConfigNames().size()) {
            return candidate.uniqueConfigNames().size() > best.uniqueConfigNames().size();
        }

        if (candidate.groups().size() != best.groups().size()) {
            return candidate.groups().size() > best.groups().size();
        }

        return candidate.candidate().priority() < best.candidate().priority();
    }

    private static Group defaultGroup(IConfigGui configGui) {
        return new Group("default", "Default", "default", copyConfigs(configGui.getConfigs()));
    }

    private static String getGroupId(Object groupValue) {
        if (groupValue instanceof Enum<?> enumValue) {
            return enumValue.name();
        }

        return readStringMethod(groupValue, "getName")
                .or(() -> readStringMethod(groupValue, "getNameKey"))
                .or(() -> readStringMethod(groupValue, "getId"))
                .orElseGet(() -> String.valueOf(groupValue));
    }

    private static String getGroupDisplayName(Object groupValue) {
        return readStringMethod(groupValue, "getTitleDisplayName")
                .or(() -> readStringMethod(groupValue, "getDisplayName"))
                .or(() -> readStringMethod(groupValue, "getStringValue"))
                .or(() -> readStringMethod(groupValue, "getTranslatedName"))
                .or(() -> readStringMethod(groupValue, "getName"))
                .or(() -> readStringMethod(groupValue, "getNameKey"))
                .orElseGet(() -> getGroupId(groupValue));
    }

    private static Optional<String> readStringMethod(Object target, String methodName) {
        if (target == null) {
            return Optional.empty();
        }

        try {
            Method method = findNoArgMethod(target.getClass(), methodName);

            if (method == null) {
                return Optional.empty();
            }

            method.setAccessible(true);
            Object value = method.invoke(target);

            if (value instanceof String stringValue && stringValue.isBlank() == false) {
                return Optional.of(stringValue);
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // 显示名只是辅助信息，失败时使用稳定的 enum 名称。
        }

        return Optional.empty();
    }

    private static Method findNoArgMethod(Class<?> type, String methodName) {
        Class<?> currentClass = type;

        while (currentClass != null && currentClass != Object.class) {
            try {
                return currentClass.getDeclaredMethod(methodName);
            } catch (NoSuchMethodException ignored) {
                currentClass = currentClass.getSuperclass();
            }
        }

        return null;
    }

    private static List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> currentClass = type;

        while (currentClass != null && currentClass != Object.class) {
            fields.addAll(List.of(currentClass.getDeclaredFields()));
            currentClass = currentClass.getSuperclass();
        }

        return fields;
    }

    public record Group(String id, String displayName, String sourceId,
            List<GuiConfigsBase.ConfigOptionWrapper> configs) {
    }

    private record CandidateOwner(Object value, String path, int priority) {
    }

    private interface SelectorAccess {
        Object get() throws ReflectiveOperationException;

        void set(Object value) throws ReflectiveOperationException;
    }

    private record FieldSelectorAccess(Field field, Object owner) implements SelectorAccess {
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

    private record MethodSelectorAccess(Method getter, Method setter) implements SelectorAccess {
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

    private record GroupCandidate(SelectorAccess access, String path, int priority, List<SelectorValue> values) {
    }

    private record SelectorValue(Object selectorValue, Object groupValue) {
    }

    private record GroupData(Object groupValue, List<GuiConfigsBase.ConfigOptionWrapper> configs,
            Set<String> configNames) {
    }

    private record CandidateResult(GroupCandidate candidate, List<Group> groups, Set<String> uniqueConfigNames) {
    }
}
