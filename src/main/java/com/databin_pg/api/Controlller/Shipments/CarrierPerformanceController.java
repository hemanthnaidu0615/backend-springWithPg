package com.databin_pg.api.Controlller.Shipments;

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
@RequestMapping("/api/carrier-performance")
@CrossOrigin(origins = "http://localhost:5173")
public class CarrierPerformanceController {

    @Autowired
    private PostgresService postgresService;

    @GetMapping
    public ResponseEntity<?> getCarrierPerformance(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "carrier", required = false) String carrier,
            @RequestParam(name = "shippingMethod", required = false) String shippingMethod
    ) {
        try {
            String query = String.format("""
                SELECT * FROM get_carrier_performance(
                    '%s'::TIMESTAMP,
                    '%s'::TIMESTAMP,
                    %s,
                    %s
                )
            """,
                startDate,
                endDate,
                carrier != null ? "'" + carrier + "'" : "NULL",
                shippingMethod != null ? "'" + shippingMethod + "'" : "NULL"
            );

            List<Map<String, Object>> data = postgresService.query(query);
            return ResponseEntity.ok(data);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch carrier performance"));
        }
    }
}
