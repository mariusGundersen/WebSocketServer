Minimal example of using the WebSocketServer:




    import net.mariusgundersen.websocket.WebSocket;
    import net.mariusgundersen.websocket.WebSocketServer;
    import net.mariusgundersen.websocket.WebSocketServer.Listener;


    public class Test implements Listener {

    	@Override
    	public void newConnection(WebSocket socket) {
    		socket.addEventListener(new WebSocket.Listener() {
    			
    			@Override
    			public void messageReceived(String message, WebSocket source) {
    				System.out.println("String received from cliennt: "+message);
    			}
    			
    			@Override
    			public void connectionClosed(WebSocket socket) {
    				System.out.println("Connection with client closed");
    			}
    		});
    		
    	}

    	@Override
    	public void error(String message) {
    		System.out.println("Error handshaking with client");
    	}


    	public static void main(String[] args) {
    		final Test test = new Test();
    		WebSocketServer server = new WebSocketServer(8765);
    		server.addEventListener(test);
    		server.startListening();
    	}
    }