package com.artspace.post.lifecycle;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.ProfileManager;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
class ApplicationLifeCycle {

  private static final String APP_NAME = "POSTS";

  void onStart(@Observes StartupEvent ev) {
    final StringBuilder appName = new StringBuilder("\n");
    appName
        .append(".______     ______        _______.___________.    _______.        ___      .______    __").append("\n")
        .append("|   _  \\   /  __  \\      /       |           |   /       |       /   \\     |   _  \\  |  |").append("\n")
        .append("|  |_)  | |  |  |  |    |   (----`---|  |----`  |   (----`      /  ^  \\    |  |_)  | |  |").append("\n")
        .append("|   ___/  |  |  |  |     \\   \\       |  |        \\   \\         /  /_\\  \\   |   ___/  |  |").append("\n")
        .append("|  |      |  `--'  | .----)   |      |  |    .----)   |       /  _____  \\  |  |      |  |").append("\n")
        .append("| _|       \\______/  |_______/       |__|    |_______/       /__/     \\__\\ | _|      |__|");

    log.info(appName.toString());
    log.info("The application "+APP_NAME+" is starting with profile " + ProfileManager.getActiveProfile());
  }

  void onStop(@Observes ShutdownEvent ev) {
    log.info("The application "+APP_NAME+" is stopping...");
  }
}
