package com.databin_pg.api.Controlller.Dashboard;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "http://localhost:5173")
public class InventoryMetricsController {

    @Autowired
    private PostgresService postgresService;

    @GetMapping("/turnover-and-alerts")
    public ResponseEntity<?> getInventoryTurnoverAndLowStock(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "threshold", defaultValue = "10") int threshold) {  // No enterpriseKey needed

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
