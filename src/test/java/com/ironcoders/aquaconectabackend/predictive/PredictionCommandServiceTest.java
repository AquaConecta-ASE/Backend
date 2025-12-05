package com.ironcoders.aquaconectabackend.predictive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ironcoders.aquaconectabackend.predictive.application.internal.PredictionCommandServiceImpl;
import com.ironcoders.aquaconectabackend.predictive.domain.model.aggregates.ConsumptionPrediction;
import com.ironcoders.aquaconectabackend.predictive.domain.model.aggregates.WaterConsumption;
import com.ironcoders.aquaconectabackend.predictive.domain.model.commands.CalculateDailyConsumptionCommand;
import com.ironcoders.aquaconectabackend.predictive.domain.model.commands.GeneratePredictionCommand;
import com.ironcoders.aquaconectabackend.predictive.domain.services.ConsumptionCalculationService;
import com.ironcoders.aquaconectabackend.predictive.infrastructure.clients.MLServiceClient;
import com.ironcoders.aquaconectabackend.predictive.infrastructure.clients.dto.DailyPredictionDTO;
import com.ironcoders.aquaconectabackend.predictive.infrastructure.clients.dto.MLPredictionResponse;
import com.ironcoders.aquaconectabackend.predictive.infrastructure.persistence.jpa.repositories.ConsumptionPredictionRepository;
import com.ironcoders.aquaconectabackend.predictive.interfaces.rest.acl.MonitoringContextFacade;
import com.ironcoders.aquaconectabackend.predictive.interfaces.rest.acl.SubscriptionContextFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PredictionCommandServiceImpl.
 * Tests the generation of water consumption predictions using ML service.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Prediction Command Service Tests")
class PredictionCommandServiceTest {

    @Mock
    private ConsumptionCalculationService consumptionCalculationService;

    @Mock
    private ConsumptionPredictionRepository predictionRepository;

    @Mock
    private SubscriptionContextFacade subscriptionContextFacade;

    @Mock
    private MonitoringContextFacade monitoringContextFacade;

    @Mock
    private MLServiceClient mlServiceClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PredictionCommandServiceImpl predictionCommandService;

    private Long subscriptionId;
    private Long residentId;
    private Long sensorId;
    private Double waterTankSize;

    @BeforeEach
    void setUp() {
        subscriptionId = 1L;
        residentId = 100L;
        sensorId = 10L;
        waterTankSize = 1000.0;
    }

    @Test
    @DisplayName("Should generate prediction successfully with sufficient consumption data")
    void shouldGeneratePredictionSuccessfully() {
        // Arrange
        GeneratePredictionCommand command = new GeneratePredictionCommand(subscriptionId, residentId);

        // Mock subscription validation
        when(subscriptionContextFacade.isSubscriptionOwnedByResident(subscriptionId, residentId))
            .thenReturn(true);
        when(subscriptionContextFacade.getSensorIdBySubscription(subscriptionId))
            .thenReturn(Optional.of(sensorId));
        when(subscriptionContextFacade.getWaterTankSizeBySubscription(subscriptionId))
            .thenReturn(Optional.of(waterTankSize));

        // Mock consumption data (10 days of historical data)
        List<WaterConsumption> mockConsumptions = createMockConsumptions(10);
        when(consumptionCalculationService.handle(any(CalculateDailyConsumptionCommand.class)))
            .thenReturn(mockConsumptions);

        // Mock current water level
        when(monitoringContextFacade.getCurrentWaterLevelBySubscription(subscriptionId))
            .thenReturn(Optional.of(650.0)); // 65% of tank

        // Mock ML Service response
        MLPredictionResponse mlResponse = new MLPredictionResponse();
        mlResponse.setDailyPredictions(Arrays.asList(
            createDailyPrediction(1, 120.0),
            createDailyPrediction(2, 115.0),
            createDailyPrediction(3, 130.0),
            createDailyPrediction(4, 125.0),
            createDailyPrediction(5, 120.0),
            createDailyPrediction(6, 110.0),
            createDailyPrediction(7, 118.0)
        ));
        when(mlServiceClient.getPrediction(any())).thenReturn(mlResponse);

        // Mock repository save
        when(predictionRepository.save(any(ConsumptionPrediction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Optional<ConsumptionPrediction> result = predictionCommandService.handle(command);

        // Assert
        assertTrue(result.isPresent(), "Prediction should be generated");
        ConsumptionPrediction prediction = result.get();
        
        assertEquals(subscriptionId, prediction.getSubscriptionId());
        assertEquals(650.0, prediction.getCurrentWaterLevel(), 0.1);
        assertNotNull(prediction.getPredictedConsumption());
        assertTrue(prediction.getPredictedConsumption() > 0, "Predicted consumption should be positive");
        assertNotNull(prediction.getEstimatedDaysRemaining());
        assertTrue(prediction.getEstimatedDaysRemaining() > 0, "Days remaining should be positive");

        // Verify interactions
        verify(subscriptionContextFacade).isSubscriptionOwnedByResident(subscriptionId, residentId);
        verify(consumptionCalculationService).handle(any(CalculateDailyConsumptionCommand.class));
        verify(mlServiceClient).getPrediction(any());
        verify(predictionRepository).save(any(ConsumptionPrediction.class));
    }

    @Test
    @DisplayName("Should return empty when subscription does not belong to resident")
    void shouldReturnEmptyWhenSubscriptionNotOwned() {
        // Arrange
        GeneratePredictionCommand command = new GeneratePredictionCommand(subscriptionId, residentId);

        // Mock subscription validation to return false
        when(subscriptionContextFacade.isSubscriptionOwnedByResident(subscriptionId, residentId))
            .thenReturn(false);

        // Act
        Optional<ConsumptionPrediction> result = predictionCommandService.handle(command);

        // Assert
        assertFalse(result.isPresent(), "Should return empty for unauthorized access");
        
        // Verify no further processing
        verify(subscriptionContextFacade).isSubscriptionOwnedByResident(subscriptionId, residentId);
        verify(consumptionCalculationService, never()).handle(any());
        verify(mlServiceClient, never()).getPrediction(any());
        verify(predictionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return empty when insufficient consumption data (less than 7 days)")
    void shouldReturnEmptyWhenInsufficientData() {
        // Arrange
        GeneratePredictionCommand command = new GeneratePredictionCommand(subscriptionId, residentId);

        when(subscriptionContextFacade.isSubscriptionOwnedByResident(subscriptionId, residentId))
            .thenReturn(true);
        when(subscriptionContextFacade.getSensorIdBySubscription(subscriptionId))
            .thenReturn(Optional.of(sensorId));

        // Mock only 5 days of consumption data (insufficient)
        List<WaterConsumption> mockConsumptions = createMockConsumptions(5);
        when(consumptionCalculationService.handle(any(CalculateDailyConsumptionCommand.class)))
            .thenReturn(mockConsumptions);

        // Act
        Optional<ConsumptionPrediction> result = predictionCommandService.handle(command);

        // Assert
        assertFalse(result.isPresent(), "Should return empty when data is insufficient");
        
        // Verify ML service was NOT called
        verify(mlServiceClient, never()).getPrediction(any());
        verify(predictionRepository, never()).save(any());
    }

    // Helper methods
    private List<WaterConsumption> createMockConsumptions(int days) {
        List<WaterConsumption> consumptions = new java.util.ArrayList<>();
        LocalDate baseDate = LocalDate.now().minusDays(days);
        
        for (int i = 0; i < days; i++) {
            WaterConsumption consumption = new WaterConsumption();
            consumption.setSubscriptionId(subscriptionId);
            consumption.setConsumptionDate(baseDate.plusDays(i));
            consumption.setStartLevel(850.0 - (i * 10));
            consumption.setEndLevel(750.0 - (i * 10));
            consumption.setConsumptionLiters(100.0 + (i * 5));
            consumption.setRefillDay(false);
            consumptions.add(consumption);
        }
        
        return consumptions;
    }

    private DailyPredictionDTO createDailyPrediction(int day, Double consumption) {
        DailyPredictionDTO prediction = new DailyPredictionDTO();
        prediction.setDay(day);
        prediction.setPredictedConsumption(consumption);
        return prediction;
    }
}
