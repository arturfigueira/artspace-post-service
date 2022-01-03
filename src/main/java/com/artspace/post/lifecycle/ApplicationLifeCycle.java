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
    log.info(".______     ______        _______.___________.    _______.        ___      .______    __");
    log.info("|   _  \\   /  __  \\      /       |           |   /       |       /   \\     |   _  \\  |  |");
    log.info("|  |_)  | |  |  |  |    |   (----`---|  |----`  |   (----`      /  ^  \\    |  |_)  | |  |");
    log.info("|   ___/  |  |  |  |     \\   \\       |  |        \\   \\         /  /_\\  \\   |   ___/  |  |");
    log.info("|  |      |  `--'  | .----)   |      |  |    .----)   |       /  _____  \\  |  |      |  |");
    log.info("| _|       \\______/  |_______/       |__|    |_______/       /__/     \\__\\ | _|      |__|");

    log.info("The application "+APP_NAME+" is starting with profile " + ProfileManager.getActiveProfile());
  }

  void onStop(@Observes ShutdownEvent ev) {
    log.info("The application "+APP_NAME+" is stopping...");
  }
}
