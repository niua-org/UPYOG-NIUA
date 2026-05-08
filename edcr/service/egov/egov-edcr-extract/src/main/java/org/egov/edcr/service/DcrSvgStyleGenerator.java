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

/**
 * Emits SVG {@code font-face} definitions from DXF styles. Two behaviours:
 * <ul>
 *   <li><b>Legacy</b> — original Times New Roman mapping (used when MDMS / {@code EDCR_DXF_PDF} sheet path runs).</li>
 *   <li><b>Direct single PDF</b> — when {@code svgContext} contains {@code egov.singlePdfUsingKabeja=true}, uses
 *       installed TTF fallbacks and style aliases so Kabeja text stays readable on typical Linux servers.</li>
 * </ul>
 */
public class DcrSvgStyleGenerator {

    /** Ordered list: first existing file wins when Kabeja has no font mapping (direct path only). */
    private static final String[] FONT_FALLBACK_PATHS = new String[] {
            "/usr/share/fonts/truetype/msttcorefonts/Times_New_Roman.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
            "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf"
    };

    public DcrSvgStyleGenerator() {
    }

    public static void toSAX(ContentHandler handler, Map svgContext, DXFStyle style) throws SAXException {
        // Set by DcrSvgGenerator.setupProperties when PROPERTY_DIRECT_SINGLE_PDF is in the generator map.
        if (Boolean.TRUE.equals(svgContext.get("egov.singlePdfUsingKabeja"))) {
            toSaxDirectSinglePdf(handler, style);
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

    /** Historical font-face output (single hardcoded TTF path). Preserves legacy PDF appearance. */
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

    /** Direct path: map DXF style names and .shx references to a real TTF on disk. */
    private static void toSaxDirectSinglePdf(ContentHandler handler, DXFStyle style) throws SAXException {
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

    /** One font-face per alias family string so SVG text {@code font-family} lookups resolve. */
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

    /** Kabeja mapping first, then common Linux font packages. */
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

    /** Strip path and .shx so e.g. {@code txt.shx} and {@code txt} share one face. */
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

    private static String normalizeFamily(String font) {
        String normalized = stripShx(font);
        return normalized.isEmpty() ? "romans" : normalized;
    }
}
