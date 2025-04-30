package com.databin_pg.api.Controlller.GlobalFilter;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.databin_pg.api.Service.PostgresService;

@RestController
@RequestMapping("/api/global-filter")
@CrossOrigin(origins = "http://localhost:5173")
public class EnterprisekeyController {
	
	@Autowired
    private PostgresService postgresService;
	
	@GetMapping("/enterprise-keys")
    public ResponseEntity<?> getEnterpriseKeys() {
        try {
            String query = "SELECT * FROM get_unique_enterprise_keys()";

            List<Map<String, Object>> results = postgresService.query(query);

            Set<String> enterpriseKeys = new HashSet<>();
            for (Map<String, Object> row : results) {
                if (row.get("enterprise_key") != null) {
                    enterpriseKeys.add(row.get("enterprise_key").toString());
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("enterprise_keys", enterpriseKeys);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to fetch enterprise keys"));
        }
    }
}
