package com.xu.layout.utils;

import com.xu.layout.core.Detection;
import com.xu.layout.entity.RegionBlock;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xuzc
 */
public class ImageUtil {

    public static float[] whc2cwh(float[] src) {
        float[] chw = new float[src.length];
        int j = 0;
        for (int ch = 0; ch < 3; ++ch) {
            for (int i = ch; i < src.length; i += 3) {
                chw[j] = src[i];
                j++;
            }
        }
        return chw;
    }

    public static void drawPredictions(BufferedImage image, List<RegionBlock> regionBlocks) {
        Graphics2D g2d = image.createGraphics();

        for (int i = 0; i < regionBlocks.size(); i++) {
            RegionBlock regionBlock = regionBlocks.get(i);
            float[] bbox = regionBlock.getBbox();
            Color color = getColorForLabel(regionBlock.getLabelIndex());

            // 绘制矩形框
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRect((int) bbox[0], (int) bbox[1],
                    (int) (bbox[2] - bbox[0]), (int) (bbox[3] - bbox[1]));

            // 绘制标签文字
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            g2d.drawString(regionBlock.getLabel() + "--" + i + "--" + regionBlock.getConfidence(),
                    (int) bbox[0] - 1, (int) bbox[1] - 5);
        }
        g2d.dispose();
    }

    private static Color getColorForLabel(int labelIndex) {
        Map<Integer, Color> colorMap = new HashMap<>();
        colorMap.put(0, new Color(220, 50, 0));
        colorMap.put(1, new Color(0, 200, 0));
        colorMap.put(2, new Color(0, 0, 200));
        colorMap.put(3, new Color(200, 200, 0));
        colorMap.put(4, new Color(200, 0, 200));
        colorMap.put(5, new Color(0, 200, 200));
        colorMap.put(6, new Color(200, 100, 60));
        colorMap.put(7, new Color(60, 50, 249));
        colorMap.put(8, new Color(10, 60, 249));
        colorMap.put(9, new Color(60, 100, 10));
        colorMap.put(10, new Color(80, 100, 30));
        return colorMap.getOrDefault(labelIndex, Color.BLACK);
    }

    public static void xywh2xyxy(float[] bbox) {
        float x = bbox[0];
        float y = bbox[1];
        float w = bbox[2];
        float h = bbox[3];
        bbox[0] = x - w * 0.5f;
        bbox[1] = y - h * 0.5f;
        bbox[2] = x + w * 0.5f;
        bbox[3] = y + h * 0.5f;
    }

    public static List<float[]> nonMaxSuppression(List<float[]> bboxes, float iouThreshold) {
        // output boxes
        List<float[]> bestBboxes = new ArrayList<>();
        // confidence
        bboxes.sort(Comparator.comparing(a -> a[4]));
        // standard nms
        while (!bboxes.isEmpty()) {
            float[] bestBbox = bboxes.remove(bboxes.size() - 1);
            bestBboxes.add(bestBbox);
            bboxes = bboxes.stream().filter(a -> computeIOU(a, bestBbox) < iouThreshold).collect(Collectors.toList());
        }
        return bestBboxes;
    }

    public static float computeIOU(float[] box1, float[] box2) {
        float area1 = (box1[2] - box1[0]) * (box1[3] - box1[1]);
        float area2 = (box2[2] - box2[0]) * (box2[3] - box2[1]);

        float left = Math.max(box1[0], box2[0]);
        float top = Math.max(box1[1], box2[1]);
        float right = Math.min(box1[2], box2[2]);
        float bottom = Math.min(box1[3], box2[3]);

        float interArea = Math.max(right - left, 0) * Math.max(bottom - top, 0);
        float unionArea = area1 + area2 - interArea;
        return Math.max(interArea / unionArea, 1e-8f);
    }

}
