# JVM GC 测试报告

## Task 1: JDK build 配置和 Shenandoah GC 控制

展示了 JDK build 时 configure 的 JVM features 结果,以及如何打开和关闭 Shenandoah GC：

\task1\configure_result.jpg
\task1\使用&不使用Shenandoah GC.jpg

## Task 2: 不同 GC 参数的测试用例

通过不同的 GC 参数(Serial GC, Parallel Scavenge, G1GC, ZGC, Shenandoah GC)运行测试用例,并通过 GC 日志展示各个 GC 的阶段。

### 测试命令

\task2\test_case\test_command.jpg

### 测试结果总结

| 指标 | Serial GC | Parallel Scavenge | G1GC | ZGC | Shenandoah GC |
|------|-----------|-------------------|------|-----|---------------|
| 平均暂停时间 | 83.7221ms | 80.3514ms | 32.3205ms | 22.8676ms | 285.632ms |
| 最大暂停时间 | 3008ms | 2477ms | 50.810ms | 326ms | 1844ms |
| 总 GC 时间 | 4841ms | 3178ms | 3057ms | 335ms | 3794ms |
| 吞吐量 | 67.57% | 79.50% | 97.32% | 97.61% | 53.58% |
| 是否出现退化 | 是 | 是 | 是 | 否 | 是 |
| 内存使用效率 | 1MB->2MB->2MB | 2MB->2MB->2MB | 1MB->1MB->不可用 | 10MB->2854MB->4072MB | 2MB->不可用->2MB |
| 完成测试所需的时间 | 15692ms | 14644ms | 17749ms | 13996ms | 8174ms |

### 结果分析

#### 暂停时间

- **Serial GC**: 平均暂停时间（83.7221ms）和最大暂停时间（3008ms）都比较高，适合单线程应用。
- **Parallel Scavenge**: 平均暂停时间（80.3514ms）稍微比 Serial GC 低，但最大暂停时间（2477ms）仍然较高。
- **G1GC**: 平均暂停时间（32.3205ms）较低，最大暂停时间（50.810ms）也远低于 Serial 和 Parallel Scavenge，适合对暂停时间敏感的应用。
- **ZGC**: 平均暂停时间（22.8676ms）最低，最大暂停时间（326ms）也相对较低，非常适合需要低延迟的应用。
- **Shenandoah GC**: 平均暂停时间（285.632ms）最高，最大暂停时间（1844ms）也较高，适合对吞吐量要求高的场景。

#### 总 GC 时间

- **ZGC**（335ms）明显低于其他 GC 收集器，表明 ZGC 在这次测试中处理垃圾回收的时间最少。
- **G1GC**（3057ms）和 **Shenandoah GC**（3794ms）相比其他 GC 收集器，总 GC 时间也较高，可能与暂停时间较长有关。

#### 吞吐量

- **G1GC**（97.32%）和 **ZGC**（97.61%）的吞吐量最高，表示这些 GC 收集器能够更好地利用 CPU 资源。
- **Parallel Scavenge**（79.50%）虽然吞吐量也不错，但相对较低，可能是因为它在处理垃圾回收时产生的暂停时间较高。
- **Serial GC**（67.57%）和 **Shenandoah GC**（53.58%）的吞吐量较低，表明它们在处理垃圾回收时花费的时间较多，导致可用 CPU 时间减少。

#### 是否出现退化

- **ZGC** 在测试中没有出现退化，说明它在处理负载增加时能够保持较好的性能。
- 其他 GC 收集器均出现了退化，可能是由于在处理高负载或大规模堆时性能下降。

#### 内存使用效率

- **ZGC** 的内存使用效率在增长，但最终可用内存相对较高，适合需要大堆内存的应用。
- **G1GC** 的内存使用效率较高，最终无法用完全部内存，可能表明它在优化内存回收方面表现较好。
- **Serial GC** 和 **Parallel Scavenge** 内存使用较为稳定，但最终内存使用情况未达到最佳状态。
- **Shenandoah GC** 内存使用效率较差，显示出较大的波动性和不稳定性。

#### 完成测试所需的时间

- **Shenandoah GC** 完成测试的时间最短（8174ms），表明它的测试效率较高。
- 其他 GC 收集器在完成测试所需的时间上有一定差距，但总体上与其性能特性一致。

测试用例实现位置: `/task2/test_case/test_case`
测试用例日志位置: `/task2/test_case/logs`
测试用例终端运行截图: `/task2/test_case/test_command.jpg`
日志结果总结: `/task2/test_case/result.xlsx`

## Task 3: 比较各个 GC 的特点

通过测试用例比较各个 GC 的特点,重点关注 concurrent GC 在回收无法赶上分配时的行为。

### 预期行为

- G1GC: To-space exhausted -> Full GC
- ZGC: Allocation Stall
- Shenandoah GC: Pacing -> Degenerated GC -> Full GC

好的，以下是格式转化后的Task 3的日志结果分析和结论部分：

### 日志结果分析

#### G1GC: To-space exhausted -> Full GC

虽然没有出现明确的"To-space exhausted"情况，但日志显示了一些关键信息：

1. **GC触发原因**:
   ```
   [0.101s][info ][gc,start ] GC(0) Pause Young (Concurrent Start) (G1 Humongous Allocation)
   ```
   GC由大对象分配（Humongous Allocation）触发，而非To-space耗尽。

2. **堆使用情况**:
   ```
   [0.101s][debug][gc,heap ] GC(0) garbage-first heap total 524288K, used 236464K [0x00000000e0000000, 0x0000000100000000)
   ```
   GC开始时，堆内存使用率约为45%，表明系统面临一定的内存压力。

3. **并发周期启动**:
   ```
   [0.100s][debug][gc,ergo,ihop ] Request concurrent cycle initiation (occupancy higher than threshold) occupancy: 241172480B allocation request: 2097168B threshold: 241591910B (45.00) source: concurrent humongous allocation
   ```
   系统请求启动并发标记周期，因为堆占用率超过了阈值（45%）。

4. **Young GC**:
   这次GC是一次Young GC，同时启动了并发标记周期（Concurrent Start）。

#### ZGC: Allocation Stall

ZGC日志中出现了多次Allocation Stall，例如：

```
[64.279s][info ][gc ] Allocation Stall (Thread-9) 13.244ms
[64.279s][info ][gc ] Allocation Stall (Thread-1) 13.352ms
[64.279s][info ][gc ] Allocation Stall (Thread-5) 13.410ms
```

这表明ZGC确实遇到了分配停顿的情况。

#### Shenandoah GC: Pacing -> Degenerated GC -> Full GC

Shenandoah GC的日志展示了完整的预期序列：

1. **Pacing**:
   ```
   [38.386s][info ][gc,ergo ] Pacer for Idle. Initial: 5242K, Alloc Tax Rate: 1.0x
   ```

2. **Degenerated GC**:
   ```
   [38.388s][info ][gc,start ] GC(4152) Pause Degenerated GC (Outside of Cycle)
   ```

3. **Full GC**:
   ```
   [38.391s][info ][gc ] GC(4152) Cannot finish degeneration, upgrading to Full GC
   ```

### 结论

1. **G1GC**:
   - 虽然没有观察到明确的"To-space exhausted"情况，但系统面临着由大对象分配引起的内存压力。
   - G1GC通过启动并发标记周期来应对这种情况，这是它处理内存压力的正常方式。
   - 要观察到"To-space exhausted"的情况，可能需要更长时间的日志或更高的内存压力。
   - 在当前情况下，G1GC似乎仍然能够有效地管理内存，尽管面临着大对象分配的挑战。

2. **ZGC**:
   - 符合预期，出现了Allocation Stall的情况。
   - 这表明ZGC在某些时刻无法及时回收内存以满足分配需求，导致了短暂的停顿。

3. **Shenandoah GC**:
   - 完全符合预期，展现了Pacing -> Degenerated GC -> Full GC的完整序列。
   - 这表明Shenandoah GC在面对内存压力时，会逐步升级其GC策略，从轻量级的Pacing到更重的Degenerated GC，最后在必要时升级到Full GC。

测试用例实现位置:
- G1GC: `/task3/specific_test_case/G1GC/test_case`
- Shenandoah GC: `/task3/specific_test_case/ShenandoahGC/test_case`
- ZGC: `/task3/specific_test_case/ZGC/test_case`

测试用例日志位置: `~/logs`
测试用例终端运行截图: `/task3/specific_test_case/test_command.jpg`
