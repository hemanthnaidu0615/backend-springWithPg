package com.databin_pg.api.DTO;

import java.time.LocalDateTime;
import java.util.List;

public class TableSchedulerRequest {
    public String title;
    public String description;
    public LocalDateTime startDate;
    public LocalDateTime endDate;
    public String email;
    public String bcc;
    public String recurrencePattern;
    public String tableName;
    public List<String> columns;
    public String dateColumn;
    public String timezone;
    public String dateRangeType; // e.g., "past week", "past month"
}
