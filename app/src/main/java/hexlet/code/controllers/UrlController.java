package hexlet.code.controllers;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import hexlet.code.util.Go;
import hexlet.code.util.Key;
import hexlet.code.util.Text;
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

    // TODO constant for conf
    public static final int URLS_PER_PAGE = 10;
    public static final Handler NEW_URL = ctx -> {
        LOG.info("Main page loading.");
        // TODO: constants
        ctx.render(Go.LOCATION_NEW_WEBSITE_HTML);
    };

    public static final Handler CREATE_URL = ctx -> {
        URL inputUrl;
        try {
            inputUrl = new URL(ctx.formParam(Key.URL));
            LOG.info("Url '{}' input.", inputUrl);
        } catch (MalformedURLException e) {
            LOG.error("Input Url is invalid.");
            ctx.sessionAttribute(Key.FLASH, Text.INVALID_URL);
            ctx.redirect(Go.LOCATION_MAIN_PAGE);
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
            ctx.sessionAttribute(Key.FLASH, Text.THE_SITE_ALREADY_EXISTS);
            LOG.info("Redirect to: {}", Go.LOCATION_MAIN_PAGE);
            ctx.redirect(Go.LOCATION_MAIN_PAGE);
            return;
        }

        LOG.info("Create Url '{}'.", siteName);
        Url siteUrl = new Url(siteName);
        LOG.info("Save Url object '{}' to DB", siteUrl);
        siteUrl.save();
        LOG.info("Url object '{}' saved.", siteUrl);

        ctx.sessionAttribute(Key.FLASH, Text.SITE_ADDED_SUCCESSFULLY);
        LOG.info("Redirect to: {}", Go.LOCATION_LIST_OF_SITES);
        ctx.redirect(Go.LOCATION_LIST_OF_SITES);
    };

    public static final Handler LIST_URLS = ctx -> {
        LOG.info("Get list of urls fro DB.");

        // TODO: PAGE
        int page;
        try {
            page = ctx.pathParamAsClass(Key.PAGE, Integer.class).getOrDefault(0);
        } catch (IllegalArgumentException e) {
            page = 0;
        }

        LOG.info("Get pages Urls from DB.");
        PagedList<Url> pagedUrls = new QUrl()
                .setFirstRow(page * URLS_PER_PAGE)
                .setMaxRows(URLS_PER_PAGE)
                .orderBy().id.asc()
                .findPagedList();

        List<Url> urls = pagedUrls.getList();

        LOG.info("Set pages Urls.");
        int lastPage = pagedUrls.getTotalPageCount() + 1;
        int currentPage = pagedUrls.getPageIndex() + 1;

        List<Integer> pages = IntStream.range(1, lastPage).boxed().toList();

        ctx.attribute(Key.URLS, urls);
        ctx.attribute(Key.PAGES, pages);
        ctx.attribute(Key.CURRENT_PAGE, currentPage);
        LOG.info("Render {}", Go.LOCATION_LIST_OF_SITES_HTML);
        ctx.render(Go.LOCATION_LIST_OF_SITES_HTML);
    };

    public static final Handler SHOW_URL = ctx -> {
        long id = ctx.pathParamAsClass(Key.ID, Long.class).getOrDefault(null);

        LOG.info("Get one url from DB by id '{}'", id);
        Url url = new QUrl().id.equalTo(id).findOne();
        List<UrlCheck> urlChecks = url.getUrlChecks();

        ctx.attribute(Key.URL, url);
        ctx.attribute(Key.URL_CHECKS, urlChecks);
        LOG.info("Render {}", Go.LOCATION_SHOW_SITE_HTML);
        ctx.render(Go.LOCATION_SHOW_SITE_HTML);
    };

    public static final Handler CHECK_URL = ctx -> {
        long id = ctx.pathParamAsClass(Key.ID, Long.class).getOrDefault(null);

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
            ctx.sessionAttribute(Key.FLASH, Text.URL_CANNOT_BE_VERIFIED);
            LOG.info("Redirect to: {}/{}", Go.LOCATION_LIST_OF_SITES, id);
            ctx.redirect(Go.LOCATION_LIST_OF_SITES + "/" + id);
            return;
        }

        ctx.sessionAttribute(Key.FLASH, Text.SITE_CHECKED_SUCCESSFULLY);
        LOG.info("Redirect to: {}/{}", Go.LOCATION_LIST_OF_SITES, id);
        ctx.redirect(Go.LOCATION_LIST_OF_SITES + "/" + id);


//        List<UrlCheck> urlChecks = url.getUrlChecks();
//
//        ctx.sessionAttribute(Key.FLASH, Text.SITE_CHECKED_SUCCESSFULLY);
//        LOG.info("Redirect to: {}", Go.LOCATION_LIST_OF_SITES);
//        ctx.redirect(Go.LOCATION_LIST_OF_SITES);
//
//        ctx.redirect(Go.LOCATION_LIST_OF_SITES + "/" + id);
//
//        ctx.sessionAttribute(Key.FLASH, Text.SITE_CHECKED_SUCCESSFULLY);
//        ctx.attribute(Key.URL, url);
//        ctx.attribute(Key.URL_CHECKS, urlChecks);
//        LOG.info("Render {}", Go.LOCATION_SHOW_SITE_HTML);
//        ctx.render(Go.LOCATION_SHOW_SITE_HTML);
    };
}
