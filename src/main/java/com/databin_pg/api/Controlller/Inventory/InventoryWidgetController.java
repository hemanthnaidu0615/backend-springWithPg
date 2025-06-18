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
            @Parameter(description = "Start date in YYYY-MM-DD format") @RequestParam(name = "startDate") String startDate,
            @Parameter(description = "End date in YYYY-MM-DD format") @RequestParam(name = "endDate") String endDate,
            @Parameter(description = "Product name search filter") @RequestParam(name = "searchProduct", required = false) String searchProduct,
            @Parameter(description = "Inventory status filter (e.g., available, low_stock)") @RequestParam(name = "statusFilter", required = false) String statusFilter,
            @Parameter(description = "Product category filter") @RequestParam(name = "categoryFilter", required = false) String categoryFilter,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of records per page") @RequestParam(defaultValue = "10") int size) {

        try {
            int offset = page * size;

            String dataQuery = String.format("""
                    SELECT * FROM get_inventory_widget_data(
                        '%s'::date,
                        '%s'::date,
                        %s,
                        %s,
                        %s
                    )
                    OFFSET %d LIMIT %d
                    """,
                    startDate,
                    endDate,
                    searchProduct == null ? "NULL" : "'" + searchProduct + "'",
                    statusFilter == null ? "NULL" : "'" + statusFilter + "'",
                    categoryFilter == null ? "NULL" : "'" + categoryFilter + "'",
                    offset,
                    size);

            String countQuery = String.format("""
                    SELECT COUNT(*) as total FROM get_inventory_widget_data(
                        '%s'::date,
                        '%s'::date,
                        %s,
                        %s,
                        %s
                    )
                    """,
                    startDate,
                    endDate,
                    searchProduct == null ? "NULL" : "'" + searchProduct + "'",
                    statusFilter == null ? "NULL" : "'" + statusFilter + "'",
                    categoryFilter == null ? "NULL" : "'" + categoryFilter + "'");

            List<Map<String, Object>> result = postgresService.query(dataQuery);
            List<Map<String, Object>> countResult = postgresService.query(countQuery);

            int totalCount = ((Number) countResult.get(0).get("total")).intValue();

            List<Map<String, Object>> widgetData = new ArrayList<>(result.size());
            for (Map<String, Object> row : result) {
                widgetData.add(Map.of(
                        "product_name", row.get("product_name"),
                        "category_name", row.get("category_name"),
                        "warehouse_name", row.get("warehouse_name"),
                        "warehouse_function", row.get("warehouse_function"),
                        "warehouse_state", row.get("warehouse_state"),
                        "inventory_status", row.get("inventory_status")));
            }

            return ResponseEntity.ok(Map.of(
                    "data", widgetData,
                    "count", totalCount,
                    "page", page,
                    "size", size));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch widget data", "details", e.getMessage()));
        }
    }
}
