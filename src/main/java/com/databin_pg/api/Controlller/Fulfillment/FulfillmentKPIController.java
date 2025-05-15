package com.databin_pg.api.Controlller.Fulfillment;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.databin_pg.api.Service.PostgresService;

@RestController
@RequestMapping("/api/fulfillment")
@CrossOrigin(origins = "http://localhost:5173")
public class FulfillmentKPIController {

    @Autowired
    private PostgresService postgresService;

    // 📌 API: Get Fulfillment Dashboard KPI (Orders in Pipeline, Avg Fulfillment Time, On-Time Rate, Top Channel)
    @GetMapping("/kpi")
    public ResponseEntity<?> getFulfillmentDashboardData(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey) {
        try {
            String formattedKey = (enterpriseKey == null || enterpriseKey.isBlank()) ? "NULL" : "'" + enterpriseKey + "'";
            String query = String.format("""
                SELECT * FROM get_fulfillment_dashboard_data('%s', '%s', %s)
            """, startDate, endDate, formattedKey);

            List<Map<String, Object>> data = postgresService.query(query);

            if (data.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "orders_in_pipeline", 0,
                    "avg_fulfillment_time", "0 days",
                    "on_time_rate", "0%",
                    "top_channel", "N/A"
                ));
            }

            Map<String, Object> row = data.get(0);
            int orders = ((Number) row.get("orders_in_pipeline")).intValue();
            double avgTime = ((Number) row.get("avg_fulfillment_time")).doubleValue();
            double onTimeRate = ((Number) row.get("on_time_rate")).doubleValue();
            String topChannel = (String) row.get("top_channel");

            return ResponseEntity.ok(Map.of(
                "orders_in_pipeline", orders,
                "avg_fulfillment_time", String.format("%.0f days", avgTime),
                "on_time_rate", String.format("%.0f%%", onTimeRate),
                "top_channel", (topChannel != null && !topChannel.isBlank()) ? topChannel : "N/A"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch dashboard data", "details", e.getMessage()));
        }
    }
}
