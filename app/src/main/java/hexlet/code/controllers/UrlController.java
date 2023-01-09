package hexlet.code.controllers;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import io.ebean.PagedList;
import io.javalin.http.Handler;
import kong.unirest.UnirestException;
import org.eclipse.jetty.util.ajax.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

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
        String protocol = inputUrl.getProtocol();
        String host = inputUrl.getHost();
        int port = inputUrl.getPort();
        if (port > 0) {
            siteName = protocol + "://" + host + ":" + port;
        } else {
            siteName = protocol + "://" + host;
        }

        log.info("Checking Url '{}'", siteName);
        Url checkUrl = new QUrl().name.equalTo(siteName).findOne();
        if (!isNull(checkUrl)) {
            log.error("Input Url '{}' already exists.", siteName);
            ctx.sessionAttribute("flash", "The site already exists");
            log.info("Redirect to '/'");
            ctx.redirect("/");
            return;
        }

        log.info("Create Url '{}'.", siteName);
        Url siteUrl = new Url(siteName);
        log.info("Save Url object '{}' to DB", siteUrl);
        siteUrl.save();
        log.info("Url object '{}' saved.", siteUrl);

        ctx.sessionAttribute("flash", "Site added successfully");
        log.info("Redirect to '/urls'");
        ctx.redirect("/urls");
    };

    public static final int URLS_PER_PAGE = 10;
    public static Handler listUrls = ctx -> {
        log.info("Get list of urls fro DB.");

        // TODO: PAGE
        int page;
        try {
            page = ctx.pathParamAsClass("page", Integer.class).getOrDefault(0);
        } catch (IllegalArgumentException e) {
            page = 0;
        }

        log.info("Get pages Urls from DB.");
        PagedList<Url> pagedUrls = new QUrl()
                // Устанавливаем смещение
                .setFirstRow(page * URLS_PER_PAGE)
                // Устанавливаем максимальное количество записей в результате
                .setMaxRows(URLS_PER_PAGE)
                // Задаём сортировку по id
                .orderBy().id.asc()
                // Получаем список PagedList, который представляет одну страницу результата
                .findPagedList();

        // Получаем список url
        List<Url> urls = pagedUrls.getList();

        log.info("Set pages Urls.");
        int lastPage = pagedUrls.getTotalPageCount() + 1;
        int currentPage = pagedUrls.getPageIndex() + 1;

        List<Integer> pages = IntStream
                .range(1, lastPage)
                .boxed()
                .toList();

        ctx.attribute("urls", urls);
        ctx.attribute("pages", pages);
        ctx.attribute("currentPage", currentPage);
        log.info("Render 'urls/list.html'");
        ctx.render("urls/list.html");
    };

    public static Handler showUrl = ctx -> {
        long id = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);

        log.info("Get one url from DB by id '{}'", id);
        Url url = new QUrl().id.equalTo(id).findOne();
        List<UrlCheck> urlChecks = url.getUrlChecks();

        ctx.attribute("url", url);
        ctx.attribute("urlChecks", urlChecks);
        log.info("Render 'urls/show.html'");
        ctx.render("urls/show.html");
    };

    public static Handler checkUrl = ctx -> {
        long id = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);

        log.info("Get one url from DB by id '{}'", id);
        Url url = new QUrl().id.equalTo(id).findOne();

        try {
            log.info("Collect url '{}' information", url);
            HttpResponse<String> response = Unirest
                    .get(url.getName())
                    .asString();

            log.info("Parsing page '{}' information", url);
            String responseBody = response.getBody();
            Document siteBody = Jsoup.parse(responseBody, "UTF-8");

            int statusCode = response.getStatus();
            String title = siteBody.title();
            String h1 = siteBody.selectFirst("h1") != null
                    ? Objects.requireNonNull(siteBody.selectFirst("h1")).text() : "";

            String description = siteBody.selectFirst("meta[name=description]") != null
                    ? Objects.requireNonNull(siteBody.selectFirst("meta[name=description]"))
                    .attr("content") : "";

            log.info("Create Url Check '{}'.", url);
            UrlCheck urlCheck = new UrlCheck(statusCode, title, h1, description, url);
            log.info("Save Url Check object '{}' to DB", urlCheck);
            urlCheck.save();
            log.info("Url Check object '{}' saved.", urlCheck);
        } catch (UnirestException e) {
            log.error("Url '{}' cannot be verified.", url);
            ctx.sessionAttribute("flash", "Url cannot be verified.");
            log.info("Redirect to '/urls/'");
            ctx.redirect("/urls/" + id);
            return;
        }

        assert url != null;
        List<UrlCheck> urlChecks = url.getUrlChecks();

        ctx.sessionAttribute("flash", "Site checked successfully");
        ctx.attribute("url", url);
        ctx.attribute("urlChecks", urlChecks);
        log.info("Render 'urls/show.html'");
        ctx.render("urls/show.html");

    };
}
