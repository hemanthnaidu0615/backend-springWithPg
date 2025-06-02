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
public class BottleneckAnalysisController {

    @Autowired
    private PostgresService postgresService;

    // 📌 API: Get Bottleneck Analysis
    @GetMapping("/bottleneck-analysis")
    public ResponseEntity<?> getBottleneckAnalysis(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey) {

        try {
            // Correctly format the enterpriseKey, if not provided, use NULL
            String formattedKey = (enterpriseKey == null || enterpriseKey.isBlank()) ? "NULL" : "'" + enterpriseKey + "'";

            // Ensure start and end date are in the correct format (YYYY-MM-DD)
            String formattedStartDate = startDate.split("T")[0];  // Only take the date part
            String formattedEndDate = endDate.split("T")[0];      // Only take the date part

            // The query that will call the stored procedure
            String query = String.format("""
                SELECT * FROM get_bottleneck_analysis('%s', '%s', %s)
            """, formattedStartDate, formattedEndDate, formattedKey);

            List<Map<String, Object>> data = postgresService.query(query);

            return ResponseEntity.ok(data);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch bottleneck analysis", "details", e.getMessage()));
        }
    }
}
