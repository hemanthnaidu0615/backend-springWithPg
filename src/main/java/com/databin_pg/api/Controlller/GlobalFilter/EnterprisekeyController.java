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
import org.springframework.web.bind.annotation.*;

import com.databin_pg.api.Service.PostgresService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/global-filter")
@CrossOrigin(origins = "*")
@Tag(name = "Global filters - Enterprise Key", description = "APIs for retrieving global filter options like enterprise keys")
public class EnterprisekeyController {

    @Autowired
    private PostgresService postgresService;

    @Operation(
        summary = "Get Enterprise Keys",
        description = "Fetches a list of unique enterprise keys from the database for global filtering."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved enterprise keys"),
        @ApiResponse(responseCode = "500", description = "Failed to fetch enterprise keys")
    })
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
