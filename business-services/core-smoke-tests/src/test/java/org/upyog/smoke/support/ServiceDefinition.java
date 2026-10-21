package org.upyog.smoke.support;

import java.util.List;
import java.util.Map;

public class ServiceDefinition {

    private String name;
    private String basePath;
    private String healthPath;
    private String versionPath;
    private List<String> dependsOn;
    private Map<String, Object> coreRead;
    private Map<String, Object> coreWrite;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getHealthPath() {
        return healthPath;
    }

    public void setHealthPath(String healthPath) {
        this.healthPath = healthPath;
    }

    public String getVersionPath() {
        return versionPath;
    }

    public void setVersionPath(String versionPath) {
        this.versionPath = versionPath;
    }

    public List<String> getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(List<String> dependsOn) {
        this.dependsOn = dependsOn;
    }

    public Map<String, Object> getCoreRead() {
        return coreRead;
    }

    public void setCoreRead(Map<String, Object> coreRead) {
        this.coreRead = coreRead;
    }

    public Map<String, Object> getCoreWrite() {
        return coreWrite;
    }

    public void setCoreWrite(Map<String, Object> coreWrite) {
        this.coreWrite = coreWrite;
    }
}
