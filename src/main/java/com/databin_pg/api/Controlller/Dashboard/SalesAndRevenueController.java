package com.databin_pg.api.Controlller.Dashboard;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/sales-revenue")
@CrossOrigin(origins = "http://localhost:5173")
public class SalesAndRevenueController {

    @Autowired
    private PostgresService postgresService;

    // 📌 API: Get Total Sales Data (with date filter)
 // 📌 API: Get Total Sales Data (with date filter)
    @GetMapping("/sales-data")
    public ResponseEntity<?> getSalesData(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        try {
            String query = String.format("""
                SELECT * FROM get_total_sales('%s'::TIMESTAMP, '%s'::TIMESTAMP)
            """, startDate, endDate);

            //System.out.println("Executing query: " + query); // Log the query being executed
            List<Map<String, Object>> data = postgresService.query(query);

            // Log the data returned by the query
           // System.out.println("Data returned: " + data);

            double totalSales = 0.0;
            if (!data.isEmpty()) {
                // Extract the BigDecimal value and convert it to double
                BigDecimal sales = (BigDecimal) data.get(0).get("get_total_sales");
                if (sales != null) {
                    totalSales = sales.doubleValue(); // Convert BigDecimal to double
                }
            }

            return ResponseEntity.ok(Map.of("total_sales", totalSales));
        } catch (Exception e) {  // Catching general exceptions
            e.printStackTrace(); // Log the stack trace to get more insight into the error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch total sales data"));
        }
    }


    // 📌 API: Get Revenue Trends Over Time (with date filter)
    @GetMapping("/revenue-trends")
    public ResponseEntity<?> getRevenueTrends(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        try {
            String query = String.format("""
                SELECT * FROM get_revenue_trends('%s'::TIMESTAMP, '%s'::TIMESTAMP)
            """, startDate, endDate);

            //System.out.println("Executing query: " + query); // Log the query being executed
            List<Map<String, Object>> data = postgresService.query(query);

            // Log the data returned by the query
           // System.out.println("Data returned: " + data);

            return ResponseEntity.ok(Map.of("revenue_trends", data));
        } catch (Exception e) {
            e.printStackTrace(); // Log the stack trace for debugging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch revenue trends"));
        }
    }

    @GetMapping("/forecasted-sales")
    public ResponseEntity<?> getForecastedSales(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        try {
            String query = String.format("""
                SELECT * FROM get_forecasted_sales('%s'::TIMESTAMP, '%s'::TIMESTAMP)
            """, startDate, endDate);

            List<Map<String, Object>> data = postgresService.query(query);
            double forecastedSales = 0.0;

            if (!data.isEmpty()) {
                Object rawValue = data.get(0).get("get_forecasted_sales");
                if (rawValue instanceof BigDecimal) {
                    forecastedSales = ((BigDecimal) rawValue).doubleValue();
                }
            }

            return ResponseEntity.ok(Map.of("forecasted_sales", forecastedSales));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch forecasted sales"));
        }
    }

}
