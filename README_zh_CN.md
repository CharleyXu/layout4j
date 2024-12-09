# Layout4j

## 版面分析

1. 使用onnx模型来对PDF文档或者图片进行版面分析，得到区域块集合

2. 使用模型 yolov8n_layout_general6

3. 支持类别，["Text", "Title", "Figure", "Table", "Caption", "Equation"]

## 区域块排序

对区域块，按人类阅读顺序排序后，得到文本内容。

### 实现思路

基于文本块位置的排序算法，能够识别并组织不同区域块（如标题、文本等）之间的层级关系。通过对区域块的有效排序与过滤，可以在复杂的文档布局中识别列、标题与文本的关联关系，最终生成一个结构化的布局树。

### 核心功能

1. 移除重叠的小区域块
2. 按 Y 坐标排序文本块，优先考虑垂直布局
3. 基于标题识别，构建包含文本块的多层次布局
4. 处理多列布局，将文本块按照列进行分类
5. 处理和存储区域块（如标题、文本块等）及其层级关系

## 参考项目

- [RapidLayout](https://github.com/RapidAI/RapidLayout)

汇集了全网开源的版面分析模型

- [GapTree_Sort_Algorithm](https://github.com/hiroi-sora/GapTree_Sort_Algorithm)

这个仅适用于标准横排或标准竖排阅读习惯，无法处理报刊等非标准布局的文档