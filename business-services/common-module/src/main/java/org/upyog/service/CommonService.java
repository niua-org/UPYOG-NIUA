package org.upyog.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.upyog.repository.CommonDetailRepositoryInterface;
import org.upyog.web.models.CommonDetails;

@Service
public class CommonService {

	private final Map<String, CommonDetailRepositoryInterface> moduleRepositories;

	public CommonService(List<CommonDetailRepositoryInterface> repositories) {
		// Map each module to its respective repository
		this.moduleRepositories = repositories.stream()
				.collect(Collectors.toMap(CommonDetailRepositoryInterface::getModuleName, repo -> repo));
	}

	public CommonDetails getApplicationCommonDetails(String moduleName, String applicationNumber) {
		// Get the appropriate repository for the module
		CommonDetailRepositoryInterface repository = moduleRepositories.get(moduleName);
		if (repository == null) {
			throw new IllegalArgumentException("Invalid module name: " + moduleName);
		}
		return repository.findApplicationDetails(applicationNumber);
	}
}
