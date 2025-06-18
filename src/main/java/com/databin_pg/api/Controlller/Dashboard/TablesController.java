package com.databin_pg.api.Controlller.Dashboard;


import com.databin_pg.api.Service.PostgresService; 
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
@Tag(name = "Dashboard - Recent Orders", description = "APIs for retrieving paginated recent orders with optional enterprise filtering")
public class TablesController {

    @Autowired
    private PostgresService postgresService;
    @Operation(
            summary = "Get recent orders",
            description = "Retrieves recent orders between the specified start and end dates. Supports pagination and optional enterprise filtering (e.g., 'AWW' or 'AWD')."
        )
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recent orders retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Failed to fetch recent orders")
        })
    @GetMapping("/recent-orders")
    public ResponseEntity<?> getRecentOrders(
    		  @Parameter(description = "Start date in YYYY-MM-DD format", required = true)
              @RequestParam(name = "startDate") String startDate,

              @Parameter(description = "End date in YYYY-MM-DD format", required = true)
              @RequestParam(name = "endDate") String endDate,

              @Parameter(description = "Optional enterprise key for filtering results 'AWW' or 'AWD'")
              @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey,

              @Parameter(description = "Page number (zero-based)", example = "0")
              @RequestParam(defaultValue = "0") int page,

              @Parameter(description = "Number of records per page", example = "10")
              @RequestParam(defaultValue = "10") int size) {

        try {
            String formattedKey = (enterpriseKey == null || enterpriseKey.isBlank()) ? "NULL" : "'" + enterpriseKey + "'";
            int offset = page * size;

            // Count query
            String countQuery = String.format("""
                SELECT COUNT(*) as total FROM get_recent_orders('%s'::TIMESTAMP, '%s'::TIMESTAMP, %s)
            """, startDate, endDate, formattedKey);

            List<Map<String, Object>> countResult = postgresService.query(countQuery);
            int totalCount = 0;
            if (!countResult.isEmpty() && countResult.get(0).get("total") != null) {
                totalCount = ((Number) countResult.get(0).get("total")).intValue();
            }

            // Paginated data query
            String dataQuery = String.format("""
                SELECT * FROM get_recent_orders('%s'::TIMESTAMP, '%s'::TIMESTAMP, %s)
                OFFSET %d LIMIT %d
            """, startDate, endDate, formattedKey, offset, size);

            List<Map<String, Object>> data = postgresService.query(dataQuery);

            return ResponseEntity.ok(Map.of(
                    "data", data,
                    "page", page,
                    "size", size,
                    "count", totalCount));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch recent orders", "details", e.getMessage()));
        }
    }

}

