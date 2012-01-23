package net.mariusgundersen.websocket;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Map.Entry;

class WebSocketDraft55 extends WebSocket{

	public WebSocketDraft55(Socket socket, Fields clientFields, Fields serverFields) throws IOException {
		super(socket, clientFields, serverFields);
	}
	
	
	public void sendMessage(String message) throws IOException{
		byte[] buffer = message.getBytes("UTF-8");
		out.write(0);
		out.write(buffer);
		out.write(0xFF);
		out.flush();
	}
	
	
	public void listen() {
		try {
			int length;
			int b;
			String inputLine;
			Charset utf = Charset.forName("UTF-8");
			CharsetDecoder decoder = utf.newDecoder();
			byte[] buffer = new byte[MAX_BUFFER_LENGTH];
			ByteBuffer bytes;
			while (isOpen) {
				decoder.reset();
				b = in.read();
				if((b & 0x80) == 0x80){
					length = 0;
					while(b != -1 && (b & 0x80) == 0x80){
						b = in.read();
						int bv = b & 0x7F;
						length *= 128;
						length += bv;
					}
					in.read(buffer, 0, length);
				}else if(b == 0x00){
					b = in.read();
					inputLine = "";
					length = 0;
					while(b != -1 && (b & 0xFF) != 0xFF){
						buffer[length] = (byte)b;
						b = in.read();
						length++;
						if(length>MAX_BUFFER_LENGTH){
							bytes = ByteBuffer.wrap(buffer);
							inputLine += decoder.decode(bytes).toString();
							length = 0;
						}
					}
				}else{
					break;
				}
				bytes = ByteBuffer.wrap(buffer, 0, length);
				inputLine = decoder.decode(bytes).toString();
				if(b == -1){
					break;
				}
				messageReceived(inputLine);
			}
			
			connectionClosed();
			
			out.close();
			in.close();
			socket.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}


	@Override
	public boolean handshake() throws IOException {
		String header = "";
		header += "HTTP/1.1 101 Web Socket Protocol Handshake\r\n";
		header += "Upgrade: WebSocket\r\n";
		header += "Connection: Upgrade\r\n";
		header += "WebSocket-Origin: " + clientFields.get("Origin") + "\r\n";
		header += "WebSocket-Location: ws://" + clientFields.get("Host")
				+ clientFields.get("Path") + "\r\n";
		for (Entry<String, String> field : serverFields.entrySet()) {
			header += field.getKey() + ": " + field.getValue() + "\r\n";
		}
		header += "\r\n";

		out.write(header.getBytes("UTF-8"));
		out.flush();
		System.out.println("Creating WebSocket");
		return true;
	}
	
	
}
