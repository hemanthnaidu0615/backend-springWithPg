package com.databin_pg.api.Controlller.Inventory;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.databin_pg.api.Service.PostgresService;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*") 

public class InventoryListFilters {
	@Autowired
    private PostgresService postgresService;

    // Endpoint to get distinct statuses from the inventory table
    @GetMapping("/status-list")
    public ResponseEntity<?> getDistinctInventoryStatuses() {
        try {
            // Call the stored procedure to get distinct statuses
            String query = "SELECT * FROM get_distinct_inventory_statuses()";
            List<Map<String, Object>> result = postgresService.query(query);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch unique statuses"));
        }
    }

    // Endpoint to get all category names
    @GetMapping("/category-names")
    public ResponseEntity<?> getAllCategoryNames() {
        try {
            // Call the stored procedure to get all category names
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
