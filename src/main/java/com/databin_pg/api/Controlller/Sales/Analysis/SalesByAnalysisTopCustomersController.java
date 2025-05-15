package com.databin_pg.api.Controlller.Sales.Analysis;
import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;

@RestController
@RequestMapping("/api/analysis")
@CrossOrigin(origins = "http://localhost:5173")

public class SalesByAnalysisTopCustomersController {
	  @Autowired
	    private PostgresService postgresService;

	    @GetMapping("/customer-order-summary")
	    public ResponseEntity<?> getCustomerOrderSummary(
	            @RequestParam(name = "startDate") String startDate,
	            @RequestParam(name = "endDate") String endDate,
	            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey
	    ) {
	        try {
	            // Format nullable enterpriseKey parameter for SQL
	            String formattedEnterpriseKey = (enterpriseKey == null || enterpriseKey.isBlank())
	                    ? "NULL" : "'" + enterpriseKey + "'";

	            // Build the SQL query
	            String query = String.format("""
	                SELECT * FROM get_customer_order_summary('%s'::timestamp, '%s'::timestamp, %s)
	            """, startDate, endDate, formattedEnterpriseKey);

	            // Execute the query
	            List<Map<String, Object>> result = postgresService.query(query);

	            if (result.isEmpty()) {
	                return ResponseEntity.status(HttpStatus.NO_CONTENT)
	                        .body(Map.of("message", "No customer order data found for the given period."));
	            }

	            // Format and return the response
	            List<Map<String, Object>> customerData = new ArrayList<>();
	            for (Map<String, Object> row : result) {
	                Map<String, Object> customer = Map.of(
	                        "customer_name", row.get("customer_name"),
	                        "total_orders", row.get("total_orders"),
	                        "total_spent", row.get("total_spent")
	                );
	                customerData.add(customer);
	            }

	            return ResponseEntity.ok(Map.of("customers", customerData));

	        } catch (Exception e) {
	            e.printStackTrace();
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                    .body(Map.of("error", "Failed to fetch customer order summary", "details", e.getMessage()));
	        }
	    }
}
