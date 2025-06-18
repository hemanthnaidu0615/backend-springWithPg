package com.databin_pg.api.Controlller.Inventory;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.databin_pg.api.Service.PostgresService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*")
@Tag(name = "Inventory - KPI", description = "APIs for Inventory KPI metrics such as stock levels")
public class InventoryKPIController {

    @Autowired
    private PostgresService postgresService;

    @Operation(
        summary = "Get Inventory Stock Summary",
        description = "Returns a summary of inventory stock levels including total products, available, low stock, and out of stock."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved stock summary"),
        @ApiResponse(responseCode = "500", description = "Failed to fetch stock summary")
    })
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
