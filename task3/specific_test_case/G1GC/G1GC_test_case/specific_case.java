import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class specific_case {
    private static final int ALLOCATION_SIZE = 2 * 1024 * 1024; // 2MB
    private static final int ALLOCATION_ITERATIONS = 5000;
    private static final int THREAD_COUNT = 8;

    private static final Random random = new Random();

    public static void main(String[] args) throws InterruptedException {
        List<byte[]> globalList = new ArrayList<>();

        System.out.println("Starting G1GC stress test...");
        long startTime = System.currentTimeMillis();

        Thread[] threads = new Thread[THREAD_COUNT];
        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i] = new Thread(() -> allocateMemory(globalList));
            threads[i].start();
        }

        try {
            // Main thread loops, allowing other threads to run and fill the memory
            for (int i = 0; i < ALLOCATION_ITERATIONS; i++) {
                if (i % 100 == 0) {
                    removeRandomObjects(globalList); // Occasionally remove some objects to prevent immediate OOM
                    Thread.sleep(20); // Short pause to prevent too rapid allocation
                }
            }
        } catch (OutOfMemoryError e) {
            System.out.println("OutOfMemoryError occurred. This is part of the test.");
        } finally {
            for (Thread thread : threads) {
                thread.interrupt();
                thread.join();
            }
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Test completed in " + (endTime - startTime) + " ms");
        System.out.println("Final list size: " + globalList.size());
    }

    private static void allocateMemory(List<byte[]> list) {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                byte[] array = new byte[ALLOCATION_SIZE];
                synchronized (list) {
                    list.add(array);
                }
                Thread.sleep(5); // Slight delay to simulate realistic allocation rate
            } catch (OutOfMemoryError | InterruptedException e) {
                break; // Exit on interruption or memory pressure
            }
        }
    }

    private static void removeRandomObjects(List<byte[]> list) {
        synchronized (list) {
            int objectsToRemove = Math.min(10, list.size() / 10);
            for (int i = 0; i < objectsToRemove; i++) {
                if (!list.isEmpty()) {
                    int indexToRemove = random.nextInt(list.size());
                    list.remove(indexToRemove);
                }
            }
        }
    }
}

