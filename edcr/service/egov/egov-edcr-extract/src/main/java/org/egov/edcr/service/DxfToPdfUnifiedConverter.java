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
 * Single entry point for DXF→PDF: runs either the Kabeja pipeline or Aspose CAD, never both.
 * <p>
 * Configure via app config module {@link DcrConstants#APPLICATION_MODULE_TYPE}, key
 * {@link DcrConstants#DXF_TO_PDF_ENGINE}: {@code KABEJA} (default) or {@code ASPOSE}.
 * Aspose reads the original DXF file from disk ({@code dxfSourceFile}); in-memory layer tweaks
 * from the extract step apply only to the Kabeja path.
 * <p>
 * For Kabeja, {@link EdcrPdfDetail#getDirectSinglePdfKabejaRendering()} drives optional SVG/PDF tuning
 * (bounds, stroke, text) for the direct single-PDF flow only; legacy sheet configs leave it unset.
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
                // Direct single-PDF: tighter viewBox + thinner strokes (legacy keeps original margin / behaviour).
                boolean directSinglePdf = Boolean.TRUE.equals(edcrPdfDetail.getDirectSinglePdfKabejaRendering());
                if (directSinglePdf) {
                    // Signals DcrSvgGenerator / DcrSvgStyleGenerator to apply direct-only rendering fixes.
                    map.put(DcrSvgGenerator.PROPERTY_DIRECT_SINGLE_PDF, "true");
                    // Use header model limits so stray geometry does not shrink the drawing to a corner.
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