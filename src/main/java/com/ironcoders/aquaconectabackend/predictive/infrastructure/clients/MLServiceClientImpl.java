package com.ironcoders.aquaconectabackend.predictive.infrastructure.clients;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.ironcoders.aquaconectabackend.predictive.infrastructure.clients.dto.MLPredictionRequest;
import com.ironcoders.aquaconectabackend.predictive.infrastructure.clients.dto.MLPredictionResponse;
import com.ironcoders.aquaconectabackend.predictive.infrastructure.clients.exceptions.MLServiceException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;


import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class MLServiceClientImpl  implements MLServiceClient{
     private static final Logger log = LoggerFactory.getLogger(MLServiceClientImpl.class);

    @Value("${ml.service.url:https://ml-service-t46u.onrender.com}")
    private String mlServiceUrl;

    @Value("${ml.service.timeout:30000}")
    private int timeout;

    private final RestTemplate restTemplate;

    /**
     * Constructor for dependency injection.
     */
    public MLServiceClientImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Gets consumption predictions from the ML service.
     * 
     * @param request Request containing historical consumption data
     * @return Response with predictions for the next 7 days
     * @throws MLServiceException if the ML service is unavailable or returns an error
     */
    @Override
    public MLPredictionResponse getPrediction(MLPredictionRequest request) {
        String url = mlServiceUrl + "/predict";
        
        log.info("Calling ML service at: {} for user: {}", url, request.getUserId());

        try {
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create HTTP entity
            HttpEntity<MLPredictionRequest> entity = new HttpEntity<>(request, headers);

            // Make POST request
            ResponseEntity<MLPredictionResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                MLPredictionResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Successfully received prediction from ML service for user: {}", 
                    request.getUserId());
                return response.getBody();
            } else {
                log.error("ML service returned unexpected status: {}", response.getStatusCode());
                throw new MLServiceException("ML service returned unexpected status: " + 
                    response.getStatusCode());
            }

        } catch (HttpClientErrorException e) {
            log.error("Client error when calling ML service: {} - {}", 
                e.getStatusCode(), e.getResponseBodyAsString());
            throw new MLServiceException("ML service client error: " + e.getMessage(), e);

        } catch (HttpServerErrorException e) {
            log.error("Server error from ML service: {} - {}", 
                e.getStatusCode(), e.getResponseBodyAsString());
            throw new MLServiceException("ML service server error: " + e.getMessage(), e);

        } catch (ResourceAccessException e) {
            log.error("ML service is unavailable at: {}", url, e);
            throw new MLServiceException(
                "ML service is unavailable. Please ensure the service is running at: " + mlServiceUrl, 
                e
            );

        } catch (Exception e) {
            log.error("Unexpected error calling ML service", e);
            throw new MLServiceException("Unexpected error calling ML service: " + e.getMessage(), e);
        }
    }

    /**
     * Triggers a model retraining in the ML service.
     * 
     * @return true if retraining was successful, false otherwise
     */
    @Override
    public boolean triggerRetrain() {
        String url = mlServiceUrl + "/retrain";
        
        log.info("Triggering ML model retraining at: {}", url);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("ML model retrain triggered successfully");
                return true;
            } else {
                log.warn("ML retrain returned unexpected status: {}", response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            log.error("Error triggering ML retrain", e);
            return false;
        }
    }

    /**
     * Checks if the ML service is healthy and available.
     * 
     * @return true if service is healthy, false otherwise
     */
    @Override
    public boolean isHealthy() {
        String url = mlServiceUrl + "/health";

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            boolean isHealthy = response.getStatusCode() == HttpStatus.OK;
            
            if (isHealthy) {
                log.debug("ML service is healthy");
            } else {
                log.warn("ML service health check failed with status: {}", response.getStatusCode());
            }
            
            return isHealthy;

        } catch (Exception e) {
            log.error("ML service health check failed", e);
            return false;
        }
    }
}
