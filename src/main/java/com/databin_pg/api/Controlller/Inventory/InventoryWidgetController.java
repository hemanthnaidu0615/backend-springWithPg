package com.databin_pg.api.Controlller.Inventory;

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
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*")
@Tag(name = "Inventory - Widgets", description = "APIs for inventory widget data with filters and pagination")
public class InventoryWidgetController {

    @Autowired
    private PostgresService postgresService;

    @Operation(
        summary = "Get Inventory Widget Data",
        description = "Retrieves filtered and paginated inventory data used for widget display, including product and warehouse details."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Widget data retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Server error while fetching widget data")
    })
    @GetMapping("/widget-data")
    public ResponseEntity<?> getInventoryWidgetData(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "searchProduct", required = false) String searchProduct,
            @RequestParam(name = "statusFilter", required = false) String statusFilter,
            @RequestParam(name = "categoryFilter", required = false) String categoryFilter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam Map<String, String> allParams) {

        try {
            int offset = page * size;

            // Allowed fields for filtering/sorting
            List<String> allowedFields = List.of(
                "product_name", "category_name", "warehouse_name",
                "warehouse_function", "warehouse_state", "inventory_status"
            );

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
            String sortField = allParams.getOrDefault("sortField", "product_name");
            String sortOrder = allParams.getOrDefault("sortOrder", "asc").equalsIgnoreCase("desc") ? "DESC" : "ASC";
            String sortColumn = allowedFields.contains(sortField) ? sortField : "product_name";

            String baseQuery = """
                SELECT * FROM get_inventory_widget_data(
                    '%s'::date,
                    '%s'::date,
                    %s,
                    %s,
                    %s
                )
            """.formatted(
                startDate,
                endDate,
                searchProduct == null ? "NULL" : "'" + searchProduct.replace("'", "''") + "'",
                statusFilter == null ? "NULL" : "'" + statusFilter.replace("'", "''") + "'",
                categoryFilter == null ? "NULL" : "'" + categoryFilter.replace("'", "''") + "'"
            );

            // Final data query
            String dataQuery = """
                SELECT * FROM (
                    %s
                ) AS data
                %s
                ORDER BY %s %s
                OFFSET %d LIMIT %d
            """.formatted(baseQuery, whereClause, sortColumn, sortOrder, offset, size);

            // Count query
            String countQuery = """
                SELECT COUNT(*) as total FROM (
                    %s
                ) AS data
                %s
            """.formatted(baseQuery, whereClause);

            List<Map<String, Object>> result = postgresService.query(dataQuery);
            List<Map<String, Object>> countResult = postgresService.query(countQuery);
            int totalCount = ((Number) countResult.get(0).get("total")).intValue();

            return ResponseEntity.ok(Map.of(
                "data", result,
                "count", totalCount,
                "page", page,
                "size", size
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch widget data", "details", e.getMessage()));
        }
    }
}
