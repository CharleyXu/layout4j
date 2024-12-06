package com.xu.layout.core;

import com.xu.layout.entity.RegionBlock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author xuzc
 * <p>
 * 移除重叠的小区域块，确保区域块的合理性
 */
public class OverlapBlockRemover {

    private static final float THRESHOLD = 3.0f;

    public static List<RegionBlock> removeOverlappingSmallerBlocks(List<RegionBlock> list) {
        list.sort(Comparator.comparingDouble((RegionBlock lc) -> -area(lc.getBbox())));

        List<RegionBlock> result = new ArrayList<>();
        for (RegionBlock current : list) {
            boolean isContained = false;
            for (RegionBlock largerBlock : result) {
                if (isContainedIn(current.getBbox(), largerBlock.getBbox())) {
                    isContained = true;
                    break;
                }
            }
            if (!isContained) {
                result.add(current);
            }
        }
        return result;
    }

    private static double area(float[] bbox) {
        return (bbox[2] - bbox[0]) * (bbox[3] - bbox[1]);
    }

    private static boolean isContainedIn(float[] smallBbox, float[] largeBbox) {
        return smallBbox[0] >= largeBbox[0] - THRESHOLD && smallBbox[1] >= largeBbox[1] - THRESHOLD &&
                smallBbox[2] <= largeBbox[2] + THRESHOLD && smallBbox[3] <= largeBbox[3] + THRESHOLD;
    }

}
