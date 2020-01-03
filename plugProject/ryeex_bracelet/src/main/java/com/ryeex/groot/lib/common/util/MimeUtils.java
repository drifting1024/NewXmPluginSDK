
package com.ryeex.groot.lib.common.util;

import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by chenhao on 16/12/22.
 */

public class MimeUtils {
    private static final Map<String, String> sMimeTypeToExtensionMap = new HashMap<String, String>();

    private static final Map<String, String> sExtensionToMimeTypeMap = new HashMap<String, String>();

    static {
        addMime("application/andrew-inset", "ez");
        addMime("application/dsptype", "tsp");
        addMime("application/futuresplash", "spl");
        addMime("application/hta", "hta");
        addMime("application/mac-binhex40", "hqx");
        addMime("application/mac-compactpro", "cpt");
        addMime("application/mathematica", "nb");
        addMime("application/msaccess", "mdb");
        addMime("application/oda", "oda");
        addMime("application/ogg", "ogg");
        addMime("application/pdf", "pdf");
        addMime("application/pgp-keys", "key");
        addMime("application/pgp-signature", "pgp");
        addMime("application/pics-rules", "prf");
        addMime("application/rar", "rar");
        addMime("application/rdf+xml", "rdf");
        addMime("application/rss+xml", "rss");
        addMime("application/zip", "zip");
        addMime("application/vnd.android.package-archive", "apk");
        addMime("application/vnd.cinderella", "cdy");
        addMime("application/vnd.ms-pki.stl", "stl");
        addMime("application/vnd.oasis.opendocument.database", "odb");
        addMime("application/vnd.oasis.opendocument.formula", "odf");
        addMime("application/vnd.oasis.opendocument.graphics", "odg");
        addMime("application/vnd.oasis.opendocument.graphics-template", "otg");
        addMime("application/vnd.oasis.opendocument.image", "odi");
        addMime("application/vnd.oasis.opendocument.spreadsheet", "ods");
        addMime("application/vnd.oasis.opendocument.spreadsheet-template", "ots");
        addMime("application/vnd.oasis.opendocument.text", "odt");
        addMime("application/vnd.oasis.opendocument.text-master", "odm");
        addMime("application/vnd.oasis.opendocument.text-template", "ott");
        addMime("application/vnd.oasis.opendocument.text-web", "oth");
        addMime("application/vnd.google-earth.kml+xml", "kml");
        addMime("application/vnd.google-earth.kmz", "kmz");
        addMime("application/msword", "doc");
        addMime("application/msword", "dot");
        addMime("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");
        addMime("application/vnd.openxmlformats-officedocument.wordprocessingml.template", "dotx");
        addMime("application/vnd.ms-excel", "xls");
        addMime("application/vnd.ms-excel", "xlt");
        addMime("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");
        addMime("application/vnd.openxmlformats-officedocument.spreadsheetml.template", "xltx");
        addMime("application/vnd.ms-powerpoint", "ppt");
        addMime("application/vnd.ms-powerpoint", "pot");
        addMime("application/vnd.ms-powerpoint", "pps");
        addMime("application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "pptx");
        addMime("application/vnd.openxmlformats-officedocument.presentationml.template", "potx");
        addMime("application/vnd.openxmlformats-officedocument.presentationml.slideshow", "ppsx");
        addMime("application/vnd.rim.cod", "cod");
        addMime("application/vnd.smaf", "mmf");
        addMime("application/vnd.stardivision.calc", "sdc");
        addMime("application/vnd.stardivision.draw", "sda");
        addMime("application/vnd.stardivision.impress", "sdd");
        addMime("application/vnd.stardivision.impress", "sdp");
        addMime("application/vnd.stardivision.math", "smf");
        addMime("application/vnd.stardivision.writer", "sdw");
        addMime("application/vnd.stardivision.writer", "vor");
        addMime("application/vnd.stardivision.writer-global", "sgl");
        addMime("application/vnd.sun.xml.calc", "sxc");
        addMime("application/vnd.sun.xml.calc.template", "stc");
        addMime("application/vnd.sun.xml.draw", "sxd");
        addMime("application/vnd.sun.xml.draw.template", "std");
        addMime("application/vnd.sun.xml.impress", "sxi");
        addMime("application/vnd.sun.xml.impress.template", "sti");
        addMime("application/vnd.sun.xml.math", "sxm");
        addMime("application/vnd.sun.xml.writer", "sxw");
        addMime("application/vnd.sun.xml.writer.global", "sxg");
        addMime("application/vnd.sun.xml.writer.template", "stw");
        addMime("application/vnd.visio", "vsd");
        addMime("application/x-abiword", "abw");
        addMime("application/x-apple-diskimage", "dmg");
        addMime("application/x-bcpio", "bcpio");
        addMime("application/x-bittorrent", "torrent");
        addMime("application/x-cdf", "cdf");
        addMime("application/x-cdlink", "vcd");
        addMime("application/x-chess-pgn", "pgn");
        addMime("application/x-cpio", "cpio");
        addMime("application/x-debian-package", "deb");
        addMime("application/x-debian-package", "udeb");
        addMime("application/x-director", "dcr");
        addMime("application/x-director", "dir");
        addMime("application/x-director", "dxr");
        addMime("application/x-dms", "dms");
        addMime("application/x-doom", "wad");
        addMime("application/x-dvi", "dvi");
        addMime("application/x-flac", "flac");
        addMime("application/x-font", "pfa");
        addMime("application/x-font", "pfb");
        addMime("application/x-font", "gsf");
        addMime("application/x-font", "pcf");
        addMime("application/x-font", "pcf.Z");
        addMime("application/x-freemind", "mm");
        addMime("application/x-futuresplash", "spl");
        addMime("application/x-gnumeric", "gnumeric");
        addMime("application/x-go-sgf", "sgf");
        addMime("application/x-graphing-calculator", "gcf");
        addMime("application/x-gtar", "gtar");
        addMime("application/x-gtar", "tgz");
        addMime("application/x-gtar", "taz");
        addMime("application/x-hdf", "hdf");
        addMime("application/x-ica", "ica");
        addMime("application/x-internet-signup", "ins");
        addMime("application/x-internet-signup", "isp");
        addMime("application/x-iphone", "iii");
        addMime("application/x-iso9660-image", "iso");
        addMime("application/x-jmol", "jmz");
        addMime("application/x-kchart", "chrt");
        addMime("application/x-killustrator", "kil");
        addMime("application/x-koan", "skp");
        addMime("application/x-koan", "skd");
        addMime("application/x-koan", "skt");
        addMime("application/x-koan", "skm");
        addMime("application/x-kpresenter", "kpr");
        addMime("application/x-kpresenter", "kpt");
        addMime("application/x-kspread", "ksp");
        addMime("application/x-kword", "kwd");
        addMime("application/x-kword", "kwt");
        addMime("application/x-latex", "latex");
        addMime("application/x-lha", "lha");
        addMime("application/x-lzh", "lzh");
        addMime("application/x-lzx", "lzx");
        addMime("application/x-maker", "frm");
        addMime("application/x-maker", "maker");
        addMime("application/x-maker", "frame");
        addMime("application/x-maker", "fb");
        addMime("application/x-maker", "book");
        addMime("application/x-maker", "fbdoc");
        addMime("application/x-mif", "mif");
        addMime("application/x-ms-wmd", "wmd");
        addMime("application/x-ms-wmz", "wmz");
        addMime("application/x-msi", "msi");
        addMime("application/x-ns-proxy-autoconfig", "pac");
        addMime("application/x-nwc", "nwc");
        addMime("application/x-object", "o");
        addMime("application/x-oz-application", "oza");
        addMime("application/x-pkcs12", "p12");
        addMime("application/x-pkcs7-certreqresp", "p7r");
        addMime("application/x-pkcs7-crl", "crl");
        addMime("application/x-quicktimeplayer", "qtl");
        addMime("application/x-shar", "shar");
        addMime("application/x-shockwave-flash", "swf");
        addMime("application/x-stuffit", "sit");
        addMime("application/x-sv4cpio", "sv4cpio");
        addMime("application/x-sv4crc", "sv4crc");
        addMime("application/x-tar", "tar");
        addMime("application/x-texinfo", "texinfo");
        addMime("application/x-texinfo", "texi");
        addMime("application/x-troff", "t");
        addMime("application/x-troff", "roff");
        addMime("application/x-troff-man", "man");
        addMime("application/x-ustar", "ustar");
        addMime("application/x-wais-source", "src");
        addMime("application/x-wingz", "wz");
        addMime("application/x-webarchive", "webarchive");
        addMime("application/x-webarchive-xml", "webarchivexml");
        addMime("application/x-x509-ca-cert", "crt");
        addMime("application/x-x509-user-cert", "crt");
        addMime("application/x-xcf", "xcf");
        addMime("application/x-xfig", "fig");
        addMime("application/xhtml+xml", "xhtml");
        addMime("application/octet-stream", "vob");
        addMime("application/wps", "wps");
        addMime("application/wpt", "wpt");
        addMime("application/et", "et");
        addMime("application/ett", "ett");
        addMime("application/dps", "dps");
        addMime("application/dpt", "dpt");

        addMime("audio/3gpp", "3gpp");
        addMime("audio/amr", "amr");
        addMime("audio/basic", "snd");
        addMime("audio/midi", "mid");
        addMime("audio/midi", "midi");
        addMime("audio/midi", "kar");
        addMime("audio/midi", "xmf");
        addMime("audio/mobile-xmf", "mxmf");
        addMime("audio/mpeg", "mpga");
        addMime("audio/mpeg", "mpega");
        addMime("audio/mpeg", "mp2");
        addMime("audio/mpeg", "mp3");
        addMime("audio/mpeg", "m4a");
        addMime("audio/mpegurl", "m3u");
        addMime("audio/aac", "aac");
        addMime("audio/prs.sid", "sid");
        addMime("audio/x-aiff", "aif");
        addMime("audio/x-aiff", "aiff");
        addMime("audio/x-aiff", "aifc");
        addMime("audio/x-gsm", "gsm");
        addMime("audio/x-mpegurl", "m3u");
        addMime("audio/x-ms-wma", "wma");
        addMime("audio/x-ms-wax", "wax");
        addMime("audio/x-pn-realaudio", "ra");
        addMime("video/x-pn-realvideo", "rm");
        addMime("video/x-pn-realvideo", "rmvb");
        addMime("audio/x-pn-realaudio", "ram");
        addMime("audio/x-realaudio", "ra");
        addMime("audio/x-scpls", "pls");
        addMime("audio/x-sd2", "sd2");
        addMime("audio/x-wav", "wav");
        addMime("audio/x-qcp", "qcp");

        addMime("image/bmp", "bmp");
        addMime("image/gif", "gif");
        addMime("image/ico", "cur");
        addMime("image/ico", "ico");
        addMime("image/ief", "ief");
        addMime("image/jpeg", "jpeg");
        addMime("image/jpeg", "jpg");
        addMime("image/jpeg", "jpe");
        addMime("image/pcx", "pcx");
        addMime("image/png", "png");
        addMime("image/svg+xml", "svg");
        addMime("image/svg+xml", "svgz");
        addMime("image/tiff", "tiff");
        addMime("image/tiff", "tif");
        addMime("image/vnd.djvu", "djvu");
        addMime("image/vnd.djvu", "djv");
        addMime("image/vnd.wap.wbmp", "wbmp");
        addMime("image/x-cmu-raster", "ras");
        addMime("image/x-coreldraw", "cdr");
        addMime("image/x-coreldrawpattern", "pat");
        addMime("image/x-coreldrawtemplate", "cdt");
        addMime("image/x-corelphotopaint", "cpt");
        addMime("image/x-icon", "ico");
        addMime("image/x-jg", "art");
        addMime("image/x-jng", "jng");
        addMime("image/x-ms-bmp", "bmp");
        addMime("image/x-photoshop", "psd");
        addMime("image/x-portable-anymap", "pnm");
        addMime("image/x-portable-bitmap", "pbm");
        addMime("image/x-portable-graymap", "pgm");
        addMime("image/x-portable-pixmap", "ppm");
        addMime("image/x-rgb", "rgb");
        addMime("image/x-xbitmap", "xbm");
        addMime("image/x-xpixmap", "xpm");
        addMime("image/x-xwindowdump", "xwd");

        addMime("model/iges", "igs");
        addMime("model/iges", "iges");
        addMime("model/mesh", "msh");
        addMime("model/mesh", "mesh");
        addMime("model/mesh", "silo");

        addMime("text/calendar", "ics");
        addMime("text/calendar", "icz");
        addMime("text/comma-separated-values", "csv");
        addMime("text/css", "css");
        addMime("text/html", "htm");
        addMime("text/html", "html");
        addMime("text/h323", "323");
        addMime("text/iuls", "uls");
        addMime("text/mathml", "mml");
        addMime("text/plain", "txt");
        addMime("text/plain", "asc");
        addMime("text/plain", "text");
        addMime("text/plain", "diff");
        addMime("text/plain", "po");
        addMime("text/richtext", "rtx");
        addMime("text/rtf", "rtf");
        addMime("text/text", "phps");
        addMime("text/tab-separated-values", "tsv");
        addMime("text/xml", "xml");
        addMime("text/x-bibtex", "bib");
        addMime("text/x-boo", "boo");
        addMime("text/x-c++hdr", "h++");
        addMime("text/x-c++hdr", "hpp");
        addMime("text/x-c++hdr", "hxx");
        addMime("text/x-c++hdr", "hh");
        addMime("text/x-c++src", "c++");
        addMime("text/x-c++src", "cpp");
        addMime("text/x-c++src", "cxx");
        addMime("text/x-chdr", "h");
        addMime("text/x-component", "htc");
        addMime("text/x-csh", "csh");
        addMime("text/x-csrc", "c");
        addMime("text/x-dsrc", "d");
        addMime("text/x-haskell", "hs");
        addMime("text/x-java", "java");
        addMime("text/x-literate-haskell", "lhs");
        addMime("text/x-moc", "moc");
        addMime("text/x-pascal", "p");
        addMime("text/x-pascal", "pas");
        addMime("text/x-pcs-gcd", "gcd");
        addMime("text/x-setext", "etx");
        addMime("text/x-tcl", "tcl");
        addMime("text/x-tex", "tex");
        addMime("text/x-tex", "ltx");
        addMime("text/x-tex", "sty");
        addMime("text/x-tex", "cls");
        addMime("text/x-vcalendar", "vcs");
        addMime("text/x-vcard", "vcf");

        addMime("video/3gpp", "3gpp");
        addMime("video/3gpp", "3gp");
        addMime("video/3gpp", "3g2");
        addMime("video/dl", "dl");
        addMime("video/dv", "dif");
        addMime("video/dv", "dv");
        addMime("video/fli", "fli");
        addMime("video/m4v", "m4v");
        addMime("video/mpeg", "mpeg");
        addMime("video/mpeg", "mpg");
        addMime("video/mpeg", "mpe");
        addMime("video/mp4", "mp4");
        addMime("video/quicktime", "qt");
        addMime("video/quicktime", "mov");
        addMime("video/vnd.mpegurl", "mxu");
        addMime("video/x-la-asf", "lsf");
        addMime("video/x-la-asf", "lsx");
        addMime("video/x-mng", "mng");
        addMime("video/x-ms-asf", "asf");
        addMime("video/x-ms-asf", "asx");
        addMime("video/x-ms-wm", "wm");
        addMime("video/x-ms-wmv", "wmv");
        addMime("video/x-ms-wmx", "wmx");
        addMime("video/x-ms-wvx", "wvx");
        addMime("video/x-msvideo", "avi");
        addMime("video/x-sgi-movie", "movie");
        addMime("video/x-flv", "flv");
        addMime("video/x-flv", "xv");
        addMime("video/x-flv", "xvx");
        addMime("video/x-matroska", "mkv");
        addMime("video/MP2T", "ts");
        addMime("video/mp4", "f4v");

        addMime("x-conference/x-cooltalk", "ice");
        addMime("x-epoc/x-sisx-app", "sisx");
    }

    private static void addMime(String mimeType, String extension) {
        if (!sMimeTypeToExtensionMap.containsKey(mimeType)) {
            sMimeTypeToExtensionMap.put(mimeType, extension);
        }
        sExtensionToMimeTypeMap.put(extension, mimeType);
    }

    public static boolean hasMimeType(String mimeType) {
        return !TextUtils.isEmpty(mimeType) && sMimeTypeToExtensionMap.containsKey(mimeType);

    }

    public static String guessMimeTypeFromExtension(String extension) {
        if (TextUtils.isEmpty(extension)) {
            return null;
        }

        return sExtensionToMimeTypeMap.get(extension);
    }

    public static boolean hasExtension(String extension) {
        return !TextUtils.isEmpty(extension) && sExtensionToMimeTypeMap.containsKey(extension);

    }

    public static String guessExtensionFromMimeType(String mimeType) {
        if (TextUtils.isEmpty(mimeType)) {
            return null;
        }

        return sMimeTypeToExtensionMap.get(mimeType);
    }
}
