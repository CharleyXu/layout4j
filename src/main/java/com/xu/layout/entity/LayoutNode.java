package com.xu.layout.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xuzc
 */
public class LayoutNode {

    public RegionBlock content;

    public List<LayoutNode> children = new ArrayList<>();

    public LayoutNode(RegionBlock content) {
        this.content = content;
    }

    public void addChild(LayoutNode child) {
        children.add(child);
    }

    public List<LayoutNode> getChildren() {
        return children;
    }

    public RegionBlock getLabelContent() {
        return content;
    }

}
