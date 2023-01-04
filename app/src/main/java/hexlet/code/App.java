package hexlet.code;

import hexlet.code.controllers.RootController;
import io.javalin.Javalin;


import io.javalin.plugin.rendering.template.JavalinThymeleaf;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;

import static io.javalin.apibuilder.ApiBuilder.*;
import static io.javalin.apibuilder.ApiBuilder.get;

public class App {
    private static int getPort() {
        String port = System.getenv().getOrDefault("PORT", "5000");
        return Integer.valueOf(port);
    }

    public static void main(String[] args) {
        Javalin app = getApp();
        app.start(getPort());
    }

    public static Javalin getApp() {

//        Javalin app = Javalin.create(/*config*/)
//                .get("/", ctx -> ctx.result("Hello World"))
//                .start(7070);

//        Javalin app = Javalin.create(config -> {
////            JavalinThymeleaf.init();
//            config.plugins.enableDevLogging();
//
//            JavalinThymeleaf.init(getTemplateEngine());
//            //config.plugins.register(new DevLoggingPlugin());
//        });

        Javalin app = Javalin.create(config -> {
            // Включаем логгирование
            config.enableDevLogging();
            // config.enableWebjars();
            // Подключаем настроенный шаблонизатор к фреймворку
            JavalinThymeleaf.configure(getTemplateEngine());
        });

        addRoutes(app);

        app.before(ctx -> {
            ctx.attribute("ctx", ctx);
        });

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
        app.get("/", RootController.welcome);

//        app.routes(() -> {
//            path("users", () -> {
//                get(UserController.listUsers);
//                post(UserController.createUser);
//                get("new", UserController.newUser);
//                get("{id}", UserController.showUser);
//            });
//        });
    }
}
