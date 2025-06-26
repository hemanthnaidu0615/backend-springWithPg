package com.databin_pg.api.Controlller.Orders;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.*;

@RestController
@RequestMapping("/api/order-filters")
@CrossOrigin(origins = "*")
@Tag(name = "Orders - Filters", description = "APIs to fetch available filter values for orders")
public class OrderFilters {

    @Autowired
    private PostgresService postgresService;

    @Operation(summary = "Get available filter values for orders",
               description = "Returns lists of possible values for order statuses, types, payment methods, and carriers.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Filter values retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error while fetching filter options")
    })
    @GetMapping("/filter-values")
    public ResponseEntity<?> getOrderFilterValues() {
        try {
            String query = "SELECT * FROM get_order_filter_arrays()";
            List<Map<String, Object>> results = postgresService.queryWithArrayHandling(query);

            if (results.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "statuses", List.of(),
                    "types", List.of(),
                    "methods", List.of(),
                    "carriers", List.of()
                ));
            }

            Map<String, Object> row = results.get(0);

            return ResponseEntity.ok(Map.of(
                "statuses", row.get("statuses"),
                "types", row.get("types"),
                "methods", row.get("methods"),
                "carriers", row.get("carriers")
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch filter options"));
        }
    }

}
