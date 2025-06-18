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
@Tag(name = "Fulfillment - Center Performance", description = "Provides performance summary of fulfillment centers with pagination support")
public class FulfillmentPerformanceController {

    @Autowired
    private PostgresService postgresService;

    @Operation(
            summary = "Get Fulfillment Center Performance Summary",
            description = "Retrieves performance data of fulfillment centers between given dates. Supports pagination and filtering by enterprise key."
        )
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Fulfillment center performance data retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Failed to fetch fulfillment center performance data")
        })
    @GetMapping("/fulfillment-performance")
    public ResponseEntity<?> getFulfillmentCenterPerformance(
    		@Parameter(description = "Start date in YYYY-MM-DD format", required = true)
            @RequestParam(name = "startDate") String startDate,

            @Parameter(description = "End date in YYYY-MM-DD format", required = true)
            @RequestParam(name = "endDate") String endDate,

            @Parameter(description = "Optional enterprise key for filtering data, e.g., 'AWW' or 'AWD'")
            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey,

            @Parameter(description = "Page number for pagination (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of records per page", example = "10")
            @RequestParam(defaultValue = "10") int size){

        try {
            String formattedKey = (enterpriseKey == null || enterpriseKey.isBlank())
                    ? "NULL"
                    : "'" + enterpriseKey + "'";
            int offset = page * size;

            // Count query
            String countQuery = String.format("""
                SELECT COUNT(*) as total FROM get_fulfillment_center_performance('%s', '%s', %s)
            """, startDate, endDate, formattedKey);

            List<Map<String, Object>> countResult = postgresService.query(countQuery);
            int totalCount = 0;
            if (!countResult.isEmpty() && countResult.get(0).get("total") != null) {
                totalCount = ((Number) countResult.get(0).get("total")).intValue();
            }

            // Data query with pagination
            String dataQuery = String.format("""
                SELECT * FROM get_fulfillment_center_performance('%s', '%s', %s)
                OFFSET %d LIMIT %d
            """, startDate, endDate, formattedKey, offset, size);

            List<Map<String, Object>> data = postgresService.query(dataQuery);

            return ResponseEntity.ok(Map.of(
                    "data", data,
                    "page", page,
                    "size", size,
                    "count", totalCount
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to fetch fulfillment center performance",
                            "details", e.getMessage()));
        }
    }
}
