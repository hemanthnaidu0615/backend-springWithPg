package com.databin_pg.api.Controlller.Sales.Analysis;
import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;

@RestController
@RequestMapping("/api/analysis")
@CrossOrigin(origins = "*")

public class SalesByAnalysisTopProductsController {
	 @Autowired
	    private PostgresService postgresService;

	    @GetMapping("/product-sales-summary")
	    public ResponseEntity<?> getProductSalesSummary(
	            @RequestParam(name = "startDate") String startDate,
	            @RequestParam(name = "endDate") String endDate,
	            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey
	    ) {
	        try {
	            // Format nullable string parameter for SQL
	            String formattedEnterpriseKey = (enterpriseKey == null || enterpriseKey.isBlank())
	                    ? "NULL" : "'" + enterpriseKey + "'";

	            // Build the query with parameters
	            String query = String.format("""
	                SELECT * FROM get_product_sales_summary('%s'::timestamp, '%s'::timestamp, %s)
	            """, startDate, endDate, formattedEnterpriseKey);

	            // Execute the query
	            List<Map<String, Object>> result = postgresService.query(query);

	            if (result.isEmpty()) {
	                return ResponseEntity.status(HttpStatus.NO_CONTENT)
	                        .body(Map.of("message", "No product sales data found for the given period."));
	            }

	            // Format and return the response
	            List<Map<String, Object>> productSales = new ArrayList<>();
	            for (Map<String, Object> row : result) {
	                Map<String, Object> product = Map.of(
	                        "product_name", row.get("product_name"),
	                        "units_sold", row.get("units_sold"),
	                        "total_sales", row.get("total_sales")
	                );
	                productSales.add(product);
	            }

	            return ResponseEntity.ok(Map.of("products", productSales));

	        } catch (Exception e) {
	            e.printStackTrace();
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                    .body(Map.of("error", "Failed to fetch product sales summary", "details", e.getMessage()));
	        }
	    }
}
