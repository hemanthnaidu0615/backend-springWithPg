package com.databin_pg.api.Controlller.Fulfillment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.databin_pg.api.Service.PostgresService;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/fulfillment")
@CrossOrigin(origins = "*")
@Tag(name = "Fulfillment - Stages Pipeline", description = "APIs related to fulfillment stages pipeline data")
public class FulfillmentStagesPipelineController {

    @Autowired
    private PostgresService postgresService;

    @Operation(
        summary = "Get Fulfillment Stage Pipeline",
        description = "Fetches fulfillment stages pipeline data between specified dates, optionally filtered by enterprise key."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Fulfillment stage pipeline data retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Failed to fetch stage pipeline data")
    })
    @GetMapping("/stages-pipeline")
    public ResponseEntity<?> getFulfillmentStagesPipeline(
            @Parameter(description = "Start date in YYYY-MM-DD format", required = true)
            @RequestParam(name = "startDate") String startDate,

            @Parameter(description = "End date in YYYY-MM-DD format", required = true)
            @RequestParam(name = "endDate") String endDate,

            @Parameter(description = "Optional enterprise key to filter data")
            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey) {

        try {
            String formattedKey = (enterpriseKey == null || enterpriseKey.isBlank()) ? "NULL" : "'" + enterpriseKey + "'";
            String query = String.format("""
                SELECT * FROM get_fulfillment_stages_pipeline('%s', '%s', %s)
            """, startDate, endDate, formattedKey);

            List<Map<String, Object>> data = postgresService.query(query);

            return ResponseEntity.ok(data);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch stage pipeline", "details", e.getMessage()));
        }
    }
}
