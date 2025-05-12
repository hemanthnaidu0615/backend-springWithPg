package com.databin_pg.api.Controlller.Scheduler;

import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.databin_pg.api.DTO.ExcelUtil;
import com.databin_pg.api.Service.EmailService;
import com.databin_pg.api.Service.PostgresService;

@Component
public class SchedulerExecutor {

    @Autowired
    private PostgresService postgresService;

    @Autowired
    private EmailService emailService;

    @Scheduled(fixedRate = 60000) // Every 60 seconds
    public void runSchedulers() {
        String sql = "SELECT * FROM report_schedulers WHERE NOW() BETWEEN start_date AND end_date";
        List<Map<String, Object>> schedulers = postgresService.query(sql);

        for (Map<String, Object> scheduler : schedulers) {
            LocalDateTime lastExecuted = scheduler.get("last_executed") == null ? null :
                    ((Timestamp) scheduler.get("last_executed")).toInstant().atZone(ZoneOffset.UTC).toLocalDateTime();

            LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);

            if (shouldRun(scheduler.get("recurrence_pattern").toString(), lastExecuted, nowUtc)) {
                generateAndSendReport(scheduler, nowUtc);
                postgresService.update(
                        "UPDATE report_schedulers SET last_executed = NOW() WHERE id = ?",
                        List.of(scheduler.get("id"))
                );
            }
        }
    }

    private boolean shouldRun(String pattern, LocalDateTime lastExecuted, LocalDateTime now) {
        if (lastExecuted == null) return true;

        return switch (pattern.toLowerCase()) {
            case "daily" -> ChronoUnit.DAYS.between(lastExecuted, now) >= 1;
            case "weekly" -> ChronoUnit.WEEKS.between(lastExecuted, now) >= 1;
            case "monthly" -> ChronoUnit.MONTHS.between(lastExecuted, now) >= 1;
            case "yearly" -> ChronoUnit.YEARS.between(lastExecuted, now) >= 1;
            default -> false;
        };
    }

    private void generateAndSendReport(Map<String, Object> scheduler, LocalDateTime nowUtc) {
        try {
            String type = scheduler.get("type").toString();
            String email = scheduler.get("email").toString();
            String bcc = scheduler.get("bcc") != null ? scheduler.get("bcc").toString() : null;
            String timeZoneStr = scheduler.get("timezone") != null ? scheduler.get("timezone").toString() : "UTC";

            ZoneId userZone = ZoneId.of(timeZoneStr);
            ZonedDateTime nowUserTime = nowUtc.atZone(ZoneOffset.UTC).withZoneSameInstant(userZone);

            List<Map<String, Object>> data;

            if ("table_column".equals(type)) {
                String tableName = scheduler.get("table_name").toString();
                List<String> columns = Arrays.asList(scheduler.get("columns").toString().split(","));

                // Fetch mapped date column from backend
                Optional<String> dateColumnOpt = postgresService.getDateColumnForTable(tableName);
                if (dateColumnOpt.isEmpty()) {
                    System.err.println("No date column mapped for table: " + tableName);
                    return;
                }
                String dateColumn = dateColumnOpt.get();

                // Compute dynamic date range
                LocalDateTime[] dateRange = computeDateRange(
                        scheduler.get("recurrence_pattern").toString(),
                        scheduler.get("date_range_type").toString(),
                        nowUserTime.toLocalDateTime()
                );
                LocalDateTime userStart = dateRange[0];
                LocalDateTime userEnd = dateRange[1];

                // Convert back to UTC for querying
                ZonedDateTime utcStart = userStart.atZone(userZone).withZoneSameInstant(ZoneOffset.UTC);
                ZonedDateTime utcEnd = userEnd.atZone(userZone).withZoneSameInstant(ZoneOffset.UTC);

                String query = String.format(
                    "SELECT %s FROM %s WHERE %s BETWEEN '%s' AND '%s'",
                    String.join(",", columns), tableName, dateColumn,
                    Timestamp.from(utcStart.toInstant()), Timestamp.from(utcEnd.toInstant())
                );

                data = postgresService.query(query);

                // Convert timestamps in data to user's local time before sending
                convertTimestampsToUserTime(data, dateColumn, userZone);

            } else {
                // For custom_query type
                String query = scheduler.get("custom_query").toString();
                data = postgresService.query(query);
            }

            byte[] excelData = ExcelUtil.generateExcel(data);
            emailService.sendEmailWithAttachment(email, bcc, scheduler.get("title").toString(), excelData);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private LocalDateTime[] computeDateRange(String pattern, String rangeType, LocalDateTime now) {
        switch (pattern.toLowerCase()) {
            case "daily" -> {
                return switch (rangeType.toLowerCase()) {
                    case "today" -> new LocalDateTime[]{now.toLocalDate().atStartOfDay(), now};
                    case "past week" -> new LocalDateTime[]{now.minusDays(7), now};
                    case "past month" -> new LocalDateTime[]{now.minusMonths(1), now};
                    case "past 3 months" -> new LocalDateTime[]{now.minusMonths(3), now};
                    default -> throw new IllegalArgumentException("Invalid range type for daily");
                };
            }
            case "weekly" -> {
                return switch (rangeType.toLowerCase()) {
                    case "past week" -> new LocalDateTime[]{now.minusWeeks(1), now};
                    case "past month" -> new LocalDateTime[]{now.minusMonths(1), now};
                    case "past 3 months" -> new LocalDateTime[]{now.minusMonths(3), now};
                    case "past 6 months" -> new LocalDateTime[]{now.minusMonths(6), now};
                    default -> throw new IllegalArgumentException("Invalid range type for weekly");
                };
            }
            case "monthly" -> {
                return switch (rangeType.toLowerCase()) {
                    case "past month" -> new LocalDateTime[]{now.minusMonths(1), now};
                    case "past 3 months" -> new LocalDateTime[]{now.minusMonths(3), now};
                    case "past 6 months" -> new LocalDateTime[]{now.minusMonths(6), now};
                    case "past year" -> new LocalDateTime[]{now.minusYears(1), now};
                    default -> throw new IllegalArgumentException("Invalid range type for monthly");
                };
            }
            case "yearly" -> {
                return switch (rangeType.toLowerCase()) {
                    case "past year" -> new LocalDateTime[]{now.minusYears(1), now};
                    case "past 2 years" -> new LocalDateTime[]{now.minusYears(2), now};
                    case "past 5 years" -> new LocalDateTime[]{now.minusYears(5), now};
                    default -> throw new IllegalArgumentException("Invalid range type for yearly");
                };
            }
            default -> throw new IllegalArgumentException("Invalid recurrence pattern");
        }
    }

    private void convertTimestampsToUserTime(List<Map<String, Object>> data, String dateColumn, ZoneId userZone) {
        for (Map<String, Object> row : data) {
            Object value = row.get(dateColumn);
            if (value instanceof Timestamp ts) {
                Instant utcInstant = ts.toInstant();
                LocalDateTime local = utcInstant.atZone(userZone).toLocalDateTime();
                row.put(dateColumn, local.toString()); // You can format differently if needed
            }
        }
    }
}
