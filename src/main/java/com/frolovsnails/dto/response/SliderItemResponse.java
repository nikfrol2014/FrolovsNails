package com.frolovsnails.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SliderItemResponse {
    private String imageUrl;
    private String title;
    private String description;
    private String actionType; // "price", "promo", "link"
    private String actionValue; // URL или ID
    private int orderIndex;
}