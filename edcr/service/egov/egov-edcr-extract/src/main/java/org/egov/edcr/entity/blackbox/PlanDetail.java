package org.egov.edcr.entity.blackbox;

import org.egov.common.entity.edcr.Plan;
import org.kabeja.dxf.DXFDocument;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.File;

/**
 * Parsed plan holder: extends Plan with an in-memory DXF document and an optional original DXF file on disk
 * (the file is used by the Aspose PDF path; the document is used by Kabeja).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlanDetail extends Plan {

    private static final long serialVersionUID = 76L;

    /** Original DXF on disk (used by Aspose path); not serialized. */
    @JsonIgnore
    private transient File dxfSourceFile;

    @JsonIgnore
    private DXFDocument doc;

    public File getDxfSourceFile() {
        return dxfSourceFile;
    }
    public void setDxfSourceFile(File dxfSourceFile) {
        this.dxfSourceFile = dxfSourceFile;
    }

    public DXFDocument getDoc() {
        return doc;
    }

    public void setDoc(DXFDocument doc) {
        this.doc = doc;
    }

    @JsonIgnore
    public DXFDocument getDxfDocument() {
        return doc;
    }

}
