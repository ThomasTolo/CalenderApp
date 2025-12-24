package CalenderApp.demo.config.db;

import CalenderApp.demo.model.CalendarItemType;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class PostgresCalendarItemTypeConstraintFixer implements ApplicationRunner {
    private final DataSource dataSource;
    private final JdbcTemplate jdbc;

    public PostgresCalendarItemTypeConstraintFixer(DataSource dataSource, JdbcTemplate jdbc) {
        this.dataSource = dataSource;
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isPostgres()) return;

        // Hibernate won't reliably update CHECK constraints created for enums.
        // If the DB was created before FIXED_COST/OTHER existed, inserts will 500.
        String allowed = Arrays.stream(CalendarItemType.values())
                .map(Enum::name)
                .map(v -> "'" + v + "'")
                .collect(Collectors.joining(","));

        try {
            jdbc.execute("ALTER TABLE calendar_items DROP CONSTRAINT IF EXISTS calendar_items_type_check");
            jdbc.execute("ALTER TABLE calendar_items ADD CONSTRAINT calendar_items_type_check CHECK (type IN (" + allowed + "))");
        } catch (Exception ignored) {
            // Best-effort: don't block app startup if schema differs.
        }
    }

    private boolean isPostgres() {
        try (Connection c = dataSource.getConnection()) {
            String name = c.getMetaData().getDatabaseProductName();
            return name != null && name.toLowerCase(Locale.ROOT).contains("postgres");
        } catch (Exception e) {
            return false;
        }
    }
}
