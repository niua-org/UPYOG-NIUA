package org.egov.infra.admin.master.service;

import java.util.List;
import java.util.Map;

import org.egov.infra.admin.master.entity.City;

public interface ICityService {

    City updateCity(City city);
    City getCityByURL(String url);
    City getCityByName(String cityName);
    City getCityByCode(String code);
    City fetchStateCityDetails();
	Map<String, Object> cityDataAsMap();
	List<City> findAll();
	Object getCityLogoURLByCurrentTenant();
	byte[] getCityLogoAsBytes();
	
}