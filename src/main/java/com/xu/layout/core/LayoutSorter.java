package com.xu.layout.core;

import com.xu.layout.entity.RegionBlock;

import java.util.List;

/**
 * @author xuzc
 * <p>
 * 排序区域块
 */
public class LayoutSorter {

    private static final float Y_THRESHOLD = 5.0f;

    /**
     * 根据 Y 坐标（从上到下），再根据 X 坐标（从左到右）进行排序
     */
    public static List<RegionBlock> sortByYThenX(List<RegionBlock> contentList) {
        contentList.sort((lc1, lc2) -> {
            double y1 = lc1.getBbox()[1];
            double y2 = lc2.getBbox()[1];
            // 如果y坐标在误差范围内，视为同一行
            if (Math.abs(y1 - y2) <= Y_THRESHOLD) {
                // 同一行时，按x坐标从左到右排序
                return Double.compare(lc1.getBbox()[0], lc2.getBbox()[0]);
            }
            return Double.compare(y1, y2);
        });
        // 不在误差范围内，则按y坐标从上到下排序
        return contentList;
    }

}
