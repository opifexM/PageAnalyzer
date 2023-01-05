package hexlet.code.controllers;

import hexlet.code.domain.Url;
import hexlet.code.domain.query.QUrl;
import io.javalin.http.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static java.util.Objects.isNull;


public final class UrlController {
    private static final Logger log = LoggerFactory.getLogger(UrlController.class);

    public static Handler newUrl = ctx -> {
        log.info("Main page loading.");
        ctx.render("urls/new.html");
    };

    public static Handler createUrl = ctx -> {
        URL inputUrl;
        try {
            inputUrl = new URL(ctx.formParam("url"));
            log.info("Url '{}' input.", inputUrl);
        } catch (MalformedURLException e) {
            log.error("Input Url is invalid.");
            ctx.sessionAttribute("flash", "Invalid URL");
            ctx.redirect("/");
            return;
        }

        String siteName;
        String host = inputUrl.getHost();
        int port = inputUrl.getPort();
        if (port > 0) {
            siteName = host + ":" + port;
        } else {
            siteName = host;
        }

        log.info("Checking Url '{}'", siteName);
        Url checkUrl = new QUrl().name.equalTo(siteName).findOne();
        if (!isNull(checkUrl)) {
            log.error("Input Url '{}' already exists.", siteName);
            ctx.sessionAttribute("flash", "The site already exists");
            ctx.redirect("/");
            return;
        }

        log.info("Create Url '{}'.", siteName);
        Url siteUrl = new Url(siteName);
        log.info("Save Url object '{}' to DB", siteUrl);
        siteUrl.save();
        log.info("Url object '{}' saved.", siteUrl);

        ctx.sessionAttribute("flash", "Site added successfully");
        ctx.redirect("/urls");
    };

    public static Handler listUrls = ctx -> {

        log.info("Get list of urls fro DB.");
        List<Url> urls = new QUrl().orderBy().id.asc().findList();

        ctx.attribute("urls", urls);
        ctx.render("urls/list.html");
    };

    public static Handler showUrl = ctx -> {
        long id = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);

        log.info("Get one url from DB by id '{}'", id);
        Url url = new QUrl().id.equalTo(id).findOne();

        ctx.attribute("url", url);
        ctx.render("urls/show.html");
    };
}
