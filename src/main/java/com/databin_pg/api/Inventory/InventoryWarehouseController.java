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
public class InventoryWarehouseController {

    @Autowired
    private PostgresService postgresService;

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
