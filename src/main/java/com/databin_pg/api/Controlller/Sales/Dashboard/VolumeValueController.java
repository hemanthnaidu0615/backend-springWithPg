package com.databin_pg.api.Controlller.Sales.Dashboard;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.*;

@RestController
@RequestMapping("/api/sales/volume-value")
@CrossOrigin(origins = "*")
@Tag(name = "Sales Dashboard - Volume & Value", description = "APIs to fetch sales volume and value by product")
public class VolumeValueController {

    @Autowired
    private PostgresService postgresService;

    @Operation(summary = "Get Volume/Value AWW", description = "Returns volume and value by product for AWW with pagination, sorting and search")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Data fetched successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/aww")
    public ResponseEntity<?> getVolumeValueAww(
            @Parameter(description = "Start date in yyyy-MM-dd format") @RequestParam(name = "startDate") String startDate,
            @Parameter(description = "End date in yyyy-MM-dd format") @RequestParam(name = "endDate") String endDate,
            @Parameter(description = "Page number for pagination") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size for pagination") @RequestParam(defaultValue = "50") int size,
            @Parameter(hidden = true) @RequestParam Map<String, String> allParams) {
        return fetchVolumeValueData("get_volume_value_by_product_aww", startDate, endDate, page, size, allParams);
    }

    @Operation(summary = "Get Volume/Value AWD", description = "Returns volume and value by product for AWD with pagination, sorting and search")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Data fetched successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/awd")
    public ResponseEntity<?> getVolumeValueAwd(
            @Parameter(description = "Start date in yyyy-MM-dd format") @RequestParam(name = "startDate") String startDate,
            @Parameter(description = "End date in yyyy-MM-dd format") @RequestParam(name = "endDate") String endDate,
            @Parameter(description = "Page number for pagination") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size for pagination") @RequestParam(defaultValue = "50") int size,
            @Parameter(hidden = true) @RequestParam Map<String, String> allParams) {
        return fetchVolumeValueData("get_volume_value_by_product_awd", startDate, endDate, page, size, allParams);
    }

    private ResponseEntity<?> fetchVolumeValueData(String procedureName, String startDate, String endDate, int page, int size, Map<String, String> allParams) {
        try {
            int offset = page * size;

            List<String> allowedFields = List.of("product_id", "product_name", "category", "total_amount", "total_quantity");

            // WHERE clause builder
            StringBuilder whereClause = new StringBuilder("WHERE 1=1");

            for (String field : allowedFields) {
                String value = allParams.get(field + ".value");
                String matchMode = allParams.getOrDefault(field + ".matchMode", "contains");

                if (value != null && !value.isBlank()) {
                    value = value.toLowerCase().replace("'", "''");
                    String condition;

                    if (field.equals("product_id") || field.startsWith("total_")) {
                        condition = switch (matchMode) {
                            case "equals" -> field + " = " + value;
                            case "greaterThan" -> field + " > " + value;
                            case "lessThan" -> field + " < " + value;
                            default -> "CAST(" + field + " AS TEXT) LIKE '%" + value + "%'";
                        };
                    } else {
                        condition = switch (matchMode) {
                            case "startsWith" -> "LOWER(" + field + "::text) LIKE '" + value + "%'";
                            case "endsWith" -> "LOWER(" + field + "::text) LIKE '%" + value + "'";
                            case "notContains" -> "LOWER(" + field + "::text) NOT LIKE '%" + value + "%'";
                            case "equals" -> "LOWER(" + field + "::text) = '" + value + "'";
                            default -> "LOWER(" + field + "::text) LIKE '%" + value + "%'";
                        };
                    }
                    whereClause.append(" AND ").append(condition);
                }
            }

            // Sorting logic
            String sortField = allParams.getOrDefault("sortField", "total_amount");
            String sortOrder = allParams.getOrDefault("sortOrder", "desc").equalsIgnoreCase("desc") ? "DESC" : "ASC";
            String sortColumn = allowedFields.contains(sortField) ? sortField : "total_amount";

            // Base procedure query
            String baseQuery = String.format("SELECT * FROM %s('%s'::timestamp, '%s'::timestamp)", procedureName, startDate, endDate);

            // Final data query with pagination
            String dataQuery = String.format("""
                SELECT * FROM (%s) AS data
                %s
                ORDER BY %s %s
                OFFSET %d LIMIT %d
            """, baseQuery, whereClause, sortColumn, sortOrder, offset, size);

            // Count query
            String countQuery = String.format("""
                SELECT COUNT(*) AS total FROM (%s) AS data %s
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
                            "error", "Failed to fetch volume/value data",
                            "details", e.getMessage()
                    ));
        }
    }
}
