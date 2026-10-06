package com.example.cv;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.example.cv.person.PersonService;
import com.example.cv.cv.CvRequest;
import com.example.cv.cv.CvService;
import com.example.cv.cv.CvStatus;

import java.util.List;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "app.frontend.url=http://localhost:5173/",
        "app.cors.allowed-origins=http://localhost:5173",
        "storage.type=local",
        "springdoc.api-docs.enabled=true",
        "springdoc.swagger-ui.enabled=true"
    })
@ActiveProfiles("prod")
@Transactional
class PostgresIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", postgres::getJdbcUrl);
        properties.add("spring.datasource.username", postgres::getUsername);
        properties.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private PersonService personService;

    @Autowired
    private CvService cvService;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void appliesMigrationsAndPersistsPostgresRecords() {
        UUID personId = UUID.randomUUID();
        jdbcTemplate.update("insert into person (id, first_name, last_name) values (?, ?, ?)",
                personId, "Ada", "Lovelace");

        String lastName = jdbcTemplate.queryForObject(
                "select last_name from person where id = ?", String.class, personId);

        assertThat(lastName).isEqualTo("Lovelace");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = true", Integer.class)).isPositive();
        assertThat(jdbcTemplate.queryForObject("""
            select count(*) from pg_indexes
            where schemaname = current_schema()
              and indexname in ('idx_person_owner_name', 'idx_cv_person_updated_at')
            """, Integer.class)).isEqualTo(2);
        }

        @Test
        void persistsAndReturnsCvTags() {
        UUID personId = UUID.randomUUID();
        jdbcTemplate.update("insert into person (id, first_name, last_name) values (?, ?, ?)",
            personId, "Ada", "Lovelace");

        var created = cvService.create(personId,
            new CvRequest("Backend", null, "en", CvStatus.DRAFT, List.of("platform", "backend")));
        entityManager.flush();
        entityManager.clear();

        assertThat(cvService.findById(created.id()).tags()).containsExactly("backend", "platform");
        assertThat(jdbcTemplate.queryForList("select tag from cv_tag where cv_id = ? order by tag", String.class,
            created.id())).containsExactly("backend", "platform");
        }

        @Test
        void loadsPersonContactsInOneQueryForAnOwnedPeopleList() {
        UUID ownerId = UUID.randomUUID();
        jdbcTemplate.update("insert into app_user (id, issuer, subject) values (?, ?, ?)",
            ownerId, "https://issuer.example", "n-plus-one-" + ownerId);
        for (int index = 0; index < 3; index++) {
            UUID personId = UUID.randomUUID();
            jdbcTemplate.update("insert into person (id, first_name, last_name, owner_id) values (?, ?, ?, ?)",
                personId, "Person", "" + index, ownerId);
            jdbcTemplate.update("insert into person_contact (id, person_id, type, value, is_primary, sort_order) "
                    + "values (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), personId, "EMAIL", "person" + index + "@example.com", true, 0);
        }

        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        var people = personService.findAll(ownerId);

        assertThat(people).hasSize(3);
        assertThat(people).allSatisfy(person -> assertThat(person.contacts()).hasSize(1));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);

        statistics.clear();
        var allPeople = personService.findAll();

        assertThat(allPeople).hasSize(3);
        assertThat(allPeople).allSatisfy(person -> assertThat(person.contacts()).hasSize(1));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void deletesImportedCvAndPreservesImportHistory() {
        UUID personId = UUID.randomUUID();
        UUID importId = UUID.randomUUID();
        jdbcTemplate.update("insert into person (id, first_name, last_name) values (?, ?, ?)",
            personId, "Ada", "Lovelace");
        var created = cvService.create(personId,
            new CvRequest("Imported", null, "en", CvStatus.DRAFT, null));
        entityManager.flush();
        jdbcTemplate.update("""
            insert into cv_document_import
                (id, file_name, media_type, file_size, status, result_json, cv_id, created_at, updated_at)
            values (?, 'cv.pdf', 'application/pdf', 100, 'APPROVED', '{}', ?, now(), now())
            """, importId, created.id());

        cvService.delete(created.id());
        entityManager.flush();
        entityManager.clear();

        assertThat(jdbcTemplate.queryForObject("select count(*) from cv where id = ?", Integer.class,
            created.id())).isZero();
        var history = jdbcTemplate.queryForMap(
            "select cv_id, status, result_json from cv_document_import where id = ?", importId);
        assertThat(history.get("cv_id")).isNull();
        assertThat(history.get("status")).isEqualTo("APPROVED");
        assertThat(history.get("result_json")).isEqualTo("{}");
    }

    @Test
    void deletesPersonWithImportedCvAndPreservesImportHistory() {
        UUID personId = UUID.randomUUID();
        UUID importId = UUID.randomUUID();
        jdbcTemplate.update("insert into person (id, first_name, last_name) values (?, ?, ?)",
            personId, "Ada", "Lovelace");
        var created = cvService.create(personId,
            new CvRequest("Imported", null, "en", CvStatus.DRAFT, null));
        entityManager.flush();
        jdbcTemplate.update("""
            insert into cv_document_import
                (id, file_name, media_type, file_size, status, cv_id, created_at, updated_at)
            values (?, 'cv.pdf', 'application/pdf', 100, 'APPROVED', ?, now(), now())
            """, importId, created.id());
        entityManager.clear();

        personService.delete(personId);
        entityManager.flush();
        entityManager.clear();

        assertThat(jdbcTemplate.queryForObject("select count(*) from person where id = ?", Integer.class,
            personId)).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from cv where id = ?", Integer.class,
            created.id())).isZero();
        assertThat(jdbcTemplate.queryForMap("select cv_id from cv_document_import where id = ?", importId))
            .containsEntry("cv_id", null);
    }

    @Test
    void exposesStatusAndProtectsPersonApiOverHttp() {
        var status = restTemplate.getForEntity("/api/v1/status", String.class);
        var persons = restTemplate.getForEntity("/api/v1/persons", String.class);
        var readiness = restTemplate.getForEntity("/actuator/health/readiness", String.class);
        var liveness = restTemplate.getForEntity("/actuator/health/liveness", String.class);
        var metrics = restTemplate.getForEntity("/actuator/prometheus", String.class);
        var apiDocs = restTemplate.getForEntity("/v3/api-docs", String.class);

        assertThat(status.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(status.getBody()).contains("\"status\":\"UP\"");
        assertThat(persons.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(readiness.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(liveness.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(metrics.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(metrics.getBody()).contains("jvm_memory_used_bytes");
        assertThat(apiDocs.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(apiDocs.getBody()).contains("CV Management API");
    }
}