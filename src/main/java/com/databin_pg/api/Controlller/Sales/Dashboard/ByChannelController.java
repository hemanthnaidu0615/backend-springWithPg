package com.databin_pg.api.Controlller.Sales.Dashboard;


import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales/by-channel")
@CrossOrigin(origins = "*")
public class ByChannelController {

    @Autowired
    private PostgresService postgresService;

    @GetMapping("/aww")
    public ResponseEntity<?> getOrderSummaryAww(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate) {
        return fetchOrderSummary("get_order_summary_by_channel_aww", startDate, endDate);
    }

    @GetMapping("/awd")
    public ResponseEntity<?> getOrderSummaryAwd(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate) {
        return fetchOrderSummary("get_order_summary_by_channel_awd", startDate, endDate);
    }

    private ResponseEntity<?> fetchOrderSummary(String procedureName, String startDate, String endDate) {
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
                        .body(Map.of("message", "No order summary data found."));
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to fetch order summary data",
                            "details", e.getMessage()
                    ));
        }
    }
}
