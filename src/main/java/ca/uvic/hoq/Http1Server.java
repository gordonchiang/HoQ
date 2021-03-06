package ca.uvic.hoq;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.hoh.sockets.CustomCertificateTlsSocketFactory;
import ca.uhn.hl7v2.hoh.util.HapiSocketTlsFactoryWrapper;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.llp.LowerLayerProtocol;
import ca.uhn.hl7v2.hoh.llp.Hl7OverHttpLowerLayerProtocol;
import ca.uhn.hl7v2.hoh.util.ServerRoleEnum;
import ca.uhn.hl7v2.app.SimpleServer;
import ca.uhn.hl7v2.app.HL7Service;

import ca.uvic.hoq.MyApplication;

import io.quiche4j.Utils;

public class Http1Server {

  private static HapiContext context = new DefaultHapiContext();

  public static void main(String[] args) throws Exception {
    // Parse arguments
    if (2 != args.length) {
      System.out.println("Usage: ./run.sh -s -u 8888 -v 1 -t");
      System.exit(1);
    }
    
    final String url = args[0];
    final URI uri;
    final int port;
    final InetAddress address;
    final String host;
    if (!url.contains(":")) {
      port = Integer.parseInt(args[0]);
    } else {
      try {
        uri = new URI(url);
        port = uri.getPort();
      } catch (URISyntaxException e) {
        System.out.println("Failed to parse URL " + url);
        System.exit(1);
        return;
      }
    }

    final boolean enableTLS = args[1].equals("true") ? true : false;

    HapiSocketTlsFactoryWrapper hapiSocketFactory = null;
    if (enableTLS) {
      // Create a socket factory which references the keystore
      CustomCertificateTlsSocketFactory serverSocketFactory = new CustomCertificateTlsSocketFactory();
      serverSocketFactory.setKeystoreFilename(Utils.copyFileFromJAR("keystore", "/keystore.pkcs12"));
      serverSocketFactory.setKeystorePassphrase("hoqpassword");

      // The socket factory needs to be wrapped for use in HAPI
      hapiSocketFactory = new HapiSocketTlsFactoryWrapper(serverSocketFactory);
      context.setSocketFactory(hapiSocketFactory);
    }

    // Create an LLP instance
    LowerLayerProtocol llp = new Hl7OverHttpLowerLayerProtocol(ServerRoleEnum.SERVER);

    // Create the server
    PipeParser parser = PipeParser.getInstanceWithNoValidation();
    context.setLowerLayerProtocol(llp);
    HL7Service server = enableTLS ? context.newServer(port, enableTLS) : new SimpleServer(port, llp, parser);

    // Register an application to the server and start it
    server.registerApplication("*", "*", new MyApplication());
    server.start();
  }

}
