package org.egov.infra.microservice.models;

import lombok.Getter;
import org.hibernate.validator.constraints.SafeHtml;

public class Tutorial {

    @Getter
    private String id;

    @Getter
    @SafeHtml
    private String title;

    @Getter
    @SafeHtml
    private String video_url;

    @Getter
    @SafeHtml
    private String description;

    @Getter
    @SafeHtml
    private Boolean isActive;

    public Tutorial(String id, String title, String video_url, String description, Boolean isActive) {
        this.id = id;
        this.title = title;
        this.video_url = video_url;
        this.description = description;
        this.isActive = isActive;
    }

    public Tutorial() {

    }


    @Override
    public String toString() {
        return "Tutorial [id=" + id + ", title="+title + ", video_url=" + video_url + ", description=" + description + "isActive=" +  isActive + "]";
    }


}
