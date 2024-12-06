package com.xu.layout.entity;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xuzc
 * <p>
 * 树形结构表示布局的节点，处理遍历和布局树构建
 */
public class LayoutNode {

    public RegionBlock content;

    @Getter
    public List<LayoutNode> children = new ArrayList<>();

    public LayoutNode(RegionBlock content) {
        this.content = content;
    }

    public void addChild(LayoutNode child) {
        children.add(child);
    }

    public RegionBlock getLabelContent() {
        return content;
    }

    // 判断节点是否为叶节点（即没有子节点）
    public boolean isLeaf() {
        return children.isEmpty();
    }

    // 预序遍历，获取该节点及其子节点的内容
    public List<RegionBlock> preorderTraverse() {
        List<RegionBlock> result = new ArrayList<>();
        if (content != null) {
            result.add(content);
        }
        for (LayoutNode child : children) {
            result.addAll(child.preorderTraverse());
        }
        return result;
    }


}
