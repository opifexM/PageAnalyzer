package hexlet.code;

import hexlet.code.domain.Url;
import hexlet.code.domain.query.QUrl;
import io.ebean.DB;
import io.ebean.Transaction;
import io.javalin.Javalin;
import kong.unirest.Empty;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class AppTest {
    public static final String MSG_SITE_ADDED_SUCCESSFULLY = "Site added successfully";
    public static final String MSG_INVALID_URL = "Invalid URL";
    public static final String MSG_SITE_ALREADY_EXISTS = "The site already exists";
    private static Javalin app;
    private static String baseUrl;
    private static Transaction transaction;

    @BeforeAll
    public static void beforeAll() {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;
    }

    @AfterAll
    public static void afterAll() {
        app.stop();
    }

    @BeforeEach
    void beforeEach() {
        transaction = DB.beginTransaction();
    }

    @AfterEach
    void afterEach() {
        transaction.rollback();
    }

    @Test
    void testRoot() {
        HttpResponse<String> response = Unirest.get(baseUrl).asString();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void testUrls() {
        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls")
                .asString();
        String content = response.getBody();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(content).contains("www.yahoo.com");
        assertThat(content).contains("www.rambler.com");
    }

    @Test
    void testUrl() {
        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls/2")
                .asString();
        String content = response.getBody();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(content).contains("www.yandex.com");
        assertThat(content).contains("06/02/2022");
    }

    @Test
    void testCreateUrl() {
        // формируем отправку данных на сайт
        String fullUrl = "https://game.com";
        HttpResponse<String> responsePost = Unirest
                .post(baseUrl + "/urls")
                .field("url", fullUrl)
                .asString();

        // проверяем возврат значения с сервера (302 - Found)
        assertThat(responsePost.getStatus()).isEqualTo(302);
        // перенаправление на список
        assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

        Url actualUrl = new QUrl()
                .name.equalTo(fullUrl)
                .findOne();
        // проверяем что fullUrl создался в базе данных
        assertThat(actualUrl).isNotNull();
        assertThat(actualUrl.getName()).isEqualTo(fullUrl);

        // проверка что вернулось сообщение о добавлении
        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls")
                .asString();
        String content = response.getBody();
        assertThat(content).contains(MSG_SITE_ADDED_SUCCESSFULLY);

        // проверка что новый сайт отображается в списке
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(content).contains(fullUrl);
    }

    @Test
    void testCreateUrlWithIncorrectName1() {
        String fullUrl = "game";
        HttpResponse<String> responsePost = Unirest
                .post(baseUrl + "/urls")
                .field("url", fullUrl)
                .asString();

        // перенаправление на главную страницу
        assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/");

        // проверка что вернулась ошибка
        HttpResponse<String> response = Unirest
                .get(baseUrl + "/")
                .asString();
        String content = response.getBody();
        assertThat(content).contains(MSG_INVALID_URL);
    }

    @Test
    void testCreateUrlWithIncorrectName2() {
        String fullUrl = "";
        HttpResponse<String> responsePost = Unirest
                .post(baseUrl + "/urls")
                .field("url", fullUrl)
                .asString();

        // перенаправление на главную страницу
        assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/");

        // проверка что вернулась ошибка
        HttpResponse<String> response = Unirest
                .get(baseUrl + "/")
                .asString();
        String content = response.getBody();
        assertThat(content).contains(MSG_INVALID_URL);
    }

    @Test
    void testCreateUrlWithIncorrectName3() {
        String fullUrl = "/";
        HttpResponse<String> responsePost = Unirest
                .post(baseUrl + "/urls")
                .field("url", fullUrl)
                .asString();

        // перенаправление на главную страницу
        assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/");

        // проверка что вернулась ошибка
        HttpResponse<String> response = Unirest
                .get(baseUrl + "/")
                .asString();
        String content = response.getBody();
        assertThat(content).contains(MSG_INVALID_URL);
    }

    @Test
    void testCreateExistUrl() {
        String fullUrl = "https://www.yahoo.com";
        HttpResponse<String> responsePost = Unirest
                .post(baseUrl + "/urls")
                .field("url", fullUrl)
                .asString();

        // перенаправление на главную страницу
        assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/");

        // проверка что вернулась ошибка
        HttpResponse<String> response = Unirest
                .get(baseUrl + "/")
                .asString();
        String content = response.getBody();
        assertThat(content).contains(MSG_SITE_ALREADY_EXISTS);
    }

    @Test
    void testCheckUrl() throws Exception {

        // Создаём инстанс `MockWebServer`.
        MockWebServer server = new MockWebServer();

        // Создаём инстанс `MockResponse`, и устанавливаем нужное тело ответа.
        // Это страница, а точнее её содержимое (html), с которой будет работать наше приложение в тестах
        String testBody = Files.readString(Paths.get("src/test/resources/fixtures/sample.html"));
        server.enqueue(new MockResponse().setBody(testBody));

        // Start the server.
        server.start();

        // указываем путь на который будет откликаться сервер "/" = "http://localhost:54595"
        HttpUrl mockSite = server.url("/");

        // запрос - создать сайт и подставляем путь мок сайта
        HttpResponse<String> response = Unirest
                .post(baseUrl + "/urls")
                .field("url", mockSite)
                .asString();

        // проверяем возврат значения с сервера (302 - Found)
        assertThat(response.getStatus()).isEqualTo(302);

        // запрос - проверить сайт
        // ответ не проверяем по этому он не нужен
        Unirest
                .post(baseUrl + "/urls/6/checks")
                .field("id", "6")
                .asEmpty();

        // получить информацию о проверке сайта
        response = Unirest
                .get(baseUrl + "/urls/6/")
                .asString();
        String content = response.getBody();

        // проверяем наличие данных из мок сайта
        // убираем лишний / из имени сайта
        assertThat(content).contains(mockSite.toString().substring(0, mockSite.toString().length() - 1));
        assertThat(content).contains("TEST_TITLE_PAGE");
        assertThat(content).contains("TEST_H1_TEXT");
        assertThat(content).contains("TEST_DESCRIPTION_TEXT");

        // Shut down the server. Instances cannot be reused.
        server.shutdown();
    }
}