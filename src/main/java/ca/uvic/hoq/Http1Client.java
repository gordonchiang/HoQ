package ca.uvic.hoq;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v24.message.ADT_A01;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.hoh.hapi.client.HohClientSimple;
import ca.uhn.hl7v2.hoh.sockets.CustomCertificateTlsSocketFactory;
import ca.uhn.hl7v2.hoh.api.ISendable;
import ca.uhn.hl7v2.hoh.api.IReceivable;
import ca.uhn.hl7v2.hoh.hapi.api.MessageSendable;
import ca.uhn.hl7v2.hoh.api.MessageMetadataKeys;

import ca.uvic.hoq.Helpers;

import io.quiche4j.Utils;

public class Http1Client {

  private final static HapiContext context = new DefaultHapiContext();
  private final static Parser parser = context.getPipeParser();
  private static int countResponses = 0;

  public static void main(String[] args) throws Exception {
    // Parse arguments
    if (2 != args.length) {
      System.out.println("Usage: ./run.sh -s -u https://localhost:8888 -v 1 -t");
      System.exit(1);
    }

    final String url = args[0];
    final URI uri;
    final int port;
    final String host;
    try {
      uri = new URI(url);
      port = uri.getPort();
      host = uri.getHost();
    } catch (URISyntaxException e) {
      System.out.println("Failed to parse URL " + url);
      System.exit(1);
      return;
    }

    final boolean enableTLS = args[1].equals("true") ? true : false;

    final ADT_A01 adt = Helpers.generateADTA01Message();
    Helpers.encodeMessage(adt);

    HohClientSimple client = new HohClientSimple(host, port, "/", parser);
    client.setAutoClose(false);

    if (enableTLS) {
      // Assign a socket factory which references the keystore
      CustomCertificateTlsSocketFactory clientSocketFactory = new CustomCertificateTlsSocketFactory();
      clientSocketFactory.setKeystoreFilename(Utils.copyFileFromJAR("keystore", "/keystore.pkcs12"));
      clientSocketFactory.setKeystorePassphrase("hoqpassword");
      client.setSocketFactory(clientSocketFactory);
    }

    // The MessageSendable provides the message to send
    ISendable sendable = new MessageSendable(adt);

    // sendAndReceive actually sends the message
    IReceivable<Message> receivable = null;
    Message message = null;
    for (int i = 0; i < 10; i++) {
      receivable = client.sendAndReceiveMessage(sendable);
    }
    
    while(countResponses < 10) {
      // receivable.getRawMessage() provides the response
      message = receivable.getMessage();
      System.out.println("Response: " + message.encode());
      
      countResponses++;
    }
    
    client.close();
    
  }

}
