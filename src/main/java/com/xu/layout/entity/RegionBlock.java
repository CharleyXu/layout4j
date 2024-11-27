package com.xu.layout.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xuzc
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegionBlock {

    // [x1, y1, x2, y2]
    public String label;

    public String content;

    public String getSimpleContent() {
        return content.replace("\n", "").replace("\r", "");
    }

    public float[] bbox;

    public float confidence;

}
