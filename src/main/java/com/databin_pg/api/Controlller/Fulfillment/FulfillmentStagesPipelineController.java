package com.databin_pg.api.Controlller.Fulfillment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.databin_pg.api.Service.PostgresService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fulfillment")
@CrossOrigin(origins = "*")
public class FulfillmentStagesPipelineController {

    @Autowired
    private PostgresService postgresService;

    // 📌 API: Get Fulfillment Stage Pipeline
    @GetMapping("/stages-pipeline")
    public ResponseEntity<?> getFulfillmentStagesPipeline(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey) {

        try {
            // Prepare query
            String formattedKey = (enterpriseKey == null || enterpriseKey.isBlank()) ? "NULL" : "'" + enterpriseKey + "'";
            String query = String.format("""
                SELECT * FROM get_fulfillment_stages_pipeline('%s', '%s', %s)
            """, startDate, endDate, formattedKey);

            List<Map<String, Object>> data = postgresService.query(query);

            return ResponseEntity.ok(data);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch stage pipeline", "details", e.getMessage()));
        }
    }
}
