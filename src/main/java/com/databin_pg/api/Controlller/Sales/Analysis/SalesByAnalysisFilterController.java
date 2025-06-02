package com.databin_pg.api.Controlller.Sales.Analysis;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/analysis")
@CrossOrigin(origins = "*")

public class SalesByAnalysisFilterController {
	 @Autowired
	    private PostgresService postgresService;

	    // 📌 API: Get distinct fulfilment channels
	    @GetMapping("/channels")
	    public ResponseEntity<?> getFulfilmentChannels() {
	        try {
	            String query = "SELECT * FROM get_distinct_fulfilment_channels()";
	            List<Map<String, Object>> result = postgresService.query(query);

	            List<String> channels = new ArrayList<>();
	            for (Map<String, Object> row : result) {
	                channels.add((String) row.get("fulfilment_channel"));
	            }

	            return ResponseEntity.ok(Map.of("channels", channels));

	        } catch (Exception e) {
	            e.printStackTrace();
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                    .body(Map.of("error", "Failed to fetch fulfilment channels", "details", e.getMessage()));
	        }
	    }
}
