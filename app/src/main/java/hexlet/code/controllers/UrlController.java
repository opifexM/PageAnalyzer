package hexlet.code.controllers;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import io.ebean.PagedList;
import io.javalin.http.Handler;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static java.util.Objects.isNull;


public final class UrlController {
    private static final Logger LOG = LoggerFactory.getLogger(UrlController.class);

    public static final Handler NEW_URL = ctx -> {
        LOG.info("Main page loading.");
        ctx.render("urls/new.html");
    };

    public static final String FLASH = "flash";
    public static final String INVALID_URL = "Invalid URL";
    public static final String SITE_ADDED_SUCCESSFULLY = "Site added successfully";
    public static final String THE_SITE_ALREADY_EXISTS = "The site already exists";
    public static final Handler CREATE_URL = ctx -> {
        URL inputUrl;
        try {
            inputUrl = new URL(ctx.formParam("url"));
            LOG.info("Url '{}' input.", inputUrl);
        } catch (MalformedURLException e) {
            LOG.error("Input Url is invalid.");
            ctx.sessionAttribute(FLASH, INVALID_URL);
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

        LOG.info("Checking Url '{}'", siteName);
        Url checkUrl = new QUrl().name.equalTo(siteName).findOne();
        if (!isNull(checkUrl)) {
            LOG.error("Input Url '{}' already exists.", siteName);
            ctx.sessionAttribute(FLASH, THE_SITE_ALREADY_EXISTS);
            LOG.info("Redirect to '/'");
            ctx.redirect("/");
            return;
        }

        LOG.info("Create Url '{}'.", siteName);
        Url siteUrl = new Url(siteName);
        LOG.info("Save Url object '{}' to DB", siteUrl);
        siteUrl.save();
        LOG.info("Url object '{}' saved.", siteUrl);

        ctx.sessionAttribute(FLASH, SITE_ADDED_SUCCESSFULLY);
        LOG.info("Redirect to '/urls'");
        ctx.redirect("/urls");
    };

    public static final int URLS_PER_PAGE = 10;
    public static final Handler LIST_URLS = ctx -> {
        LOG.info("Get list of urls fro DB.");

        // TODO: PAGE
        int page;
        try {
            page = ctx.pathParamAsClass("page", Integer.class).getOrDefault(0);
        } catch (IllegalArgumentException e) {
            page = 0;
        }

        LOG.info("Get pages Urls from DB.");
        PagedList<Url> pagedUrls = new QUrl().setFirstRow(page * URLS_PER_PAGE).setMaxRows(URLS_PER_PAGE).orderBy().id.asc().findPagedList();

        List<Url> urls = pagedUrls.getList();

        LOG.info("Set pages Urls.");
        int lastPage = pagedUrls.getTotalPageCount() + 1;
        int currentPage = pagedUrls.getPageIndex() + 1;

        List<Integer> pages = IntStream.range(1, lastPage).boxed().toList();

        ctx.attribute("urls", urls);
        ctx.attribute("pages", pages);
        ctx.attribute("currentPage", currentPage);
        LOG.info("Render 'urls/list.html'");
        ctx.render("urls/list.html");
    };

    public static final Handler SHOW_URL = ctx -> {
        long id = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);

        LOG.info("Get one url from DB by id '{}'", id);
        Url url = new QUrl().id.equalTo(id).findOne();
        List<UrlCheck> urlChecks = url.getUrlChecks();

        ctx.attribute("url", url);
        ctx.attribute("urlChecks", urlChecks);
        LOG.info("Render 'urls/show.html'");
        ctx.render("urls/show.html");
    };

    public static final String SITE_CHECKED_SUCCESSFULLY = "Site checked successfully";
    public static final String URL_CANNOT_BE_VERIFIED = "Url cannot be verified.";
    public static final Handler CHECK_URL = ctx -> {
        long id = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);

        LOG.info("Get one url from DB by id '{}'", id);
        Url url = new QUrl().id.equalTo(id).findOne();

        try {
            LOG.info("Collect url '{}' information", url);
            HttpResponse<String> response = Unirest.get(url.getName()).asString();

            LOG.info("Parsing page '{}' information", url);
            String responseBody = response.getBody();
            Document siteBody = Jsoup.parse(responseBody, "UTF-8");

            int statusCode = response.getStatus();
            String title = siteBody.title();
            String h1 = siteBody.selectFirst("h1") != null ? Objects.requireNonNull(siteBody.selectFirst("h1")).text() : "";

            String description = siteBody.selectFirst("meta[name=description]") != null ? Objects.requireNonNull(siteBody.selectFirst("meta[name=description]")).attr("content") : "";

            LOG.info("Create Url Check '{}'.", url);
            UrlCheck urlCheck = new UrlCheck(statusCode, title, h1, description, url);
            LOG.info("Save Url Check object '{}' to DB", urlCheck);
            urlCheck.save();
            LOG.info("Url Check object '{}' saved.", urlCheck);
        } catch (UnirestException e) {
            LOG.error("Url '{}' cannot be verified.", url);
            ctx.sessionAttribute(FLASH, URL_CANNOT_BE_VERIFIED);
            LOG.info("Redirect to '/urls/'");
            ctx.redirect("/urls/" + id);
            return;
        }

        List<UrlCheck> urlChecks = url.getUrlChecks();

        ctx.sessionAttribute(FLASH, SITE_CHECKED_SUCCESSFULLY);
        ctx.attribute("url", url);
        ctx.attribute("urlChecks", urlChecks);
        LOG.info("Render 'urls/show.html'");
        ctx.render("urls/show.html");

    };
}
