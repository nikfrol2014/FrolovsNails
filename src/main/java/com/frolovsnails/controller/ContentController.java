package com.frolovsnails.controller;

import com.frolovsnails.dto.response.ApiResponse;
import com.frolovsnails.dto.response.SliderItemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/content")
@Tag(name = "Content", description = "Контент для приложения")
@RequiredArgsConstructor
public class ContentController {

    @GetMapping("/slider")
    @Operation(summary = "Получить слайды для главного экрана")
    public ResponseEntity<ApiResponse> getSliderImages() {

        List<SliderItemResponse> slides = List.of(
                SliderItemResponse.builder()
                        .imageUrl("/uploads/slider/Mobile-6.png")
                        .title("")
                        .description("")
                        .actionType("price")
                        .orderIndex(1)
                        .build(),
                SliderItemResponse.builder()
                        .imageUrl("/uploads/slider/Mobile.png")
                        .title("")
                        .description("")
                        .actionType("price")
                        .orderIndex(2)
                        .build(),
                SliderItemResponse.builder()
                        .imageUrl("/uploads/slider/Mobile-1.png")
                        .title("")
                        .description("")
                        .actionType("promo")
                        .orderIndex(3)
                        .build(),
                SliderItemResponse.builder()
                        .imageUrl("/uploads/slider/Mobile-2.png")
                        .title("")
                        .description("")
                        .actionType("price")
                        .orderIndex(4)
                        .build(),
                SliderItemResponse.builder()
                        .imageUrl("/uploads/slider/Mobile-3.png")
                        .title("")
                        .description("")
                        .actionType("price")
                        .orderIndex(5)
                        .build(),
                SliderItemResponse.builder()
                        .imageUrl("/uploads/slider/Mobile-4.png")
                        .title("")
                        .description("")
                        .actionType("price")
                        .orderIndex(6)
                        .build(),
                SliderItemResponse.builder()
                        .imageUrl("/uploads/slider/Mobile-5.png")
                        .title("")
                        .description("")
                        .actionType("price")
                        .orderIndex(7)
                        .build()
        );

        return ResponseEntity.ok(ApiResponse.success("Слайды загружены", slides));
    }
}