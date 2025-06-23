package com.databin_pg.api.Controlller.Sales.Dashboard;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.*;

@RestController
@RequestMapping("/api/sales/by-fulfillment")
@CrossOrigin(origins = "*")
@Tag(name = "Sales Dashboard -", description = "APIs for sales dashboard fulfillment data with filters and pagination")
public class ByFulfillmentController {

    @Autowired
    private PostgresService postgresService;

    @Operation(
        summary = "Get Fulfillment Summary AWW",
        description = "Fetches filtered and paginated fulfillment summary for AWW report."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Fulfillment AWW data fetched successfully"),
        @ApiResponse(responseCode = "500", description = "Server error while fetching AWW fulfillment data")
    })
    @GetMapping("/aww")
    public ResponseEntity<?> getFulfillmentSummaryAww(
            @Parameter(description = "Start date in yyyy-MM-dd format") @RequestParam(name = "startDate") String startDate,
            @Parameter(description = "End date in yyyy-MM-dd format") @RequestParam(name = "endDate") String endDate,
            @Parameter(description = "Page number for pagination") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size for pagination") @RequestParam(defaultValue = "50") int size,
            @Parameter(hidden = true) @RequestParam Map<String, String> allParams) {

        return fetchFulfillmentSummary("get_fulfillment_summary_by_channel_aww", startDate, endDate, page, size, allParams);
    }

    @Operation(
        summary = "Get Fulfillment Summary AWD",
        description = "Fetches filtered and paginated fulfillment summary for AWD report."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Fulfillment AWD data fetched successfully"),
        @ApiResponse(responseCode = "500", description = "Server error while fetching AWD fulfillment data")
    })
    @GetMapping("/awd")
    public ResponseEntity<?> getFulfillmentSummaryAwd(
            @Parameter(description = "Start date in yyyy-MM-dd format") @RequestParam(name = "startDate") String startDate,
            @Parameter(description = "End date in yyyy-MM-dd format") @RequestParam(name = "endDate") String endDate,
            @Parameter(description = "Page number for pagination") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size for pagination") @RequestParam(defaultValue = "50") int size,
            @Parameter(hidden = true) @RequestParam Map<String, String> allParams) {

        return fetchFulfillmentSummary("get_fulfillment_summary_by_channel_awd", startDate, endDate, page, size, allParams);
    }

    private ResponseEntity<?> fetchFulfillmentSummary(String procedureName, String startDate, String endDate, int page, int size, Map<String, String> allParams) {
        try {
            int offset = page * size;

            List<String> allowedFields = List.of("fulfilment_channel", "total_quantity", "total_amount");

            StringBuilder whereClause = new StringBuilder("WHERE 1=1");

            for (String field : allowedFields) {
                String value = allParams.get(field + ".value");
                String matchMode = allParams.getOrDefault(field + ".matchMode", "contains");

                if (value != null && !value.isBlank()) {
                    value = value.toLowerCase().replace("'", "''");
                    String condition;
                    if (field.equals("total_quantity") || field.equals("total_amount")) {
                        condition = switch (matchMode) {
                            case "equals" -> "%s = %s".formatted(field, value);
                            case "greaterThan" -> "%s > %s".formatted(field, value);
                            case "lessThan" -> "%s < %s".formatted(field, value);
                            default -> "%s::text LIKE '%%%s%%'".formatted(field, value);
                        };
                    } else {
                        condition = switch (matchMode) {
                            case "startsWith" -> "LOWER(%s::text) LIKE '%s%%'".formatted(field, value);
                            case "endsWith" -> "LOWER(%s::text) LIKE '%%%s'".formatted(field, value);
                            case "notContains" -> "LOWER(%s::text) NOT LIKE '%%%s%%'".formatted(field, value);
                            case "equals" -> "LOWER(%s::text) = '%s'".formatted(field, value);
                            default -> "LOWER(%s::text) LIKE '%%%s%%'".formatted(field, value);
                        };
                    }
                    whereClause.append(" AND ").append(condition);
                }
            }

            String sortField = allParams.getOrDefault("sortField", "total_amount");
            String sortOrder = allParams.getOrDefault("sortOrder", "desc").equalsIgnoreCase("desc") ? "DESC" : "ASC";
            String sortColumn = allowedFields.contains(sortField) ? sortField : "total_amount";

            String baseQuery = String.format("""
                SELECT * FROM %s('%s'::timestamp, '%s'::timestamp)
            """, procedureName, startDate, endDate);

            String dataQuery = String.format("""
                SELECT * FROM (
                    %s
                ) AS data
                %s
                ORDER BY %s %s
                OFFSET %d LIMIT %d
            """, baseQuery, whereClause, sortColumn, sortOrder, offset, size);

            String countQuery = String.format("""
                SELECT COUNT(*) AS total FROM (
                    %s
                ) AS data
                %s
            """, baseQuery, whereClause);

            List<Map<String, Object>> result = postgresService.query(dataQuery);
            List<Map<String, Object>> countResult = postgresService.query(countQuery);
            int totalCount = (!countResult.isEmpty() && countResult.get(0).get("total") != null)
                    ? ((Number) countResult.get(0).get("total")).intValue()
                    : 0;

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
                            "error", "Failed to fetch fulfillment data",
                            "details", e.getMessage()
                    ));
        }
    }
}
