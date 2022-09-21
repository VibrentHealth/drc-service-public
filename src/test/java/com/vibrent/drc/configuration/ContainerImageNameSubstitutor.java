package com.vibrent.drc.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

@Component
@Slf4j
public class ContainerImageNameSubstitutor extends ImageNameSubstitutor {

  private static String prefix = "harbor.ssk8s.vibrenthealth.com/dockerhub/";

  @Override
  public DockerImageName apply(DockerImageName original) {

    String repository = original.asCanonicalNameString();

    if (repository.contains("/")) {
      repository = String.format("%s%s",  prefix, repository );
    } else {
      repository = String.format("%slibrary/%s",  prefix, repository );
    }
    ;
    // convert the original name to something appropriate for
    // our build environment
    return DockerImageName.parse(
        // your code goes here - silly example of capitalising
        // the original name is shown
        repository
    );
  }

  @Override
  protected String getDescription() {
    // used in logs
    return "example image name substitutor";
  }
}
