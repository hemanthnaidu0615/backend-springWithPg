package com.databin_pg.api.Controlller.Scheduler;

import com.databin_pg.api.Service.PostgresService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedulers/view")
@CrossOrigin(origins = "*")
@Tag(name = "Inventory-", description = "APIs for managing scheduler view and summary")
public class ViewSchedulerController {

    @Autowired
    private PostgresService postgresService;

    @Operation(summary = "Get Scheduler Summary", description = "Returns paginated list of scheduler summaries with search and sort.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Scheduler data fetched successfully"),
        @ApiResponse(responseCode = "500", description = "Server error while fetching scheduler data")
    })
    @GetMapping
    public ResponseEntity<?> getSchedulerSummary(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of records per page") @RequestParam(defaultValue = "50") int size,
            @RequestParam Map<String, String> allParams
    ) {
        try {
            int offset = page * size;

            // Allowed filter fields
            List<String> allowedFields = List.of("title", "description", "email", "start_date", "recurrence_pattern", "date_range_type");

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

            // Sorting
            String sortField = allParams.getOrDefault("sortField", "start_date");
            String sortOrder = allParams.getOrDefault("sortOrder", "asc").equalsIgnoreCase("desc") ? "DESC" : "ASC";
            String sortColumn = allowedFields.contains(sortField) ? sortField : "start_date";

            // Count query
            String countQuery = """
                SELECT COUNT(*) as total FROM (
                    SELECT * FROM get_scheduler_summary()
                ) AS sched
                %s
            """.formatted(whereClause);

            List<Map<String, Object>> countResult = postgresService.query(countQuery);
            int totalCount = (!countResult.isEmpty() && countResult.get(0).get("total") != null)
                    ? ((Number) countResult.get(0).get("total")).intValue()
                    : 0;

            // Data query
            String dataQuery = """
                SELECT * FROM (
                    SELECT * FROM get_scheduler_summary()
                ) AS sched
                %s
                ORDER BY %s %s
                OFFSET %d LIMIT %d
            """.formatted(whereClause, sortColumn, sortOrder, offset, size);

            List<Map<String, Object>> result = postgresService.query(dataQuery);

            return ResponseEntity.ok(Map.of(
                "data", result,
                "page", page,
                "size", size,
                "count", totalCount
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Failed to fetch scheduler data",
                    "details", e.getMessage()
                ));
        }
    }
}
