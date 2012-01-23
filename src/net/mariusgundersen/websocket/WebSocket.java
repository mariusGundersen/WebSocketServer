package net.mariusgundersen.websocket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;


public abstract class WebSocket {

	protected static final int MAX_BUFFER_LENGTH = 512;

	protected Socket socket;
	protected OutputStream out;
	protected InputStream in;
	protected Fields clientFields;
	protected ArrayList<Listener> listeners = new ArrayList<Listener>();
	protected volatile boolean isOpen = true;

	protected Fields serverFields;

	public WebSocket(Socket socket, Fields clientFields, Fields serverFields)
			throws IOException {
		this.socket = socket;
		this.clientFields = clientFields;
		this.serverFields = serverFields;

		out = socket.getOutputStream();
		in = socket.getInputStream();
	}

	public abstract void sendMessage(String message) throws IOException;

	public abstract void listen();

	public void closeConnection() {
		isOpen = false;
		try {
			out.close();
			in.close();
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void addEventListener(WebSocketDraft55.Listener listener) {
		listeners.add(listener);
	}

	public void removeEventListener(Listener listener) {
		listeners.remove(listener);
	}

	public String getProperty(String name) {
		return clientFields.get(name);
	}

	protected void connectionClosed() {
		for (int i = 0; i < listeners.size(); i++) {
			listeners.get(i).connectionClosed(this);
		}
	}

	protected void messageReceived(String message) {
		for (int i = 0; i < listeners.size(); i++) {
			listeners.get(i).messageReceived(message, this);
		}
	}

	public interface Listener {
		public void messageReceived(String message, WebSocket source);

		public void connectionClosed(WebSocket source);
	}

	public static class Fields extends HashMap<String, String> {

	}

	public static class HandshakeFailedException extends Exception {

		public HandshakeFailedException(String message, Exception e) {
			super(message, e);
		}

	}

	public static WebSocket handshake(Socket socket, Fields serverFields)
			throws HandshakeFailedException, IOException {

		System.out.println("Handshaking");
		BufferedReader in = new BufferedReader(new InputStreamReader(
				socket.getInputStream()));
		Fields clientFields = new WebSocketDraft55.Fields();

		String path = in.readLine();
		path = path.substring(4);
		path = path.substring(0, path.indexOf(' '));
		clientFields.put("Path", path);
		System.out.println("=> '" + path + "'");
		String input;
		while ((input = in.readLine()) != null) {
			int colon = input.indexOf(':');
			if (colon < 0)
				break;
			System.out.println("=> '" + input + "'");
			clientFields.put(input.substring(0, colon).trim(),
					input.substring(colon + 2).trim());
		}
		WebSocket webSocket;
		if(clientFields.containsKey(WebSocketVersion7.SEC_WEBSOCKET_VERSION)){
			webSocket = new WebSocketVersion7(socket, clientFields, serverFields);
		}else if(clientFields.containsKey(WebSocketDraft76.SEC_WEBSOCKET_KEY2)){
			webSocket = new WebSocketDraft76(socket, clientFields, serverFields);
		}else{
			webSocket = new WebSocketDraft55(socket, clientFields, serverFields);
		}
		
		webSocket.handshake();

		return webSocket;

	}

	public abstract boolean handshake() throws IOException;

}
