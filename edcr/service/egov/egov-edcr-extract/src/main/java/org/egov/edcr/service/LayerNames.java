package org.egov.edcr.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.egov.common.entity.edcr.LayerName;
import org.egov.commons.service.LayerNameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
@Service
public class LayerNames {

    private Map<String, String> layerNamesMap = null; 
    @Autowired
    public LayerNameService layerNameService;

    @Autowired
    public LayerNames(LayerNameService layerNameService) {
        this.layerNameService = layerNameService;
       
    }

    /**
     * Lazily loads and caches layer name mappings from the database.
     *
     * <p>The layer names are fetched only once and stored in an in-memory map
     * for fast subsequent lookups. This method uses double-checked locking to
     * ensure that the cache is initialized only once in a thread-safe manner.</p>
     *
     * <p>If the cache has already been initialized, the method returns
     * immediately without performing any database access.</p>
     */
    private void loadIfNeeded() {
        if (layerNamesMap == null) {
            synchronized (this) {
                if (layerNamesMap == null) {
                    Map<String, String> map = new HashMap<>();
                    List<LayerName> layerNames = layerNameService.findAll();
                    for (LayerName l : layerNames)
                        map.put(l.getKey(), l.getValue());
                    layerNamesMap = map;
                }
            }
        }
    }

    public String getLayerName(String layerNameKey) {
        loadIfNeeded(); // ← loads on first real request
        return layerNamesMap.get(layerNameKey);
    }

    public void setLayerNameService(LayerNameService layerNameService) {
        this.layerNameService = layerNameService;
    }
}