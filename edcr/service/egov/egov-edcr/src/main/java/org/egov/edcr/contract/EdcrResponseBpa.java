package org.egov.edcr.contract;

import java.util.List;

import org.egov.infra.microservice.contract.ResponseInfo;

public class EdcrResponseBpa {

    private ResponseInfo responseInfo;

    private List<EdcrDetailBpa> edcrDetail;

    private int count;

    public ResponseInfo getResponseInfo() {
        return responseInfo;
    }

    public void setResponseInfo(ResponseInfo responseInfo) {
        this.responseInfo = responseInfo;
    }

    public List<EdcrDetailBpa> getEdcrDetail() {
        return edcrDetail;
    }

    public void setEdcrDetail(List<EdcrDetailBpa> edcrDetail) {
        this.edcrDetail = edcrDetail;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "EdcrResponse [responseInfo=" + responseInfo + ", edcrDetail=" + edcrDetail + "]";
    }

}
