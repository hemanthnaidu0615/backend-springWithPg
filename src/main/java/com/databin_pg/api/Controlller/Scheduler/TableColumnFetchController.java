package com.databin_pg.api.Controlller.Scheduler;

import com.databin_pg.api.Service.PostgresService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/table-column-fetch")
@CrossOrigin(origins = "http://localhost:5173")
public class TableColumnFetchController {

    @Autowired
    private PostgresService postgresService;

    // 📌 API: Get all table names in the public schema
    @GetMapping("/tables")
    public ResponseEntity<?> getAllTables() {
        try {
            String query = """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                AND table_type = 'BASE TABLE'
            """;

            List<Map<String, Object>> data = postgresService.query(query);
            List<String> tableNames = data.stream()
                    .map(row -> (String) row.get("table_name"))
                    .toList();

            return ResponseEntity.ok(Map.of("tables", tableNames));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch table names"));
        }
    }

    // 📌 API: Get column names for a given table
    @GetMapping("/columns")
    public ResponseEntity<?> getTableColumns(@RequestParam String tableName) {
        try {
            String query = String.format("""
                SELECT column_name
                FROM information_schema.columns
                WHERE table_name = '%s'
                ORDER BY ordinal_position
            """, tableName);

            List<Map<String, Object>> data = postgresService.query(query);
            List<String> columnNames = data.stream()
                    .map(row -> (String) row.get("column_name"))
                    .toList();

            return ResponseEntity.ok(Map.of("columns", columnNames));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch columns for table: " + tableName));
        }
    }
}
