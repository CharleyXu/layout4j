package com.xu.layout.core;

import com.xu.layout.entity.RegionBlock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author xuzc
 * <p>
 * 处理布局逻辑的流程，但是将具体的逻辑委托给其他类处理
 */
public class LayoutParser {

    private static final String TITLE = "Title";

    /**
     * 处理布局，移除重叠的小区域块并按坐标排序，然后按标题和非标题对区域块进行处理
     *
     * @param contentList 包含所有区域块的列表
     * @return 处理后的区域块列表
     */
    public static List<RegionBlock> processLayout(List<RegionBlock> contentList) {
        // 移除重叠的小区域块
        List<RegionBlock> filteredList = OverlapBlockRemover.removeOverlappingSmallerBlocks(contentList);

        // 按 y 坐标排序，处理标题
        List<RegionBlock> sortedList = LayoutSorter.sortByYThenX(filteredList);

        // 用于存储已处理的区域块，以避免重复处理
        Set<RegionBlock> processedBlocks = new HashSet<>();
        // 用于存储标题前的区域块
        List<RegionBlock> firstProcessList = new ArrayList<>();

        // 处理标题前的区域块
        for (RegionBlock content : sortedList) {
            if (!TITLE.equalsIgnoreCase(content.getLabel())) {
                firstProcessList.add(content);
                processedBlocks.add(content);
            } else {
                break;
            }
        }

        // 处理并返回结果
        List<RegionBlock> preTitleBlocks = TitleBlockProcessor.processAndTraverse(firstProcessList);
        List<RegionBlock> result = new ArrayList<>(preTitleBlocks);

        // 提取所有标题
        List<RegionBlock> titles = sortedList.stream().filter(lc -> TITLE.equalsIgnoreCase(lc.getLabel())).collect(Collectors.toList());
        processedBlocks.addAll(titles);

        // 遍历处理每个标题下的内容
        for (int i = 0; i < titles.size(); i++) {
            RegionBlock title = titles.get(i);
            int current = i;
            // 筛选属于当前标题下的区域块
            List<RegionBlock> textsUnderTitle = sortedList.stream().filter(lc -> !TITLE.equalsIgnoreCase(lc.getLabel())).filter(lc -> TitleBlockProcessor.isUnderTitle(title, lc, titles.subList(current + 1, titles.size()))).collect(Collectors.toList());
            processedBlocks.addAll(textsUnderTitle);
            // 处理当前标题下的区域块并添加到结果中
            List<RegionBlock> titleBlocks = TitleBlockProcessor.processAndTraverse(textsUnderTitle, title);
            result.addAll(titleBlocks);
        }

        // 处理未被处理的区域块
        List<RegionBlock> unprocessedBlocks = sortedList.stream().filter(lc -> !processedBlocks.contains(lc)).toList();

        // 将未处理的区域块添加到结果中
        result.addAll(unprocessedBlocks);
        return result;
    }

}
