package ca.uvic.hoq;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.parser.Parser;
import ca.uvic.hoq.Http3Server.PartialResponse;
import io.quiche4j.Config;
import io.quiche4j.ConfigBuilder;
import io.quiche4j.Connection;
import io.quiche4j.http3.Http3;
import io.quiche4j.http3.Http3Config;
import io.quiche4j.http3.Http3ConfigBuilder;
import io.quiche4j.http3.Http3Connection;
import io.quiche4j.http3.Http3EventListener;
import io.quiche4j.http3.Http3Header;
import io.quiche4j.Quiche;
import io.quiche4j.Utils;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v24.message.ADT_A01;
import ca.uhn.hl7v2.model.v24.segment.MSH;
import ca.uhn.hl7v2.model.v24.segment.PID;
import ca.uhn.hl7v2.parser.Parser;

import ca.uvic.hoq.Helpers;

public class Http3Client {

  public static final int MAX_DATAGRAM_SIZE = 2048;

  public static final String CLIENT_NAME = "Quiche4j";
  public static final String CONTENT_TYPE = "application/hl7-v2; charset=UTF-8";

  public static void main(String[] args) throws UnknownHostException, IOException {
    // Parse arguments
    if (1 != args.length) {
      System.out.println("Usage: ./run.sh -c -u https://localhost:8888 -v 3");
      System.exit(1);
    }

    final String url = args[0];
    final URI uri;
    final int port;
    final InetAddress address;
    final String host;
    try {
      uri = new URI(url);
      port = uri.getPort();
      host = uri.getHost();
      address = InetAddress.getByName(host);
    } catch (URISyntaxException e) {
      System.out.println("Failed to parse URL " + url);
      System.exit(1);
      return;
    }
    
    final byte[] hl7_body = Helpers.encodeMessage(Helpers.generateADTA01Message()).getBytes();

    final Config config = new ConfigBuilder(Quiche.PROTOCOL_VERSION)
        .withApplicationProtos(Http3.APPLICATION_PROTOCOL)
        .withVerifyPeer(false)
        .loadCertChainFromPemFile(Utils.copyFileFromJAR("certs", "/cert.crt"))
        .loadPrivKeyFromPemFile(Utils.copyFileFromJAR("certs", "/cert.key"))
        .withMaxIdleTimeout(5_000)
        .withMaxUdpPayloadSize(MAX_DATAGRAM_SIZE)
        .withInitialMaxData(10_000_000)
        .withInitialMaxStreamDataBidiLocal(1_000_000)
        .withInitialMaxStreamDataBidiRemote(1_000_000)
        .withInitialMaxStreamDataUni(1_000_000)
        .withInitialMaxStreamsBidi(100)
        .withInitialMaxStreamsUni(100)
        .withDisableActiveMigration(false)
        .build();

    final byte[] connId = Quiche.newConnectionId();
    final Connection conn = Quiche.connect(host, connId, config);

    int len = 0;
    final byte[] buffer = new byte[MAX_DATAGRAM_SIZE];
    len = conn.send(buffer);
    if (len < 0 && len != Quiche.ErrorCode.DONE) {
      System.out.println("! handshake init problem " + len);
      System.exit(1);
      return;
    }
    
    Long streamId = null;
    final AtomicBoolean reading = new AtomicBoolean(true);
    final Http3Config h3Config = new Http3ConfigBuilder().build();
    DatagramPacket packet;
    Http3Connection h3Conn = null;

    final DatagramPacket handshakePacket = new DatagramPacket(buffer, len, address, port);
    final DatagramSocket socket = new DatagramSocket(0);
    socket.setSoTimeout(1);
    socket.send(handshakePacket);

    while (!conn.isClosed()) {
      // READING LOOP
      while (reading.get()) {
        packet = new DatagramPacket(buffer, buffer.length);
        try {
          socket.receive(packet);
          final int recvBytes = packet.getLength();

          final int read = conn.recv(Arrays.copyOfRange(packet.getData(), packet.getOffset(), recvBytes));
          if (read < 0 && read != Quiche.ErrorCode.DONE) {
            System.out.println("> conn.recv failed " + read);
            reading.set(false);
          }
        } catch (SocketTimeoutException e) {
          conn.onTimeout();
          reading.set(false);
        }

        // POLL
        if (null != h3Conn) {
          final Http3Connection h3c = h3Conn;
          streamId = h3c.poll(new Http3EventListener() {
            public void onHeaders(long streamId, List<Http3Header> headers, boolean hasBody) {}

            public void onData(long streamId) {
              final int bodyLength = h3c.recvBody(streamId, buffer);
              if (bodyLength < 0 && bodyLength != Quiche.ErrorCode.DONE) {
                System.out.println("! recv body failed " + bodyLength);
              } else {
                final byte[] body = Arrays.copyOfRange(buffer, 0, bodyLength);
                System.out.println("Response: " + new String(body, StandardCharsets.UTF_8));

                // Close connection immediately after receiving ACK to HL7 message
                conn.close(true, 0x00, "kthxbye");
                reading.set(false);
              }
            }

            public void onFinished(long streamId) {}
          });

          if (streamId < 0 && streamId != Quiche.ErrorCode.DONE) {
            System.out.println("> poll failed " + streamId);
            reading.set(false);
            break;
          }

          if (Quiche.ErrorCode.DONE == streamId)
            reading.set(false);
        }
      }

      if (conn.isClosed()) {
        socket.close();
        System.exit(1);
        return;
      }

      if (conn.isEstablished() && null == h3Conn) {
        h3Conn = Http3Connection.withTransport(conn, h3Config);

        List<Http3Header> req = new ArrayList<>();
        req.add(new Http3Header(":method", "POST"));
        req.add(new Http3Header(":scheme", uri.getScheme()));
        req.add(new Http3Header(":authority", uri.getAuthority()));
        req.add(new Http3Header(":path", uri.getPath()));
        req.add(new Http3Header("user-agent", CLIENT_NAME));
        req.add(new Http3Header("content-type", CONTENT_TYPE));
        req.add(new Http3Header("content-length", Integer.toString(hl7_body.length)));

        streamId = h3Conn.sendRequest(req, false);
        final long written = h3Conn.sendBody(streamId, hl7_body, true);
        if (written < 0) {
          System.out.println("! h3 send body failed " + written);
          return;
        }
      }

      // WRITING LOOP
      while (true) {
        len = conn.send(buffer);
        if (len < 0 && len != Quiche.ErrorCode.DONE) {
          System.out.println("! conn.send failed " + len);
          break;
        }
        if (len <= 0)
          break;

        packet = new DatagramPacket(buffer, len, address, port);
        socket.send(packet);
      }

      if (conn.isClosed()) {
        socket.close();
        System.exit(1);
        return;
      }

      reading.set(true);
    }

    socket.close();
  }

}
