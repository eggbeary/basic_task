#!/bin/bash

JAVA_OPTS="-Xms512m -Xmx4g -verbose:gc -Xlog:gc*=debug:file=gc_%p.log:time,uptime,level,tags"

compile() {
    javac AdvancedGCTest.java
}

run_test() {
    gc_name=$1
    gc_opts=$2
    echo "Running test with $gc_name"
    java $JAVA_OPTS $gc_opts AdvancedGCTest > ${gc_name}_output.txt 2>&1
    echo "Test completed for $gc_name"
}

analyze_gc_log() {
    gc_name=$1
    log_file=$(ls gc_*.log)
    echo "Analyzing GC log for $gc_name"
    
    total_gc_time=$(grep "Total GC time:" ${gc_name}_output.txt | awk '{print $4}')
    avg_pause_time=$(grep "Pause" $log_file | awk '{sum += $NF; count++} END {print sum/count}')
    max_pause_time=$(grep "Max Pause Time:" ${gc_name}_output.txt | awk '{print $4}')
    total_runtime=$(grep "Total runtime:" ${gc_name}_output.txt | awk '{print $3}')
    throughput=$(grep "Throughput:" ${gc_name}_output.txt | awk '{print $2}')
    initial_memory=$(grep "Initial Heap Usage:" ${gc_name}_output.txt | awk '{print $4}')
    final_memory=$(grep "Final Heap Usage:" ${gc_name}_output.txt | awk '{print $4}')
    max_memory=$(grep "Current Heap Usage:" ${gc_name}_output.txt | awk '{if ($4>max) max=$4} END {print max}')
    
    echo "Total GC Time: $total_gc_time ms"
    echo "Average Pause Time: $avg_pause_time ms"
    echo "Max Pause Time: $max_pause_time ms"
    echo "Total Runtime: $total_runtime ms"
    echo "Throughput: $throughput"
    echo "Memory Usage: ${initial_memory}MB -> ${max_memory}MB -> ${final_memory}MB"
    echo ""
}

compile

run_test "SerialGC" "-XX:+UseSerialGC"
analyze_gc_log "SerialGC"

run_test "ParallelGC" "-XX:+UseParallelGC"
analyze_gc_log "ParallelGC"

run_test "G1GC" "-XX:+UseG1GC"
analyze_gc_log "G1GC"

run_test "ZGC" "-XX:+UseZGC"
analyze_gc_log "ZGC"

run_test "ShenandoahGC" "-XX:+UseShenandoahGC"
analyze_gc_log "ShenandoahGC"

echo "All tests completed."

