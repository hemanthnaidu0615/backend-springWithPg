package com.databin_pg.api.Controlller.Dashboard;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
@RequestMapping("/api/fulfillment-efficiency")
@CrossOrigin(origins = "*")
public class FulfillmentEfficiencyTrackerController {

    @Autowired
    private PostgresService postgresService;

    @GetMapping("/summary")
    public ResponseEntity<?> getFulfillmentSummary(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "enterpriseKey", required=false) String enterpriseKey) { // Added enterpriseKey parameter

        try {
            // Ensure only YYYY-MM-DD is passed (strip time if present)
            String formattedStartDate = startDate.length() > 10 ? startDate.substring(0, 10) : startDate;
            String formattedEndDate = endDate.length() > 10 ? endDate.substring(0, 10) : endDate;

            // Call the PostgreSQL function with explicit type casting, passing the enterpriseKey
            String query = String.format("""
            	    SELECT * FROM get_fulfillment_summary('%s'::date, '%s'::date, %s)
            	""", formattedStartDate, formattedEndDate,
            	     enterpriseKey == null ? "NULL" : String.format("'%s'", enterpriseKey));

            List<Map<String, Object>> rows = postgresService.query(query);
            Map<String, Map<String, Integer>> summary = new LinkedHashMap<>();

            for (Map<String, Object> row : rows) {
                String eventDay = row.get("event_day").toString();
                String category = Objects.toString(row.get("category"), "Unknown");
                int count = ((Number) row.get("event_count")).intValue();

                if (category.equals("Unknown")) continue;

                summary.computeIfAbsent(category, k -> new LinkedHashMap<>());
                summary.get(category).merge(eventDay, count, Integer::sum);
            }


            return ResponseEntity.ok(Map.of("fulfillment_summary", summary));

        } catch (Exception e) {
            e.printStackTrace(); // Helpful for local debugging/logging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch fulfillment event data"));
        }
    }

}
