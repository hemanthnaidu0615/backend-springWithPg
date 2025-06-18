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
@RequestMapping("/api/revenue")
@CrossOrigin(origins = "*")
@Tag(name = "Dashboard - Revenue Per Customer", description = "APIs for Revenue Analytics by Customer")
public class RevenuePerCustomerController {

    @Autowired
    private PostgresService postgresService;

    @Operation(
            summary = "Get Top 7 Customers by Revenue",
            description = "Fetches the top 7 customers based on total revenue within the specified date range, optionally filtered by enterprise key."
        )
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved top customers"),
            @ApiResponse(responseCode = "500", description = "Failed to fetch top customers by revenue")
        })
    @GetMapping("/top-customers")
    public ResponseEntity<?> getTopCustomersByRevenue(
    		 @Parameter(description = "Start date in YYYY-MM-DD format", required = true)
             @RequestParam(name = "startDate") String startDate,

             @Parameter(description = "End date in YYYY-MM-DD format", required = true)
             @RequestParam(name = "endDate") String endDate,

             @Parameter(description = "Optional enterprise key for filtering results 'AWW' or 'AWD'")
             @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey) {  
        try {
            
            String formattedStartDate = startDate.length() > 10 ? startDate.substring(0, 10) : startDate;
            String formattedEndDate = endDate.length() > 10 ? endDate.substring(0, 10) : endDate;

            
            String query = String.format("""
                SELECT * FROM get_top_customers_by_revenue('%s'::TIMESTAMP, '%s'::TIMESTAMP, %s)
            """, formattedStartDate, formattedEndDate, 
                    enterpriseKey == null ? "NULL" : String.format("'%s'", enterpriseKey));

            List<Map<String, Object>> data = postgresService.query(query);

            List<Map<String, Object>> topCustomers = new ArrayList<>();

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
