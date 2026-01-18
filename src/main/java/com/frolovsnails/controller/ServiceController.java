package com.frolovsnails.controller;

import com.frolovsnails.dto.request.ServiceRequest;
import com.frolovsnails.dto.response.ApiResponse;
import com.frolovsnails.entity.Service;
import com.frolovsnails.repository.ServiceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/services")
@Tag(name = "Services", description = "Управление услугами")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceRepository serviceRepository;

    @GetMapping
    @Operation(summary = "Получить все активные услуги")
    public ResponseEntity<ApiResponse> getAllServices() {
        List<Service> services = serviceRepository.findByIsActiveTrue();
        return ResponseEntity.ok(ApiResponse.success(
                "Список активных услуг",
                services
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить услугу по ID")
    public ResponseEntity<ApiResponse> getServiceById(@PathVariable Long id) {
        return serviceRepository.findById(id)
                .map(service -> ResponseEntity.ok(ApiResponse.success("Услуга найдена", service)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Получить услуги по категории")
    public ResponseEntity<ApiResponse> getServicesByCategory(@PathVariable String category) {
        List<Service> services = serviceRepository.findByCategoryAndIsActiveTrue(category);
        return ResponseEntity.ok(ApiResponse.success(
                "Услуги в категории: " + category,
                services
        ));
    }

    @GetMapping("/categories")
    @Operation(summary = "Получить все категории услуг")
    public ResponseEntity<ApiResponse> getAllCategories() {
        List<String> categories = serviceRepository.findAll()
                .stream()
                .filter(Service::getIsActive)
                .map(Service::getCategory)
                .distinct()
                .toList();

        return ResponseEntity.ok(ApiResponse.success(
                "Список категорий услуг",
                Map.of("categories", categories)
        ));
    }

    @PostMapping
    @Operation(summary = "Создать новую услугу (только для ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> createService(@Valid @RequestBody ServiceRequest request) {
        try {
            Service service = new Service();
            service.setName(request.getName());
            service.setDescription(request.getDescription());
            service.setDurationMinutes(request.getDurationMinutes());
            service.setPrice(request.getPrice());
            service.setCategory(request.getCategory());
            service.setIsActive(true);

            Service savedService = serviceRepository.save(service);
            return ResponseEntity.ok(ApiResponse.success(
                    "Услуга создана успешно",
                    savedService
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Ошибка создания услуги: " + e.getMessage())
            );
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить услугу (только для ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> updateService(
            @PathVariable Long id,
            @Valid @RequestBody ServiceRequest request) {

        return serviceRepository.findById(id)
                .map(service -> {
                    service.setName(request.getName());
                    service.setDescription(request.getDescription());
                    service.setDurationMinutes(request.getDurationMinutes());
                    service.setPrice(request.getPrice());
                    service.setCategory(request.getCategory());

                    Service updatedService = serviceRepository.save(service);
                    return ResponseEntity.ok(ApiResponse.success(
                            "Услуга обновлена",
                            updatedService
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Деактивировать услугу (только для ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deactivateService(@PathVariable Long id) {
        return serviceRepository.findById(id)
                .map(service -> {
                    service.setIsActive(false);
                    serviceRepository.save(service);
                    return ResponseEntity.ok(ApiResponse.success("Услуга деактивирована"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Активировать услугу (только для ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> activateService(@PathVariable Long id) {
        return serviceRepository.findById(id)
                .map(service -> {
                    service.setIsActive(true);
                    serviceRepository.save(service);
                    return ResponseEntity.ok(ApiResponse.success("Услуга активирована"));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}