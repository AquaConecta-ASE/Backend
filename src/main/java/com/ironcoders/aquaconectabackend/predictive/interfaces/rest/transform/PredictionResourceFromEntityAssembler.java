package com.ironcoders.aquaconectabackend.predictive.interfaces.rest.transform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ironcoders.aquaconectabackend.predictive.domain.model.aggregates.ConsumptionPrediction;
import com.ironcoders.aquaconectabackend.predictive.infrastructure.clients.dto.DailyPredictionDTO;
import com.ironcoders.aquaconectabackend.predictive.interfaces.rest.resources.DailyPredictionResource;
import com.ironcoders.aquaconectabackend.predictive.interfaces.rest.resources.PredictionResponseResource;
import com.ironcoders.aquaconectabackend.predictive.interfaces.rest.resources.RefillInfoResource;

import java.util.ArrayList;
import java.util.List;

/**
 * Assembler to transform ConsumptionPrediction entity to PredictionResponseResource.
 */
public class PredictionResourceFromEntityAssembler {

    private static final Logger log = LoggerFactory.getLogger(PredictionResourceFromEntityAssembler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Transforms a ConsumptionPrediction entity to a REST resource.
     */
    public static PredictionResponseResource toResourceFromEntity(
        ConsumptionPrediction entity, 
        RefillInfoResource refillInfo) 
        {
        
        // Parse predictions JSON to list of resources
        List<DailyPredictionResource> predictions = parsePredictions(entity.getPredictionsJson());

        return new PredictionResponseResource(
            entity.getResidentId(),
            entity.getPredictionDate().toString(),
            entity.getDailyAverageConsumption(),
            predictions,
            entity.getWaterRunoutDate().toString(),
            entity.getDaysUntilRunout(),
            entity.getConfidenceScore(),
            entity.getCurrentWaterLevel(),
            entity.getTotalPredictedConsumption7Days(),
            entity.getStatus().toString(),
            refillInfo
        );
    }

    /**
     * Parses predictions JSON string to list of DailyPredictionResource.
     */
    private static List<DailyPredictionResource> parsePredictions(String predictionsJson) {
        try {
            // Parse JSON to list of DTOs
            List<DailyPredictionDTO> dtos = objectMapper.readValue(
                predictionsJson,
                new TypeReference<List<DailyPredictionDTO>>() {}
            );

            // Transform DTOs to Resources
            return dtos.stream()
                .map(dto -> new DailyPredictionResource(
                    dto.getDate(),
                    dto.getPredictedConsumption(),
                    dto.getDayOfWeek()
                ))
                .toList();

        } catch (Exception e) {
            log.error("Error parsing predictions JSON", e);
            return new ArrayList<>();
        }
    }
}