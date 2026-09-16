package com.securefiles.config;

import com.securefiles.domain.file.port.out.AntivirusScanner;
import com.securefiles.infrastructure.clamav.ClamAvAntivirusScanner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ClamAvProperties.class)
public class ClamAvConfiguration {

    @Bean
    public AntivirusScanner antivirusScanner(ClamAvProperties properties) {
        return new ClamAvAntivirusScanner(
                properties.host(),
                properties.port(),
                Math.toIntExact(properties.connectTimeout().toMillis()),
                Math.toIntExact(properties.readTimeout().toMillis()),
                properties.chunkSize());
    }
}
