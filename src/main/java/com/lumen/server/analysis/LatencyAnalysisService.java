package com.lumen.server.analysis;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.lumen.server.domain.SpanNode;

import java.util.ArrayList;

@Service
public class LatencyAnalysisService {

    // Input:  [[10,50], [30,80], [85,95]]
    // Output: [[10,80], [85,95]]  ← overlapping ones merged

    public List<long[]> mergeIntervals(List<long[]> input) {
        // Step 1: sort by start time (copy first — callers may pass unmodifiable lists)
        List<long[]> intervals = new ArrayList<>(input);
        Collections.sort(intervals, (a, b) -> Long.compare(a[0], b[0]));
        // Step 2: walk through, if current start <= previous end, merge
        List<long[]> merged = new ArrayList<>();
        long[] current = intervals.get(0);
        for (int i = 1; i < intervals.size(); i++) {
            long[] next = intervals.get(i);
            if (next[0] <= current[1]) {
                current[1] = Math.max(current[1], next[1]);
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current); // add the last interval
        return merged;
        // Step 3: otherwise start a new interval
    }

    public void calculateSelfTime(SpanNode node) {
        if (node == null) return;

        if (node.getChildrens().isEmpty()) {
            // Leaf node — all time is self time
            node.setSelfTimeMs(node.getSpan().getDurationMs());
            return;
        }
        
        // First recurse into children
        // (children must have their selfTime calculated before parent)
        for (SpanNode child : node.getChildrens()) {
            calculateSelfTime(child);
        }
        
        // Collect child intervals
        List<long[]> intervals = new ArrayList<>();
        for (SpanNode child : node.getChildrens()) {
            intervals.add(new long[]{
                child.getSpan().getstartTimeNano() / 1_000_000,
                child.getSpan().getEndTimeNano()/ 1_000_000
            });
        }
        
        // Merge overlapping intervals
        List<long[]> merged = mergeIntervals(intervals);
        
        // Sum merged durations
        long coveredTime = 0;
        for (long[] interval : merged) {
            coveredTime += interval[1] - interval[0];
        }
        
        // Self time = total - covered by children
        long selfTime = node.getSpan().getDurationMs() - coveredTime;
        node.setSelfTimeMs(Math.max(0, selfTime));
    }
    
    public long computeCriticalTime(SpanNode node, Map<String,String> criticalTimeMap) {
        if(node == null) return 0;

        //leaf node
        if(node.getChildrens().isEmpty()){
            return node.getSelfTimeMs();
        }

        long maxChildCriticalTime = 0;
        String criticalChildId = null;
        //Child node
        for(SpanNode child : node.getChildrens()){
            long childTime = computeCriticalTime(child,criticalTimeMap);
            if(childTime>maxChildCriticalTime){
                maxChildCriticalTime = childTime;
                criticalChildId = child.getSpan().getSpanId();
            }
        }

        if(criticalChildId != null){
            criticalTimeMap.put(node.getSpan().getSpanId(),criticalChildId);
        }

        return node.getSelfTimeMs() + maxChildCriticalTime;

    }

    public List<String> collectCriticalPath(SpanNode node,Map<String,String> criticalPathMap,Map<String,SpanNode> nodeMap){
        List<String> criticalPathSpanIdList = new ArrayList<>();
        if(node == null) return criticalPathSpanIdList;

        SpanNode current = node;
        while (current != null) {
            criticalPathSpanIdList.add(current.getSpan().getSpanId());
            if (current.getChildrens().isEmpty()) break;

            // Pick the child whose total critical time equals the stored max
            String expectedMax = criticalPathMap.getOrDefault(current.getSpan().getSpanId(), null);
            SpanNode criticalChild = nodeMap.get(expectedMax);
            if(criticalChild == null) break;
            current = criticalChild;
        }

        return criticalPathSpanIdList;

    }

    public SpanNode findBottleNeck(SpanNode root,List<String> criticalPathSpanIdList,Map<String,SpanNode> nodeMap){
        /*
         We need to walk through the criticalPathSpanIdList
         with nodeMap to get the spanNode with the SpanId in the List
         to find the highest selftime in the List
         and return it as bottleNeck
        */

         SpanNode bottleNeckNode = null;
         for(String criticalSpanId : criticalPathSpanIdList){
            SpanNode currNode = nodeMap.get(criticalSpanId);
            if(currNode != null){
                if(bottleNeckNode != null){
                    if(currNode.getSelfTimeMs()>bottleNeckNode.getSelfTimeMs()) bottleNeckNode = currNode;
                }else bottleNeckNode = currNode;
                
            }
         }

         return bottleNeckNode;
    }

    
}