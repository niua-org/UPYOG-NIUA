package org.egov.edcr.service;

import org.egov.edcr.utility.DcrConstants;
import org.egov.infra.admin.master.entity.AppConfigValues;
import org.egov.infra.admin.master.service.AppConfigValueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConfigCacheService {

    @Autowired
    private AppConfigValueService appConfigValueService;

    private Boolean unitLayerEnabled;

    public boolean isUnitLayerEnabled() {

        if (unitLayerEnabled != null) {
            return unitLayerEnabled;
        }

        List<AppConfigValues> appConfigValues =
                appConfigValueService.getConfigValuesByModuleAndKey(
                        DcrConstants.APPLICATION_MODULE_TYPE,
                        DcrConstants.FLOOR_UNIT_LAYER_ENABLED);

        unitLayerEnabled = appConfigValues != null
                && !appConfigValues.isEmpty()
                && DcrConstants.YES.equalsIgnoreCase(appConfigValues.get(0).getValue());

        return unitLayerEnabled;
    }
}