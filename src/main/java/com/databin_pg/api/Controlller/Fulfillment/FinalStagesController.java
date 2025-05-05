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
@CrossOrigin(origins = "http://localhost:5173")
public class FinalStagesController {

    @Autowired
    private PostgresService postgresService;

    // 📌 API: Get Final Stages Summary
    @GetMapping("/final-stages")
    public ResponseEntity<?> getFinalStagesSummary(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey) {

        try {
            String formattedKey = (enterpriseKey == null || enterpriseKey.isBlank()) ? "NULL" : "'" + enterpriseKey + "'";
            String query = String.format("""
                SELECT * FROM get_final_stage_summary('%s', '%s', %s)
            """, startDate, endDate, formattedKey);

            List<Map<String, Object>> data = postgresService.query(query);

            return ResponseEntity.ok(data);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch final stage summary", "details", e.getMessage()));
        }
    }
}
