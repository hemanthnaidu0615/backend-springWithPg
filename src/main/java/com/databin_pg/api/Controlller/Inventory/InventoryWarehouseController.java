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
@Tag(name = "Inventory - Warehouse", description = "APIs for region-wise inventory and warehouse-related insights")
public class InventoryWarehouseController {

    @Autowired
    private PostgresService postgresService;

    @Operation(
        summary = "Get region-wise inventory distribution",
        description = "Returns the inventory distribution grouped by regions based on the specified date range."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved region-wise inventory distribution"),
        @ApiResponse(responseCode = "400", description = "Missing or invalid request parameters"),
        @ApiResponse(responseCode = "500", description = "Server error while fetching region-wise inventory distribution")
    })
    @GetMapping("/region-distribution")
    public ResponseEntity<?> getRegionInventoryDistribution(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate) {

        try {
            if (startDate == null || endDate == null || startDate.isEmpty() || endDate.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Start date and end date are required."));
            }

            String query = String.format("""
                SELECT * FROM get_region_inventory_distribution('%s'::timestamp, '%s'::timestamp)
            """, startDate, endDate);

            List<Map<String, Object>> result = postgresService.query(query);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch region-wise inventory distribution"));
        }
    }
}
