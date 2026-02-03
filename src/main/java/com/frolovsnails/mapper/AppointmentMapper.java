package com.frolovsnails.mapper;

import com.frolovsnails.dto.response.AppointmentResponse;
import com.frolovsnails.entity.Appointment;
import org.springframework.stereotype.Component;

@Component
public class AppointmentMapper {

    public AppointmentResponse toResponse(Appointment appointment) {
        if (appointment == null) {
            return null;
        }

        return AppointmentResponse.builder()
                .id(appointment.getId())
                .client(appointment.getClient() != null ?
                        AppointmentResponse.ClientInfo.builder()
                        .id(appointment.getClient().getId())
                        .firstName(appointment.getClient().getFirstName())
                        .lastName(appointment.getClient().getLastName())
                        .phone(appointment.getClient().getUser() != null ?
                                appointment.getClient().getUser().getPhone() : null)
                        .build() : null)
                .service(appointment.getService() != null ?
                        AppointmentResponse.ServiceInfo.builder()
                        .id(appointment.getService().getId())
                        .name(appointment.getService().getName())
                        .description(appointment.getService().getDescription())
                        .durationMinutes(appointment.getService().getDurationMinutes())
                        .price(appointment.getService().getPrice())
                        .category(appointment.getService().getCategory())
                        .build() : null)
                // УБИРАЕМ slot - теперь есть startTime и endTime
                .startTime(appointment.getStartTime())
                .endTime(appointment.getEndTime())
                .status(appointment.getStatus())
                .clientNotes(appointment.getClientNotes())
                .masterNotes(appointment.getMasterNotes())
                .createdAt(appointment.getCreatedAt())
                .updatedAt(appointment.getUpdatedAt())
                .build();
    }
}