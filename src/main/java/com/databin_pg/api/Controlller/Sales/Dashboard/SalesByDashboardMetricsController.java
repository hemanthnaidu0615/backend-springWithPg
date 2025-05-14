package com.databin_pg.api.Controlller.Sales.Dashboard;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;

@RestController
@RequestMapping("/api/sales")
@CrossOrigin(origins = "http://localhost:5173")

public class SalesByDashboardMetricsController {
	@Autowired
    private PostgresService postgresService;

    @GetMapping("/sales-metrics")
    public ResponseEntity<?> getSalesMetricsDashboard(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey) {

        try {
            // Use NULL if enterpriseKey is not provided
            String formattedKey = (enterpriseKey == null || enterpriseKey.isBlank()) ? "NULL" : "'" + enterpriseKey + "'";

            String query = String.format("""
                SELECT * FROM get_sales_metrics_dashboard('%s'::timestamp, '%s'::timestamp, %s)
            """, startDate, endDate, formattedKey);

            List<Map<String, Object>> result = postgresService.query(query);

            if (result.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body(Map.of("message", "No sales metrics data found."));
            }

            return ResponseEntity.ok(result.get(0));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch sales metrics dashboard", "details", e.getMessage()));
        }
    }
}
