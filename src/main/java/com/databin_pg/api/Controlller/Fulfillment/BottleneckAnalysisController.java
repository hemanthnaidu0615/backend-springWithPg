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
@Tag(name = "Fulfillment - Bottleneck Analysis", description = "Analyze operational bottlenecks in the fulfillment process")
public class BottleneckAnalysisController {

    @Autowired
    private PostgresService postgresService;

    @Operation(
            summary = "Get Bottleneck Analysis",
            description = "Retrieves bottleneck insights from the fulfillment pipeline for a given date range. Optionally filtered by enterprise key ('AWW' or 'AWD')."
        )
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bottleneck analysis data fetched successfully"),
            @ApiResponse(responseCode = "500", description = "Failed to fetch bottleneck analysis data")
        })
    @GetMapping("/bottleneck-analysis")
    public ResponseEntity<?> getBottleneckAnalysis(
    		@Parameter(description = "Start date in YYYY-MM-DD format", required = true)
            @RequestParam(name = "startDate") String startDate,

            @Parameter(description = "End date in YYYY-MM-DD format", required = true)
            @RequestParam(name = "endDate") String endDate,

            @Parameter(description = "Optional enterprise key for filtering results 'AWW' or 'AWD'")
            @RequestParam(name = "enterpriseKey", required = false) String enterpriseKey) {

        try {
            // Correctly format the enterpriseKey, if not provided, use NULL
            String formattedKey = (enterpriseKey == null || enterpriseKey.isBlank()) ? "NULL" : "'" + enterpriseKey + "'";

            // Ensure start and end date are in the correct format (YYYY-MM-DD)
            String formattedStartDate = startDate.split("T")[0];  // Only take the date part
            String formattedEndDate = endDate.split("T")[0];      // Only take the date part

            // The query that will call the stored procedure
            String query = String.format("""
                SELECT * FROM get_bottleneck_analysis('%s', '%s', %s)
            """, formattedStartDate, formattedEndDate, formattedKey);

            List<Map<String, Object>> data = postgresService.query(query);

            return ResponseEntity.ok(data);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch bottleneck analysis", "details", e.getMessage()));
        }
    }
}
