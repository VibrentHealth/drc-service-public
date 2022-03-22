package com.vibrent.drc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages = {"com.vibrent.drc", "com.vibrent.vxp.drc.resource", "com.vibrent.vrp.oidc"})
@SpringBootApplication
@EnableCaching
public class DrcServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(DrcServiceApplication.class, args);
  }

}
