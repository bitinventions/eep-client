package eep;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class HEP3 {
	
	public static final int PROTOCOL_ID_TCP = 6;
	public static final int PROTOCOL_ID_UDP = 17;
	
	public static final int PROTOCOL_TYPE_SIP = 0x01;
	public static final int PROTOCOL_TYPE_XMPP = 0x02;
	public static final int PROTOCOL_TYPE_SDP = 0x03;
	public static final int PROTOCOL_TYPE_RTP = 0x04;
	public static final int PROTOCOL_TYPE_RTCP = 0x51;
	public static final int PROTOCOL_TYPE_MGCP = 0x06;
	public static final int PROTOCOL_TYPE_MEGACO = 0x07;
	public static final int PROTOCOL_TYPE_MTP2 = 0x08;
	public static final int PROTOCOL_TYPE_MTP3 = 0x09;
	public static final int PROTOCOL_TYPE_M2UA = 0x0a;
	public static final int PROTOCOL_TYPE_M2PA = 0x0b;
	public static final int PROTOCOL_TYPE_V5UA = 0x0c;
	public static final int PROTOCOL_TYPE_M3UA = 0x0d;
	public static final int PROTOCOL_TYPE_IUA = 0x0e;
	public static final int PROTOCOL_TYPE_SUA = 0x0f;
	
	private long clientId;
	private InetAddress host;
	private int port;
	private DatagramSocket socket;
	private Executor worker = Executors.newSingleThreadExecutor();
	
	public HEP3(long id, String h, int p) {
		clientId = id;
		try {
			host = InetAddress.getByName(h);
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		port = p;
		try {
			socket = new DatagramSocket();
			
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	public void finalize() {
		if (socket != null)
			socket.close();
	}

	public void send(final int protocolId, final InetAddress src, final int srcport, final InetAddress dst,
			final int dstport, final int protocolType, final byte [] message) {
		worker.execute(new Runnable() {
			public void run() {
				List<Chunk> chunks = new ArrayList<>();
				// protocol family
				Chunk chunk = new Chunk();
				chunk.vendor = 0x0000;
				chunk.type = 0x0001;
				chunk.payload = new byte[] { 2 };
				chunks.add(chunk);
				
				// protocol ID
				chunk = new Chunk();
				chunk.vendor = 0x0000;
				chunk.type = 0x0002;
				chunk.payload = new byte[] { (byte)((protocolId >>> 8) & 0xff), (byte)(protocolId & 0xff)};
				chunks.add(chunk);
				
				// source address
				chunk = new Chunk();
				chunk.vendor = 0x0000;
				chunk.type = 0x0003;
				chunk.payload = src.getAddress();
				chunks.add(chunk);
				
				// destination address
				chunk = new Chunk();
				chunk.vendor = 0x0000;
				chunk.type = 0x0004;
				chunk.payload = dst.getAddress();
				chunks.add(chunk);
				
				// source port
				chunk = new Chunk();
				chunk.vendor = 0x0000;
				chunk.type = 0x0007;
				chunk.payload = new byte[] { (byte)((srcport >>> 8) & 0xff), (byte)(srcport & 0xff)};
				chunks.add(chunk);
				
				// destination port
				chunk = new Chunk();
				chunk.vendor = 0x0000;
				chunk.type = 0x0008;
				chunk.payload = new byte[] { (byte)((dstport >>> 8) & 0xff), (byte)(dstport & 0xff)};
				chunks.add(chunk);
				
				// timestamp
				chunk = new Chunk();
				chunk.vendor = 0x0000;
				chunk.type = 0x0009;
				long milis = System.currentTimeMillis(); 
				long time = milis/1000L;
				long micros = (milis - time*1000)*1000;
				chunk.payload = new byte[] { 
						(byte)((time >>> 24) & 0xff),
						(byte)((time >>> 16) & 0xff),
						(byte)((time >>> 8) & 0xff),
						(byte)(time & 0xff)};
				chunks.add(chunk);
				
				// timestamp
				chunk = new Chunk();
				chunk.vendor = 0x0000;
				chunk.type = 0x000a;
				chunk.payload = new byte[] { 
						(byte)((micros >>> 24) & 0xff),
						(byte)((micros >>> 16) & 0xff),
						(byte)((micros >>> 8) & 0xff),
						(byte)(micros & 0xff)};
				chunks.add(chunk);
				
				// protocol type
				chunk = new Chunk();
				chunk.vendor = 0x0000;
				chunk.type = 0x000b;
				chunk.payload = new byte[] { (byte)(protocolType & 0xff)};
				chunks.add(chunk);
				
				// clientId
				chunk = new Chunk();
				chunk.vendor = 0x0000;
				chunk.type = 0x000c;
				chunk.payload = new byte[] { 
						(byte)((clientId >>> 24) & 0xff),
						(byte)((clientId >>> 16) & 0xff),
						(byte)((clientId >>> 8) & 0xff),
						(byte)(clientId & 0xff)};
				chunks.add(chunk);
				
				// Message
				chunk = new Chunk();
				chunk.vendor = 0x0000;
				chunk.type = 0x000f;
				chunk.payload = message;
				chunks.add(chunk);
						
				// serialize
				int totalLen = 6;
				for (Chunk c : chunks) 
					totalLen += (6 + c.payload.length);
				
				byte [] packet = new byte[totalLen];
				packet[0] = 0x48; // H
				packet[1] = 0x45; // E
				packet[2] = 0x50; // P
				packet[3] = 0x33; // 3
				packet[4] = (byte) ((totalLen >>> 8) & 0xff);
				packet[5] = (byte) (totalLen & 0xff);
				int idx = 6;
				for (Chunk c : chunks) {
					packet[idx++] = (byte) ((c.vendor >>> 8) & 0xff);
					packet[idx++] = (byte) (c.vendor & 0xff);
					packet[idx++] = (byte) ((c.type >>> 8) & 0xff);
					packet[idx++] = (byte) (c.type & 0xff);
					int len = 6 + c.payload.length;
					packet[idx++] = (byte) ((len >>> 8) & 0xff);
					packet[idx++] = (byte) (len & 0xff);
					System.arraycopy(c.payload, 0, packet, idx, c.payload.length);
					idx += c.payload.length;
				}
				
				try {
					socket.send(new DatagramPacket(packet, packet.length, host, port));
				
				} catch (IOException e) {
					e.printStackTrace();
				}	
			}
		});
	}
}
