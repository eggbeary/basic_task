import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class specific_case {
    private static final int SMALL_ALLOCATION_SIZE = 1024 * 1024; // 1MB
    private static final int MEDIUM_ALLOCATION_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int LARGE_ALLOCATION_SIZE = 100 * 1024 * 1024; // 100MB
    private static final int ALLOCATION_ITERATIONS = 1000;
    private static final int THREAD_COUNT = 10;

    private static final Random random = new Random();
    private static final AtomicLong allocatedMemory = new AtomicLong(0);

    private static volatile boolean shouldStop = false;

    public static void main(String[] args) throws InterruptedException {
        List<byte[]> globalList = new ArrayList<>();

        System.out.println("Starting Extreme GC stress test...");
        long startTime = System.currentTimeMillis();

        Thread[] threads = new Thread[THREAD_COUNT];
        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i] = new Thread(() -> allocateMemory(globalList));
            threads[i].start();
        }

        for (int i = 0; i < ALLOCATION_ITERATIONS; i++) {
            try {
                if (i % 10 == 0) {
                    allocateLargeObject(globalList);
                } else if (i % 3 == 0) {
                    allocateMediumObject(globalList);
                }

                if (i % 50 == 0) {
                    System.gc(); // Explicitly request GC
                }

                removeRandomObjects(globalList);
            } catch (OutOfMemoryError e) {
                System.out.println("OutOfMemoryError occurred. Clearing some memory...");
                globalList.subList(0, globalList.size() / 2).clear();
                System.gc();
            }
        }

        shouldStop = true;
        for (Thread thread : threads) {
            thread.join();
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Test completed in " + (endTime - startTime) + " ms");
        System.out.println("Final list size: " + globalList.size());
        System.out.println("Total allocated memory: " + allocatedMemory.get() / (1024 * 1024) + " MB");
    }

    private static void allocateMemory(List<byte[]> list) {
        while (!shouldStop) {
            try {
                byte[] array = new byte[SMALL_ALLOCATION_SIZE];
                synchronized (list) {
                    list.add(array);
                }
                allocatedMemory.addAndGet(SMALL_ALLOCATION_SIZE);
            } catch (OutOfMemoryError e) {
                // Ignore and continue
            }
        }
    }

    private static void allocateMediumObject(List<byte[]> list) {
        try {
            byte[] array = new byte[MEDIUM_ALLOCATION_SIZE];
            synchronized (list) {
                list.add(array);
            }
            allocatedMemory.addAndGet(MEDIUM_ALLOCATION_SIZE);
        } catch (OutOfMemoryError e) {
            System.out.println("OutOfMemoryError occurred during medium allocation.");
        }
    }

    private static void allocateLargeObject(List<byte[]> list) {
        try {
            byte[] array = new byte[LARGE_ALLOCATION_SIZE];
            synchronized (list) {
                list.add(array);
            }
            allocatedMemory.addAndGet(LARGE_ALLOCATION_SIZE);
        } catch (OutOfMemoryError e) {
            System.out.println("OutOfMemoryError occurred during large allocation.");
        }
    }

    private static void removeRandomObjects(List<byte[]> list) {
        synchronized (list) {
            int objectsToRemove = Math.min(10, list.size() / 2);
            for (int i = 0; i < objectsToRemove; i++) {
                if (!list.isEmpty()) {
                    int indexToRemove = random.nextInt(list.size());
                    byte[] removed = list.remove(indexToRemove);
                    allocatedMemory.addAndGet(-removed.length);
                }
            }
        }
    }
}
