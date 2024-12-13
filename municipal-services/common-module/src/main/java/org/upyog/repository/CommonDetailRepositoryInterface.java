package org.upyog.repository;

import org.upyog.web.models.CommonDetails;

public interface CommonDetailRepositoryInterface {

	String getModuleName(); // To identify the module
    CommonDetails findApplicationDetails(String applicationNumber);
    
}
