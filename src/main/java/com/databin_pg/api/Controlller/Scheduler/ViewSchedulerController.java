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
@CrossOrigin(origins = "http://localhost:5173")
public class ViewSchedulerController {

    @Autowired
    private PostgresService postgresService;

    @GetMapping
    public ResponseEntity<?> getSchedulerSummary() {
        try {
            String query = "SELECT * FROM get_scheduler_summary()";
            List<Map<String, Object>> result = postgresService.query(query);

            if (result.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body(Map.of("message", "No scheduler data found."));
            }

            return ResponseEntity.ok(result);

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
