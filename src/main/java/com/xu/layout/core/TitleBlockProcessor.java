package com.xu.layout.core;

import com.xu.layout.entity.LayoutNode;
import com.xu.layout.entity.RegionBlock;

import java.util.List;
import java.util.Map;

/**
 * @author xuzc
 * <p>
 * 处理标题块的逻辑
 */
public class TitleBlockProcessor {

    public static boolean isUnderTitle(RegionBlock title, RegionBlock text, List<RegionBlock> latterTitles) {
        float[] titleBbox = title.getBbox();
        float[] textBbox = text.getBbox();
        boolean isBelow = textBbox[1] > titleBbox[3];

        if (!isBelow) {
            return false;
        }

        if (!horizontalOverlap(titleBbox, textBbox)) {
            return false;
        }

        for (RegionBlock otherTitle : latterTitles) {
            if (otherTitle == title) {
                continue;
            }
            float[] otherBbox = otherTitle.getBbox();
            boolean isOtherBelow = textBbox[3] > otherBbox[1];
            boolean hasXOverlap = horizontalOverlapWithBelowTitle(otherBbox, textBbox);
            if (isOtherBelow && hasXOverlap) {
                return false;
            }
        }
        return true;
    }

    public static List<RegionBlock> processAndTraverse(List<RegionBlock> texts) {
        return processAndTraverse(texts, null);
    }

    public static List<RegionBlock> processAndTraverse(List<RegionBlock> texts, RegionBlock parent) {
        int columns = ColumnDetector.detectColumns(texts);
        Map<Integer, List<RegionBlock>> columnMap = ColumnDetector.distributeToColumns(texts, columns);

        LayoutNode root = new LayoutNode(parent);
        for (int col = 0; col < columns; col++) {
            List<RegionBlock> columnTexts = columnMap.get(col);
            LayoutNode columnNode = new LayoutNode(null);
            for (RegionBlock text : columnTexts) {
                columnNode.children.add(new LayoutNode(text));
            }
            root.children.add(columnNode);
        }
        return root.preorderTraverse();
    }

    private static boolean horizontalOverlap(float[] titleBbox, float[] textBbox) {
        return (textBbox[0] < titleBbox[2] && textBbox[2] > titleBbox[0]) ||
                (textBbox[0] < titleBbox[0] && textBbox[2] > titleBbox[0]) ||
                (textBbox[0] < titleBbox[2] && textBbox[2] > titleBbox[2]);
    }

    private static boolean horizontalOverlapWithBelowTitle(float[] titleBbox, float[] textBbox) {
        return (textBbox[0] < titleBbox[2] && textBbox[2] > titleBbox[0]) ||
                (textBbox[0] < titleBbox[0] && textBbox[2] > titleBbox[0]) ||
                (textBbox[0] < titleBbox[2] && textBbox[2] > titleBbox[2]) ||
                (textBbox[0] <= titleBbox[0] && textBbox[2] >= titleBbox[2]);
    }

}
