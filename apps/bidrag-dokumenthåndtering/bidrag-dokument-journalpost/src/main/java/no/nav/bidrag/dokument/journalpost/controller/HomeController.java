package no.nav.bidrag.dokument.journalpost.controller;

import no.nav.security.token.support.core.api.Unprotected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@RestController
public class HomeController {

  private static final Logger LOGGER = LoggerFactory.getLogger(HomeController.class);

  @GetMapping("/")
  @Unprotected
  public ModelAndView index() {
    return new ModelAndView("index");
  }

}
