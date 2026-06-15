package org.egov.edcr.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.egov.common.entity.edcr.LayerName;
import org.egov.commons.service.LayerNameService;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import javax.annotation.PreDestroy;
import org.springframework.context.event.ContextRefreshedEvent;

@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
@Service
public class LayerNames {

    private final Map<String, String> layerNamesMap = new HashMap<>();
    private final LayerNameService layerNameService;

    public LayerNames(LayerNameService layerNameService) {
        this.layerNameService = layerNameService;
    }

     /**
     * Loads all layer name mappings into the in-memory cache when the Spring
     * application context is fully initialized.
     * <p>
     * This method is triggered automatically on {@link ContextRefreshedEvent}
     * and populates {@code layerNamesMap} with data retrieved from the database
     * to avoid repeated database lookups during runtime.
     * </p>
     */
    @EventListener(ContextRefreshedEvent.class)
    private void loadLayerNames() {
        if(layerNamesMap.isEmpty()) {
            List<LayerName> layerNames = layerNameService.findAll();
            for (LayerName l : layerNames)
                layerNamesMap.put(l.getKey(), l.getValue());
        }
    }

    @PreDestroy
    public void destroy() {
        layerNamesMap.clear();
    }

    public String getLayerName(String layerNameKey) {
        return layerNamesMap.get(layerNameKey);
    }

}