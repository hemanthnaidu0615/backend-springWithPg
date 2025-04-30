package com.databin_pg.api.Inventory;

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
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "http://localhost:5173")


public class InventoryKPIController {
	
	 @Autowired
	    private PostgresService postgresService;
	 
	@GetMapping("/stock-summary")
	public ResponseEntity<?> getStockSummary(
	        @RequestParam(name = "startDate") String startDate,
	        @RequestParam(name = "endDate") String endDate) {

	    try {
	        String summaryQuery = String.format("""
	            SELECT * FROM get_inventory_summary('%s'::date, '%s'::date)
	        """, startDate, endDate);

	        List<Map<String, Object>> result = postgresService.query(summaryQuery);
	        if (result.isEmpty()) {
	            return ResponseEntity.ok(Map.of(
	                "total_products", 0,
	                "available", 0,
	                "low_stock", 0,
	                "out_of_stock", 0
	            ));
	        }

	        Map<String, Object> summary = result.get(0);

	        return ResponseEntity.ok(Map.of(
	                "total_products", summary.get("total_products"),
	                "available", summary.get("available"),
	                "low_stock", summary.get("low_stock"),
	                "out_of_stock", summary.get("out_of_stock")
	        ));

	    } catch (Exception e) {
	        e.printStackTrace();
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body(Map.of("error", "Failed to fetch stock summary"));
	    }
	}

}
