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
@RequestMapping("/api/sales/by-item")
@CrossOrigin(origins = "*")
@Tag(name = "Sales Dashboard -", description = "APIs for sales dashboard data by item with filters and pagination")
public class ByItemController {

    @Autowired
    private PostgresService postgresService;

    @Operation(
        summary = "Get Item Summary AWW",
        description = "Fetches filtered and paginated item summary for AWW report."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Item summary AWW data fetched successfully"),
        @ApiResponse(responseCode = "500", description = "Server error while fetching item summary AWW data")
    })
    @GetMapping("/aww")
    public ResponseEntity<?> getItemSummaryAww(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam Map<String, String> allParams
    ) {
        return fetchItemSummary("get_item_summary_aww", startDate, endDate, page, size, allParams);
    }

    @Operation(
        summary = "Get Item Summary AWD",
        description = "Fetches filtered and paginated item summary for AWD report."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Item summary AWD data fetched successfully"),
        @ApiResponse(responseCode = "500", description = "Server error while fetching item summary AWD data")
    })
    @GetMapping("/awd")
    public ResponseEntity<?> getItemSummaryAwd(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam Map<String, String> allParams
    ) {
        return fetchItemSummary("get_item_summary_awd", startDate, endDate, page, size, allParams);
    }

    private ResponseEntity<?> fetchItemSummary(String procedureName, String startDate, String endDate, int page, int size, Map<String, String> allParams) {
        try {
            int offset = page * size;

            // Fields allowed for filtering/sorting based on API output
            List<String> allowedFields = List.of("product_id", "total_quantity", "total_amount");

            StringBuilder whereClause = new StringBuilder("WHERE 1=1");

            for (String field : allowedFields) {
                String value = allParams.get(field + ".value");
                String matchMode = allParams.getOrDefault(field + ".matchMode", "contains");

                if (value != null && !value.isBlank()) {
                    value = value.toLowerCase().replace("'", "''");
                    String condition;
                    if (field.equals("product_id") || field.equals("total_quantity") || field.equals("total_amount")) {
                        condition = switch (matchMode) {
                            case "equals" -> "%s = %s".formatted(field, value);
                            case "greaterThan" -> "%s > %s".formatted(field, value);
                            case "lessThan" -> "%s < %s".formatted(field, value);
                            default -> "%s::text LIKE '%%%s%%'".formatted(field, value);
                        };
                    } else {
                        condition = "LOWER(%s::text) LIKE '%%%s%%'".formatted(field, value);
                    }
                    whereClause.append(" AND ").append(condition);
                }
            }

            // Sorting
            String sortField = allParams.getOrDefault("sortField", "total_amount");
            String sortOrder = allParams.getOrDefault("sortOrder", "desc").equalsIgnoreCase("desc") ? "DESC" : "ASC";
            String sortColumn = allowedFields.contains(sortField) ? sortField : "total_amount";

            // Base Query
            String baseQuery = String.format("""
                SELECT * FROM %s('%s'::timestamp, '%s'::timestamp)
            """, procedureName, startDate, endDate);

            // Final Data Query
            String dataQuery = String.format("""
                SELECT * FROM (
                    %s
                ) AS data
                %s
                ORDER BY %s %s
                OFFSET %d LIMIT %d
            """, baseQuery, whereClause, sortColumn, sortOrder, offset, size);

            // Count Query
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
                            "error", "Failed to fetch item summary data",
                            "details", e.getMessage()
                    ));
        }
    }
}
