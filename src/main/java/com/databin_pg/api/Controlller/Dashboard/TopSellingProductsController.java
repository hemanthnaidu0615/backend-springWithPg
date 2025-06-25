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

    @GetMapping("/top-products")
    @Operation(
        summary = "Get top selling products with pagination, sorting, and search",
        description = "Retrieves the top selling products within a date range. Supports optional filtering by enterprise key, pagination, sorting, and search."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Top selling products fetched successfully"),
        @ApiResponse(responseCode = "500", description = "Failed to fetch top selling products")
    })
    public ResponseEntity<?> getTopSellingProducts(
        @Parameter(description = "Start date in YYYY-MM-DD format", required = true)
        @RequestParam(name = "startDate") String startDate,

        @Parameter(description = "End date in YYYY-MM-DD format", required = true)
        @RequestParam(name = "endDate") String endDate,

        @Parameter(description = "Optional enterprise key (e.g., AWW, AWD)")
        @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey,

        @Parameter(description = "Page number (default 0)")
        @RequestParam(name = "page", required = false, defaultValue = "0") int page,

        @Parameter(description = "Page size (default 10)")
        @RequestParam(name = "size", required = false, defaultValue = "10") int size,

        @Parameter(description = "Field to sort by (e.g., quantity, percentage, price)")
        @RequestParam(name = "sortField", required = false, defaultValue = "quantity") String sortField,

        @Parameter(description = "Sort order (asc or desc)")
        @RequestParam(name = "sortOrder", required = false, defaultValue = "desc") String sortOrder,

        @Parameter(description = "Search keyword for product name or description")
        @RequestParam(name = "search", required = false) String search
    ) {
        try {
            int offset = page * size;

            // Sanitize and map sort field
            Map<String, String> allowedSortFields = Map.of(
                "product_name", "product_name",
                "quantity", "total_quantity",
                "percentage", "percentage",
                "price", "price"
            );
            String sortColumn = allowedSortFields.getOrDefault(sortField, "total_quantity");
            String sortDirection = sortOrder.equalsIgnoreCase("asc") ? "ASC" : "DESC";

            String formattedKey = enterpriseKey == null ? "NULL" : "'" + enterpriseKey.replace("'", "''") + "'";
            String filterClause = "";

            if (search != null && !search.trim().isEmpty()) {
                String safeSearch = search.toLowerCase().replace("'", "''");
                filterClause = String.format(
                    "WHERE LOWER(product_name) LIKE '%%%s%%' OR LOWER(description) LIKE '%%%s%%'",
                    safeSearch, safeSearch
                );
            }

            // Count query
            String countQuery = String.format(
                """
                SELECT COUNT(*) AS total FROM (
                    SELECT * FROM get_top_selling_products(TIMESTAMP '%s', TIMESTAMP '%s', %s)
                ) AS result
                %s
                """,
                startDate, endDate, formattedKey,
                filterClause
            );

            int totalCount = ((Number) postgresService.query(countQuery).get(0).get("total")).intValue();

            // Data query
            String dataQuery = String.format(
                """
                SELECT * FROM (
                    SELECT * FROM get_top_selling_products(TIMESTAMP '%s', TIMESTAMP '%s', %s)
                ) AS result
                %s
                ORDER BY %s %s
                OFFSET %d LIMIT %d
                """,
                startDate, endDate, formattedKey,
                filterClause,
                sortColumn, sortDirection,
                offset, size
            );

            List<Map<String, Object>> data = postgresService.query(dataQuery);
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

            return ResponseEntity.ok(Map.of(
                "data", topProducts,
                "page", page,
                "size", size,
                "count", totalCount
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch top selling products", "details", e.getMessage()));
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
