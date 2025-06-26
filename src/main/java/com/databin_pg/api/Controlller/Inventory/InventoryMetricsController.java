package com.databin_pg.api.Controlller.Inventory;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*")
@Tag(name = "Inventory - Metrics", description = "APIs for inventory turnover and low stock alert metrics")
public class InventoryMetricsController {

    @Autowired
    private PostgresService postgresService;

    @Operation(
        summary = "Get inventory turnover and low stock alerts",
        description = "Fetches inventory turnover rates over time and generates alerts for low stock products below a specified threshold"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved turnover rates and low stock alerts"),
        @ApiResponse(responseCode = "500", description = "Internal server error occurred while fetching inventory metrics")
    })
    @GetMapping("/turnover-and-alerts")
    public ResponseEntity<?> getInventoryTurnoverAndLowStock(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "threshold", defaultValue = "10") int threshold) {

        try {
            String turnoverQuery = String.format("""
                SELECT * FROM get_inventory_turnover_and_alerts('%s'::date, '%s'::date)
            """, startDate, endDate);
            List<Map<String, Object>> turnoverData = postgresService.query(turnoverQuery);

            List<Map<String, Object>> turnoverList = new ArrayList<>(turnoverData.size());
            for (Map<String, Object> row : turnoverData) {
                turnoverList.add(Map.of(
                        "month", row.get("month"),
                        "turnover_rate", row.get("turnover_rate")
                ));
            }

            String lowStockQuery = String.format("""
                SELECT * FROM get_low_stock_alerts(%d)
            """, threshold);
            List<Map<String, Object>> lowStockData = postgresService.query(lowStockQuery);

            List<Map<String, Object>> lowStockList = new ArrayList<>(lowStockData.size());
            for (Map<String, Object> row : lowStockData) {
                lowStockList.add(Map.of(
                        "product_name", row.get("product_name"),
                        "stock_quantity", row.get("stock_quantity"),
                        "restock_date", row.get("restock_date")
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "turnover_rates", turnoverList,
                    "low_stock_alerts", lowStockList
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch inventory metrics"));
        }
    }
}
