package com.databin_pg.api.Controlller.Sales.Dashboard;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;

@RestController
@RequestMapping("/api/sales/volume-value")
@CrossOrigin(origins = "*")
public class VolumeValueController {

    @Autowired
    private PostgresService postgresService;

    // Endpoint for enterpriseKey AWW
    @GetMapping("/aww")
    public ResponseEntity<?> getVolumeValueAww(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return fetchVolumeValueData("get_volume_value_by_product_aww", startDate, endDate, page, size);
    }

    // Endpoint for enterpriseKey AWD
    @GetMapping("/awd")
    public ResponseEntity<?> getVolumeValueAwd(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return fetchVolumeValueData("get_volume_value_by_product_awd", startDate, endDate, page, size);
    }

    private ResponseEntity<?> fetchVolumeValueData(String procedureName, String startDate, String endDate, int page, int size) {
        try {
            int offset = page * size;

            // Count query
            String countQuery = String.format("""
                SELECT COUNT(*) AS total FROM %s('%s'::timestamp, '%s'::timestamp)
            """, procedureName, startDate, endDate);
            List<Map<String, Object>> countResult = postgresService.query(countQuery);
            int totalCount = (!countResult.isEmpty() && countResult.get(0).get("total") != null)
                    ? ((Number) countResult.get(0).get("total")).intValue()
                    : 0;

            // Paginated query
            String dataQuery = String.format("""
                SELECT * FROM %s('%s'::timestamp, '%s'::timestamp)
                OFFSET %d LIMIT %d
            """, procedureName, startDate, endDate, offset, size);
            List<Map<String, Object>> result = postgresService.query(dataQuery);

            return ResponseEntity.ok(Map.of(
                    "data", result,
                    "page", page,
                    "size", size,
                    "count", totalCount
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to fetch volume/value data",
                            "details", e.getMessage()
                    ));
        }
    }
}
