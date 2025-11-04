# AquaConecta Backend API Documentation

This document provides a comprehensive overview of all REST API endpoints, their roles, and security restrictions for the AquaConecta backend project. All endpoints require authentication unless otherwise specified.

---

## Table of Contents
- [Authentication](#authentication)
- [Profiles](#profiles)
- [Providers](#providers)
- [Residents](#residents)
- [Subscriptions](#subscriptions)
- [Devices](#devices)
- [Events](#events)
- [Issue Reports](#issue-reports)
- [Water Supply Requests](#water-supply-requests)
- [Roles](#roles)
- [Users](#users)
- [Dashboard](#dashboard)
- [General Security](#general-security)

---

## Authentication

### Sign Up
- **POST** `/api/v1/authentication/sign-up`
- **Description**: Register a new user.

### Sign In
- **POST** `/api/v1/authentication/sign-in`
- **Description**: Authenticate and obtain a JWT token.

---

## Profiles

### Create Profile
- **POST** `/api/v1/profiles`
- **Description**: Create a new profile.

### Update Profile
- **PUT** `/api/v1/profiles`
- **Description**: Update an existing profile.

### Get Profile by ID
- **GET** `/api/v1/profiles/{id}`
- **Description**: Retrieve a profile by its ID.

---

## Providers

### Create Provider
- **POST** `/api/v1/providers`
- **Description**: Register a new provider.

### Update Provider Profile
- **PUT** `/api/v1/providers/{providerId}/profiles`
- **Description**: Update provider's profile.

### Get All Providers
- **GET** `/api/v1/providers`
- **Description**: Retrieve all providers.

### Get Provider by ID
- **GET** `/api/v1/providers/{providerId}`
- **Description**: Retrieve a provider by its ID.

### Get Provider Profile
- **GET** `/api/v1/providers/{providerId}/profiles`
- **Description**: Retrieve provider's profile.

### Get Residents by Provider
- **GET** `/api/v1/providers/{providerId}/residents`
- **Description**: Retrieve all residents associated with a provider.

---

## Residents

### Create Resident
- **POST** `/api/v1/residents`
- **Description**: Register a new resident.

### Get All Residents
- **GET** `/api/v1/residents`
- **Description**: Retrieve all residents for the authenticated provider or admin.

### Get Resident by ID
- **GET** `/api/v1/residents/{residentId}`
- **Description**: Retrieve a resident by their ID.

### Get Resident Profile
- **GET** `/api/v1/residents/{residentId}/profiles`
- **Description**: Retrieve the profile of a resident.

### Update Resident Profile
- **PUT** `/api/v1/residents/{residentId}/profiles`
- **Description**: Update the profile of a resident.

### Get Water Supply Requests by Resident
- **GET** `/api/v1/residents/{residentId}/water-supply-requests`
- **Description**: Retrieve water supply requests for a resident.

### Get Subscriptions by Resident
- **GET** `/api/v1/residents/{residentId}/subscriptions`
- **Description**: Retrieve subscriptions for a resident.

### Get Issue Reports by Resident
- **GET** `/api/v1/residents/{residentId}/issue-reports`
- **Description**: Retrieve issue reports for a resident.

### Get Devices by Resident
- **GET** `/api/v1/residents/{residentId}/devices`
- **Description**: Retrieve devices for a resident.

---

## Subscriptions

### Create Subscription
- **POST** `/api/v1/subscriptions`
- **Description**: Create a new subscription.

### Get All Subscriptions
- **GET** `/api/v1/subscriptions`
- **Description**: Retrieve all subscriptions.

### Update Subscription
- **PUT** `/api/v1/subscriptions/{id}`
- **Description**: Update a subscription by its ID.

---

## Devices

### Get Device by ID
- **GET** `/api/v1/devices/{id}`
- **Description**: Retrieve a device by its ID.

### Get Events by Device
- **GET** `/api/v1/devices/{id}/events`
- **Description**: Retrieve all events for a device.

---

## Events

### Create Event
- **POST** `/api/v1/events`
- **Description**: Create a new event.

---

## Issue Reports

### Get Issue Report by ID
- **GET** `/api/v1/issue-reports/{id}`
- **Description**: Retrieve an issue report by its ID.

### Update Issue Report
- **PUT** `/api/v1/issue-reports/{id}`
- **Description**: Update an issue report by its ID.

### Get All Issue Reports
- **GET** `/api/v1/issue-reports`
- **Description**: Retrieve all issue reports.

### Create Issue Report
- **POST** `/api/v1/issue-reports`
- **Description**: Create a new issue report.

---

## Water Supply Requests

### Create Water Supply Request
- **POST** `/api/v1/water-supply-requests`
- **Description**: Create a new water supply request.

### Get All Water Supply Requests
- **GET** `/api/v1/water-supply-requests`
- **Description**: Retrieve all water supply requests.

### Get Water Supply Request by ID
- **GET** `/api/v1/water-supply-requests/{id}`
- **Description**: Retrieve a water supply request by its ID.

### Update Water Supply Request
- **PUT** `/api/v1/water-supply-requests/{id}`
- **Description**: Update a water supply request by its ID.

---

## Roles

### Get All Roles
- **GET** `/ap/v1/roles`
- **Description**: Retrieve all roles.

---

## Users

### Get All Users
- **GET** `/api/v1/users`
- **Description**: Retrieve all users.

### Get User by ID
- **GET** `/api/v1/users/{id}`
- **Description**: Retrieve a user by their ID.

---

## Dashboard

### Get Dashboard Summary
- **GET** `/api/v1/dashboard/summary`
- **Description**: Retrieve dashboard summary statistics.

---

## General Security
All controllers have class-level `@PreAuthorize("isAuthenticated()")` to ensure that only authenticated users can access any endpoint.

---

## Notes
- All endpoints return standard HTTP status codes for success and error handling.
- The service layer enforces business rules and additional security checks.
- This documentation is intended for backend developers, frontend integrators, and API consumers.