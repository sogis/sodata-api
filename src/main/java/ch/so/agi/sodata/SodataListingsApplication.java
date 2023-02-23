package ch.so.agi.sodata;

import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.filter.ForwardedHeaderFilter;

@SpringBootApplication
@EnableScheduling
@Configuration
public class SodataListingsApplication {

	public static void main(String[] args) {
		SpringApplication.run(SodataListingsApplication.class, args);
	}
	
    @Bean
    ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    } 
    
    @Bean 
    HttpClient createHttpClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(Version.HTTP_1_1)
                .build();
        return httpClient;
    }

    // Anwendung ist fertig gestartet: live aber nicht ready.
    @Bean
    CommandLineRunner init(ConfigService configService) {
        return args -> {
            configService.parse();
        };
    }

}
