package hexlet.code;

import hexlet.code.controllers.UrlController;
import io.javalin.Javalin;


import io.javalin.plugin.rendering.template.JavalinThymeleaf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;

import static io.javalin.apibuilder.ApiBuilder.*;

public class App {
    private static final Logger log = LoggerFactory.getLogger(App.class);

    private static int getPort() {
        String port = System.getenv().getOrDefault("PORT", "5000");
        log.info("Get port: {}", port);
        return Integer.valueOf(port);
    }

    public static void main(String[] args) {
        Javalin app = getApp();
        app.start(getPort());
    }

    public static Javalin getApp() {
        Javalin app = Javalin.create(config -> {
            // Включаем логгирование
            config.enableDevLogging();
            // config.enableWebjars();
            // Подключаем настроенный шаблонизатор к фреймворку
            JavalinThymeleaf.configure(getTemplateEngine());
            log.info("JavalinThymeleaf configured.");
        });
        log.info("Javalin app created.");

        addRoutes(app);
        log.info("Javalin routes created.");

        app.before(ctx -> ctx.attribute("ctx", ctx));

        return app;
    }

    private static TemplateEngine getTemplateEngine() {
        // Создаём инстанс движка шаблонизатора
        TemplateEngine templateEngine = new TemplateEngine();

        // Добавляем к нему диалекты
        templateEngine.addDialect(new LayoutDialect());
        templateEngine.addDialect(new Java8TimeDialect());

        // Настраиваем преобразователь шаблонов, так, чтобы обрабатывались
        // шаблоны в директории /templates/
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("/templates/");

        // Добавляем преобразователь шаблонов к движку шаблонизатора
        templateEngine.addTemplateResolver(templateResolver);

        return templateEngine;
    }

    private static void addRoutes(Javalin app) {

        // GET: /
        app.get("/", UrlController.newUrl);
        app.routes(() -> {
            // path: /urls
            path("urls", () -> {
                // GET: /urls
                get(UrlController.listUrls);

                // POST: /urls
                post(UrlController.createUrl);

                // path: /urls/{id}
                path("{id}", () -> {

                    // GET: /urls/{id}
                    get(UrlController.showUrl);

                    // POST: /urls/{id}/checks
                    post("checks", UrlController.checkUrl);
                });
            });
        });
    }
}
