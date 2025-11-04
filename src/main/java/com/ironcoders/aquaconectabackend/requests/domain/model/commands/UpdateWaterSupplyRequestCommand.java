package com.ironcoders.aquaconectabackend.requests.domain.model.commands;

import java.time.LocalDateTime;

public record UpdateWaterSupplyRequestCommand(
        Long id,                        // ID de la solicitud
        String status,                 // Nuevo estado (por ejemplo: "ENTREGADO")
        LocalDateTime deliveredAt,     // Fecha de entrega que asigna el proveedor
        Long userId                    // ID del usuario que realiza la actualización
) {
}
