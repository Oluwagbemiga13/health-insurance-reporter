package cz.oluwagbemiga.eutax.ui;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.function.Predicate;
import java.util.function.Supplier;

final class SwingTestUtils {

    private SwingTestUtils() {
        throw new IllegalStateException("Utility class");
    }

    static void runOnEdt(Runnable action) {
        Objects.requireNonNull(action, "action");
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(action);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("EDT action interrupted", ex);
        } catch (InvocationTargetException ex) {
            throw new IllegalStateException("EDT action failed", ex.getCause());
        }
    }

    static <T> T callOnEdt(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        FutureTask<T> task = new FutureTask<>(supplier::get);
        runTask(task);
        try {
            return task.get();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to obtain EDT value", ex);
        }
    }

    static <T> T createOnEdt(Callable<T> creator) {
        Objects.requireNonNull(creator, "creator");
        FutureTask<T> task = new FutureTask<>(creator);
        runTask(task);
        try {
            return task.get();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create Swing component", ex);
        }
    }

    private static void runTask(FutureTask<?> task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(task);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("EDT task interrupted", ex);
        } catch (InvocationTargetException ex) {
            throw new IllegalStateException("EDT task failed", ex.getCause());
        }
    }

    static void dispose(Window window) {
        if (window == null) {
            return;
        }
        runOnEdt(window::dispose);
    }

    static AbstractButton findButton(Container root, String text) {
        Component component = findComponent(root, c -> c instanceof AbstractButton button && text.equals(button.getText()));
        return (AbstractButton) component;
    }

    static void flushEdt() {
        runOnEdt(() -> {
            // intentionally empty; ensures EDT queue is drained
        });
    }

    static <T> T getField(Object target, String fieldName, Class<T> type) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(fieldName, "fieldName");
        Objects.requireNonNull(type, "type");
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(target);
                return type.cast(value);
            } catch (NoSuchFieldException ignored) {
                clazz = clazz.getSuperclass();
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException("Cannot access field " + fieldName, ex);
            }
        }
        throw new IllegalStateException("Field " + fieldName + " not found on " + target.getClass());
    }

    static Component findComponent(Container root, Predicate<Component> matcher) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(matcher, "matcher");
        if (matcher.test(root)) {
            return root;
        }
        for (Component child : root.getComponents()) {
            if (matcher.test(child)) {
                return child;
            }
            if (child instanceof Container container) {
                Component match = findComponent(container, matcher);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    static boolean windowExists(Class<? extends Window> type) {
        return Arrays.stream(Window.getWindows())
                .anyMatch(window -> type.isInstance(window) && window.isDisplayable());
    }

    static void disposeAll(Class<? extends Window> type) {
        Arrays.stream(Window.getWindows())
                .filter(type::isInstance)
                .forEach(SwingTestUtils::dispose);
    }
}
