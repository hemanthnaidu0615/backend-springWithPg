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
@Tag(name = "Fulfillment - Bottleneck Analysis", description = "Analyze operational bottlenecks in the fulfillment process")
public class BottleneckAnalysisController {

    @Autowired
    private PostgresService postgresService;

    @Operation(
            summary = "Get Bottleneck Analysis",
            description = "Retrieves bottleneck insights from the fulfillment pipeline for a given date range. Optionally filtered by enterprise key ('AWW' or 'AWD')."
        )
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bottleneck analysis data fetched successfully"),
            @ApiResponse(responseCode = "500", description = "Failed to fetch bottleneck analysis data")
        })
    @GetMapping("/bottleneck-analysis")
    public ResponseEntity<?> getBottleneckAnalysis(
    		@Parameter(description = "Start date in YYYY-MM-DD format", required = true)
            @RequestParam(name = "startDate") String startDate,

            @Parameter(description = "End date in YYYY-MM-DD format", required = true)
            @RequestParam(name = "endDate") String endDate,

            @Parameter(description = "Optional enterprise key for filtering results 'AWW' or 'AWD'")
            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey) {

        try {
            String formattedKey = (enterpriseKey == null || enterpriseKey.isBlank()) ? "NULL" : "'" + enterpriseKey + "'";

            String formattedStartDate = startDate.split("T")[0];  
            String formattedEndDate = endDate.split("T")[0];     

            String query = String.format("""
                SELECT * FROM get_bottleneck_analysis('%s', '%s', %s)
            """, formattedStartDate, formattedEndDate, formattedKey);

            List<Map<String, Object>> data = postgresService.query(query);

            return ResponseEntity.ok(data);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch bottleneck analysis", "details", e.getMessage()));
        }
    }
    @GetMapping("/details-grid")
    public ResponseEntity<?> getBottleneckDetailsGrid(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey,
            @RequestParam(name = "eventType", required = false) String eventType,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortField", defaultValue = "order_date") String sortField,
            @RequestParam(name = "sortOrder", defaultValue = "asc") String sortOrder,
            @RequestParam Map<String, String> allParams
    ) {
        try {
            if (startDate == null || endDate == null || startDate.isEmpty() || endDate.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "startDate and endDate are required."));
            }

            String key = enterpriseKey == null ? "NULL" : "'" + enterpriseKey + "'";
            String evt = eventType == null ? "NULL" : "'" + eventType + "'";

            String baseQuery = String.format("""
                SELECT * FROM get_bottleneck_event_details('%s', '%s', %s, %s)
            """, startDate, endDate, key, evt);

            int offset = page * size;

            Map<String, String> allowedSort = Map.of(
                "order_id", "order_id",
                "event_id", "event_id",
                "order_date", "order_date",
                "enterprise_key", "enterprise_key",
                "event_type", "event_type",
                "event_time", "event_time",
                "fulfillment_city", "fulfillment_city"
            );

            List<String> filterFields = List.of(
                "order_id", "event_id", "order_date",
                "enterprise_key", "event_type",
                "event_time", "fulfillment_city"
            );

            String sortCol = allowedSort.getOrDefault(sortField, "order_date");
            String sortDir = sortOrder.equalsIgnoreCase("desc") ? "DESC" : "ASC";

            StringBuilder where = new StringBuilder("WHERE 1=1");
            for (String field : filterFields) {
                String val = allParams.get(field + ".value");
                String mode = allParams.getOrDefault(field + ".matchMode", "contains");

                if (val != null && !val.isEmpty()) {
                    String safeVal = val.toLowerCase().replace("'", "''");
                    String condition;
                    switch (mode) {
                        case "startsWith": condition = "LOWER(" + field + "::text) LIKE '" + safeVal + "%'"; break;
                        case "endsWith": condition = "LOWER(" + field + "::text) LIKE '%" + safeVal + "'"; break;
                        case "notContains": condition = "LOWER(" + field + "::text) NOT LIKE '%" + safeVal + "%'"; break;
                        case "equals": condition = "LOWER(" + field + "::text) = '" + safeVal + "'"; break;
                        default: condition = "LOWER(" + field + "::text) LIKE '%" + safeVal + "%'";
                    }
                    where.append(" AND ").append(condition);
                }
            }

            String countQuery = String.format("""
                SELECT COUNT(*) AS total FROM (%s) AS sub %s
            """, baseQuery, where);

            int total = ((Number) postgresService.query(countQuery)
                    .get(0).get("total")).intValue();

            String dataQuery = String.format("""
                SELECT * FROM (%s) AS sub
                %s
                ORDER BY %s %s
                OFFSET %d LIMIT %d
            """, baseQuery, where, sortCol, sortDir, offset, size);

            List<Map<String, Object>> rows = postgresService.query(dataQuery);

            return ResponseEntity.ok(Map.of(
                "data", rows,
                "page", page,
                "size", size,
                "count", total
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch fulfillment details grid", "details", e.getMessage()));
        }
    }
}
