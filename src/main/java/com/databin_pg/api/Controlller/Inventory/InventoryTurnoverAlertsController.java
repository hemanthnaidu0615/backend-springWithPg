package com.databin_pg.api.Controlller.Inventory;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.databin_pg.api.Service.PostgresService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*")
@Tag(name = "Inventory - Metrics", description = "APIs related to inventory turnover and stock status with percentages")
public class InventoryTurnoverAlertsController {

    @Autowired
    private PostgresService postgresService;

    @Operation(
        summary = "Get inventory stock summary with percentage breakdown",
        description = "Returns inventory summary for available, low stock, and out-of-stock products including their respective percentage values."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved inventory turnover summary with percentages"),
        @ApiResponse(responseCode = "500", description = "Failed to fetch inventory turnover summary due to server error")
    })
    @GetMapping("/turnover-alerts")
    public ResponseEntity<?> getInventoryTurnoverSummary(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate) {

        try {
            String query = String.format("""
                SELECT * FROM get_inventory_summary_with_percentages('%s'::date, '%s'::date)
            """, startDate, endDate);

            List<Map<String, Object>> result = postgresService.query(query);

            if (result.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "total_products", 0,
                        "available", 0,
                        "low_stock", 0,
                        "out_of_stock", 0,
                        "available_percentage", 0,
                        "low_stock_percentage", 0,
                        "out_of_stock_percentage", 0
                ));
            }

            Map<String, Object> summary = result.get(0);

            return ResponseEntity.ok(Map.of(
                    "total_products", summary.get("total_products"),
                    "available", summary.get("available"),
                    "low_stock", summary.get("low_stock"),
                    "out_of_stock", summary.get("out_of_stock"),
                    "available_percentage", summary.get("available_percentage"),
                    "low_stock_percentage", summary.get("low_stock_percentage"),
                    "out_of_stock_percentage", summary.get("out_of_stock_percentage")
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch inventory turnover summary"));
        }
    }
    @GetMapping("/details-grid-turnover")
    public ResponseEntity<?> getInventoryTurnoverDetailsGrid(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortField", defaultValue = "product_id") String sortField,
            @RequestParam(name = "sortOrder", defaultValue = "asc") String sortOrder,
            @RequestParam Map<String, String> allParams
    ) {
        try {
            if (startDate == null || endDate == null || startDate.isEmpty() || endDate.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Start date and end date are required."));
            }

            int offset = page * size;

            Map<String, String> allowedSortFields = Map.of(
                    "product_id", "product_id",
                    "name", "name",
                    "stock_quantity", "stock_quantity",
                    "restock_date", "restock_date",
                    "status", "status"
            );

            List<String> filterableFields = List.of(
                    "product_id", "name", "stock_quantity", "restock_date", "status"
            );

            String sortColumn = allowedSortFields.getOrDefault(sortField, "product_id");
            String sortDirection = sortOrder.equalsIgnoreCase("desc") ? "DESC" : "ASC";

            StringBuilder whereClause = new StringBuilder("WHERE 1=1");
            for (String field : filterableFields) {
                String value = allParams.get(field + ".value");
                String matchMode = allParams.getOrDefault(field + ".matchMode", "contains");

                if (value != null && !value.isEmpty()) {
                    value = value.toLowerCase().replace("'", "''");
                    String condition;
                    switch (matchMode) {
                        case "startsWith" -> condition = "LOWER(" + field + "::text) LIKE '" + value + "%'";
                        case "endsWith" -> condition = "LOWER(" + field + "::text) LIKE '%" + value + "'";
                        case "notContains" -> condition = "LOWER(" + field + "::text) NOT LIKE '%" + value + "%'";
                        case "equals" -> condition = "LOWER(" + field + "::text) = '" + value + "'";
                        default -> condition = "LOWER(" + field + "::text) LIKE '%" + value + "%'";
                    }
                    whereClause.append(" AND ").append(condition);
                }
            }

            String baseQuery = String.format("""
                SELECT * FROM get_inventory_turnover_details('%s'::date, '%s'::date)
            """, startDate, endDate);

            String countQuery = String.format("SELECT COUNT(*) AS total FROM (%s) AS result %s",
                    baseQuery, whereClause);

            int totalCount = ((Number) postgresService.query(countQuery).get(0).get("total")).intValue();

            String dataQuery = String.format("""
                SELECT * FROM (%s) AS result
                %s
                ORDER BY %s %s
                OFFSET %d LIMIT %d
            """, baseQuery, whereClause, sortColumn, sortDirection, offset, size);

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
                    .body(Map.of("error", "Failed to fetch inventory turnover details", "details", e.getMessage()));
        }
    }

}
