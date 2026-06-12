package upyog;


import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.egov.tracer.config.TracerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

@Import({ TracerConfiguration.class })
@SpringBootApplication
@ComponentScan(basePackages = { "upyog", "upyog.web.controllers" , "upyog.config"})
@EnableFeignClients(basePackages = "upyog.*")
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class EstateManagement {

    /**
	 * Configures the ShedLock LockProvider bean using JDBC.
	 * <p>
	 * This bean sets up ShedLock to use the application's shared database for acquiring and managing distributed locks.
	 * It uses Spring's JdbcTemplate and ensures all instances of the service check and update the same `shedlock` table,
	 * enabling only one instance to run a scheduled task at any given time.
	 * <p>
	 * The `.usingDbTime()` ensures that all instances rely on the database server's time to avoid clock drift issues
	 * across different machines.
	 *
	 * @param dataSource the shared application DataSource
	 * @return a configured LockProvider instance for ShedsLock
	 */
	@Bean
	public LockProvider lockProvider(DataSource dataSource) {
		return new JdbcTemplateLockProvider(
			JdbcTemplateLockProvider.Configuration.builder()
				.withJdbcTemplate(new JdbcTemplate(dataSource))
				.usingDbTime()
				.build()
		);
	}

    public static void main(String[] args) throws Exception {
        SpringApplication.run(EstateManagement.class, args);
    }

}
