package com.ironcoders.aquaconectabackend.predictive.interfaces.rest.acl.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class EventDTO {

    private Long id;
    private String eventType;
    private String qualityValue;
    private String levelValue;
    private Long deviceId;
    private LocalDateTime timestamp;

    /**
     * Gets the date portion of the timestamp.
     */
    public LocalDate getDate() {
        return timestamp.toLocalDate();
    }

    /**
     * Parses the level value as a Double.
     * Handles various string formats.
     *
     * @return Level value as Double, or 0.0 if parsing fails
     */
    public Double getLevelValueAsDouble() {
        try {
            String cleaned = levelValue.replaceAll("[^0-9.]", "");
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}