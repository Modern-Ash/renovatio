package org.modernash.renovatio.generated.cobol;

/**
 * Screen handler for online program Cics001.
 */
public class Cics001CicsController {
  private final Cics001Service service;

  public Cics001CicsController(Cics001Service service) {
    this.service = service;
  }

  /**
   * Runs one task for {@code request} and returns the resulting screen and navigation.
   */
  public Cics001DTO.ScreenResponse handle(Cics001DTO.ScreenRequest request) {
    Cics001DTO input = new Cics001DTO();
    input.setScreenRequest(request);
    return service.process(input).getScreenResponse();
  }
}
