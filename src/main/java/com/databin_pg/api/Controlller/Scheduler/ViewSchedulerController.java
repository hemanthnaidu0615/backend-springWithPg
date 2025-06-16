package com.databin_pg.api.Controlller.Scheduler;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedulers/view")
@CrossOrigin(origins = "*")
public class ViewSchedulerController {

    @Autowired
    private PostgresService postgresService;

    @GetMapping
    public ResponseEntity<?> getSchedulerSummary(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        try {
            int offset = page * size;

            // Count query
            String countQuery = "SELECT COUNT(*) AS total FROM get_scheduler_summary()";
            List<Map<String, Object>> countResult = postgresService.query(countQuery);

            int totalCount = (!countResult.isEmpty() && countResult.get(0).get("total") != null)
                    ? ((Number) countResult.get(0).get("total")).intValue()
                    : 0;

            // Paginated query
            String dataQuery = String.format("""
                SELECT * FROM get_scheduler_summary()
                OFFSET %d LIMIT %d
            """, offset, size);

            List<Map<String, Object>> result = postgresService.query(dataQuery);

            return ResponseEntity.ok(Map.of(
                    "data", result,
                    "page", page,
                    "size", size,
                    "count", totalCount
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to fetch scheduler data",
                            "details", e.getMessage()
                    ));
        }
    }
}
