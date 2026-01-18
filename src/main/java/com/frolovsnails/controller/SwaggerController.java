package com.frolovsnails.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
@Hidden  // Скрываем из Swagger
public class SwaggerController {

    @GetMapping
    public String redirectToSwagger() {
        return "redirect:/swagger-ui.html";
    }

    @GetMapping("/docs")
    public String redirectToDocs() {
        return "redirect:/swagger-ui.html";
    }

    @GetMapping("/api")
    public String redirectToApi() {
        return "redirect:/swagger-ui.html";
    }
}