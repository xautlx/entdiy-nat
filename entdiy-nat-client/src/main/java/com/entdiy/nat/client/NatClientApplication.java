package com.entdiy.nat.client;

import com.entdiy.nat.client.config.NatClientConfigProperties;
import com.entdiy.nat.client.listener.NatClientListener;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@SpringBootApplication
@EnableConfigurationProperties(NatClientConfigProperties.class)
public class NatClientApplication implements ApplicationContextAware {

    @Autowired
    private NatClientConfigProperties config;

    private NatClientListener natClientListener = new NatClientListener();


    public static void main(String[] args) {
        SpringApplication.run(NatClientApplication.class, args);
    }

    @PostConstruct
    public void init() {
        ClientContext.setConfig(config);
        natClientListener.run();
    }

    @PreDestroy
    public void shutdown() {
        natClientListener.shutdown();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        //ContextUtil.setApplicationContext(applicationContext);
    }
}
