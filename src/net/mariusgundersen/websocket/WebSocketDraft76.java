package net.mariusgundersen.websocket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.Map.Entry;


class WebSocketDraft76 extends WebSocketDraft55 {

	public static final String SEC_WEBSOCKET_KEY2 = "Sec-WebSocket-Key2";

	public WebSocketDraft76(Socket socket, Fields clientFields,
			Fields serverFields) throws IOException {
		super(socket, clientFields, serverFields);
	}

	@Override
	public boolean handshake() throws IOException {

		BufferedReader in = new BufferedReader(new InputStreamReader(this.in));
		char[] secCode = new char[8];
		in.read(secCode, 0, 8);
		System.out.println("=>\n=> '" + new String(secCode) + "'");

		System.out.println("Got all headers");

		// /////STARTING RESPONSE
		String location = "ws://" + clientFields.get("Host")
				+ clientFields.get("Path");

		byte[] response;
		try {
			response = calculateResponse(clientFields, secCode);
		} catch (Exception e) {
			System.out.println("Security Failed: " + e.getMessage());
			return false;
		}

		String header = "";
		header += "HTTP/1.1 101 WebSocket Protocol Handshake\r\n";
		header += "Upgrade: WebSocket\r\n";
		header += "Connection: Upgrade\r\n";
		header += "Sec-WebSocket-Origin: "
				+ (clientFields.containsKey("Origin") ? clientFields
						.get("Origin") : "null") + "\r\n";
		header += "Sec-WebSocket-Location: " + location + "\r\n";
		if (clientFields.containsKey("Sec-WebSocket-Protocol")) {
			header += "Sec-WebSocket-Protocol: "
					+ clientFields.get("Sec-WebSocket-Protocol") + "\r\n";
		}
		for (Entry<String, String> field : serverFields.entrySet()) {
			header += field.getKey() + ": " + field.getValue() + "\r\n";
		}
		header += "\r\n";
		System.out.println("'" + header + "'");

		out.write(header.getBytes());
		out.write(response);
		out.flush();
		return true;
	}

	public byte[] calculateResponse(WebSocketDraft55.Fields properties,
			char[] secKey3) throws Exception {
		String secKey1 = properties.get("Sec-WebSocket-Key1");
		String secKey2 = properties.get("Sec-WebSocket-Key2");
		System.out.println("secKey1: " + secKey1 + "\nsecKey2: " + secKey2
				+ "\nsecKey3: " + new String(secKey3));
		long num1 = parseKey(secKey1);
		long num2 = parseKey(secKey2);
		int space1 = countSpaces(secKey1);
		int space2 = countSpaces(secKey2);

		System.out.println(num1 + " " + num2 + " (" + space1 + ", " + space2
				+ ")");

		if (space1 == 0 || space2 == 0) {
			throw new Exception(
					"Security Errror in handshake (number of spaces is 0)");
		} else if (num1 % space1 != 0) {
			throw new Exception(
					"Security Errror in handshake (key1 is not divisible by spaces)");
		} else if (num2 % space2 != 0) {
			throw new Exception(
					"Security Errror in handshake (key2 is not divisible by spaces)");
		}

		long part1 = num1 / space1;
		long part2 = num2 / space2;

		System.out.println(part1 + ", " + part2);
		byte[] challenge = new byte[16];
		// (part1+""+part2+""+secKey3).getBytes();
		challenge[0] = (byte) (0xFF & (part1 >> 24));
		challenge[1] = (byte) (0xFF & (part1 >> 16));
		challenge[2] = (byte) (0xFF & (part1 >> 8));
		challenge[3] = (byte) (0xFF & (part1 >> 0));
		challenge[4] = (byte) (0xFF & (part2 >> 24));
		challenge[5] = (byte) (0xFF & (part2 >> 16));
		challenge[6] = (byte) (0xFF & (part2 >> 8));
		challenge[7] = (byte) (0xFF & (part2 >> 0));
		System.out.print("challenge: ");
		for (int i = 0; i < 8; i++) {
			System.out.print(String.format("0x%x ", challenge[i]));
		}
		for (int i = 0; i < 8; i++) {
			System.out.print(String.format("0x%x ", (int) secKey3[i]));
			challenge[i + 8] = (byte) secKey3[i];
		}
		System.out.println();
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] response = md.digest(challenge);
		String ret = "";
		System.out.print("response: 0x");
		for (int i = 0; i < response.length; i++) {
			System.out.print(String.format("0x%x ", response[i]));
			ret += String.valueOf(response[i]);
		}
		System.out.println();
		System.out.println(ret);
		return response;
	}

	private int countSpaces(String key) {
		int ret = 0;
		for (int i = 0; i < key.length(); i++) {
			char c = key.charAt(i);
			if (c == 0x20) {
				ret++;
			}
		}
		return ret;
	}

	private long parseKey(String key) {
		long ret = 0;
		for (int i = 0; i < key.length(); i++) {
			char c = key.charAt(i);
			if (c >= 0x30 && c <= 0x39) {
				ret *= 10;
				ret += c - 48;
			}
		}
		return ret;
	}

}
