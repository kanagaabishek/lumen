package com.lumen.server.domain;

import java.util.Map;

public class TraceBuildResult {
    private SpanNode root;
    private Map<String, SpanNode> nodeMap;

    public TraceBuildResult(SpanNode root, Map<String, SpanNode> nodeMap) {
        this.root = root;
        this.nodeMap = nodeMap;
    }

    public SpanNode getRoot() {
        return root;
    }

    public Map<String, SpanNode> getNodeMap() {
        return nodeMap;
    }
}
