package com.thesis.workout.shared.web;

import java.util.List;

/**
 * Minimal page envelope shared by paginated endpoints. Avoids serializing Spring Data's
 * {@code PageImpl}, whose JSON shape is unstable across versions.
 */
public record PagedResponse<T>(List<T> items, int page, int size, long totalItems, boolean hasNext) {
}
