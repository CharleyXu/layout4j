# Layout4j
[![CN doc](https://img.shields.io/badge/文档-中文版-blue.svg)](README_zh_CN.md)
## Layout Analysis
1. Utilize ONNX models to analyze the layout of PDF documents or images, identifying a collection of layout regions.

2. Employ the model yolov8n_layout_general6.

3. Supported categories: ["Text", "Title", "Figure", "Table", "Caption", "Equation"].

## Region Sorting
Sort the layout regions according to human reading order to extract structured text content.

## Implementation Approach
The sorting algorithm, based on the positions of text blocks, identifies and organizes the hierarchical relationships between different regions (e.g., titles, text). By efficiently sorting and filtering the regions, the algorithm can recognize associations between columns, titles, and text in complex document layouts, ultimately generating a structured layout tree.

## Core Features
1. Remove small overlapping regions.
2. Sort text blocks by Y-coordinate, prioritizing vertical layouts.
3. Build a multi-level layout structure containing text blocks based on title recognition.
4. Handle multi-column layouts by classifying text blocks into columns.
5. Process and store regions (e.g., titles, text blocks) along with their hierarchical relationships.

## Reference Projects
 
- [RapidLayout](https://github.com/RapidAI/RapidLayout)

A collection of open-source layout analysis models.

- [GapTree_Sort_Algorithm](https://github.com/hiroi-sora/GapTree_Sort_Algorithm)

This algorithm is suitable only for standard horizontal or vertical reading formats and cannot handle non-standard layouts such as newspapers.