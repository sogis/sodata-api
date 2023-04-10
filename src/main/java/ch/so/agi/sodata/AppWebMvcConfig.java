package ch.so.agi.sodata;

import java.io.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AppWebMvcConfig implements WebMvcConfigurer {
    @Value("${app.ilidataDir}")
    private String ilidataDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // File.separator wird benötigt, weil tmpdir im Dockerimage diesen im Gegensatz zu macOS
        // weglässt (auch wenn explizit gesetzt) und Spring Boot diesen bei einer
        // Verzeichnisangabe explizit verlangt.
        registry.addResourceHandler("/ilidata.xml").addResourceLocations("file:"+ilidataDir+File.separator);
    }
}
