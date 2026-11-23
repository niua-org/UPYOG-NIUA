package org.egov.dx.adapter.vendor.gst;

import org.egov.dx.adapter.vendor.dto.VendorGstProfileResponse;

/**
 * Gateway adapter abstraction for GST verification and taxpayer profile lookup.
 *
 * <p>Enforces the <b>Dependency Inversion Principle (DIP)</b> and <b>Open/Closed Principle (OCP)</b>.
 * Downstream components depend purely on this abstraction, remaining completely decoupled from
 * underlying vendor transport mechanisms (mock vs. IRIS Taxpayer API).</p>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
public interface GstGatewayAdapter {

    /**
     * Searches taxpayer profile and returns vendor response payload.
     *
     * @param gstin 15-character alphanumeric GSTIN
     * @return {@link VendorGstProfileResponse} containing raw vendor profile details
     */
    VendorGstProfileResponse searchGstin(String gstin);
}
