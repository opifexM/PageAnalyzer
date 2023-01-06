package hexlet.code;

import hexlet.code.domain.Url;
import hexlet.code.domain.query.QUrl;
import io.ebean.DB;
import io.ebean.Transaction;
import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.*;

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
        assertThat(content).contains("2023-01-06");
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
        assertThat(content).contains("2022-02-06");
    }

    @Test
    void testCreateUrl() {
        // формируем отправку данных на сайт
        String fullUrl = "https://game.com";
        String url = "game.com";
        HttpResponse<String> responsePost = Unirest
                .post(baseUrl + "/urls")
                .field("url", fullUrl)
                .asString();

        // проверяем возврат значения с сервера (302 - Found)
        assertThat(responsePost.getStatus()).isEqualTo(302);
        // перенаправление на список
        assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

        Url actualUrl = new QUrl()
                .name.equalTo(url)
                .findOne();
        // проверяем что url создался в базе данных
        assertThat(actualUrl).isNotNull();
        assertThat(actualUrl.getName()).isEqualTo(url);

        // проверка что вернулось сообщение о добавлении
        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls")
                .asString();
        String content = response.getBody();
        assertThat(content).contains(MSG_SITE_ADDED_SUCCESSFULLY);

        // проверка что новый сайт отображается в списке
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(content).contains(url);
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
        String fullUrl = "http://www.yahoo.com";
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
}