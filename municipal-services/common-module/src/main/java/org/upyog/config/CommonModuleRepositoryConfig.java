package org.upyog.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.upyog.repository.CommonDetailRepositoryInterface;
import org.upyog.repository.impl.StreetVendingDetailRepository;

@Configuration
public class CommonModuleRepositoryConfig {

	@Bean
	public List<CommonDetailRepositoryInterface> moduleRepositories(

			StreetVendingDetailRepository streetVendingDetailRepository) {
		return Arrays.asList(streetVendingDetailRepository);
	}
}
