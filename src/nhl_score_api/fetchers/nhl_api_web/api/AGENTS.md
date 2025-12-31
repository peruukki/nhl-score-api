# NHL Web API Module

This module contains implementations for each NHL Web API endpoint that the application calls.

## ApiRequest Protocol

Each NHL Web API endpoint has an `ApiRequest` protocol implementation. These implementations define:
- The endpoint URL
- Response validation schemas
- Caching behavior
- Archive conditions

## Malli Schemas

The Malli schemas in this module are used for validating NHL Web API responses. These schemas only contain the fields that are referenced in the application code, not the complete API response structure.
