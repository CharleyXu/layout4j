package com.xu.layout.core;

import com.xu.layout.entity.RegionBlock;

import java.util.*;

/**
 * @author xuzc
 * <p>
 * 负责列数和列边界的检测，基于文本块的 x 坐标来判断有多少列
 */
public class ColumnDetector {

    private static final float THRESHOLD = 3.0f;

    public static int detectColumns(List<RegionBlock> texts) {
        List<Float> xCoordinates = new ArrayList<>();
        for (RegionBlock text : texts) {
            xCoordinates.add(text.getBbox()[0]);
        }
        Collections.sort(xCoordinates);

        int columns = 0;
        float currentColumnX = Float.MIN_VALUE;

        for (float x : xCoordinates) {
            if (currentColumnX == Float.MIN_VALUE || Math.abs(x - currentColumnX) > THRESHOLD) {
                columns++;
                currentColumnX = x;
            }
        }
        return columns;
    }

    public static Map<Integer, List<RegionBlock>> distributeToColumns(List<RegionBlock> texts, int columns) {
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

}
