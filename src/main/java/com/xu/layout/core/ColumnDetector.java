package com.xu.layout.core;

import com.xu.layout.entity.RegionBlock;

import java.util.*;

/**
 * @author xuzc
 */
public class ColumnDetector {

    public static Map<Integer, List<RegionBlock>> detectColumns(List<RegionBlock> texts) {
        // 存储每个文本块的 x1 和 x2
        List<Float> xStarts = new ArrayList<>();
        List<Float> xEnds = new ArrayList<>();

        for (RegionBlock text : texts) {
            float x1 = text.getBbox()[0];
            float x2 = text.getBbox()[2];
            xStarts.add(x1);
            xEnds.add(x2);
        }

        // 排序文本块的 x1 和 x2
        List<Float> sortedStarts = new ArrayList<>(xStarts);
        List<Float> sortedEnds = new ArrayList<>(xEnds);
        Collections.sort(sortedStarts);
        Collections.sort(sortedEnds);

        // 计算文本块之间的间距（忽略异常值）
        List<Float> gaps = new ArrayList<>();
        for (int i = 1; i < sortedStarts.size(); i++) {
            float gap = sortedStarts.get(i) - sortedEnds.get(i - 1);
            // 排除间距过大的情况，比如页面空白部分
            // 只考虑合理的间距
            if (gap > 10) {
                gaps.add(gap);
            }
        }

        // 计算平均间距（稳定性更高）
        float averageGap = (float) gaps.stream().mapToDouble(Float::doubleValue).average().orElse(0);
        // 阈值最大为20
        averageGap = Math.min(averageGap, 20f);

        // 存储列的文本块
        Map<Integer, List<RegionBlock>> columnsMap = new HashMap<>();
        int columnIndex = 0;

        for (RegionBlock text : texts) {
            boolean assigned = false;
            // 遍历现有列，判断当前文本块是否应该归入其中
            for (Map.Entry<Integer, List<RegionBlock>> entry : columnsMap.entrySet()) {
                List<RegionBlock> column = entry.getValue();
                // 获取该列的范围（x1, x2）
                float columnStart = column.get(0).getBbox()[0];
                float columnEnd = column.get(column.size() - 1).getBbox()[2];

                // 判断当前文本块是否与已有列重叠（间距小于平均间距）
                if (isOverlap(text, columnStart, columnEnd, averageGap) || shouldMergeColumns(column, text, averageGap)) {
                    column.add(text);
                    assigned = true;
                    break;
                }
            }

            // 如果没有找到合适的列，则新建一列
            if (!assigned) {
                List<RegionBlock> newColumn = new ArrayList<>();
                newColumn.add(text);
                columnsMap.put(columnIndex++, newColumn);
            }
        }

        // 返回列的 Map
        return columnsMap;
    }

    private static boolean isOverlap(RegionBlock text, float columnStart, float columnEnd, float adjustedThreshold) {
        // 判断当前文本块是否与列的范围重叠
        float x1 = text.getBbox()[0];
        float x2 = text.getBbox()[2];

        // 如果文本块的范围与列的范围有交集，或者它们之间的间距小于调整后的阈值，则认为重叠
        return !(x2 + adjustedThreshold < columnStart || x1 - adjustedThreshold > columnEnd);
    }

    // 判断是否需要合并列
    private static boolean shouldMergeColumns(List<RegionBlock> column, RegionBlock text, float averageGap) {
        // 获取列的宽度和当前文本块的宽度
        float columnWidth = column.get(column.size() - 1).getBbox()[2] - column.get(0).getBbox()[0];
        float textWidth = text.getBbox()[2] - text.getBbox()[0];

        // 计算当前列内的密集度：列内所有文本块的平均宽度
        double averageColumnWidth = column.stream()
                .map(b -> b.getBbox()[2] - b.getBbox()[0])
                .mapToDouble(Float::doubleValue)
                .average()
                .orElse(0.0);

        // 容忍度：如果列内已有文本块密集，可能需要更宽松的合并条件
        double widthTolerance = Math.max(averageGap, averageColumnWidth * 0.4f);

        // 1. 判断文本块宽度与列宽度差异
        boolean widthCondition = textWidth < columnWidth * 0.5f && Math.abs(columnWidth - textWidth) < widthTolerance;

        // 2. 判断是否有足够重叠或接近
        boolean overlapCondition = isOverlap(text, column.get(0).getBbox()[0], column.get(column.size() - 1).getBbox()[2], averageGap);

        // 3. 判断文本块是否接近当前列的边界
        float columnStart = column.get(0).getBbox()[0];
        float columnEnd = column.get(column.size() - 1).getBbox()[2];
        boolean boundaryCondition = Math.abs(columnStart - text.getBbox()[0]) < widthTolerance || Math.abs(columnEnd - text.getBbox()[2]) < widthTolerance;

        return (widthCondition || overlapCondition || boundaryCondition);
    }

}
