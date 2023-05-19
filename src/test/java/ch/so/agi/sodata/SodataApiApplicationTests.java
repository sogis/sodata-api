package ch.so.agi.sodata;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SodataApiApplicationTests {

    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    public void ilisite_Ok() throws IOException {
        String response = this.restTemplate.getForObject("http://localhost:" + port + "/ilisite.xml", String.class);
        assertTrue(response.contains("<shortDescription>Geodatenablage des Kantons Solothurn</shortDescription>"));
    }

    @Test
    public void ilimodels_Ok() throws IOException {
        String response = this.restTemplate.getForObject("http://localhost:" + port + "/ilimodels.xml", String.class);
        assertTrue(response.contains("<IliRepository09.RepositoryIndex BID=\"b1\">"));
        assertFalse(response.contains("IliRepository09.RepositoryIndex.ModelMetadata"));
    }
    @Test
    public void ilidata_Ok() throws IOException {
        String response = this.restTemplate.getForObject("http://localhost:" + port + "/ilidata.xml", String.class);
        assertTrue(response.contains("<id>2613.ch.so.agi.av.mopublic</id>"));
        assertTrue(response.contains("<id>ch.so.afu.abbaustellen</id>"));
    }
}
