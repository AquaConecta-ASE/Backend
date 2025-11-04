package com.ironcoders.aquaconectabackend.requests.interfaces.rest.resources;

import java.time.LocalDateTime;

public record UpdateWaterResource(String status,
                                  LocalDateTime deliveredAt) {
}
