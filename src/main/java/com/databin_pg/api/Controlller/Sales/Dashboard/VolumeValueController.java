package com.databin_pg.api.Controlller.Sales.Dashboard;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;

@RestController
@RequestMapping("/api/sales/volume-value")
@CrossOrigin(origins = "http://localhost:5173")
public class VolumeValueController {

    @Autowired
    private PostgresService postgresService;

    // Endpoint for enterpriseKey AWW
    @GetMapping("/aww")
    public ResponseEntity<?> getVolumeValueAww(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate) {
        return fetchVolumeValueData("get_volume_value_by_product_aww", startDate, endDate);
    }

    // Endpoint for enterpriseKey AWD
    @GetMapping("/awd")
    public ResponseEntity<?> getVolumeValueAwd(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate) {
        return fetchVolumeValueData("get_volume_value_by_product_awd", startDate, endDate);
    }

    private ResponseEntity<?> fetchVolumeValueData(String procedureName, String startDate, String endDate) {
        try {
            // NOTE: Make sure startDate and endDate are ISO-8601 strings like "2024-01-01T00:00:00"
            String query = String.format(
                    "SELECT * FROM %s('%s'::timestamp, '%s'::timestamp)",
                    procedureName,
                    startDate,
                    endDate
            );

            List<Map<String, Object>> result = postgresService.query(query);

            if (result.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body(Map.of("message", "No volume/value data found."));
            }

            return ResponseEntity.ok(result);

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
