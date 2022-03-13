package ca.uvic.hoq;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v24.message.ADT_A01;
import ca.uhn.hl7v2.model.v24.segment.MSH;
import ca.uhn.hl7v2.model.v24.segment.PID;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.hoh.hapi.client.HohClientSimple;
import ca.uhn.hl7v2.hoh.sockets.CustomCertificateTlsSocketFactory;
import ca.uhn.hl7v2.hoh.api.ISendable;
import ca.uhn.hl7v2.hoh.api.IReceivable;
import ca.uhn.hl7v2.hoh.hapi.api.MessageSendable;
import ca.uhn.hl7v2.hoh.api.MessageMetadataKeys;

import io.quiche4j.Utils;

public class Http1Client {

	private static HapiContext context = new DefaultHapiContext();

	public static void main(String[] args) throws Exception {
		// Parse arguments
        if (2 != args.length) {
            System.out.println("Usage: ./http1.sh -c -t -u https://localhost:8888");
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

		// Create HL7 message
		ADT_A01 adt = new ADT_A01();
		adt.initQuickstart("ADT", "A01", "P");

		// Populate the MSH Segment
		MSH mshSegment = adt.getMSH();
		mshSegment.getSendingApplication().getNamespaceID().setValue("TestSendingSystem");
		mshSegment.getSequenceNumber().setValue("123");

		// Populate the PID Segment
		PID pid = adt.getPID();
		pid.getPatientName(0).getFamilyName().getSurname().setValue("Doe");
		pid.getPatientName(0).getGivenName().setValue("John");
		pid.getPatientIdentifierList(0).getID().setValue("123456");

		/*
		 * In a real situation, of course, many more segments and fields would be
		 * populated
		 */

		// Encode the message
		HapiContext context = new DefaultHapiContext();
		Parser parser = context.getPipeParser();
		String encodedMessage = parser.encode(adt);
		System.out.println("HL7 message: " + encodedMessage);

		HohClientSimple client = new HohClientSimple(host, port, "/", parser);

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
		IReceivable<Message> receivable = client.sendAndReceiveMessage(sendable);

		// receivable.getRawMessage() provides the response
		Message message = receivable.getMessage();
		System.out.println("Response: " + message.encode());
	}

}
