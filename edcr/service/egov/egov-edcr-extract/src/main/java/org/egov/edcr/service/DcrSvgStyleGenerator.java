package org.egov.edcr.service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.kabeja.dxf.DXFStyle;
import org.kabeja.svg.SVGUtils;
import org.kabeja.tools.FontManager;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/*
 * NOTE:
 * Called from DcrSvgGenerator while writing SVG defs for each DXFStyle in the drawing.
 *
 * Two paths, chosen only by the context flag (no extra Spring beans):
 *
 * 1) Legacy when egov.singlePdfUsingKabeja is missing or false:
 *    Same as before: every style points at the hard-coded Times New Roman file URL.
 *    Keeps old multi-sheet PDFs stable for tenants still on MDMS / EDCR_DXF_PDF rules.
 *
 * 2) Direct single PDF when egov.singlePdfUsingKabeja is Boolean.TRUE:
 *    Picks a real font file on disk (Kabeja FontManager first, then FONT_FALLBACK_PATHS).
 *    Writes several font-face entries with aliases so whatever font-family Kabeja emits still matches a TTF
 *    when MS Core Fonts are not installed (typical on Linux).
 */
public class DcrSvgStyleGenerator {

    /** Ordered list: first existing file runs when Kabeja has no font mapping (direct path only). */
    private static final String[] FONT_FALLBACK_PATHS = new String[] {
            "/usr/share/fonts/truetype/msttcorefonts/Times_New_Roman.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
            "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf"
    };

    public DcrSvgStyleGenerator() {
    }

    /*
     * Kabeja calls this with the same svgContext map DcrSvgGenerator fills in setupProperties. The flag alone
     * switches legacy vs direct behaviour so we do not thread new parameters through Kabeja.
     */
    public static void toSAX(ContentHandler handler, Map svgContext, DXFStyle style) throws SAXException {
        if (Boolean.TRUE.equals(svgContext.get("egov.singlePdfUsingKabeja"))) {
            toSaxSinglePdf(handler, style);
        } else {
            toSaxLegacy(handler, style);
        }
    }

    /** Original behaviour — unchanged for legacy multi-sheet Kabeja PDFs. */
    private static void toSaxLegacy(ContentHandler handler, DXFStyle style) throws SAXException {
        FontManager manager = FontManager.getInstance();
        if (manager.hasFontDescription(style.getBigFontFile())) {
            generateSaxFontDescriptionLegacy(handler, style.getBigFontFile());
        } else if (manager.hasFontDescription(style.getFontFile())) {
            generateSaxFontDescriptionLegacy(handler, style.getFontFile());
        }
    }

    /*
     * Legacy font-face: lower-case family, strip .shx from the name, xlink always targets Times_New_Roman.ttf under
     * msttcorefonts. Only used from toSaxLegacy.
     */
    private static void generateSaxFontDescriptionLegacy(ContentHandler handler, String font) throws SAXException {
        String fontFamily = font.toLowerCase();
        if (fontFamily.endsWith(".shx")) {
            fontFamily = fontFamily.substring(0, fontFamily.indexOf(".shx"));
        }
        AttributesImpl attr = new AttributesImpl();

        SVGUtils.addAttribute(attr, "font-family", fontFamily);
        SVGUtils.addAttribute(attr, "font-face", "Regular");
        SVGUtils.startElement(handler, "font-face", attr);
        attr = new AttributesImpl();
        SVGUtils.startElement(handler, "font-face-src", attr);
        attr = new AttributesImpl();

        String url = "file:///usr/share/fonts/truetype/msttcorefonts/Times_New_Roman.ttf" + "#" + fontFamily;
        attr.addAttribute("", "", "xmlns:xlink", "CDATA", "http://www.w3.org/1999/xlink");
        attr.addAttribute("http://www.w3.org/1999/xlink", "href", "xlink:href", "CDATA", url);
        SVGUtils.emptyElement(handler, "font-face-uri", attr);
        SVGUtils.endElement(handler, "font-face-src");
        SVGUtils.endElement(handler, "font-face");
    }

    /*
     * Direct path: pick primary font the same order as legacy (big font, then regular, else style name), gather aliases,
     * then generateSaxFontDescriptionDirect emits one font-face per distinct family string.
     */
    private static void toSaxSinglePdf(ContentHandler handler, DXFStyle style) throws SAXException {
        FontManager manager = FontManager.getInstance();
        Set<String> aliases = new LinkedHashSet<>();
        addAlias(aliases, style.getName());
        addAlias(aliases, style.getFontFile());
        addAlias(aliases, style.getBigFontFile());

        if (manager.hasFontDescription(style.getBigFontFile())) {
            generateSaxFontDescriptionDirect(handler, style.getBigFontFile(), aliases);
        } else if (manager.hasFontDescription(style.getFontFile())) {
            generateSaxFontDescriptionDirect(handler, style.getFontFile(), aliases);
        } else {
            generateSaxFontDescriptionDirect(handler, style.getName(), aliases);
        }
    }

    /*
     * One SVG font-face per family string (primary, lower-case, and aliases). All point at the same resolved TTF so
     * Batik can load glyphs no matter which alias appears in the drawing text.
     */
    private static void generateSaxFontDescriptionDirect(ContentHandler handler, String font, Set<String> aliases)
            throws SAXException {
        String primaryFamily = normalizeFamily(font);
        String resolvedPath = resolveFontPath(primaryFamily);
        Set<String> families = new LinkedHashSet<>();
        families.add(primaryFamily);
        families.add(primaryFamily.toLowerCase());
        for (String alias : aliases) {
            String family = normalizeFamily(alias);
            if (!family.isEmpty()) {
                families.add(family);
                families.add(family.toLowerCase());
            }
        }

        for (String family : families) {
            AttributesImpl attr = new AttributesImpl();
            SVGUtils.addAttribute(attr, "font-family", family);
            SVGUtils.addAttribute(attr, "font-style", "normal");
            SVGUtils.addAttribute(attr, "font-weight", "normal");
            SVGUtils.startElement(handler, "font-face", attr);
            attr = new AttributesImpl();
            SVGUtils.startElement(handler, "font-face-src", attr);
            attr = new AttributesImpl();

            String url = "file://" + resolvedPath + "#" + family;
            attr.addAttribute("", "", "xmlns:xlink", "CDATA", "http://www.w3.org/1999/xlink");
            attr.addAttribute("http://www.w3.org/1999/xlink", "href", "xlink:href", "CDATA", url);
            SVGUtils.emptyElement(handler, "font-face-uri", attr);
            SVGUtils.endElement(handler, "font-face-src");
            SVGUtils.endElement(handler, "font-face");
        }
    }

    /*
     * Absolute path to a TTF: Kabeja mapping for the family name if present, else first existing FONT_FALLBACK_PATHS
     * entry, else the first list path even if missing (last resort).
     */
    private static String resolveFontPath(String fontFamily) {
        String fromKabeja = FontManager.getInstance().getFontDescription(fontFamily);
        if (fromKabeja != null && !fromKabeja.trim().isEmpty()) {
            return fromKabeja;
        }
        for (String candidate : FONT_FALLBACK_PATHS) {
            if (Files.exists(Paths.get(candidate))) {
                return candidate;
            }
        }
        return FONT_FALLBACK_PATHS[0];
    }

    /*
     * Pushes the raw font string and the SHX-stripped basename into aliases so style names and file names map to one font
     * in generateSaxFontDescriptionDirect.
     */
    private static void addAlias(Set<String> aliases, String fontRef) {
        if (fontRef == null || fontRef.trim().isEmpty()) {
            return;
        }
        aliases.add(fontRef.trim());
        String stripped = stripShx(fontRef);
        if (!stripped.isEmpty()) {
            aliases.add(stripped);
        }
    }

    /*
     * Turn a DXF font reference (paths, drives, .shx) into a bare name for SVG font-family matching, e.g. C:\fonts\romans.shx → romans.
     */
    private static String stripShx(String fontRef) {
        if (fontRef == null) {
            return "";
        }
        String value = fontRef.trim();
        int slash = Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\'));
        if (slash >= 0 && slash < value.length() - 1) {
            value = value.substring(slash + 1);
        }
        String lower = value.toLowerCase();
        if (lower.endsWith(".shx")) {
            return value.substring(0, value.length() - 4);
        }
        return value;
    }

    /*
     * Non-empty family string for SVG; uses "romans" when nothing usable remains after stripping path and .shx.
     */
    private static String normalizeFamily(String font) {
        String normalized = stripShx(font);
        return normalized.isEmpty() ? "romans" : normalized;
    }
}
