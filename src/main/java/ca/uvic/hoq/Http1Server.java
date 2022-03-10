package ca.uvic.hoq;

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

	private static final String HOST = "localhost";
	private static final String URI = "/";
	private static final int PORT_NUMBER = 8888;

	private static HapiContext context = new DefaultHapiContext();

	public static void main(String[] args) throws Exception {

		final boolean enableTLS = args.length > 0 && args[0].equals("--tls") ? true : false;

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

		/*
		 * Sending a message with HAPI and HL7 over HTTP. First an LLP instance is
		 * created. Note that you must tell the LLP class whether it will be used in a
		 * client or a server.
		 */
		LowerLayerProtocol llp = new Hl7OverHttpLowerLayerProtocol(ServerRoleEnum.SERVER);

		// Create the server
		PipeParser parser = PipeParser.getInstanceWithNoValidation();
		context.setLowerLayerProtocol(llp);
		HL7Service server = enableTLS ? context.newServer(PORT_NUMBER, enableTLS) // Start a server listening at port with a pipe parser
				: new SimpleServer(PORT_NUMBER, llp, parser); // Start a server with the llp and parser
		;

		// Register an application to the server and start it
		server.registerApplication("*", "*", new MyApplication());
		server.start();
	}

}
