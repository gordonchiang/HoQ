package ca.uvic.hoq;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.llp.LowerLayerProtocol;
import ca.uhn.hl7v2.hoh.llp.Hl7OverHttpLowerLayerProtocol;
import ca.uhn.hl7v2.hoh.util.ServerRoleEnum;
import ca.uhn.hl7v2.app.SimpleServer;
import ca.uhn.hl7v2.app.DefaultApplication;
import ca.uhn.hl7v2.protocol.ReceivingApplication;

public class Http1Server {

	private static final String HOST = "localhost";
	private static final String URI = "/";
	private static final int PORT_NUMBER = 8888;

	private static HapiContext context = new DefaultHapiContext();

	public static void main(String[] args) throws Exception {
		
		System.out.println("Hello world!");
		
		/*
		 * Sending a message with HAPI and HL7 over HTTP. First
		 * an LLP instance is created. Note that you must tell
		 * the LLP class whether it will be used in a client
		 * or a server.
		 */
		LowerLayerProtocol llp;
		llp = new Hl7OverHttpLowerLayerProtocol(ServerRoleEnum.SERVER);

		/* 
		 * Create the server, and pass in the HoH LLP instance
		 * 
		 * Note that the HoH LLP implementation will not
		 * work in two-socket servers
		 */
		PipeParser parser = PipeParser.getInstanceWithNoValidation();
		SimpleServer server = new SimpleServer(PORT_NUMBER, llp, parser);

		// Register an application to the server, and start it
		// You are now ready to receive HL7 messages!
		server.registerApplication("*", "*", (ReceivingApplication)new DefaultApplication());
		server.start();
	}

}
