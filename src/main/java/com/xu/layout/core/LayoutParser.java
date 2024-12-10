package com.xu.layout.core;

import com.xu.layout.entity.RegionBlock;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xuzc
 * <p>
 * 处理布局逻辑的流程，但是将具体的逻辑委托给其他类处理
 */
@Slf4j
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

        // 检测是否为规整的排版布局
        Map<Integer, List<RegionBlock>> columnRegionBlockMap = ColumnDetector.detectColumns(filteredList);
        log.info("columns size: {}, {}", columnRegionBlockMap.size(), columnRegionBlockMap);
        int columns = columnRegionBlockMap.size();
        if (columns <= 5) {
            // 如果是规整的一列、两列或三列排版，直接按列构建树
            return TitleBlockProcessor.processAndTraverse(columnRegionBlockMap);
        }
        // 如果不是规整排版，则继续处理标题块的逻辑
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
        Map<Integer, List<RegionBlock>> firstProcessLBlockMap = ColumnDetector.detectColumns(firstProcessList);
        List<RegionBlock> preTitleBlocks = TitleBlockProcessor.processAndTraverse(firstProcessLBlockMap);
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
            Map<Integer, List<RegionBlock>> textsUnderTitleBlockMap = ColumnDetector.detectColumns(textsUnderTitle);
            List<RegionBlock> titleBlocks = TitleBlockProcessor.processAndTraverse(textsUnderTitleBlockMap, title);
            result.addAll(titleBlocks);
        }

        // 处理未被处理的区域块
        List<RegionBlock> unprocessedBlocks = sortedList.stream().filter(lc -> !processedBlocks.contains(lc)).toList();

        // 将未处理的区域块添加到结果中
        result.addAll(unprocessedBlocks);
        return result;
    }

}
