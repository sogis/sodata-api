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
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@EnableScheduling
@Configuration
public class SodataApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SodataApiApplication.class, args);
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
    
    // Damit STAC mit stac browser funktioniert.
    // TODO: Wahrscheinlich reicht GET?
    @Bean
    WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedOrigins("*")
                .allowedHeaders("*");
            }
        };
    }

    // Anwendung ist fertig gestartet: live aber nicht ready.
    @Bean
    CommandLineRunner init(IlidataConfigService ilidataConfigService, StacConfigService stacConfigService) {
        return args -> {
            ilidataConfigService.createFile();
            stacConfigService.createFiles();
        };
    }

}
