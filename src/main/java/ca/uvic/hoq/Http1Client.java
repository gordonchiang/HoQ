package ca.uvic.hoq;

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

	private static final String HOST = "localhost";
	private static final String URI = "/";
	private static final int PORT_NUMBER = 8888;

	private static HapiContext context = new DefaultHapiContext();

	public static void main(String[] args) throws Exception {

		final boolean enableTLS = args.length > 0 && args[0].equals("--tls") ? true : false;

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

		// Now, let's encode the message and look at the output
		HapiContext context = new DefaultHapiContext();
		Parser parser = context.getPipeParser();
		String encodedMessage = parser.encode(adt);
		System.out.println("Printing ER7 Encoded Message:");
		System.out.println(encodedMessage);

		/*
		 * Prints:
		 * 
		 * MSH|^~\&|TestSendingSystem||||200701011539||ADT^A01^ADT A01||||123
		 * PID|||123456||Doe^John
		 */

		HohClientSimple client = new HohClientSimple(HOST, PORT_NUMBER, URI, parser);

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

		// receivavle.getRawMessage() provides the response
		Message message = receivable.getMessage();
		System.out.println("Response was:\n" + message.encode());

		// IReceivable also stores metadata about the message
		String remoteHostIp = (String) receivable.getMetadata().get(MessageMetadataKeys.REMOTE_HOST_ADDRESS);
		System.out.println("From:\n" + remoteHostIp);
	}

}
