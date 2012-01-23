package net.mariusgundersen.websocket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import net.mariusgundersen.websocket.WebSocket.HandshakeFailedException;


public class WebSocketServer implements Runnable {

	protected ServerSocket serverSocket;
	protected int port;
	protected boolean listening = false;;
	protected Thread listeningThread;
	protected ArrayList<WebSocketServer.Listener> listeners = new ArrayList<WebSocketServer.Listener>();
	public WebSocketDraft55.Fields serverFields = new WebSocketDraft55.Fields();

	public WebSocketServer(int port) {

		this.port = port;
		listeningThread = new Thread(this);
	}

	@Override
	public void run() {
		serverSocket = null;

		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			System.out.println("Error setting up server");
			e.printStackTrace();
		}

		while (listening) {
			System.out.println("Listening");
			try {
				Socket socket = serverSocket.accept();
				new Connection(socket).start();
			} catch (IOException e) {
				System.out.println("Server shutdown");
			}
		}
		try {
			serverSocket.close();
		} catch (IOException e) {
			System.out.println("Error closing down up server");
			e.printStackTrace();
		}
	}

	protected void throwError(String message) {
		for (int i = 0; i < listeners.size(); i++) {
			listeners.get(i).error(message);
		}
	}

	public void addEventListener(WebSocketServer.Listener listener) {
		listeners.add(listener);
	}

	public void removeEventListener(WebSocketServer.Listener listener) {
		listeners.remove(listener);
	}

	public void startListening() {
		if (!listening) {
			listeningThread.start();
			listening = true;
		}
	}

	public void pauseListening() {
		listening = false;
	}

	public void stopListening() {
		listening = false;
		try {
			serverSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean isListening() {
		return listening;
	}

	protected void newConnection(WebSocket socket) {
		for (int i = 0; i < listeners.size(); i++) {
			listeners.get(i).newConnection(socket);
		}
	}

	private class Connection extends Thread {

		private Socket socket;

		public Connection(Socket socket) {
			this.socket = socket;
		}

		public void run() {
			try {
				WebSocket webSocket = WebSocket.handshake(socket, serverFields);
				newConnection(webSocket);
				webSocket.listen();
				System.out.println("Handshaking done");
			} catch (HandshakeFailedException e) {
				System.out.println("Handshake failed because: "
						+ e.getMessage());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		
	}

	public interface Listener {
		public void newConnection(WebSocket socket);

		public void error(String message);
	}
	
	

	

}
