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
@RequestMapping("/api/top-sellers")
@CrossOrigin(origins = "*")
@Tag(name = "Dashboard - Top Selling Products", description = "APIs for retrieving top selling products by quantity")
public class TopSellingProductsController {

    @Autowired
    private PostgresService postgresService;

    @Operation(
            summary = "Get top 5 selling products",
            description = "Retrieves the top 5 selling products within a given date range. Optional filtering by enterprise key such as 'AWW' or 'AWD'."
        )
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Top selling products fetched successfully"),
            @ApiResponse(responseCode = "500", description = "Failed to fetch top selling products")
        })
    @GetMapping("/top-products")
    public ResponseEntity<?> getTopSellingProducts(
    		@Parameter(description = "Start date in YYYY-MM-DD format", required = true)
            @RequestParam(name = "startDate") String startDate,

            @Parameter(description = "End date in YYYY-MM-DD format", required = true)
            @RequestParam(name = "endDate") String endDate,

            @Parameter(description = "Optional enterprise key for filtering results 'AWW' or 'AWD'")
            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey) {  

        try {
            // Include enterpriseKey in the query
        	String query = String.format(
        		    "SELECT * FROM get_top_selling_products(TIMESTAMP '%s', TIMESTAMP '%s', %s)", 
        		    startDate, endDate,
        		    enterpriseKey == null ? "NULL" : String.format("'%s'", enterpriseKey)
        		);


            List<Map<String, Object>> data = postgresService.query(query);
            List<Map<String, Object>> topProducts = new ArrayList<>();

            for (Map<String, Object> row : data) {
                String name = Objects.toString(row.get("product_name"), "N/A");
                int quantity = parseInteger(row.get("total_quantity"));
                double percent = parseDouble(row.get("percentage"));
                String description = Objects.toString(row.get("description"), "N/A");
                double price = parseDouble(row.get("price"));

                topProducts.add(Map.of(
                    "product_name", name,
                    "quantity_sold", quantity,
                    "percentage", String.format("%.2f%%", percent),
                    "description", description,
                    "price", price
                ));
            }

            return ResponseEntity.ok(Map.of("top_products", topProducts));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch top selling products"));
        }
    }

    // 🔹 Helper Method: Convert Object to Integer
    private int parseInteger(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid integer format: " + obj, e);
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
