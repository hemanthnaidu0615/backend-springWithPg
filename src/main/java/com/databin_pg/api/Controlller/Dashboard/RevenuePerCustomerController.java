package com.databin_pg.api.Controlller.Dashboard;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;

@RestController
@RequestMapping("/api/revenue")
@CrossOrigin(origins = "http://localhost:5173")
public class RevenuePerCustomerController {

    @Autowired
    private PostgresService postgresService;

    // 📌 API: Get Top 7 Customers by Revenue (using stored procedure)
    @GetMapping("/top-customers")
    public ResponseEntity<?> getTopCustomersByRevenue(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "enterpriseKey", required=false) String enterpriseKey) {  // Added enterpriseKey parameter
        try {
            // Update the query to include enterpriseKey
            String query = String.format("""
                SELECT * FROM get_top_customers_by_revenue('%s'::TIMESTAMP, '%s'::TIMESTAMP, '%s'::TEXT)
            """, startDate, endDate, enterpriseKey);  // Pass enterpriseKey to the stored procedure

            List<Map<String, Object>> data = postgresService.query(query);

            List<Map<String, Object>> topCustomers = new ArrayList<>();

            // Transform result data into the expected format
            for (Map<String, Object> row : data) {
                String customerId = Objects.toString(row.get("customer_id"), "N/A");
                String customerName = Objects.toString(row.get("customer_name"), "N/A");
                double revenue = parseDouble(row.get("total_revenue"));

                topCustomers.add(Map.of(
                    "customer_id", customerId,
                    "customer_name", customerName,
                    "revenue", revenue
                ));
            }

            return ResponseEntity.ok(Map.of("top_customers", topCustomers));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch top customers by revenue"));
        }
    }

    // 🔹 Helper Method: Convert Object to Double
    private double parseDouble(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try {
            return Double.parseDouble(obj.toString());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid double format: " + obj, e);
        }
    }
}
