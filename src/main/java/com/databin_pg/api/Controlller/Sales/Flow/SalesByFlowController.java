package com.databin_pg.api.Controlller.Sales.Flow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.databin_pg.api.Service.PostgresService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/flow")
@CrossOrigin(origins = "*")

public class SalesByFlowController {
	   @Autowired
	    private PostgresService postgresService;

	    // Endpoint to get the hierarchical sales breakdown
	    @GetMapping("/breakdown")
	    public ResponseEntity<?> getSalesBreakdown(
	            @RequestParam(name = "startDate") String startDate,
	            @RequestParam(name = "endDate") String endDate) {

	        try {
	            // Construct the query string for the stored procedure call
	            String query = String.format("""
	                SELECT * FROM get_hierarchical_sales_breakdown(
	                    '%s'::TIMESTAMP,
	                    '%s'::TIMESTAMP
	                )
	            """, startDate, endDate);

	            // Execute the query using PostgresService
	            List<Map<String, Object>> result = postgresService.query(query);

	            if (result.isEmpty()) {
	                return ResponseEntity.ok(Map.of(
	                        "message", "No data found for the given date range."
	                ));
	            }

	            return ResponseEntity.ok(result);

	        } catch (Exception e) {
	            e.printStackTrace();
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                    .body(Map.of("error", "Failed to fetch hierarchical sales breakdown"));
	        }
	    }
}
