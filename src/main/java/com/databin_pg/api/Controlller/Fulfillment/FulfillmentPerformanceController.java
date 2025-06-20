package com.databin_pg.api.Controlller.Fulfillment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.databin_pg.api.Service.PostgresService;

import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/fulfillment")
@CrossOrigin(origins = "*")
@Tag(name = "Fulfillment - Center Performance", description = "Provides performance summary of fulfillment centers with filtering, sorting, and pagination support")
public class FulfillmentPerformanceController {

    @Autowired
    private PostgresService postgresService;

    @GetMapping("/fulfillment-performance")
    @Operation(
        summary = "Get Fulfillment Center Performance Summary",
        description = "Retrieves performance data of fulfillment centers between given dates. Supports filtering, sorting, and pagination."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Fulfillment center performance data retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Failed to fetch fulfillment center performance data")
    })
    public ResponseEntity<?> getFulfillmentCenterPerformance(
        @Parameter(description = "Start date in YYYY-MM-DD format", required = true)
        @RequestParam(name = "startDate") String startDate,

        @Parameter(description = "End date in YYYY-MM-DD format", required = true)
        @RequestParam(name = "endDate") String endDate,

        @Parameter(description = "Optional enterprise key for filtering data", example = "AWW or AWD")
        @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey,

        @Parameter(description = "Page number for pagination (0-indexed)", example = "0")
        @RequestParam(defaultValue = "0") int page,

        @Parameter(description = "Number of records per page", example = "10")
        @RequestParam(defaultValue = "10") int size,

        @RequestParam Map<String, String> allParams
    ) {
        try {
            int offset = page * size;

            String formattedKey = (enterpriseKey == null || enterpriseKey.isBlank())
                ? "NULL"
                : "'" + enterpriseKey.replace("'", "''") + "'";

            // Allowed fields based on your SP output columns
            List<String> allowedFields = List.of("center", "orders", "avg_time_days", "on_time_rate");

            StringBuilder whereClause = new StringBuilder("WHERE 1=1");

            for (String field : allowedFields) {
                String value = allParams.get(field + ".value");
                String matchMode = allParams.getOrDefault(field + ".matchMode", "contains");

                if (value != null && !value.isBlank()) {
                    value = value.toLowerCase().replace("'", "''");
                    String condition = switch (matchMode) {
                        case "startsWith" -> "LOWER(%s::text) LIKE '%s%%'".formatted(field, value);
                        case "endsWith" -> "LOWER(%s::text) LIKE '%%%s'".formatted(field, value);
                        case "notContains" -> "LOWER(%s::text) NOT LIKE '%%%s%%'".formatted(field, value);
                        case "equals" -> "LOWER(%s::text) = '%s'".formatted(field, value);
                        default -> "LOWER(%s::text) LIKE '%%%s%%'".formatted(field, value);
                    };
                    whereClause.append(" AND ").append(condition);
                }
            }

            String sortField = allParams.getOrDefault("sortField", "center");
            String sortOrder = allParams.getOrDefault("sortOrder", "asc").equalsIgnoreCase("desc") ? "DESC" : "ASC";
            String sortColumn = sortField.replaceAll("[^a-zA-Z0-9_]", "");


            // Count query
            String countQuery = """
                SELECT COUNT(*) as total FROM (
                    SELECT * FROM get_fulfillment_center_performance('%s', '%s', %s)
                ) AS result
                %s
            """.formatted(startDate, endDate, formattedKey, whereClause);
 
            int totalCount = ((Number) postgresService.query(countQuery).get(0).get("total")).intValue();

            // Data query
            String dataQuery = """
                SELECT * FROM (
                    SELECT * FROM get_fulfillment_center_performance('%s', '%s', %s)
                ) AS result
                %s
                ORDER BY %s %s
                OFFSET %d LIMIT %d
            """.formatted(startDate, endDate, formattedKey, whereClause, sortColumn, sortOrder, offset, size);

            List<Map<String, Object>> data = postgresService.query(dataQuery);

            return ResponseEntity.ok(Map.of(
                "data", data,
                "page", page,
                "size", size,
                "count", totalCount
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch fulfillment center performance", "details", e.getMessage()));
        }
    }
}
