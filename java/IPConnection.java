/*
 * Copyright (C) 2012 Matthias Bolte <matthias@tinkerforge.com>
 * Copyright (C) 2011-2012 Olaf Lüke <olaf@tinkerforge.com>
 *
 * Redistribution and use in source and binary forms of this file,
 * with or without modification, are permitted.
 */

package com.tinkerforge;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.List;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class ReceiveThread extends Thread {
	IPConnection ipcon = null;

	ReceiveThread(IPConnection ipcon) {
		super("Brickd-Receiver");

		setDaemon(true);
		this.ipcon = ipcon;
	}

	@Override
	public void run() {
		byte[] pendingData = new byte[8192];
		int pendingLength = 0;

		while(ipcon.receiveFlag) {
			int length;

			try {
				length = ipcon.in.read(pendingData, pendingLength,
				                       pendingData.length - pendingLength);
			} catch(java.net.SocketException e) {
				if(ipcon.receiveFlag) {
					ipcon.autoReconnectAllowed = true;
					ipcon.receiveFlag = false;
					ByteBuffer bb = ByteBuffer.allocate(3);
					bb.order(ByteOrder.LITTLE_ENDIAN);
					bb.put(IPConnection.CALLBACK_DISCONNECTED);
					bb.putShort(IPConnection.DISCONNECT_REASON_ERROR);

					try {
						ipcon.callbackQueue.put(new IPConnection.CallbackQueueObject(IPConnection.QUEUE_META, bb.array()));
					} catch(java.lang.InterruptedException e2) {
					}
				}
				return;
			} catch(Exception e) {
				if(ipcon.receiveFlag) {
					ipcon.receiveFlag = false;
					e.printStackTrace();
				}
				return;
			}

			if(length <= 0) {
				if(ipcon.receiveFlag) {
					ipcon.autoReconnectAllowed = true;
					ipcon.receiveFlag = false;
					ByteBuffer bb = ByteBuffer.allocate(8);
					bb.order(ByteOrder.LITTLE_ENDIAN);
					bb.put(IPConnection.CALLBACK_DISCONNECTED);
					bb.putShort(IPConnection.DISCONNECT_REASON_SHUTDOWN);

					try {
						ipcon.callbackQueue.put(new IPConnection.CallbackQueueObject(IPConnection.QUEUE_META, bb.array()));
					} catch(java.lang.InterruptedException e2) {
					}
				}
				return;
			}

			pendingLength += length;

			while(true) {
				if(pendingLength < 8) {
					// Wait for complete header
					break;
				}

				length = IPConnection.getLengthFromData(pendingData);

				if(pendingLength < length) {
					// Wait for complete packet
					break;
				}

				byte[] packet = new byte[length];

				System.arraycopy(pendingData, 0, packet, 0, length);
				System.arraycopy(pendingData, length, pendingData, 0, pendingLength - length);
				pendingLength -= length;
				ipcon.handleResponse(packet);
			}
		}
	}
}

class CallbackThreadRestarter implements Thread.UncaughtExceptionHandler {
	IPConnection ipcon = null;

	CallbackThreadRestarter(IPConnection ipcon) {
		this.ipcon = ipcon;
	}

	@Override
	public void uncaughtException(Thread thread, Throwable exception) {
		System.err.print("Exception in thread \"" + thread.getName() + "\" ");
		exception.printStackTrace();

		ipcon.callbackThread = new CallbackThread(ipcon, ((CallbackThread)thread).callbackQueue);
		ipcon.callbackThread.start();
	}
}

class CallbackThread extends Thread {
	IPConnection ipcon = null;
	LinkedBlockingQueue<IPConnection.CallbackQueueObject> callbackQueue = null;

	CallbackThread(IPConnection ipcon,
	               LinkedBlockingQueue<IPConnection.CallbackQueueObject> callbackQueue) {
		super("Callback-Processor");

		setDaemon(true);
		this.ipcon = ipcon;
		this.callbackQueue = callbackQueue;
		this.setUncaughtExceptionHandler(new CallbackThreadRestarter(ipcon));
	}

	@Override
	public void run() {
		while(true) {
			IPConnection.CallbackQueueObject cqo = null;
			try {
				cqo = callbackQueue.take();
			} catch(InterruptedException e) {
				e.printStackTrace();
				continue;
			}

			if(cqo == null) {
				continue;
			}

			switch(cqo.kind) {
				case IPConnection.QUEUE_EXIT: {
					return;
				}

				case IPConnection.QUEUE_META: {
					ByteBuffer bb = ByteBuffer.wrap(cqo.data, 0, 3);
					bb.order(ByteOrder.LITTLE_ENDIAN);

					byte id = bb.get();
					short parameter = bb.getShort();

					switch(id) {
						case IPConnection.CALLBACK_CONNECTED:
							for(IPConnection.ConnectedListener listener: ipcon.listenerConnected) {
								listener.connected(parameter);
							}

							break;

						case IPConnection.CALLBACK_DISCONNECTED:
							synchronized(ipcon.socketMutex) {
								ipcon.closeSocket();
							}

							try {
								Thread.sleep(100);
							} catch(InterruptedException e) {
								e.printStackTrace();
							}

							for(IPConnection.DisconnectedListener listener: ipcon.listenerDisconnected) {
								listener.disconnected(parameter);
							}

							if(parameter != IPConnection.DISCONNECT_REASON_REQUEST && ipcon.autoReconnect && ipcon.autoReconnectAllowed) {
								ipcon.autoReconnectPending = true;
								boolean retry = true;

								while(retry) {
									retry = false;

									synchronized(ipcon.socketMutex) {
										if(ipcon.autoReconnectAllowed && ipcon.socket == null) {
											try {
												ipcon.connectUnlocked(true);
											} catch(Exception e) {
												retry = true;
											}
										} else {
											ipcon.autoReconnectPending = true;
										}
									}

									if(retry) {
										try {
											Thread.sleep(100);
										} catch(InterruptedException e) {
											e.printStackTrace();
										}
									}
								}
							}

							break;
					}

					break;
				}

				case IPConnection.QUEUE_PACKET: {
					if (!ipcon.receiveFlag) {
						// don't dispatch callbacks when the receive thread isn't running
						continue;
					}

					byte functionID = IPConnection.getFunctionIDFromData(cqo.data);
					if(functionID == IPConnection.CALLBACK_ENUMERATE) {
						if(!ipcon.listenerEnumerate.isEmpty()) {
							int length = IPConnection.getLengthFromData(cqo.data);
							ByteBuffer bb = ByteBuffer.wrap(cqo.data, 8, length - 8);
							bb.order(ByteOrder.LITTLE_ENDIAN);
							String uid_str = "";
							for(int i = 0; i < 8; i++) {
								char c = (char)bb.get();
								if(c != '\0') {
									uid_str += c;
								}
							}
							String connectedUid_str = "";
							for(int i = 0; i < 8; i++) {
								char c = (char)bb.get();
								if(c != '\0') {
									connectedUid_str += c;
								}
							}
							char position = (char)bb.get();
							short[] hardwareVersion = new short[3];
							hardwareVersion[0] = IPConnection.unsignedByte(bb.get());
							hardwareVersion[1] = IPConnection.unsignedByte(bb.get());
							hardwareVersion[2] = IPConnection.unsignedByte(bb.get());
							short[] firmwareVersion = new short[3];
							firmwareVersion[0] = IPConnection.unsignedByte(bb.get());
							firmwareVersion[1] = IPConnection.unsignedByte(bb.get());
							firmwareVersion[2] = IPConnection.unsignedByte(bb.get());
							int deviceIdentifier = IPConnection.unsignedShort(bb.getShort());
							short enumerationType = IPConnection.unsignedByte(bb.get());

							for(IPConnection.EnumerateListener listener: ipcon.listenerEnumerate) {
								listener.enumerate(uid_str, connectedUid_str, position,
								                   hardwareVersion, firmwareVersion,
								                   deviceIdentifier, enumerationType);
							}
						}
					} else {
						long uid = IPConnection.getUIDFromData(cqo.data);
						Device device = ipcon.devices.get(uid);
						if(device.callbacks[functionID] != null) {
							device.callbacks[functionID].callback(cqo.data);
						}
					}

					break;
				}
			}
		}
	}
}

public class IPConnection {
	private final static String BASE58 = "123456789abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";

	public final static byte FUNCTION_ENUMERATE = (byte)254;
	public final static byte CALLBACK_ENUMERATE = (byte)253;

	public final static byte CALLBACK_CONNECTED = 0;
	public final static byte CALLBACK_DISCONNECTED = 1;
	public final static byte CALLBACK_AUTHENTICATION_ERROR = 2;

	private final static int BROADCAST_UID = 0;

	// enumeration_type parameter to the enumerate callback
	public final static short ENUMERATION_TYPE_AVAILABLE = 0;
	public final static short ENUMERATION_TYPE_CONNECTED = 1;
	public final static short ENUMERATION_TYPE_DISCONNECTED = 2;

	// connect_reason parameter to the connected callback
	public final static short CONNECT_REASON_REQUEST = 0;
	public final static short CONNECT_REASON_AUTO_RECONNECT = 1;

	// disconnect_reason parameter to the disconnected callback
	public final static short DISCONNECT_REASON_REQUEST = 0;
	public final static short DISCONNECT_REASON_ERROR = 1;
	public final static short DISCONNECT_REASON_SHUTDOWN = 2;

	// returned by get_connection_state
	public final static short CONNECTION_STATE_DISCONNECTED = 0;
	public final static short CONNECTION_STATE_CONNECTED = 1;
	public final static short CONNECTION_STATE_PENDING = 2; // auto-reconnect in process

	final static int QUEUE_EXIT = 0;
	final static int QUEUE_META = 1;
	final static int QUEUE_PACKET = 2;

	int responseTimeout = 2500;

	Hashtable<Long, Device> devices = new Hashtable<Long, Device>();
	LinkedBlockingQueue<CallbackQueueObject> callbackQueue = null;

	Object socketMutex = new Object();
	private Object sequenceNumberMutex = new Object();

	private String host;
	private int port;

	private final static int SEQUENCE_NUMBER_POS = 4;
	private int nextSequenceNumber = 0;

	boolean receiveFlag = false;

	boolean autoReconnect = true;
	boolean autoReconnectAllowed = false;
	boolean autoReconnectPending = false;
	Socket socket = null;
	OutputStream out = null;
	InputStream in = null;
	List<EnumerateListener> listenerEnumerate = new ArrayList<EnumerateListener>();
	List<ConnectedListener> listenerConnected = new ArrayList<ConnectedListener>();
	List<DisconnectedListener> listenerDisconnected = new ArrayList<DisconnectedListener>();
	ReceiveThread receiveThread = null;
	CallbackThread callbackThread = null;

	static class CallbackQueueObject {
		public final int kind;
		public final byte[] data;
		public CallbackQueueObject(int kind, byte[] data) {
			this.kind = kind;
			this.data = data;
		}
	}

	public interface EnumerateListener {
		public void enumerate(String uid, String connectedUid, char position,
		                      short[] hardwareVersion, short[] firmwareVersion,
		                      int deviceIdentifier, short enumerationType);
	}

	public interface ConnectedListener {
		public void connected(short connectReason);
	}

	public interface DisconnectedListener {
		public void disconnected(short disconnectReason);
	}

	/**
	 * Creates an IP Connection object that can be used to enumerate the available
	 * devices. It is also required for the constructor of Bricks and Bricklets.
	 */
	public IPConnection() {
	}

	/**
	 * Creates a TCP/IP connection to the given \c host and \c port. The host
	 * and port can point to a Brick Daemon or to a WIFI/Ethernet Extension.
	 *
	 * Devices can only be controlled when the connection was established
	 * successfully.
	 *
	 * Blocks until the connection is established and throws an exception if
	 * there is no Brick Daemon or WIFI/Ethernet Extension listening at the
	 * given host and port.
	 */
	public void connect(String host, int port) throws java.net.UnknownHostException, java.io.IOException, AlreadyConnectedException {
		synchronized(socketMutex) {
			if (socket != null) {
				throw new AlreadyConnectedException("Already connected to " + this.host + ":" + this.port);
			}

			this.host = host;
			this.port = port;

			connectUnlocked(false);
		}
	}

	// NOTE: Assumes that socketMutex is locked
	void connectUnlocked(boolean isAutoReconnect) throws java.net.UnknownHostException, java.io.IOException {
		if(callbackThread == null) {
			callbackQueue = new LinkedBlockingQueue<CallbackQueueObject>();
			callbackThread = new CallbackThread(this, callbackQueue);
			callbackThread.start();
		}

		try {
			socket = new Socket(host, port);
			socket.setTcpNoDelay(true);
			in = socket.getInputStream();
			out = socket.getOutputStream();
			out.flush();
		} catch(java.net.UnknownHostException e) {
			socket = null;
			in = null;
			out = null;
			throw(e);
		} catch(java.io.IOException e) {
			socket = null;
			in = null;
			out = null;
			throw(e);
		}

		receiveFlag = true;
		receiveThread = new ReceiveThread(this);
		receiveThread.start();

		autoReconnectAllowed = false;
		autoReconnectPending = false;

		short connectReason = IPConnection.CONNECT_REASON_REQUEST;
		if(isAutoReconnect) {
			connectReason = CONNECT_REASON_AUTO_RECONNECT;
		}

		ByteBuffer bb = ByteBuffer.allocate(3);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.put(CALLBACK_CONNECTED);
		bb.putShort(connectReason);

		try {
			callbackQueue.put(new CallbackQueueObject(QUEUE_META, bb.array()));
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Disconnects the TCP/IP connection from the Brick Daemon or the
	 * WIFI/Ethernet Extension.
	 */
	public void disconnect() throws NotConnectedException {
		CallbackThread callbackThreadTmp = null;
		LinkedBlockingQueue<CallbackQueueObject> callbackQueueTmp = null;

		synchronized(socketMutex) {
			autoReconnectAllowed = false;

			if(autoReconnectPending) {
				autoReconnectPending = false;
			} else {
				if(socket == null) {
					throw new NotConnectedException();
				}

				disconnectUnlocked();
			}

			callbackThreadTmp = callbackThread;
			callbackQueueTmp = callbackQueue;

			callbackThread = null;
			callbackQueue = null;
		}

		ByteBuffer bb = ByteBuffer.allocate(3);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.put(CALLBACK_DISCONNECTED);
		bb.putShort(DISCONNECT_REASON_REQUEST);

		try {
			callbackQueueTmp.put(new CallbackQueueObject(QUEUE_META, bb.array()));
		} catch(InterruptedException e) {
			e.printStackTrace();
		}

		try {
			callbackQueueTmp.put(new CallbackQueueObject(QUEUE_EXIT, null));
		} catch(InterruptedException e) {
			e.printStackTrace();
		}

		if(Thread.currentThread() != callbackThreadTmp) {
			try {
				callbackThreadTmp.join();
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	void disconnectUnlocked() {
		receiveFlag = false;

		closeSocket();

		if(receiveThread != null) {
			try {
				receiveThread.join();
			} catch(InterruptedException e) {
				e.printStackTrace();
			}

			receiveThread = null;
		}
	}

	/**
	 * Can return the following states:
	 *
	 * - CONNECTION_STATE_DISCONNECTED: No connection is established.
	 * - CONNECTION_STATE_CONNECTED: A connection to the Brick Daemon or
	 *   the WIFI/Ethernet Extension is established.
	 * - CONNECTION_STATE_PENDING: IP Connection is currently trying to
	 *   connect.
	 */
	public short getConnectionState() {
		if(socket != null) {
			return CONNECTION_STATE_CONNECTED;
		}

		if(autoReconnectPending) {
			return CONNECTION_STATE_PENDING;
		}

		return CONNECTION_STATE_DISCONNECTED;
	}

	/**
	 * Enables or disables auto-reconnect. If auto-reconnect is enabled,
	 * the IP Connection will try to reconnect to the previously given
	 * host and port, if the connection is lost.
	 *
	 * Default value is *true*.
	 */
	public void setAutoReconnect(boolean autoReconnect) {
		this.autoReconnect = autoReconnect;

		if(!autoReconnect) {
			autoReconnectAllowed = false;
		}
	}

	/**
	 * Returns *true* if auto-reconnect is enabled, *false* otherwise.
	 */
	public boolean getAutoReconnect() {
		return autoReconnect;
	}

	/**
	 * Sets the timeout in milliseconds for getters and for setters for which the
	 * response expected flag is activated.
	 *
	 * Default timeout is 2500.
	 */
	public void setTimeout(int timeout) {
		if(timeout < 0) {
			throw new IllegalArgumentException("Timeout cannot be negative");
		}

		responseTimeout = timeout;
	}

	/**
	 * Returns the timeout as set by setTimeout.
	 */
	public int getTimeout() {
		return responseTimeout;
	}

	/**
	 * Broadcasts an enumerate request. All devices will respond with an enumerate
	 * callback.
	 */
	public void enumerate() throws NotConnectedException {
		ByteBuffer request = createRequestPacket((byte)8, FUNCTION_ENUMERATE, null);

		sendRequest(request.array());
	}

	/**
	 * Adds a Enumerate listener.
	 */
	public void addEnumerateListener(EnumerateListener listener) {
		listenerEnumerate.add(listener);
	}

	/**
	 * Removes a Enumerate listener.
	 */
	public void removeEnumerateListener(EnumerateListener listener) {
		listenerEnumerate.remove(listener);
	}

	/**
	 * Adds a Connected listener.
	 */
	public void addConnectedListener(ConnectedListener listener) {
		listenerConnected.add(listener);
	}

	/**
	 * Removes a Connected listener.
	 */
	public void removeConnectedListener(ConnectedListener listener) {
		listenerConnected.remove(listener);
	}

	/**
	 * Adds a Disconnected listener.
	 */
	public void addDisconnectedListener(DisconnectedListener listener) {
		listenerDisconnected.add(listener);
	}

	/**
	 * Removes a Disconnected listener.
	 */
	public void removeDisconnectedListener(DisconnectedListener listener) {
		listenerDisconnected.remove(listener);
	}

	/**
	 * Registers a listener object.
	 *
	 * @deprecated
	 * Use the add and remove listener function per listener type instead.
	 */
	@Deprecated
	public void addListener(Object object) {
		boolean knownListener = false;

		if(object instanceof EnumerateListener) {
			knownListener = true;

			if (!listenerEnumerate.contains((EnumerateListener)object)) {
				listenerEnumerate.add((EnumerateListener)object);
			}
		}

		if(object instanceof ConnectedListener) {
			knownListener = true;

			if (!listenerConnected.contains((ConnectedListener)object)) {
				listenerConnected.add((ConnectedListener)object);
			}
		}

		if(object instanceof DisconnectedListener) {
			knownListener = true;

			if (!listenerDisconnected.contains((DisconnectedListener)object)) {
				listenerDisconnected.add((DisconnectedListener)object);
			}
		}

		if(!knownListener) {
			throw new IllegalArgumentException("Unknown listener type");
		}
	}

	void handleResponse(byte[] packet) {
		byte functionID = getFunctionIDFromData(packet);
		short sequenceNumber = unsignedByte(getSequenceNumberFromData(packet));

		if(sequenceNumber == 0 && functionID == CALLBACK_ENUMERATE) {
			handleEnumerate(packet);
			return;
		}

		long uid = getUIDFromData(packet);

		if(!devices.containsKey(uid)) {
			// Message for an unknown device, ignoring it
			return;
		}

		Device device = devices.get(uid);

		if(sequenceNumber == 0) {
			if(device.callbacks[functionID] != null) {
				try {
					callbackQueue.put(new CallbackQueueObject(QUEUE_PACKET, packet));
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
			}

			return;
		}

		if(functionID == device.expectedResponseFunctionID &&
		   sequenceNumber == device.expectedResponseSequenceNumber) {
			try {
				device.responseQueue.put(packet);
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
			return;
		}

		// Response seems to be OK, but can't be handled, most likely
		// a callback without registered listener
	}


	// NOTE: Assumes that socketMutex is locked
	void closeSocket() {
		if(in != null) {
			try {
				in.close();
			} catch(java.io.IOException e) {
				e.printStackTrace();
			}
			in = null;
		}

		if(out != null) {
			try {
				out.close();
			} catch(java.io.IOException e) {
				e.printStackTrace();
			}
			out = null;
		}

		if(socket != null) {
			try {
				socket.close();
			} catch(java.io.IOException e) {
				e.printStackTrace();
			}
			socket = null;
		}
	}

	static long getUIDFromData(byte[] data) {
		return (long)(data[0] & 0xFF)        | ((long)(data[1] & 0xFF) << 8) |
		      ((long)(data[2] & 0xFF) << 16) | ((long)(data[3] & 0xFF) << 24);
	}

	static byte getLengthFromData(byte[] data) {
		return data[4];
	}

	static byte getFunctionIDFromData(byte[] data) {
		return data[5];
	}

	static byte getSequenceNumberFromData(byte[] data) {
		return (byte)((((int)data[6]) >> 4) & 0x0F);
	}

	static boolean getResponseExpectedFromData(byte[] data) {
		return (((int)(data[6]) >> 3) & 0x01) == 0x01;
	}

	static byte getErrorCodeFromData(byte[] data) {
		return (byte)(((int)(data[7] >> 6)) & 0x03);
	}

	static String string(ByteBuffer buffer, int length) {
		StringBuilder builder = new StringBuilder(length);
		int i = 0;

		while(i < length) {
			char c = (char)buffer.get();
			++i;

			if (c == 0) {
				break;
			}

			builder.append(c);
		}

		while(i < length) {
			buffer.get();
			++i;
		}

		return builder.toString();
	}

	static short unsignedByte(byte data) {
		return (short)(data & 0xFF);
	}

	static int unsignedShort(short data) {
		return (int)(data & 0xFFFF);
	}

	static long unsignedInt(int data) {
		return (long)(((long)data) & 0xFFFFFFFF);
	}

	void sendRequest(byte[] request) throws NotConnectedException {
		synchronized(socketMutex) {
			if (getConnectionState() != CONNECTION_STATE_CONNECTED) {
				throw new NotConnectedException();
			}

			try {
				out.write(request);
			} catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void handleEnumerate(byte[] packet) {
		if(!listenerEnumerate.isEmpty()) {
			try {
				callbackQueue.put(new CallbackQueueObject(QUEUE_PACKET, packet));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	byte getNextSequenceNumber() {
		synchronized(sequenceNumberMutex) {
			int sequenceNumber = nextSequenceNumber + 1;
			nextSequenceNumber = sequenceNumber % 15;
			return (byte)sequenceNumber;
		}
	}

	ByteBuffer createRequestPacket(byte length, byte functionID, Device device) {
		int uid = BROADCAST_UID;
		byte options = 0;
		byte flags = 0;

		if (device != null) {
			uid = (int)device.uid;

			if (device.getResponseExpected(functionID)) {
				options = 8;
			}
		}

		options |= getNextSequenceNumber() << SEQUENCE_NUMBER_POS;

		ByteBuffer packet = ByteBuffer.allocate(length);

		packet.order(ByteOrder.LITTLE_ENDIAN);
		packet.putInt(uid);
		packet.put(length);
		packet.put(functionID);
		packet.put(options);
		packet.put(flags);

		return packet;
	}

	static String base58Encode(long value) {
		String encoded = "";

		while(value >= 58) {
			long div = value / 58;
			int mod = (int)(value % 58);
			encoded = BASE58.charAt(mod) + encoded;
			value = div;
		}

		return BASE58.charAt((int)value) + encoded;
	}

	static long base58Decode(String encoded) {
		long value = 0;
		long columnMultiplier = 1;

		for(int i = encoded.length() - 1; i >= 0; i--) {
			int column = BASE58.indexOf(encoded.charAt(i));
			value += column * columnMultiplier;
			columnMultiplier *= 58;
		}

		return value;
	}
}
