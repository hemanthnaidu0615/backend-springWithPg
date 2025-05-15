package com.databin_pg.api.Controlller.Sales.Dashboard;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales/by-fulfillment")
@CrossOrigin(origins = "http://localhost:5173")
public class ByFulfillmentController {

    @Autowired
    private PostgresService postgresService;

    @GetMapping("/aww")
    public ResponseEntity<?> getFulfillmentSummaryAww(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate) {
        return fetchFulfillmentSummary("get_fulfillment_summary_by_channel_aww", startDate, endDate);
    }

    @GetMapping("/awd")
    public ResponseEntity<?> getFulfillmentSummaryAwd(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate) {
        return fetchFulfillmentSummary("get_fulfillment_summary_by_channel_awd", startDate, endDate);
    }

    private ResponseEntity<?> fetchFulfillmentSummary(String procedureName, String startDate, String endDate) {
        try {
            String query = String.format(
                    "SELECT * FROM %s('%s'::timestamp, '%s'::timestamp)",
                    procedureName,
                    startDate,
                    endDate
            );

            List<Map<String, Object>> result = postgresService.query(query);

            if (result.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body(Map.of("message", "No fulfillment data found."));
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to fetch fulfillment data",
                            "details", e.getMessage()
                    ));
        }
    }
}
