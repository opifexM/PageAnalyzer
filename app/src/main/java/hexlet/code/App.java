package hexlet.code;

import hexlet.code.controllers.UrlController;
import io.javalin.Javalin;
import io.javalin.plugin.rendering.template.JavalinThymeleaf;
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;

public class App {
    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    private static int getPort() {
        String port = System.getenv().getOrDefault("PORT", "5000");
        LOG.info("Get port: {}", port);
        return Integer.parseInt(port);
    }

    public static void main(String[] args) {
        Javalin app = getApp();
        app.start(getPort());
    }

    public static Javalin getApp() {
        Javalin app = Javalin.create(config -> {
            config.enableDevLogging();
            JavalinThymeleaf.configure(getTemplateEngine());
            LOG.info("JavalinThymeleaf configured.");
        });
        LOG.info("Javalin app created.");

        addRoutes(app);
        LOG.info("Javalin routes created.");

        app.before(ctx -> ctx.attribute("ctx", ctx));
        return app;
    }

    private static TemplateEngine getTemplateEngine() {
        TemplateEngine templateEngine = new TemplateEngine();

        templateEngine.addDialect(new LayoutDialect());
        templateEngine.addDialect(new Java8TimeDialect());

        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("/templates/");

        templateEngine.addTemplateResolver(templateResolver);

        return templateEngine;
    }

    private static void addRoutes(Javalin app) {
        app.get("/", UrlController.NEW_URL);
        app.routes(() -> path("urls", () -> {
            get(UrlController.LIST_URLS);
            post(UrlController.CREATE_URL);

            path("{id}", () -> {
                get(UrlController.SHOW_URL);
                post("checks", UrlController.CHECK_URL);
            });
        }));
    }
}
