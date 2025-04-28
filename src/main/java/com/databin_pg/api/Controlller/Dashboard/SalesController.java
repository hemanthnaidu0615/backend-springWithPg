package com.databin_pg.api.Controlller.Dashboard;

import com.databin_pg.api.Service.PostgresService;

import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/sales")
@CrossOrigin(origins = "http://localhost:5173")
public class SalesController {

    @Autowired
    private PostgresService postgresService;

    @GetMapping("/metrics")
    public ResponseEntity<?> getSalesMetrics(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "enterpriseKey") String enterpriseKey) {
        try {
            String query = String.format("""
                SELECT * FROM get_sales_metrics('%s'::TIMESTAMP, '%s'::TIMESTAMP, '%s'::TEXT)
            """, startDate, endDate, enterpriseKey);

            List<Map<String, Object>> result = postgresService.query(query);

            if (result.isEmpty()) {
                return ResponseEntity.ok(Collections.singletonMap("message", "No data found for given range"));
            }

            Map<String, Object> row = result.get(0);

            Map<String, Object> response = new HashMap<>();
            response.put("avg_order_value", parseDouble(row.get("avg_order_value")));
            response.put("high_spenders", parseInteger(row.get("high_spenders")));
            response.put("new_customers", parseInteger(row.get("new_customers")));
            response.put("returning_customers", parseInteger(row.get("returning_customers")));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to fetch sales metrics"));
        }
    }

    // ✅ Helper: Convert Object to Double Safely
    private double parseDouble(Object obj) {
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try {
            return Double.parseDouble(obj.toString().trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // ✅ Helper: Convert Object to Integer Safely
    private int parseInteger(Object obj) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(obj.toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
