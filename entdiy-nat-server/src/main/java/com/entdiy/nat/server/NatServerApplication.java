package com.entdiy.nat.server;

import com.entdiy.nat.server.config.NatServerConfigProperties;
import com.entdiy.nat.server.listener.NatServerListener;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.PostConstruct;

@SpringBootApplication
@EnableConfigurationProperties(NatServerConfigProperties.class)
public class NatServerApplication implements ApplicationContextAware {

    @Autowired
    private NatServerConfigProperties config;

    private NatServerListener natServerListener = new NatServerListener();

    public static void main(String[] args) {
        SpringApplication.run(NatServerApplication.class, args);
    }

    @PostConstruct
    public void init() {
        ServerContext.setConfig(config);
        natServerListener.run();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ServerContext.setApplicationContext(applicationContext);
    }
}
