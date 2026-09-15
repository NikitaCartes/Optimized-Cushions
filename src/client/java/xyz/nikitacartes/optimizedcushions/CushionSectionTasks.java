package xyz.nikitacartes.optimizedcushions;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Tasks deferred until a rebuilt section mesh is installed.
 *
 * <p>The baked flag on a cushion must flip at exactly the moment the mesh changes:
 * flipping it when the tracker updates would leave a gap (entity hidden before its
 * mesh copy exists) or an overlap (entity shown while its mesh copy still exists),
 * because section remeshing completes asynchronously. Producers run on the client
 * main thread (tracker) and on meshing worker threads (section compiler); the
 * consumer runs wherever {@code RenderSection.setSectionMesh} runs, so everything
 * here is concurrent and tasks must only do map reads plus volatile flag writes.
 */
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
