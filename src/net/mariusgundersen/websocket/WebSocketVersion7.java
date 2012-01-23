package net.mariusgundersen.websocket;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map.Entry;


class WebSocketVersion7 extends WebSocket {
	

	private static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
	public static final String SEC_WEBSOCKET_VERSION = "Sec-WebSocket-Version";

	public WebSocketVersion7(Socket socket, Fields clientFields, Fields serverFields) throws IOException {
		super(socket, clientFields, serverFields);
	}

	
	public void listen() {
		try {
			int length = 0;
			int b;
			String inputLine = "";
			Charset utf = Charset.forName("UTF-8");
			CharsetDecoder decoder = utf.newDecoder();
			byte[] buffer = new byte[MAX_BUFFER_LENGTH];
			ByteBuffer bytes;
			while (isOpen) {
				decoder.reset();
				inputLine = "";
				boolean notFinished = true;
				
				int opcode;
				boolean masking;
				int[] mask = new int[4];
				while(notFinished){
					b = in.read();
					notFinished = ((b & 0x80) == 0x00);
					System.out.println("\n==New frame==");
					opcode = (0x0f & b);
					System.out.println("Opcode: "+opcode);
					if(opcode == 0x08){
						isOpen = false;
						break;
					}
					length = (0xff & in.read());
					masking = ((length & 0x80) == 0x80);
					length = (0x7F & length);
					if(length == 0x7F){
						length = (0xff & in.read())<<24;
						length |= (0xff & in.read())<<16;
						length |= (0xff & in.read())<<8;
						length |= (0xff & in.read());
					}else if(length == 0x7E){
						length = (0xff & in.read())<<8;
						length |= (0xff & in.read());
					}
					System.out.println("Masking: "+masking+", Length: "+length);
					if(masking){
						mask[0] = (0xff & in.read());
						mask[1] = (0xff & in.read());
						mask[2] = (0xff & in.read());
						mask[3] = (0xff & in.read());
					}else{
						mask[0] = 0;
						mask[1] = 0;
						mask[2] = 0;
						mask[3] = 0;
					}
					for(int i=0; i<length; i++){
						buffer[i] = (byte) ((0xff & in.read()) ^ mask[i%4]);
						if(i==MAX_BUFFER_LENGTH){
							bytes = ByteBuffer.wrap(buffer);
							inputLine += decoder.decode(bytes).toString();
							length -= i;
							i = 0;
						}
					}
				}
				if(length == -1){
					isOpen = false;
				}
				if(isOpen){
					bytes = ByteBuffer.wrap(buffer, 0, length);
					inputLine += decoder.decode(bytes).toString();
					System.out.println(inputLine);
					messageReceived(inputLine);
				}
			}
			
			
			out.close();
			in.close();
			socket.close();

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			System.out.println("Connection closed");
			connectionClosed();
		}

	}
	

	
	public void sendMessage(String message) throws IOException{
		byte[] buffer = message.getBytes("UTF-8");
		out.write(0x81);//the only frame, opcode=text
		if(buffer.length < 126){
			out.write(buffer.length);
		}else if(buffer.length < 0xffff){
			out.write(0x7e);
			out.write(buffer.length>>8);
			out.write(buffer.length);
		}else{
			out.write(0x7f);
			out.write(buffer.length>>24);
			out.write(buffer.length>>16);
			out.write(buffer.length>>8);
			out.write(buffer.length);
		}
		out.write(buffer);
		out.flush();
	}
	
	@Override
	public boolean handshake() throws IOException {
		String header = "";
		header += "HTTP/1.1 101 Switching Protocols\r\n";
		header += "Upgrade: WebSocket\r\n";
		header += "Connection: Upgrade\r\n";
		header += "Sec-WebSocket-Accept: "
				+ calculateResponse(clientFields.get("Sec-WebSocket-Key"))
				+ "\r\n";
		if (clientFields.containsKey("Sec-WebSocket-Protocol")) {
			header += "Sec-WebSocket-Protocol: "
					+ clientFields.get("Sec-WebSocket-Protocol") + "\r\n";
		}
		for (Entry<String, String> field : serverFields.entrySet()) {
			header += field.getKey() + ": " + field.getValue() + "\r\n";
		}
		header += "\r\n";

		out.write(header.getBytes("UTF-8"));
		out.flush();
		System.out.println("Creating WebSocket");
		return true;
	}

	private static String calculateResponse(String key) {
		String shaBase = key + GUID;

		MessageDigest mdSha1;
		byte[] hash;
		try {
			mdSha1 = MessageDigest.getInstance("SHA-1");
			mdSha1.update(shaBase.getBytes());
			hash = mdSha1.digest();
			return encode(hash);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}

	private static final String base64code = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
			+ "abcdefghijklmnopqrstuvwxyz" + "0123456789" + "+/";

	private static byte[] zeroPad(int length, byte[] bytes) {
		byte[] padded = new byte[length]; // initialized to zero by JVM
		System.arraycopy(bytes, 0, padded, 0, bytes.length);
		return padded;
	}

	private static String encode(byte[] stringArray) {

		String encoded = "";

		// determine how many padding bytes to add to the output
		int paddingCount = (3 - (stringArray.length % 3)) % 3;
		// add any necessary padding to the input
		stringArray = zeroPad(stringArray.length + paddingCount, stringArray);
		// process 3 bytes at a time, churning out 4 output bytes
		// worry about CRLF insertions later
		for (int i = 0; i < stringArray.length; i += 3) {
			
			int j = ((stringArray[i] & 0xff) << 16)
					+ ((stringArray[i + 1] & 0xff) << 8)
					+ (stringArray[i + 2] & 0xff);
			encoded = encoded + base64code.charAt((j >> 18) & 0x3f)
					+ base64code.charAt((j >> 12) & 0x3f)
					+ base64code.charAt((j >> 6) & 0x3f)
					+ base64code.charAt(j & 0x3f);
		}
		// replace encoded padding nulls with "="
		return encoded.substring(0, encoded.length() - paddingCount)
				+ "==".substring(0, paddingCount);

	}
}
