package com.databin_pg.api.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.databin_pg.api.DTO.QuerySchedulerRequest;
import com.databin_pg.api.DTO.TableSchedulerRequest;

@Service
public class SchedulerService {

    @Autowired
    private PostgresService postgresService;

    public void saveTableScheduler(TableSchedulerRequest request) {
        // ⛔ Removed `date_column` from the SQL
        String sql = """
            INSERT INTO report_schedulers 
            (title, description, start_date, end_date, email, bcc, recurrence_pattern, type, table_name, columns, date_range_type)
            VALUES (?, ?, ?, ?, ?, ?, ?, 'table_column', ?, ?, ?)
        """;

        List<String> columns = (request.columns != null && !request.columns.isEmpty()) 
            ? request.columns 
            : Collections.emptyList();

        String columnsStr = String.join(",", columns); 

        postgresService.update(sql, List.of(
            Objects.toString(request.title, ""),
            Objects.toString(request.description, ""),
            request.startDate,   // assumed to be LocalDateTime
            request.endDate,     // assumed to be LocalDateTime
            Objects.toString(request.email, ""),
            Objects.toString(request.bcc, ""),
            Objects.toString(request.recurrencePattern, ""),
            Objects.toString(request.tableName, ""),
            columnsStr,
            Objects.toString(request.dateRangeType, "")
        ));
    }

    public void saveQueryScheduler(QuerySchedulerRequest request) {
        String sql = """
            INSERT INTO report_schedulers 
            (title, description, start_date, end_date, email, bcc, recurrence_pattern, type, custom_query)
            VALUES (?, ?, ?, ?, ?, ?, ?, 'query', ?)
        """; 

        // Safeguard against null fields
        String title = Objects.toString(request.title, "");
        String description = Objects.toString(request.description, "");
        String startDateStr = Objects.toString(request.startDate, "");
        String endDateStr = Objects.toString(request.endDate, "");
        String email = Objects.toString(request.email, "");
        String bcc = Objects.toString(request.bcc, "");
        String recurrencePattern = Objects.toString(request.recurrencePattern, "");
        String customQuery = Objects.toString(request.customQuery, "");

        // Use ISO_LOCAL_DATE_TIME to parse the date-time string correctly
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime startDateTime = LocalDateTime.parse(startDateStr, formatter);
        LocalDateTime endDateTime = LocalDateTime.parse(endDateStr, formatter);

        // Convert LocalDateTime to Timestamp
        Timestamp startDate = Timestamp.valueOf(startDateTime);
        Timestamp endDate = Timestamp.valueOf(endDateTime);

        postgresService.update(sql, List.of(
            title,
            description,
            startDate,
            endDate,
            email,
            bcc,
            recurrencePattern,
            customQuery
        ));
    }
}
