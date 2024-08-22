import java.util.ArrayList;
import java.util.List;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.GarbageCollectorMXBean;

public class AdvancedGCTest {
    private static final int OBJECT_SIZE = 1024 * 1024; // 1MB
    private static final int ALLOCATION_INTERVAL = 1; // 毫秒
    private static final long RUNTIME = 2 * 60 * 1000; // 2分钟

    public static void main(String[] args) {
        List<byte[]> list = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        long lastPrintTime = startTime;
        int allocations = 0;
        long totalAllocated = 0;
        long gcStartTime = getTotalGCTime();
        long maxPauseTime = 0;

        System.out.println("Initial Heap Usage: " + getUsedMemory() + "MB");

        while (System.currentTimeMillis() - startTime < RUNTIME) {
            try {
                list.add(new byte[OBJECT_SIZE]);
                allocations++;
                totalAllocated += OBJECT_SIZE;
                if (allocations % 100 == 0) {
                    list.subList(0, 50).clear(); // 清除一些对象以模拟对象生命周期
                }
                Thread.sleep(ALLOCATION_INTERVAL);

                // 每10秒打印一次当前内存使用情况
                if (System.currentTimeMillis() - lastPrintTime > 10000) {
                    System.out.println("Current Heap Usage: " + getUsedMemory() + "MB");
                    lastPrintTime = System.currentTimeMillis();
                }

                // 更新最大暂停时间
                for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
                    long pauseTime = gcBean.getCollectionTime();
                    if (pauseTime > maxPauseTime) {
                        maxPauseTime = pauseTime;
                    }
                }

            } catch (OutOfMemoryError e) {
                System.out.println("OutOfMemoryError occurred after " + allocations + " allocations");
                break;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        long gcTime = getTotalGCTime() - gcStartTime;
        double throughput = 100.0 * (totalTime - gcTime) / totalTime;

        System.out.println("Final Heap Usage: " + getUsedMemory() + "MB");
        System.out.println("Test completed. Total allocations: " + allocations);
        System.out.println("Total allocated memory: " + (totalAllocated / (1024 * 1024)) + " MB");
        System.out.println("Total runtime: " + totalTime + " ms");
        System.out.println("Total GC time: " + gcTime + " ms");
        System.out.println("Max Pause Time: " + maxPauseTime + " ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + "%");
    }

    private static long getUsedMemory() {
        long usedMemory = 0;
        for (MemoryPoolMXBean memoryPoolMXBean : ManagementFactory.getMemoryPoolMXBeans()) {
            if (memoryPoolMXBean.getName().toLowerCase().contains("heap")) {
                MemoryUsage usage = memoryPoolMXBean.getUsage();
                usedMemory += usage.getUsed();
            }
        }
        return usedMemory / (1024 * 1024);
    }

    private static long getTotalGCTime() {
        long gcTime = 0;
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcTime += gcBean.getCollectionTime();
        }
        return gcTime;
    }
}

