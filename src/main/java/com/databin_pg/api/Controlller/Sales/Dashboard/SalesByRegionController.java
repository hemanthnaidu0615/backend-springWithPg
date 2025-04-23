package com.databin_pg.api.Controlller.Sales.Dashboard;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.databin_pg.api.Service.PostgresService;

@RestController
@RequestMapping("/api/sales-by-region")
@CrossOrigin(origins = "http://localhost:5173")
public class SalesByRegionController {

    @Autowired
    private PostgresService postgresService;

    @GetMapping
    public ResponseEntity<?> getSalesByRegion(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate
    ) {
        try {
            String query = String.format("""
                SELECT * FROM get_sales_by_region(
                    '%s'::TIMESTAMP,
                    '%s'::TIMESTAMP
                )
            """, startDate, endDate);

            List<Map<String, Object>> data = postgresService.query(query);
            return ResponseEntity.ok(data);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch sales by region"));
        }
    }
    
    @GetMapping("/top5")
    public ResponseEntity<?> getTop5RevenueStates(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate
    ) {
        try {
            String query = String.format("""
                SELECT * FROM get_top5_states_by_revenue(
                    '%s'::TIMESTAMP,
                    '%s'::TIMESTAMP
                )
            """, startDate, endDate);

            List<Map<String, Object>> data = postgresService.query(query);
            return ResponseEntity.ok(data);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/countrywide")
    public ResponseEntity<?> getCountrywideSales(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate
    ) {
        try {
            String query = String.format("""
                SELECT * FROM get_countrywide_sales(
                    '%s'::TIMESTAMP,
                    '%s'::TIMESTAMP
                )
            """, startDate, endDate);

            List<Map<String, Object>> data = postgresService.query(query);
            return ResponseEntity.ok(data);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }


}
