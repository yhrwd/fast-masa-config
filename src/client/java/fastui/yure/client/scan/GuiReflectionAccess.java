package fastui.yure.client.scan;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Shared, failure-tolerant reflection primitives for third-party config screens. */
final class GuiReflectionAccess {
    private GuiReflectionAccess() {
    }

    static Optional<Object> readFieldValue(Field field, Object owner) {
        try {
            field.setAccessible(true);
            return Optional.ofNullable(field.get(owner));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    static Method findNoArgMethod(Class<?> type, String methodName) {
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

    static List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> currentClass = type;
        while (currentClass != null && currentClass != Object.class) {
            fields.addAll(List.of(currentClass.getDeclaredFields()));
            currentClass = currentClass.getSuperclass();
        }
        return fields;
    }

    interface SelectorAccess {
        Object get() throws ReflectiveOperationException;

        void set(Object value) throws ReflectiveOperationException;
    }

    record FieldSelectorAccess(Field field, Object owner) implements SelectorAccess {
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

    record MethodSelectorAccess(Method getter, Method setter) implements SelectorAccess {
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
