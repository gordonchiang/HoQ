package ca.uvic.hoq;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.v24.message.ADT_A01;
import ca.uhn.hl7v2.model.v24.segment.EVN;
import ca.uhn.hl7v2.model.v24.segment.MSH;
import ca.uhn.hl7v2.model.v24.segment.PID;
import ca.uhn.hl7v2.model.v24.segment.PV1;
import ca.uhn.hl7v2.parser.Parser;

public class Helpers {

  private final static HapiContext context = new DefaultHapiContext();
  private final static Parser parser = context.getPipeParser();

  public static String encodeMessage(Message message) {
    // Encode the message
    String encodedMessage;
    try {
      encodedMessage = parser.encode(message);
      System.out.println("HL7 message: " + encodedMessage);
    } catch (HL7Exception e) {
      System.out.println("Error encoding HL7 message: " + e);
      System.exit(1);
      return null;
    }

    return encodedMessage;
  }

  public static ADT_A01 generateADTA01Message() {
    ADT_A01 adt;
    try {
      // Create HL7 message
      adt = new ADT_A01();
      adt.initQuickstart("ADT", "AO1", "P");

      final String currentTimestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

      // Populate the EVN segment
      EVN evn = adt.getEVN();
      evn.getEventTypeCode().setValue("AO1");
      evn.getRecordedDateTime().getTimeOfAnEvent().setValue(currentTimestamp);

      // Populate the MSH Segment
      MSH mshSegment = adt.getMSH();
      mshSegment.getFieldSeparator().setValue("|");
      mshSegment.getEncodingCharacters().setValue("^~\\&");
      mshSegment.getSendingApplication().getNamespaceID().setValue("HTTP Client");
      mshSegment.getSendingFacility().getNamespaceID().setValue("Test Client Machine");
      mshSegment.getReceivingApplication().getNamespaceID().setValue("HTTP Server");
      mshSegment.getReceivingFacility().getNamespaceID().setValue("Test Server Machine");
      mshSegment.getDateTimeOfMessage().getTimeOfAnEvent().setValue(currentTimestamp);
      mshSegment.getMessageControlID().setValue("123");
      mshSegment.getSequenceNumber().setValue("123");
      mshSegment.getVersionID().getVersionID().setValue("2.4");

      // Populate the PID Segment
      PID pid = adt.getPID();
      pid.getPatientName(0).getFamilyName().getSurname().setValue("Doe");
      pid.getPatientName(0).getGivenName().setValue("John");
      pid.getPatientIdentifierList(0).getID().setValue("123456");
      pid.getPatientAddress(0).getStreetAddress().getStreetOrMailingAddress().setValue("123 Street");
      pid.getPatientAddress(0).getCity().setValue("Victoria");
      pid.getPatientAddress(0).getStateOrProvince().setValue("BC");
      pid.getPatientAddress(0).getCountry().setValue("Canada");

      // Populate the PV1 segment
      PV1 pv1 = adt.getPV1();
      pv1.getPatientClass().setValue("N");
      pv1.getAssignedPatientLocation().getFacility().getNamespaceID().setValue("Hospital");
      pv1.getAssignedPatientLocation().getPointOfCare().setValue("Department");
      pv1.getAdmissionType().setValue("E");
      pv1.getReferringDoctor(0).getIDNumber().setValue("321");
      pv1.getReferringDoctor(0).getFamilyName().getSurname().setValue("Goose");
      pv1.getReferringDoctor(0).getGivenName().setValue("George");
      pv1.getAdmitDateTime().getTimeOfAnEvent().setValue(currentTimestamp);
    } catch (HL7Exception | IOException e) {
      System.out.println("Error generating ADT_A01 HL7 message: " + e);
      System.exit(1);
      return null;
    }

    return adt;
  }
}
