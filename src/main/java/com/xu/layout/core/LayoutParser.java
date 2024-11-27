package com.xu.layout.core;

import com.xu.layout.entity.LayoutNode;
import com.xu.layout.entity.RegionBlock;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xuzc
 * <p>
 * 基于文本位置的版面分析/文本排序算法
 */
@NoArgsConstructor
public class LayoutParser {

    // Y坐标误差阈值
    private static final float Y_THRESHOLD = 5.0f;

    // 区域块误差阈值
    private static final float THRESHOLD = 3.0f;

    private static final String TITLE = "Title";

    public static List<RegionBlock> processLayout(List<RegionBlock> contentList) {
        // 移除重叠的小区域块
        List<RegionBlock> filteredList = removeOverlappingSmallerBlocks(contentList);

        // 按 y 坐标（从上到下），再按 x 坐标（从左到右）排序
        filteredList.sort((lc1, lc2) -> {
            double y1 = lc1.getBbox()[1];
            double y2 = lc2.getBbox()[1];
            // 如果y坐标在误差范围内，视为同一行
            if (Math.abs(y1 - y2) <= Y_THRESHOLD) {
                // 同一行时，按x坐标从左到右排序
                return Double.compare(lc1.getBbox()[0], lc2.getBbox()[0]);
            }
            // 不在误差范围内，则按y坐标从上到下排序
            return Double.compare(y1, y2);
        });
        // 标记已处理的区域块
        Set<RegionBlock> processedBlocks = new HashSet<>();
        // 处理标题前的区域块
        List<RegionBlock> firstProcessList = new ArrayList<>();
        for (RegionBlock content : filteredList) {
            if (!TITLE.equalsIgnoreCase(content.getLabel())) {
                firstProcessList.add(content);
                processedBlocks.add(content);
            } else {
                break;
            }
        }
        List<RegionBlock> preTitleBlocks = processAndTraverse(firstProcessList);
        // 最终存储结果
        List<RegionBlock> result = new ArrayList<>(preTitleBlocks);

        // 第四步：找到所有 Title，作为顶层分组
        List<RegionBlock> titles = filteredList.stream().filter(lc -> TITLE.equalsIgnoreCase(lc.getLabel())).collect(Collectors.toList());
        processedBlocks.addAll(titles);
        // 找到Title下的文本块
        for (int i = 0; i < titles.size(); i++) {
            RegionBlock title = titles.get(i);
//            if (title.getContent().contains("深海一号”二期完成首船")) {
//                System.out.println();
//            }
            int current = i;
            // 找到当前 Title 覆盖的文本块
            List<RegionBlock> textsUnderTitle = filteredList.stream().filter(lc -> !TITLE.equalsIgnoreCase(lc.getLabel())).filter(lc -> isUnderTitle(title, lc, titles.subList(current + 1, titles.size()))).collect(Collectors.toList());
            processedBlocks.addAll(textsUnderTitle);
            // 计算列数、分列、构建 N 叉树并前序遍历
            List<RegionBlock> titleBlocks = processAndTraverse(textsUnderTitle, title);
            result.addAll(titleBlocks);
        }
        // 处理未被包含的剩余区域块
        List<RegionBlock> unprocessedBlocks = filteredList.stream().filter(lc -> !processedBlocks.contains(lc)).toList();
        result.addAll(unprocessedBlocks);
        return result;
    }

    private static List<RegionBlock> removeOverlappingSmallerBlocks(List<RegionBlock> list) {
        // 按面积从大到小排序
        list.sort(Comparator.comparingDouble((RegionBlock lc) -> -area(lc.getBbox())));

        // 用于存储最终保留的区域块
        List<RegionBlock> result = new ArrayList<>();

        // 遍历区域块
        for (RegionBlock current : list) {
            boolean isContained = false;

            // 检查是否被已保留的块包含
            for (RegionBlock largerBlock : result) {
                if (isContainedIn(current.getBbox(), largerBlock.getBbox())) {
                    isContained = true;
                    break;
                }
            }

            // 如果没有被包含，则保留
            if (!isContained) {
                result.add(current);
            }
        }

        return result;
    }

    /**
     * 计算区域块的面积
     */
    private static double area(float[] bbox) {
        return (bbox[2] - bbox[0]) * (bbox[3] - bbox[1]);
    }

    /**
     * 判断小区域是否被大区域完全包含, 允许一定的误差
     */
    private static boolean isContainedIn(float[] smallBbox, float[] largeBbox) {
        return smallBbox[0] >= largeBbox[0] - THRESHOLD && smallBbox[1] >= largeBbox[1] - THRESHOLD && smallBbox[2] <= largeBbox[2] + THRESHOLD && smallBbox[3] <= largeBbox[3] + THRESHOLD;
    }

    private static boolean isUnderTitle(RegionBlock title, RegionBlock text, List<RegionBlock> latterTitles) {
        float[] titleBbox = title.getBbox();
        float[] textBbox = text.getBbox();
        // 判断 text 是否在 title 的下方（纵坐标判断）
        // text 上边界在 title 下边界下方
        boolean isBelow = textBbox[1] > titleBbox[3];
        // 如果不在 title 下方，返回 false
        if (!isBelow) {
            return false;
        }

        // 判断 text 是否与 title 在横坐标上有重叠
        boolean hasHorizontalOverlap = horizontalOverlap(titleBbox, textBbox);
        // 如果横坐标没有重叠，返回 false
        if (!hasHorizontalOverlap) {
            return false;
        }

        // 判断 text 是否与后续的 title 有横坐标或纵坐标重叠
        for (RegionBlock otherTitle : latterTitles) {
            if (otherTitle == title) {
                // 跳过当前 title
                continue;
            }
            float[] otherBbox = otherTitle.getBbox();
            // 判断 text 的下边界是否大于其他 title 的上边界
            boolean isOtherBelow = textBbox[3] > otherBbox[1];
            // 判断横坐标是否有重叠
            boolean hasXOverlap = horizontalOverlapWithBelowTitle(otherBbox, textBbox);
            // 与其他 title 有重叠，返回 false
            if (isOtherBelow && hasXOverlap) {
                return false;
            }
        }
        // 满足条件
        return true;
    }

    /**
     * 判断 text 是否与 title 在横坐标上有重叠
     */
    private static boolean horizontalOverlap(float[] titleBbox, float[] textBbox) {
        // 完全重叠
        return (textBbox[0] < titleBbox[2] && textBbox[2] > titleBbox[0])
                // 左侧重叠
                || (textBbox[0] < titleBbox[0] && textBbox[2] > titleBbox[0])
                // 右侧重叠
                || (textBbox[0] < titleBbox[2] && textBbox[2] > titleBbox[2]);
    }

    /**
     * 判断 text 是否与 title 在横坐标上有重叠
     */
    private static boolean horizontalOverlapWithBelowTitle(float[] titleBbox, float[] textBbox) {
        // 完全重叠
        return (textBbox[0] < titleBbox[2] && textBbox[2] > titleBbox[0])
                // 左侧重叠
                || (textBbox[0] < titleBbox[0] && textBbox[2] > titleBbox[0])
                // 右侧重叠
                || (textBbox[0] < titleBbox[2] && textBbox[2] > titleBbox[2])
                // 完全包含（text 的左右边界分别覆盖了 title 的左右边界）
                || (textBbox[0] <= titleBbox[0] && textBbox[2] >= titleBbox[2]);
    }

    private static List<RegionBlock> processAndTraverse(List<RegionBlock> texts) {
        return processAndTraverse(texts, null);
    }

    private static List<RegionBlock> processAndTraverse(List<RegionBlock> texts, RegionBlock parent) {
        int columns = detectColumns(texts);
        Map<Integer, List<RegionBlock>> columnMap = distributeToColumns(texts, columns);

        LayoutNode root = new LayoutNode(parent);
        for (int col = 0; col < columns; col++) {
            List<RegionBlock> columnTexts = columnMap.get(col);
            LayoutNode columnNode = new LayoutNode(null); // 列节点
            for (RegionBlock text : columnTexts) {
                columnNode.children.add(new LayoutNode(text));
            }
            root.children.add(columnNode);
        }
        return preorderTraverse(root);
    }

    private static int detectColumns(List<RegionBlock> texts) {
        // 根据文本块的 x 坐标分布来判断列数，考虑误差阈值
        List<Float> xCoordinates = new ArrayList<>();
        for (RegionBlock text : texts) {
            xCoordinates.add(text.getBbox()[0]); // 起始 x 坐标
        }
        // 排序 x 坐标
        Collections.sort(xCoordinates);

        int columns = 0;
        float currentColumnX = Float.MIN_VALUE;

        for (float x : xCoordinates) {
            if (currentColumnX == Float.MIN_VALUE || Math.abs(x - currentColumnX) > THRESHOLD) {
                // 新的一列
                columns++;
                // 更新当前列的 x 坐标
                currentColumnX = x;
            }
        }
        return columns;
    }

    private static Map<Integer, List<RegionBlock>> distributeToColumns(List<RegionBlock> texts, int columns) {
        // 按 x 坐标分列
        Map<Integer, List<RegionBlock>> columnMap = new HashMap<>();
        float[] columnBoundaries = calculateColumnBoundaries(texts, columns);

        for (int i = 0; i < columns; i++) {
            columnMap.put(i, new ArrayList<>());
        }

        for (RegionBlock text : texts) {
            float x = text.getBbox()[0];
            for (int col = 0; col < columns; col++) {
                if (x >= columnBoundaries[col] && x < columnBoundaries[col + 1]) {
                    columnMap.get(col).add(text);
                    break;
                }
            }
        }
        return columnMap;
    }

    private static float[] calculateColumnBoundaries(List<RegionBlock> texts, int columns) {
        // 根据 x 坐标范围划分列边界
        float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
        for (RegionBlock text : texts) {
            minX = Math.min(minX, text.getBbox()[0]);
            maxX = Math.max(maxX, text.getBbox()[2]);
        }
        float[] boundaries = new float[columns + 1];
        float step = (maxX - minX) / columns;
        for (int i = 0; i <= columns; i++) {
            boundaries[i] = minX + i * step;
        }
        return boundaries;
    }

    private static List<RegionBlock> preorderTraverse(LayoutNode node) {
        List<RegionBlock> result = new ArrayList<>();
        if (node.content != null) {
            result.add(node.content);
        }
        for (LayoutNode child : node.children) {
            result.addAll(preorderTraverse(child));
        }
        return result;
    }

}
