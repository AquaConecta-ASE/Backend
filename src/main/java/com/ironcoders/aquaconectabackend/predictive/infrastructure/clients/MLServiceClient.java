package com.ironcoders.aquaconectabackend.predictive.infrastructure.clients;

import com.ironcoders.aquaconectabackend.predictive.infrastructure.clients.dto.MLPredictionRequest;
import com.ironcoders.aquaconectabackend.predictive.infrastructure.clients.dto.MLPredictionResponse;

public interface MLServiceClient {
    
    /**
     * Gets consumption predictions from the ML service.
     * 
     * @param request Request containing historical consumption data
     * @return Response with predictions for the next 7 days
     * @throws MLServiceException if the ML service is unavailable or returns an error
     */
    MLPredictionResponse getPrediction(MLPredictionRequest request);

    /**
     * Triggers a model retraining in the ML service.
     * 
     * @return true if retraining was successful, false otherwise
     */
    boolean triggerRetrain();

    /**
     * Checks if the ML service is healthy and available.
     * 
     * @return true if service is healthy, false otherwise
     */
    boolean isHealthy();
}
