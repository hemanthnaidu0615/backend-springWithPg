package com.databin_pg.api.Controlller.Dashboard;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;

@RestController
@RequestMapping("/api/revenue")
@CrossOrigin(origins = "*")
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
            // Format the startDate and endDate to ensure correct format (if necessary)
            String formattedStartDate = startDate.length() > 10 ? startDate.substring(0, 10) : startDate;
            String formattedEndDate = endDate.length() > 10 ? endDate.substring(0, 10) : endDate;

            // Construct the query, passing NULL if enterpriseKey is not provided
            String query = String.format("""
                SELECT * FROM get_top_customers_by_revenue('%s'::TIMESTAMP, '%s'::TIMESTAMP, %s)
            """, formattedStartDate, formattedEndDate, 
                    enterpriseKey == null ? "NULL" : String.format("'%s'", enterpriseKey));

            // Execute the query
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
            e.printStackTrace();  // Log the exception for debugging
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
