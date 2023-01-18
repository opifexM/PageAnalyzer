package hexlet.code.controllers;

import hexlet.code.constants.Config;
import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import hexlet.code.constants.Link;
import hexlet.code.constants.Attribute;
import hexlet.code.constants.Message;
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

    private UrlController() {
    }

    public static final Handler NEW_URL = ctx -> {
        LOG.info("Main page loading.");
        ctx.render(Link.NEW_SITE_HTML);
    };

    public static final Handler CREATE_URL = ctx -> {
        URL inputUrl;
        try {
            inputUrl = new URL(ctx.formParam(Attribute.URL));
            LOG.info("Url '{}' input.", inputUrl);
        } catch (MalformedURLException e) {
            LOG.error("Input Url is invalid.");
            ctx.sessionAttribute(Attribute.FLASH_MESSAGE, Message.INVALID_URL);
            ctx.redirect(Link.MAIN_PAGE);
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
            ctx.sessionAttribute(Attribute.FLASH_MESSAGE, Message.SITE_ALREADY_EXISTS);
            LOG.info("Redirect to: {}", Link.MAIN_PAGE);
            ctx.redirect(Link.MAIN_PAGE);
            return;
        }

        LOG.info("Create Url '{}'.", siteName);
        Url siteUrl = new Url(siteName);
        LOG.info("Save Url object '{}' to DB", siteUrl);
        siteUrl.save();
        LOG.info("Url object '{}' saved.", siteUrl);

        ctx.sessionAttribute(Attribute.FLASH_MESSAGE, Message.SITE_ADDED_SUCCESSFULLY);
        LOG.info("Redirect to: {}", Link.LIST_OF_SITES);
        ctx.redirect(Link.LIST_OF_SITES);
    };

    public static final Handler LIST_URLS = ctx -> {
        LOG.info("Get list of urls fro DB.");

        // TODO: PAGE
        int page;
        try {
            page = ctx.pathParamAsClass(Attribute.PAGE, Integer.class).getOrDefault(0);
        } catch (IllegalArgumentException e) {
            page = 0;
        }
//        String tmp = ctx.pathParamAsClass("NON_EXIST", String.class).getOrDefault("");

        LOG.info("Get pages Urls from DB.");
        PagedList<Url> pagedUrls = new QUrl()
                .setFirstRow(page * Config.URLS_PER_PAGE)
                .setMaxRows(Config.URLS_PER_PAGE)
                .orderBy().id.asc()
                .findPagedList();

        List<Url> urls = pagedUrls.getList();

        LOG.info("Set pages Urls.");
        int lastPage = pagedUrls.getTotalPageCount() + 1;
        int currentPage = pagedUrls.getPageIndex() + 1;

        List<Integer> pages = IntStream.range(1, lastPage).boxed().toList();

        ctx.attribute(Attribute.URLS, urls);
        ctx.attribute(Attribute.PAGES, pages);
        ctx.attribute(Attribute.CURRENT_PAGE, currentPage);
        LOG.info("Render {}", Link.LIST_OF_SITES_HTML);
        ctx.render(Link.LIST_OF_SITES_HTML);
    };

    public static final Handler SHOW_URL = ctx -> {
        long id = ctx.pathParamAsClass(Attribute.ID, Long.class).getOrDefault(null);

        LOG.info("Get one url from DB by id '{}'", id);
        Url url = new QUrl().id.equalTo(id).findOne();
        List<UrlCheck> urlChecks = Objects.requireNonNull(url).getUrlChecks();

        ctx.attribute(Attribute.URL, url);
        ctx.attribute(Attribute.URL_CHECKS, urlChecks);
        LOG.info("Render {}", Link.SHOW_SITE_HTML);
        ctx.render(Link.SHOW_SITE_HTML);
    };

    public static final Handler CHECK_URL = ctx -> {
        long id = ctx.pathParamAsClass(Attribute.ID, Long.class).getOrDefault(null);

        LOG.info("Get one url from DB by id '{}'", id);
        Url url = new QUrl().id.equalTo(id).findOne();

        try {
            LOG.info("Collect url '{}' information", url);
            HttpResponse<String> response = Unirest.get(Objects.requireNonNull(url).getName()).asString();

            LOG.info("Parsing page '{}' information", url);
            String responseBody = response.getBody();
            Document siteBody = Jsoup.parse(responseBody, "UTF-8");

            int statusCode = response.getStatus();
            String title = siteBody.title();
            String h1 = siteBody.selectFirst("h1") != null
                    ? Objects.requireNonNull(siteBody.selectFirst("h1")).text() : "";

            String description = siteBody.selectFirst("meta[name=description]") != null
                    ? Objects.requireNonNull(siteBody.selectFirst("meta[name=description]"))
                    .attr("content") : "";

            LOG.info("Create Url Check '{}'.", url);
            UrlCheck urlCheck = new UrlCheck(statusCode, title, h1, description, url);
            LOG.info("Save Url Check object '{}' to DB", urlCheck);
            urlCheck.save();
            LOG.info("Url Check object '{}' saved.", urlCheck);
        } catch (UnirestException e) {
            LOG.error("Url '{}' cannot be verified.", url);
            ctx.sessionAttribute(Attribute.FLASH_MESSAGE, Message.URL_CANNOT_BE_VERIFIED);
            LOG.info("Redirect to: {}/{}", Link.LIST_OF_SITES, id);
            ctx.redirect(Link.LIST_OF_SITES + "/" + id);
            return;
        }

        ctx.sessionAttribute(Attribute.FLASH_MESSAGE, Message.SITE_CHECKED_SUCCESSFULLY);
        LOG.info("Redirect to: {}/{}", Link.LIST_OF_SITES, id);
        ctx.redirect(Link.LIST_OF_SITES + "/" + id);
    };
}
