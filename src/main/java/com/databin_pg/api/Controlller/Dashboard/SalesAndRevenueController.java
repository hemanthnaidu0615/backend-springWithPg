package com.databin_pg.api.Controlller.Dashboard;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
@RequestMapping("/api/sales-revenue")
@CrossOrigin(origins = "*")
@Tag(name = "Dashboard - Sales & Revenue", description = "APIs for Sales Data, Revenue Trends, and Forecasting")
public class SalesAndRevenueController {

    @Autowired
    private PostgresService postgresService;

    @Operation(
            summary = "Get Total Sales Data",
            description = "Returns the total sales grouped by month within a date range and optional enterprise key."
        )
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved sales data"),
            @ApiResponse(responseCode = "500", description = "Failed to fetch total sales data")
        })    
    @GetMapping("/sales-data")
    public ResponseEntity<?> getSalesData(
    		 @Parameter(description = "Start date in YYYY-MM-DD format", required = true)
             @RequestParam(name = "startDate") String startDate,

             @Parameter(description = "End date in YYYY-MM-DD format", required = true)
             @RequestParam(name = "endDate") String endDate,

             @Parameter(description = "Optional enterprise key for filtering results 'AWW' or 'AWD'")
             @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey) {  
        try {
        	String query = String.format("""
                    SELECT * FROM get_total_sales('%s'::TIMESTAMP, '%s'::TIMESTAMP, %s)
                """, startDate, endDate,
                    enterpriseKey == null ? "NULL" : String.format("'%s'", enterpriseKey));

            List<Map<String, Object>> data = postgresService.query(query);

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


    @Operation(
            summary = "Get Revenue Trends",
            description = "Returns revenue trends grouped by month, filtered by date range and optional enterprise key."
        )
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved revenue trends"),
            @ApiResponse(responseCode = "500", description = "Failed to fetch revenue trends")
        })
    @GetMapping("/revenue-trends")
    public ResponseEntity<?> getRevenueTrends(
    		@Parameter(description = "Start date in YYYY-MM-DD format", required = true)
            @RequestParam(name = "startDate") String startDate,

            @Parameter(description = "End date in YYYY-MM-DD format", required = true)
            @RequestParam(name = "endDate") String endDate,

            @Parameter(description = "Optional enterprise key for filtering results 'AWW' or 'AWD'")
            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey) {  // Added enterpriseKey
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

    @Operation(
            summary = "Get Forecasted Sales",
            description = "Returns forecasted monthly sales for a given date range and optional enterprise key."
        )
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved forecasted sales"),
            @ApiResponse(responseCode = "500", description = "Failed to fetch forecasted sales")
        })
    @GetMapping("/forecasted-sales")
    public ResponseEntity<?> getForecastedSales(
    		@Parameter(description = "Start date in YYYY-MM-DD format", required = true)
            @RequestParam(name = "startDate") String startDate,

            @Parameter(description = "End date in YYYY-MM-DD format", required = true)
            @RequestParam(name = "endDate") String endDate,

            @Parameter(description = "Optional enterprise key for filtering results 'AWW' or 'AWD'")
            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey) {  // Added enterpriseKey
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

   
}
