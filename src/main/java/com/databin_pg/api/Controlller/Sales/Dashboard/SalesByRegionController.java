package com.databin_pg.api.Controlller.Sales.Dashboard;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

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

    // 🗺️ State abbreviation to full name map
    private static final Map<String, String> STATE_ABBREVIATIONS = Map.ofEntries(
    	    Map.entry("AL", "Alabama"),
    	    Map.entry("AK", "Alaska"),
    	    Map.entry("AZ", "Arizona"),
    	    Map.entry("AR", "Arkansas"),
    	    Map.entry("CA", "California"),
    	    Map.entry("CO", "Colorado"),
    	    Map.entry("CT", "Connecticut"),
    	    Map.entry("DE", "Delaware"),
    	    Map.entry("FL", "Florida"),
    	    Map.entry("GA", "Georgia"),
    	    Map.entry("HI", "Hawaii"),
    	    Map.entry("ID", "Idaho"),
    	    Map.entry("IL", "Illinois"),
    	    Map.entry("IN", "Indiana"),
    	    Map.entry("IA", "Iowa"),
    	    Map.entry("KS", "Kansas"),
    	    Map.entry("KY", "Kentucky"),
    	    Map.entry("LA", "Louisiana"),
    	    Map.entry("ME", "Maine"),
    	    Map.entry("MD", "Maryland"),
    	    Map.entry("MA", "Massachusetts"),
    	    Map.entry("MI", "Michigan"),
    	    Map.entry("MN", "Minnesota"),
    	    Map.entry("MS", "Mississippi"),
    	    Map.entry("MO", "Missouri"),
    	    Map.entry("MT", "Montana"),
    	    Map.entry("NE", "Nebraska"),
    	    Map.entry("NV", "Nevada"),
    	    Map.entry("NH", "New Hampshire"),
    	    Map.entry("NJ", "New Jersey"),
    	    Map.entry("NM", "New Mexico"),
    	    Map.entry("NY", "New York"),
    	    Map.entry("NC", "North Carolina"),
    	    Map.entry("ND", "North Dakota"),
    	    Map.entry("OH", "Ohio"),
    	    Map.entry("OK", "Oklahoma"),
    	    Map.entry("OR", "Oregon"),
    	    Map.entry("PA", "Pennsylvania"),
    	    Map.entry("RI", "Rhode Island"),
    	    Map.entry("SC", "South Carolina"),
    	    Map.entry("SD", "South Dakota"),
    	    Map.entry("TN", "Tennessee"),
    	    Map.entry("TX", "Texas"),
    	    Map.entry("UT", "Utah"),
    	    Map.entry("VT", "Vermont"),
    	    Map.entry("VA", "Virginia"),
    	    Map.entry("WA", "Washington"),
    	    Map.entry("WV", "West Virginia"),
    	    Map.entry("WI", "Wisconsin"),
    	    Map.entry("WY", "Wyoming"),
    	    Map.entry("DC", "District of Columbia")
    	);


    @GetMapping
    public ResponseEntity<?> getSalesByRegion(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "enterpriseKey", required=false) String enterpriseKey  // ✅ added
    ) {
        try {
            String query = String.format("""
                SELECT * FROM get_sales_by_region(
                    '%s'::TIMESTAMP,
                    '%s'::TIMESTAMP,
                    '%s'::TEXT
                )
            """, startDate, endDate, enterpriseKey);  // ✅ added

            List<Map<String, Object>> data = postgresService.query(query);

            mapStateNames(data);

            return ResponseEntity.ok(data);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch sales by region"));
        }
    }


    @GetMapping("/top5")
    public ResponseEntity<?> getTop5RevenueStates(
            @RequestParam(name = "startDate") String startDate,
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "enterpriseKey", required=false) String enterpriseKey
    ) {
        try {
            String query = String.format("""
                SELECT * FROM get_top5_states_by_revenue(
                    '%s'::TIMESTAMP,
                    '%s'::TIMESTAMP,
                    '%s'
                )
            """, startDate, endDate, enterpriseKey);

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
            @RequestParam(name = "endDate") String endDate,
            @RequestParam(name = "enterpriseKey", required=false) String enterpriseKey
    ) {
        try {
            String query = String.format("""
                SELECT * FROM get_countrywide_sales(
                    '%s'::TIMESTAMP,
                    '%s'::TIMESTAMP,
                    '%s'
                )
            """, startDate, endDate, enterpriseKey);

            List<Map<String, Object>> data = postgresService.query(query);
            mapStateNames(data);

            return ResponseEntity.ok(data);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }


    // 🧠 Helper method to replace state abbreviations with full names
    private void mapStateNames(List<Map<String, Object>> data) {
        for (Map<String, Object> row : data) {
            Object abbrevObj = row.get("state_name");
            if (abbrevObj != null) {
                String abbreviation = abbrevObj.toString();
                String fullName = STATE_ABBREVIATIONS.getOrDefault(abbreviation, abbreviation);
                row.put("state_name", fullName);
            }
        }
    }
}
