package org.egov.edcr.service;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.printing.Orientation;
import org.egov.common.entity.edcr.EdcrPdfDetail;
import org.egov.edcr.entity.PdfPageSize;
import org.egov.edcr.utility.DcrConstants;
import org.egov.infra.admin.master.entity.AppConfigValues;
import org.egov.infra.admin.master.service.AppConfigValueService;
import org.kabeja.batik.tools.SAXPDFSerializer;
import org.kabeja.dxf.DXFDocument;
import org.kabeja.xml.SAXSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.aspose.cad.Color;
import com.aspose.cad.Image;
import com.aspose.cad.fileformats.cad.CadDrawTypeMode;
import com.aspose.cad.imageoptions.CadRasterizationOptions;
import com.aspose.cad.imageoptions.PdfOptions;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;
/**
 * Single place that turns a DXF into a PDF using either Kabeja or Aspose, never both at once.
 *
 * Engine is chosen from Digit DCR app config: module type on DcrConstants.APPLICATION_MODULE_TYPE,
 * key DXF_TO_PDF_ENGINE. Value ASPOSE selects Aspose; anything else (including missing) selects Kabeja.
 *
 * Aspose reads the file from disk (dxfSourceFile argument). Kabeja uses the in-memory document; any layer edits
 * from the extract step apply only on the Kabeja path.
 *
 * For Kabeja only, EdcrPdfDetail.getKabejaSinglePageDXFToPdf() turns on extra SVG and PDF tuning for the
 * one-full-drawing flow; legacy multi-sheet rows leave that flag unset.
 *
 * This class exists so DxfToPdfConverterExtract does not duplicate engine logic. The same EdcrPdfDetail instance
 * the extract built is passed through; that boolean is the only switch between legacy-style and single-full-DXF output.
 */
@Service
public class DxfToPdfUnifiedConverter {
    private static final Logger LOG = LogManager.getLogger(DxfToPdfUnifiedConverter.class);

    public enum Engine {
        KABEJA,
        ASPOSE
    }

    @Autowired
    private AppConfigValueService appConfigValueService;

    /*
     * NOTE:
     * Main entry: called from DxfToPdfConverterExtract (or others) after the extract filled edcrPdfDetail
     * (page size, output name / layer name, and optionally kabejaSinglePageDXFToPdf).
     *
     * Aspose needs the DXF file on disk and loads the full drawing; it does not use in-memory DXF edits.
     * Kabeja uses the in-memory DXF document and reads kabejaSinglePageDXFToPdf for extra generator map entries.
     * If Aspose throws or the file is missing, we fall back to Kabeja so the user still gets a PDF when possible.
     */
    public File convert(File dxfSourceFile, DXFDocument dxfDocument, String fileName, String layerName,
                        EdcrPdfDetail edcrPdfDetail) {
        Engine engine = resolveEngine();
        if (engine == Engine.ASPOSE) {
            if (dxfSourceFile == null || !dxfSourceFile.isFile()) {
                LOG.warn("DXF_TO_PDF_ENGINE=ASPOSE but DXF source file is missing; using Kabeja.");
                return convertWithKabeja(dxfDocument, fileName, layerName, edcrPdfDetail);
            }
            try {
                return convertWithAspose(dxfSourceFile, layerName, edcrPdfDetail);
            } catch (Exception ex) {
                LOG.error("Aspose DXF→PDF failed for {} sheet {}; falling back to Kabeja.", fileName, layerName, ex);
                return convertWithKabeja(dxfDocument, fileName, layerName, edcrPdfDetail);
            }
        }
        return convertWithKabeja(dxfDocument, fileName, layerName, edcrPdfDetail);
    }

    /*
     * Reads DXF_TO_PDF_ENGINE from Digit DCR app config. Any value other than ASPOSE (case-insensitive) means Kabeja,
     * including when the setting is missing.
     */
    private Engine resolveEngine() {
        List<AppConfigValues> vals = appConfigValueService.getConfigValuesByModuleAndKey(
                DcrConstants.APPLICATION_MODULE_TYPE, DcrConstants.DXF_TO_PDF_ENGINE);
        if (vals != null && !vals.isEmpty()) {
            String v = vals.get(0).getValue();
            if (v != null && "ASPOSE".equalsIgnoreCase(v.trim())) {
                return Engine.ASPOSE;
            }
        }
        return Engine.KABEJA;
    }

    /*
     * NOTE (Kabeja generator map has two shapes):
     *
     * Legacy multi-sheet PDFs:
     * - Margin 0.5 and optional hatch stripping only, same as before.
     *
     * Direct single full-DXF PDF when EdcrPdfDetail.getKabejaSinglePageDXFToPdf() is true:
     * - Sets DcrSvgGenerator.PROPERTY_SINGLE_PDF so DcrSvgGenerator and DcrSvgStyleGenerator adjust text, bounds,
     *   and fonts without touching legacy runs.
     * - bounds-rule Modelspace-Limits: viewport from DXF header so the drawing is not a tiny dot on the page.
     * - margin 0: do not shrink the usable area for that mode.
     * - stroke-width 0.12: thin lines so dimensions do not look like thick bars.
     *
     * Output file name is layerName plus ".pdf"; in direct mode layerName is the DXF file name without extension.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private File convertWithKabeja(DXFDocument dxfDocument, String fileName, String layerName,
                                   EdcrPdfDetail edcrPdfDetail) {
        LOG.info("Converting Dxf to Pdf with Kabeja...");
        File fileOut = new File(layerName + ".pdf");
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("---------converting {} - {} to pdf (Kabeja)----------", fileName, layerName);
            }
            try (FileOutputStream fout = new FileOutputStream(fileOut)) {
                DcrSvgGenerator generator = new DcrSvgGenerator();
                SAXSerializer out = new SAXPDFSerializer();
                out.setOutput(fout);
                HashMap map = new HashMap();
                Rectangle rectangle = PageSize.getRectangle(edcrPdfDetail.getPageSize().getSize());
                if (edcrPdfDetail.getPageSize().getOrientation().ordinal() == Orientation.PORTRAIT.ordinal()) {
                    map.put("width", String.valueOf(rectangle.getWidth() * edcrPdfDetail.getPageSize().getEnlarge()));
                    map.put("height", String.valueOf(rectangle.getHeight() * edcrPdfDetail.getPageSize().getEnlarge()));
                } else {
                    map.put("width", String.valueOf(rectangle.getHeight() * edcrPdfDetail.getPageSize().getEnlarge()));
                    map.put("height", String.valueOf(rectangle.getWidth() * edcrPdfDetail.getPageSize().getEnlarge()));
                }
                // Single-PDF: tighter viewBox + thinner strokes (legacy keeps original margin / behaviour).
                boolean directSinglePdf = Boolean.TRUE.equals(edcrPdfDetail.getKabejaSinglePageDXFToPdf());
                if (directSinglePdf) {
                    map.put(DcrSvgGenerator.PROPERTY_SINGLE_PDF, "true");
                    map.put("bounds-rule", "Modelspace-Limits");
                    map.put("margin", String.valueOf(0));
                    // Uniform thin linework (dimension ticks, etc.); avoids heavy DXF lineweights in PDF.
                    map.put("stroke-width", String.valueOf(0.12));
                    if (Boolean.TRUE.equals(edcrPdfDetail.getPageSize().getRemoveHatch())) {
                        map.put("stroke.width", Double.valueOf(0));
                    }
                } else {
                    // Legacy per-sheet path: unchanged from historical Kabeja integration.
                    map.put("margin", String.valueOf(0.5));
                    if (edcrPdfDetail.getPageSize().getRemoveHatch()) {
                        map.put("stroke.width", Double.valueOf(0));
                    }
                }
                generator.generate(dxfDocument, out, map);
                fout.flush();
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("---------conversion success {} - {} (Kabeja)----------", fileName, layerName);
            }
            return fileOut.length() > 0 ? fileOut : null;
        } catch (Exception ep) {
            LOG.error("Pdf convertion failed for {} - {} due to {}", fileName, layerName, ep.getMessage());
            ep.printStackTrace();
            edcrPdfDetail.setFailureReasons(ep.getMessage());
        }
        return null;
    }

    /*
     * Aspose path: reads the DXF from disk and exports to PDF. Page size comes from edcrPdfDetail.getPageSize().
     * kabejaSinglePageDXFToPdf is not used here; Aspose behaves the same for legacy and direct extract rows.
     */
    private File convertWithAspose(File dxfSourceFile, String layerName, EdcrPdfDetail edcrPdfDetail) {
        LOG.info("Converting Dxf to Pdf with Aspose...");

        File fileOut = new File(layerName + ".pdf");
        float pageWidth = 3370f;
        float pageHeight = 2384f;
        PdfPageSize ps = edcrPdfDetail.getPageSize();
        if (ps != null && ps.getSize() != null) {
            try {
                Rectangle rectangle = PageSize.getRectangle(ps.getSize());
                int enlarge = ps.getEnlarge() > 0 ? ps.getEnlarge() : 1;
                if (ps.getOrientation() != null && ps.getOrientation() == Orientation.PORTRAIT) {
                    pageWidth = rectangle.getWidth() * enlarge;
                    pageHeight = rectangle.getHeight() * enlarge;
                } else {
                    pageWidth = rectangle.getHeight() * enlarge;
                    pageHeight = rectangle.getWidth() * enlarge;
                }
            } catch (RuntimeException ex) {
                LOG.debug("Using default Aspose page size: {}", ex.getMessage());
            }
        }
        Image image = Image.load(dxfSourceFile.getAbsolutePath());
        try {
            PdfOptions pdfOptions = new PdfOptions();
            CadRasterizationOptions rasterizationOptions = new CadRasterizationOptions();
            rasterizationOptions.setBackgroundColor(Color.getWhite());
            rasterizationOptions.setDrawType(CadDrawTypeMode.UseObjectColor);
            rasterizationOptions.setPageWidth(pageWidth);
            rasterizationOptions.setPageHeight(pageHeight);
            rasterizationOptions.setAutomaticLayoutsScaling(true);
            rasterizationOptions.setNoScaling(false);
            pdfOptions.setVectorRasterizationOptions(rasterizationOptions);
            image.save(fileOut.getAbsolutePath(), pdfOptions);
        } finally {
            image.dispose();
        }
        return fileOut.exists() && fileOut.length() > 0 ? fileOut : null;
    }
}