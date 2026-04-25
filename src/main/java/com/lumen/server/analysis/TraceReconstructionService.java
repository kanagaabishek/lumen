package com.lumen.server.analysis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.lumen.server.domain.Span;
import com.lumen.server.domain.SpanNode;

@Service
public class TraceReconstructionService {
     
    public SpanNode buildTree(List<Span> spans) {

        Map<String, SpanNode> nodeMap = new HashMap<>();

        for(Span span : spans){
            SpanNode spanNode = new SpanNode();
            spanNode.setSpan(span);
            nodeMap.put(span.getSpanId(),spanNode);
        }
        
        SpanNode root = null;

        for(Span span : spans){
            SpanNode node = nodeMap.get(span.getSpanId());
            if(span.getParentSpanId() == null || span.getParentSpanId().isEmpty()){
                root = node;
                continue;
            }
            SpanNode parentNode = nodeMap.get(span.getParentSpanId());
            if(parentNode != null){
                if(parentNode.getChildrens() != null){
                    List<SpanNode> childrens = parentNode.getChildrens();
                    childrens.add(node);
                    parentNode.setChildrens(childrens);
                }
            }else{
                // FIX ME 
                // Ignoring the orphan spans for now
            }
        }
        
        return root;

    }
    
}
