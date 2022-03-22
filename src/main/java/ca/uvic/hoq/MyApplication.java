package ca.uvic.hoq;

import java.io.IOException;
import java.util.Map;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.validation.ValidationContext;
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.AcknowledgmentCode;
import ca.uhn.hl7v2.protocol.ReceivingApplication;
import ca.uhn.hl7v2.protocol.ReceivingApplicationException;

public class MyApplication implements ReceivingApplication {

  private static final HapiContext context = new DefaultHapiContext();
  private static final PipeParser parser = context.getPipeParser();

  /**
   * processMessage is fired each time a new message arrives.
   * 
   * @param theMessage  The message which was received
   * @param theMetadata A map containing additional information about the message,
   *                    where it came from, etc.
   */
  public Message processMessage(Message theMessage, Map theMetadata)
      throws ReceivingApplicationException, HL7Exception {
    System.out.println("Received HL7 message: " + theMessage.encode());

    boolean somethingFailed = false;

    // Process the message (validate the message)
    context.setValidationContext((ValidationContext) ValidationContextFactory.defaultValidation());
    try {
      String msg = parser.encode(theMessage);
      parser.parse(msg);
    } catch (HL7Exception e) {
      somethingFailed = true;
    }

    // Generate response
    Message response;
    try {
      response = theMessage.generateACK();
    } catch (IOException e) {
      throw new ReceivingApplicationException(e);
    }

    // Respond with AE response code and HTTP 500 status code
    if (somethingFailed) {
      try {
        response = theMessage.generateACK(AcknowledgmentCode.AE, new HL7Exception("MyApplication experienced an error when processing the input HL7 message"));
      } catch (IOException e) {
        throw new ReceivingApplicationException(e);
      }
    }

    System.out.println("Response: " + response);

    return response;
  }

  public boolean canProcess(Message theMessage) {
    return true;
  }

}
