package com.webmini.miniweb.auth.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {
    private String issuer;
    private String secret;
    private Duration accessTtl;
    private Duration refreshTtl;
}

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
class JwtPropsEnabler {}