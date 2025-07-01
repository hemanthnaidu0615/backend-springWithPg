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
@Tag(name = "Inventory - Warehouse", description = "APIs for region-wise inventory and warehouse-related insights")
public class InventoryWarehouseController {

    @Autowired
    private PostgresService postgresService;

    @Operation(
        summary = "Get region-wise inventory distribution",
        description = "Returns the inventory distribution grouped by regions based on the specified date range."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved region-wise inventory distribution"),
        @ApiResponse(responseCode = "400", description = "Missing or invalid request parameters"),
        @ApiResponse(responseCode = "500", description = "Server error while fetching region-wise inventory distribution")
    })
    @GetMapping("/region-distribution")
    public ResponseEntity<?> getRegionInventoryDistribution(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate) {

        try {
            if (startDate == null || endDate == null || startDate.isEmpty() || endDate.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Start date and end date are required."));
            }

            String query = String.format("""
                SELECT * FROM get_region_inventory_distribution('%s'::timestamp, '%s'::timestamp)
            """, startDate, endDate);

            List<Map<String, Object>> result = postgresService.query(query);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch region-wise inventory distribution"));
        }
    }
    @GetMapping("/details-grid")
    public ResponseEntity<?> getInventoryDetailsGrid(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "warehouseId", required = false) Integer warehouseId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortField", defaultValue = "inventory_id") String sortField,
            @RequestParam(name = "sortOrder", defaultValue = "asc") String sortOrder,
            @RequestParam Map<String, String> allParams
    ) {
        try {
            if (startDate == null || endDate == null || startDate.isEmpty() || endDate.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Start date and end date are required."));
            }

            int offset = page * size;

            // Allowlist of valid sortable fields
            Map<String, String> allowedSortFields = Map.of(
                    "inventory_id", "inventory_id",
                    "product_id", "product_id",
                    "region", "region",
                    "stock_quantity", "stock_quantity",
                    "reserved_quantity", "reserved_quantity"
            );

            // Allowlist of searchable fields
            List<String> filterableFields = List.of(
                    "inventory_id", "product_id", "region", "reserved_quantity", "stock_quantity"
            );

            String sortColumn = allowedSortFields.getOrDefault(sortField, "inventory_id");
            String sortDirection = sortOrder.equalsIgnoreCase("desc") ? "DESC" : "ASC";

            // WHERE clause with filtering
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

            // Base query (calls the PostgreSQL function)
            String baseQuery = String.format("""
                SELECT * FROM get_inventory_details('%s'::timestamp, '%s'::timestamp, %s)
            """, startDate, endDate, warehouseId == null ? "NULL" : warehouseId.toString());

            // Count query
            String countQuery = String.format("SELECT COUNT(*) AS total FROM (%s) AS result %s",
                    baseQuery, whereClause);

            int totalCount = ((Number) postgresService.query(countQuery).get(0).get("total")).intValue();

            // Final paginated query
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
                    .body(Map.of("error", "Failed to fetch inventory details", "details", e.getMessage()));
        }
    }

}
