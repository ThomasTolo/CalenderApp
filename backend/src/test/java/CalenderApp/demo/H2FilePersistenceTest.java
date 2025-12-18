package CalenderApp.demo;

import CalenderApp.demo.model.AppUser;
import CalenderApp.demo.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class H2FilePersistenceTest {

    @Test
    void fileBackedH2PersistsUsersAcrossRestart() throws Exception {
        Path dir = Files.createTempDirectory("calenderapp-h2-");
        Path dbFile = dir.resolve("calender");

        String url = "jdbc:h2:file:" + dbFile.toAbsolutePath() + ";MODE=PostgreSQL";
        String username = "persist_test_" + System.currentTimeMillis();

        try (ConfigurableApplicationContext ctx1 = new SpringApplicationBuilder(CalenderAppApplication.class)
                .properties(
                        "server.port=0",
                        "spring.task.scheduling.enabled=false",
                        "spring.h2.console.enabled=false",
                        "spring.datasource.url=" + url,
                        "spring.datasource.driverClassName=org.h2.Driver",
                        "spring.datasource.username=sa",
                        "spring.datasource.password=",
                        "spring.jpa.hibernate.ddl-auto=update"
                )
                .run()) {
            AppUserRepository repo = ctx1.getBean(AppUserRepository.class);
            repo.save(new AppUser(username, "hash"));
        }

        try (ConfigurableApplicationContext ctx2 = new SpringApplicationBuilder(CalenderAppApplication.class)
                .properties(
                        "server.port=0",
                        "spring.task.scheduling.enabled=false",
                        "spring.h2.console.enabled=false",
                        "spring.datasource.url=" + url,
                        "spring.datasource.driverClassName=org.h2.Driver",
                        "spring.datasource.username=sa",
                        "spring.datasource.password=",
                        "spring.jpa.hibernate.ddl-auto=update"
                )
                .run()) {
            AppUserRepository repo = ctx2.getBean(AppUserRepository.class);
            assertTrue(repo.findByUsername(username).isPresent(), "Expected user to persist in file-backed H2 database");
        }
    }
}
