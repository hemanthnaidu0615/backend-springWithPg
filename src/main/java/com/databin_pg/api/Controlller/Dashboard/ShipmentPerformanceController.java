package com.databin_pg.api.Controlller.Dashboard;

import java.util.ArrayList;
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
@RequestMapping("/api/shipment-performance")
@CrossOrigin(origins = "http://localhost:5173")
public class ShipmentPerformanceController {

    @Autowired
    private PostgresService postgresService;

    @GetMapping
    public ResponseEntity<?> getShipmentPerformance(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate) {
        try {
            String query = String.format("""
                SELECT * FROM get_shipment_performance('%s'::TIMESTAMP, '%s'::TIMESTAMP)
            """, startDate, endDate);

            List<Map<String, Object>> result = postgresService.query(query);
            List<Map<String, Object>> shipments = new ArrayList<>();

            for (Map<String, Object> row : result) {
                shipments.add(Map.of(
                        "carrier", Objects.toString(row.get("carrier"), "Unknown"),
                        "standard", parseInteger(row.get("standard_shipments")),
                        "expedited", parseInteger(row.get("expedited_shipments")),
                        "same_day", parseInteger(row.get("same_day_shipments"))
                ));
            }

            return ResponseEntity.ok(Map.of("shipment_performance", shipments));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch shipment performance data"));
        }
    }

    private int parseInteger(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
