package ru.dima.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BotProperties.class)
public class Config {

//    @Bean
//    @ConditionalOnProperty(prefix = "bot")
//    public BotProperties botProperties() {
//        return new BotProperties();
//    }
}
