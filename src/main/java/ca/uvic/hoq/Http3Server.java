package ca.uvic.hoq;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.quiche4j.Config;
import io.quiche4j.ConfigBuilder;
import io.quiche4j.Connection;
import io.quiche4j.http3.Http3;
import io.quiche4j.http3.Http3Config;
import io.quiche4j.http3.Http3ConfigBuilder;
import io.quiche4j.http3.Http3Connection;
import io.quiche4j.http3.Http3Header;
import io.quiche4j.http3.Http3EventListener;
import io.quiche4j.PacketHeader;
import io.quiche4j.PacketType;
import io.quiche4j.Quiche;
import io.quiche4j.Utils;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.protocol.ReceivingApplication;

import ca.uvic.hoq.MyApplication;
import ca.uvic.hoq.Helpers;

public class Http3Server {

  protected final static class PartialResponse {
    protected List<Http3Header> headers;
    protected byte[] body;
    protected long written;

    PartialResponse(List<Http3Header> headers, byte[] body, long written) {
      this.headers = headers;
      this.body = body;
      this.written = written;
    }
  }

  protected final static class Client {

    private final Connection conn;
    private Http3Connection h3Conn;
    private HashMap<Long, PartialResponse> partialResponses;
    private SocketAddress sender;

    public Client(Connection conn, SocketAddress sender) {
      this.conn = conn;
      this.sender = sender;
      this.h3Conn = null;
      this.partialResponses = new HashMap<>();
    }

    public final Connection connection() {
      return this.conn;
    }

    public final SocketAddress sender() {
      return this.sender;
    }

    public final Http3Connection http3Connection() {
      return this.h3Conn;
    }

    public final void setHttp3Connection(Http3Connection conn) {
      this.h3Conn = conn;
    }

  }

  private static final int MAX_DATAGRAM_SIZE = 2048;
  private static final String SERVER_NAME = "Quiche4j";
  private static final byte[] SERVER_NAME_BYTES = SERVER_NAME.getBytes();
  private static final int SERVER_NAME_BYTES_LEN = SERVER_NAME_BYTES.length;

  private static final String HEADER_NAME_STATUS = ":status";
  private static final String HEADER_NAME_SERVER = "server";
  private static final String HEADER_NAME_CONTENT_LENGTH = "content-length";

  private static final HapiContext context = new DefaultHapiContext();
  private static final Parser parser = PipeParser.getInstanceWithNoValidation();
  private static final ReceivingApplication myApplication = new MyApplication();

  public static void main(String[] args) throws IOException {
    // Parse arguments
    if (1 != args.length) {
      System.out.println("Usage: ./run.sh -s -u https://localhost:8888 -v 3");
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

    final byte[] buf = new byte[65535];
    final byte[] out = new byte[MAX_DATAGRAM_SIZE];

    final Config config = new ConfigBuilder(Quiche.PROTOCOL_VERSION)
        .withApplicationProtos(Http3.APPLICATION_PROTOCOL)
        .withVerifyPeer(false).loadCertChainFromPemFile(Utils.copyFileFromJAR("certs", "/cert.crt"))
        .loadPrivKeyFromPemFile(Utils.copyFileFromJAR("certs", "/cert.key"))
        .withMaxIdleTimeout(5_000)
        .withMaxUdpPayloadSize(MAX_DATAGRAM_SIZE)
        .withInitialMaxData(10_000_000)
        .withInitialMaxStreamDataBidiLocal(1_000_000)
        .withInitialMaxStreamDataBidiRemote(1_000_000)
        .withInitialMaxStreamDataUni(1_000_000)
        .withInitialMaxStreamsBidi(100).withInitialMaxStreamsUni(100)
        .withDisableActiveMigration(false)
        .enableEarlyData()
        .build();

    final DatagramSocket socket = new DatagramSocket(port, address);
    socket.setSoTimeout(1);

    final Http3Config h3Config = new Http3ConfigBuilder().build();
    final byte[] connIdSeed = Quiche.newConnectionIdSeed();
    final HashMap<String, Client> clients = new HashMap<>();
    final AtomicBoolean running = new AtomicBoolean(true);

    while (running.get()) {
      // READING
      while (true) {
        final DatagramPacket packet = new DatagramPacket(buf, buf.length);
        try {
          socket.receive(packet);
        } catch (SocketTimeoutException e) {
          // TIMERS
          for (Client client : clients.values()) {
            client.connection().onTimeout();
          }
          break;
        }

        final int offset = packet.getOffset();
        final int len = packet.getLength();
        final byte[] packetBuf = Arrays.copyOfRange(packet.getData(), offset, len);

        // PARSE QUIC HEADER
        final PacketHeader hdr;
        try {
          hdr = PacketHeader.parse(packetBuf, Quiche.MAX_CONN_ID_LEN);
        } catch (Exception e) {
          System.out.println("! failed to parse headers " + e);
          continue;
        }

        // SIGN CONN ID
        final byte[] connId = Quiche.signConnectionId(connIdSeed, hdr.destinationConnectionId());
        Client client = clients.get(Utils.asHex(hdr.destinationConnectionId()));
        if (null == client)
          client = clients.get(Utils.asHex(connId));
        if (null == client) {
          // CREATE CLIENT IF MISSING
          if (PacketType.INITIAL != hdr.packetType()) {
            System.out.println("! wrong packet type");
            continue;
          }

          // NEGOTIATE VERSION
          if (!Quiche.versionIsSupported(hdr.version())) {
            final int negLength = Quiche.negotiateVersion(hdr.sourceConnectionId(), hdr.destinationConnectionId(), out);
            if (negLength < 0) {
              System.out.println("! failed to negotiate version " + negLength);
              System.exit(1);
              return;
            }
            final DatagramPacket negPacket = new DatagramPacket(out, negLength, packet.getAddress(), packet.getPort());
            socket.send(negPacket);
            continue;
          }

          // RETRY IF TOKEN IS EMPTY
          if (null == hdr.token()) {
            final byte[] token = mintToken(hdr, packet.getAddress());
            final int retryLength = Quiche.retry(hdr.sourceConnectionId(), hdr.destinationConnectionId(), connId, token,
                hdr.version(), out);
            if (retryLength < 0) {
              System.out.println("! retry failed " + retryLength);
              System.exit(1);
              return;
            }

            final DatagramPacket retryPacket = new DatagramPacket(out, retryLength, packet.getAddress(),
                packet.getPort());
            socket.send(retryPacket);
            continue;
          }

          // VALIDATE TOKEN
          final byte[] odcid = validateToken(packet.getAddress(), hdr.token());
          if (null == odcid) {
            System.out.println("! invalid address validation token");
            continue;
          }

          byte[] sourceConnId = connId;
          final byte[] destinationConnId = hdr.destinationConnectionId();
          if (sourceConnId.length != destinationConnId.length) {
            System.out.println("! invalid destination connection id");
            continue;
          }
          sourceConnId = destinationConnId;

          final Connection conn = Quiche.accept(sourceConnId, odcid, config);

          client = new Client(conn, packet.getSocketAddress());
          clients.put(Utils.asHex(sourceConnId), client);
        }

        // POTENTIALLY COALESCED PACKETS
        final Connection conn = client.connection();
        final int read = conn.recv(packetBuf);
        if (read < 0 && read != Quiche.ErrorCode.DONE) {
          System.out.println("> recv failed " + read);
          break;
        }
        if (read <= 0)
          break;

        // ESTABLISH H3 CONNECTION IF NONE
        Http3Connection h3Conn = client.http3Connection();
        if ((conn.isInEarlyData() || conn.isEstablished()) && null == h3Conn) {
          h3Conn = Http3Connection.withTransport(conn, h3Config);
          client.setHttp3Connection(h3Conn);
        }

        if (null != h3Conn) {
          // PROCESS WRITABLES
          final Client current = client;
          client.connection().writable().forEach(streamId -> {
            handleWritable(current, streamId);
          });

          // H3 POLL
          while (true) {
            final Http3Connection h3c = h3Conn;
            final long streamId = h3c.poll(new Http3EventListener() {
              public void onHeaders(long streamId, List<Http3Header> headers, boolean hasBody) {}

              public void onData(long streamId) {
                final int bodyLength = h3c.recvBody(streamId, buf);
                String res = null;
                if (bodyLength < 0 && bodyLength != Quiche.ErrorCode.DONE) {
                  System.out.println("! recv body failed " + bodyLength);
                } else {
                  final byte[] body = Arrays.copyOfRange(buf, 0, bodyLength);
                  final String encodedMessage = new String(body, StandardCharsets.UTF_8);
                  res = handleHL7Message(encodedMessage);
                }

                handleData(current, streamId, res); // Closes the connection
              }

              public void onFinished(long streamId) {}
            });

            if (streamId < 0 && streamId != Quiche.ErrorCode.DONE) {
              System.out.println("! poll failed " + streamId);
              break;
            }
            if (Quiche.ErrorCode.DONE == streamId)
              break;
          }
        }
      }

      // WRITES
      int len = 0;
      for (Client client : clients.values()) {
        final Connection conn = client.connection();

        while (true) {
          len = conn.send(out);
          if (len < 0 && len != Quiche.ErrorCode.DONE) {
            System.out.println("! conn.send failed " + len);
            break;
          }
          if (len <= 0)
            break;

          final DatagramPacket packet = new DatagramPacket(out, len, client.sender());
          socket.send(packet);
        }
      }

      // CLEANUP CLOSED CONNS
      for (String connId : clients.keySet()) {
        if (clients.get(connId).connection().isClosed()) {
          clients.remove(connId);
        }
      }

      // BACK TO READING
    }

    socket.close();
  }

  /**
   * Generate a stateless retry token.
   * 
   * The token includes the static string {@code "Quiche4j"} followed by the IP
   * address of the client and by the original destination connection ID generated
   * by the client.
   * 
   * Note that this function is only an example and doesn't do any cryptographic
   * authenticate of the token. *It should not be used in production system*.
   */
  public final static byte[] mintToken(PacketHeader hdr, InetAddress address) {
    final byte[] addr = address.getAddress();
    final byte[] dcid = hdr.destinationConnectionId();
    final int total = SERVER_NAME_BYTES_LEN + addr.length + dcid.length;
    final ByteBuffer buf = ByteBuffer.allocate(total);
    buf.put(SERVER_NAME_BYTES);
    buf.put(addr);
    buf.put(dcid);
    return buf.array();
  }

  public final static byte[] validateToken(InetAddress address, byte[] token) {
    if (token.length <= 8)
      return null;
    if (!Arrays.equals(SERVER_NAME_BYTES, Arrays.copyOfRange(token, 0, SERVER_NAME_BYTES_LEN)))
      return null;
    final byte[] addr = address.getAddress();
    if (!Arrays.equals(addr, Arrays.copyOfRange(token, SERVER_NAME_BYTES_LEN, addr.length + SERVER_NAME_BYTES_LEN)))
      return null;
    return Arrays.copyOfRange(token, SERVER_NAME_BYTES_LEN + addr.length, token.length);
  }

  public final static void handleData(Client client, Long streamId, String res) {
    final Connection conn = client.connection();
    final Http3Connection h3Conn = client.http3Connection();

    // SHUTDOWN STREAM
    conn.streamShutdown(streamId, Quiche.Shutdown.READ, 0L);

    final byte[] body = res.getBytes();
    final List<Http3Header> headers = new ArrayList<>();
    headers.add(new Http3Header(HEADER_NAME_STATUS, "200"));
    headers.add(new Http3Header(HEADER_NAME_SERVER, SERVER_NAME));
    headers.add(new Http3Header(HEADER_NAME_CONTENT_LENGTH, Integer.toString(body.length)));

    final long sent = h3Conn.sendResponse(streamId, headers, false);
    if (sent == Http3.ErrorCode.STREAM_BLOCKED) {
      // STREAM BLOCKED
      System.out.print("> stream " + streamId + " blocked");

      // STASH PARTIAL RESPONSE
      final PartialResponse part = new PartialResponse(headers, body, 0L);
      client.partialResponses.put(streamId, part);
      return;
    }

    if (sent < 0) {
      System.out.println("! h3.send response failed " + sent);
      return;
    }

    final long written = h3Conn.sendBody(streamId, body, true);
    if (written < 0) {
      System.out.println("! h3 send body failed " + written);
      return;
    }

    if (written < body.length) {
      // STASH PARTIAL RESPONSE
      final PartialResponse part = new PartialResponse(null, body, written);
      client.partialResponses.put(streamId, part);
    }
  }

  public final static String handleHL7Message(String encodedMessage) {
    String response = null;
    try {
      Message message = parser.parse(encodedMessage);
      Message res = myApplication.processMessage(message, null);
      response = parser.encode(res);
    } catch (Exception e) {
      System.out.println("Error handling HL7 message: " + e);
      System.exit(1);
    }
    return response;
  }

  public final static void handleWritable(Client client, long streamId) {
    final PartialResponse resp = client.partialResponses.get(streamId);
    if (null == resp)
      return;

    final Http3Connection h3 = client.http3Connection();
    if (null != resp.headers) {
      final long sent = h3.sendResponse(streamId, resp.headers, false);
      if (sent == Http3.ErrorCode.STREAM_BLOCKED)
        return;
      if (sent < 0) {
        System.out.println("! h3.send response failed " + sent);
        return;
      }
    }

    resp.headers = null;

    final byte[] body = Arrays.copyOfRange(resp.body, (int) resp.written, resp.body.length);
    final long written = h3.sendBody(streamId, body, true);
    if (written < 0 && written != Quiche.ErrorCode.DONE) {
      System.out.println("! h3 send body failed " + written);
      return;
    }

    resp.written += written;
    if (resp.written < resp.body.length) {
      client.partialResponses.remove(streamId);
    }
  }

}
