package com.tugalsan.api.file.client;

import java.util.stream.Stream;

public enum TGS_FileTypes {
    aac("aac", "audio/aac", false),
    abw("abw", "application/x-abiword", false),
    arc("arc", "application/x-freearc", false),
    audio_3gp("3gp_audio", "audio/3gpp", false),
    audio_3g2_("3g2_audio", "audio/3gpp2", false),
    avif("avif", "image/avif", false),
    avi("avi", "video/x-msvideo", false),
    azw("azw", "application/vnd.amazon.ebook", false),
    bin("bin", "application/octet-stream", false),
    bmp("bmp", "image/bmp", false),
    bz("bz", "application/x-bzip", false),
    bz2("bz2", "application/x-bzip2", false),
    cda("cda", "application/x-cdf", false),
    csh("csh", "application/x-csh", false),
    css_utf8("css_utf8", "text/css;charset=UTF-8", true),
    csv_utf8("csv_utf8", "text/csv;charset=UTF-8", true),
    doc("doc", "application/msword", false),
    docx("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", false),
    eot("eot", "application/vnd.ms-fontobject", false),
    epub("epub", "application/epub+zip", false),
    gz("gz", "application/gzip", false),
    gif("gif", "image/gif", false),
    htm_utf8("htm_utf8", "text/html;charset=UTF-8", true),
    ico("ico", "image/vnd.microsoft.icon", false),
    ics("ics", "text/calendar", false),
    jar("jar", "application/java-archive", false),
    jpeg("jpeg", "image/jpeg", false),
    js_utf8("js_utf8", "text/javascript", true),
    json_utf8("json_utf8", "application/json;charset=UTF-8", true),
    jsonld("jsonld", "application/ld+json", false),
    mid("mid", "audio/midi", false),
    mjs_utf8("mjs_utf8", "text/javascript;charset=UTF-8", true),
    mp3("mp3", "audio/mpeg", false),
    mp4("mp4", "video/mp4", false),
    mpeg("mpeg", "video/mpeg", false),
    mpkg("mpkg", "application/vnd.apple.installer+xml", false),
    odp("odp", "application/vnd.oasis.opendocument.presentation", false),
    ods("ods", "application/vnd.oasis.opendocument.spreadsheet", false),
    odt("odt", "application/vnd.oasis.opendocument.text", false),
    oga("oga", "audio/ogg", false),
    ogv("ogv", "video/ogg", false),
    ogx("ogx", "application/ogg", false),
    opus("opus", "audio/opus", false),
    otf("otf", "font/otf", false),
    png("png", "image/png", false),
    pdf("pdf", "application/pdf", false),
    php_utf8("php_utf8", "application/x-httpd-php;charset=UTF-8", true),
    ppt("ppt", "application/vnd.ms-powerpoint", false),
    pptx("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation", false),
    rar("rar", "application/vnd.rar", false),
    rtf("rtf", "application/rtf", false),
    sh_utf8("sh_utf8", "application/x-sh;charset=UTF-8", true),
    svg("svg", "image/svg+xml", false),
    tar("tar", "application/x-tar", false),
    tif("tif", "image/tiff", false),
    ts("ts", "video/mp2t", false),
    ttf("ttf", "font/ttf", false),
    txt_utf8("txt_utf8", "text/plain;charset=UTF-8", true),
    vsd("vsd", "application/vnd.visio", false),
    video_3gp("3gp_video", "video/3gpp", false),
    video_3g2_("3g2_video", "video/3gpp2", false),
    wav("wav", "audio/wav", false),
    weba("weba", "audio/webm", false),
    webm("webm", "video/webm", false),
    webp("webp", "image/webp", false),
    woff("woff", "font/woff", false),
    woff2("woff2", "font/woff2", false),
    xhtml_utf8("xhtml_utf8", "application/xhtml+xml;charset=UTF-8", true),
    xls("xls", "application/vnd.ms-excel", false),
    xlsx("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", false),
    xml_utf8("xml_utf8", "application/xml;charset=UTF-8", true),
    xul("xul", "application/vnd.mozilla.xul+xml", false),
    zip("zip", "application/zip", false),
    zip_7z("7z", "application/x-7z-compresse", false);

    public static Stream<TGS_FileTypes> stream() {
        return Stream.of(values());
    }

    public static Stream<TGS_FileTypes> streamUTF8() {
        return stream().filter(t -> t.utf8);
    }

    public static TGS_FileTypes findByContenTypePrefix(String contenTypePrefix) {
        return streamUTF8()
                .filter(t -> t.name.startsWith(String.valueOf(contenTypePrefix)))
                .findAny().orElse(null);
    }

    private TGS_FileTypes(String name, String content, boolean utf8) {
        this.name = name;
        this.content = content;
        this.utf8 = utf8;
    }
    final public String name, content;
    final public boolean utf8;
}
