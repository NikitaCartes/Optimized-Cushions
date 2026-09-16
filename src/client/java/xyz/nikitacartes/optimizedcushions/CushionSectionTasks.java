package xyz.nikitacartes.optimizedcushions;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class CushionSectionTasks {
    private static final Map<Long, Queue<Runnable>> TASKS = new ConcurrentHashMap<>();

    private CushionSectionTasks() {
    }

    public static void addTask(final long sectionKey, final Runnable task) {
        TASKS.computeIfAbsent(sectionKey, key -> new ConcurrentLinkedQueue<>()).add(task);
    }

    public static void executeTasks(final long sectionKey) {
        Queue<Runnable> tasks = TASKS.remove(sectionKey);
        if (tasks == null) {
            return;
        }
        Runnable task;
        while ((task = tasks.poll()) != null) {
            try {
                task.run();
            } catch (Exception ignored) {
            }
        }
    }

    public static void clear() {
        TASKS.clear();
    }
}
