package hexlet.code;

import io.ebean.DB;
import io.ebean.SqlRow;
import io.ebean.Transaction;
import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class AppHexletTest {

    private static Javalin app;
    private static String baseUrl;
    private static SqlRow existingUrl;
    private static SqlRow existingUrlCheck;
    private static Transaction transaction;
    private static MockWebServer mockServer;

    private static Path getFixturePath(String fileName) {
        return Paths.get("src", "test", "resources", "fixtures", fileName)
            .toAbsolutePath().normalize();
    }

    private static String readFixture(String fileName) throws IOException {
        Path filePath = getFixturePath(fileName);
        return Files.readString(filePath).trim();
    }

    @BeforeAll
    public static void beforeAll() throws IOException {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;

        String url = "https://en.hexlet.io";
        String createUrl = String.format(
            "INSERT INTO url (name, created_at) VALUES ('%s', '2021-09-27 14:20:19.13');",
            url
        );
        DB.sqlUpdate(createUrl).execute();

        String selectUrl = String.format("SELECT * FROM url WHERE name = '%s';", url);
        existingUrl = DB.sqlQuery(selectUrl).findOne();

        String createUrlCheck = String.format(
            "INSERT INTO url_check (url_id, status_code, title, description, h1, created_at)"
            + "VALUES (%s, 200, 'en title', 'en description', 'en h1', '2021-09-27 14:20:19.13');",
            existingUrl.getString("id")
        );
        DB.sqlUpdate(createUrlCheck).execute();

        String selectUrlCheck = String.format(
            "SELECT * FROM url_check WHERE url_id = '%s';",
            existingUrl.getString("id")
        );
        existingUrlCheck = DB.sqlQuery(selectUrlCheck).findOne();

        mockServer = new MockWebServer();
        MockResponse mockedResponse = new MockResponse()
            .setBody(readFixture("index.html"));
        mockServer.enqueue(mockedResponse);
        mockServer.start();
    }

    @AfterAll
    public static void afterAll() throws IOException {
        app.stop();
        mockServer.shutdown();
    }

    @BeforeEach
    void beforeEach() {
        transaction = DB.beginTransaction();
    }

    @AfterEach
    void afterEach() {
        transaction.rollback();
    }

    @Nested
    class RootTest {

        @Test
        void testIndex() {
            HttpResponse<String> response = Unirest.get(baseUrl).asString();
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getBody()).contains("Анализатор страниц");
        }
    }

    @Nested
    class UrlTest {

        @Test
        void testIndex() {
            HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls")
                .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains(existingUrl.getString("name"));
            assertThat(body).contains(existingUrlCheck.getString("status_code"));
        }

        @Test
        void testShow() {
            HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls/" + existingUrl.getString("id"))
                .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains(existingUrl.getString("name"));
            assertThat(body).contains(existingUrlCheck.getString("status_code"));
        }

        @Test
        void testStore() {
            String inputUrl = "https://ru.hexlet.io";
            HttpResponse responsePost = Unirest
                .post(baseUrl + "/urls")
                .field("url", inputUrl)
                .asEmpty();

            assertThat(responsePost.getStatus()).isEqualTo(302);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

            HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls")
                .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains(inputUrl);
            assertThat(body).contains("Страница успешно добавлена");

            String selectUrl = String.format("SELECT * FROM url WHERE name = '%s';", inputUrl);
            SqlRow actualUrl = DB.sqlQuery(selectUrl).findOne();

            assertThat(actualUrl).isNotNull();
            assertThat(actualUrl.getString("name")).isEqualTo(inputUrl);
        }
    }

    @Nested
    class UrlCheckTest {

        @Test
        void testStore() {
            String url = mockServer.url("/").toString().replaceAll("/$", "");

            Unirest.post(baseUrl + "/urls")
                .field("url", url)
                .asEmpty();

            String selectUrl = String.format("SELECT * FROM url WHERE name = '%s';", url);
            SqlRow actualUrl = DB.sqlQuery(selectUrl).findOne();

            assertThat(actualUrl).isNotNull();
            assertThat(actualUrl.getString("name")).isEqualTo(url);

            Unirest.post(baseUrl + "/urls/" + actualUrl.getString("id") + "/checks")
                .asEmpty();

            HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls/" + actualUrl.getString("id"))
                .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains("Страница успешно проверена");

            String selectUrlCheck = String.format(
                "SELECT * FROM url_check WHERE url_id = '%s' ORDER BY created_at DESC;",
                actualUrl.getString("id")
            );
            SqlRow actualCheckUrl = DB.sqlQuery(selectUrlCheck).findOne();

            assertThat(actualCheckUrl).isNotNull();
            assertThat(actualCheckUrl.getString("status_code")).isEqualTo("200");
            assertThat(actualCheckUrl.getString("title")).isEqualTo("Test page");
            assertThat(actualCheckUrl.getString("h1")).isEqualTo("Do not expect a miracle, miracles yourself!");
            assertThat(actualCheckUrl.getString("description")).contains("statements of great people");
        }
    }
}
