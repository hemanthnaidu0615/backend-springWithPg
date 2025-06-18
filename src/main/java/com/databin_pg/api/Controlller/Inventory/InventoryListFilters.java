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
@Tag(name = "Inventory - Filters", description = "APIs for retrieving inventory filter values like status and category names")
public class InventoryListFilters {

    @Autowired
    private PostgresService postgresService;

    @Operation(
        summary = "Get distinct inventory statuses",
        description = "Fetches a list of unique inventory statuses from the inventory database"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved inventory statuses"),
        @ApiResponse(responseCode = "500", description = "Failed to fetch unique statuses")
    })
    @GetMapping("/status-list")
    public ResponseEntity<?> getDistinctInventoryStatuses() {
        try {
            String query = "SELECT * FROM get_distinct_inventory_statuses()";
            List<Map<String, Object>> result = postgresService.query(query);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch unique statuses"));
        }
    }

    @Operation(
        summary = "Get all category names",
        description = "Fetches a list of all category names available in inventory"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved category names"),
        @ApiResponse(responseCode = "500", description = "Failed to fetch category names")
    })
    @GetMapping("/category-names")
    public ResponseEntity<?> getAllCategoryNames() {
        try {
            String query = "SELECT * FROM get_all_category_names()";
            List<Map<String, Object>> result = postgresService.query(query);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch category names"));
        }
    }
}
