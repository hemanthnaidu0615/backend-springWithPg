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
@CrossOrigin(origins = "*")
public class SalesAndRevenueController {

    @Autowired
    private PostgresService postgresService;

    // 📌 API: Get Total Sales Data (with date filter and enterprise_key)
    @GetMapping("/sales-data")
    public ResponseEntity<?> getSalesData(
            @RequestParam(name= "startDate") String startDate,
            @RequestParam(name= "endDate") String endDate,
            @RequestParam(name= "enterpriseKey", required=false) String enterpriseKey) {  
        try {
        	String query = String.format("""
                    SELECT * FROM get_total_sales('%s'::TIMESTAMP, '%s'::TIMESTAMP, %s)
                """, startDate, endDate,
                    enterpriseKey == null ? "NULL" : String.format("'%s'", enterpriseKey));

            // Log the generated query
           // System.out.println("Executing Query: " + query);  // Debug log

            List<Map<String, Object>> data = postgresService.query(query);

            // Log the returned data
           // System.out.println("Returned Data: " + data);  // Debug log

            List<Map<String, Object>> salesData = new ArrayList<>();

            for (Map<String, Object> row : data) {
                String month = Objects.toString(row.get("month"), "N/A");

                double totalSales = 0.0;
                Object salesObj = row.get("total_sales");
                if (salesObj instanceof Number) {
                    totalSales = ((Number) salesObj).doubleValue();
                }

                salesData.add(Map.of(
                    "month", month,
                    "total_sales", totalSales
                ));
            }


            return ResponseEntity.ok(Map.of("sales_data", salesData));
        } catch (Exception e) {
            e.printStackTrace();  
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch total sales data"));
        }
    }


    // 📌 API: Get Revenue Trends Over Time (with date filter and enterprise_key)
    @GetMapping("/revenue-trends")
    public ResponseEntity<?> getRevenueTrends(
            @RequestParam(name= "startDate") String startDate,
            @RequestParam(name= "endDate") String endDate,
            @RequestParam(name= "enterpriseKey", required=false) String enterpriseKey) {  // Added enterpriseKey
        try {
        	String query = String.format("""
                    SELECT * FROM get_revenue_trends('%s'::TIMESTAMP, '%s'::TIMESTAMP, %s)
                """, startDate, endDate,
                    enterpriseKey == null ? "NULL" : String.format("'%s'", enterpriseKey));
    

            List<Map<String, Object>> data = postgresService.query(query);
            List<Map<String, Object>> trends = new ArrayList<>();

            for (Map<String, Object> row : data) {
                String month = Objects.toString(row.get("month"), "N/A");

                double monthlyRevenue = 0.0;
                Object revenueObj = row.get("monthly_revenue");
                if (revenueObj instanceof Number) {
                    monthlyRevenue = ((Number) revenueObj).doubleValue();
                }

                trends.add(Map.of(
                    "month", month,
                    "monthly_revenue", monthlyRevenue
                ));
            }

            return ResponseEntity.ok(Map.of("revenue_trends", trends));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch revenue trends"));
        }
    }

    // 📌 API: Get Forecasted Sales (with date filter and enterprise_key)
    @GetMapping("/forecasted-sales")
    public ResponseEntity<?> getForecastedSales(
            @RequestParam(name= "startDate") String startDate,
            @RequestParam(name= "endDate") String endDate,
            @RequestParam(name= "enterpriseKey", required=false) String enterpriseKey) {  // Added enterpriseKey
        try {
        	String query = String.format("""
                    SELECT * FROM get_forecasted_sales('%s'::TIMESTAMP, '%s'::TIMESTAMP, %s)
                """, startDate, endDate,
                    enterpriseKey == null ? "NULL" : String.format("'%s'", enterpriseKey));
            
            List<Map<String, Object>> data = postgresService.query(query);
            List<Map<String, Object>> forecasts = new ArrayList<>();

            for (Map<String, Object> row : data) {
                String month = Objects.toString(row.get("month"), "N/A");

                double forecasted = 0.0;
                Object forecastObj = row.get("forecasted_sales");
                if (forecastObj instanceof Number) {
                    forecasted = ((Number) forecastObj).doubleValue();
                }

                forecasts.add(Map.of(
                    "month", month,
                    "forecasted_sales", forecasted
                ));
            }

            return ResponseEntity.ok(Map.of("forecasted_sales", forecasts));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch forecasted sales"));
        }
    }

    // Helper method to parse BigDecimal to double
    private double parseDouble(Object value) {
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).doubleValue();
        }
        return 0.0;
    }
}
