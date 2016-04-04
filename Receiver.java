
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * Receiver program receives the data, verifies it and sends an ACK packet 
 * This program uses Java's UDP sockets (classes DatagramPacket and DatagramSocket and their methods)
 * and provides a reliable data transfer functionality on the top of UDP’s unreliable communication 
 * services by implementing a data transfer protocol
 * 
 * @author Group 13 {Navanitha Rao(114203236), Sandeep R Panuganti(114440747)} 
 * @since 2015-11-28
 */
public class Receiver {

	//sequence number of expected byte
	private static byte[] sequenceNumber = new byte[4];
	//previous sequence number is used in the case of unsuccessful transmission of ACK
	private static byte[] previousSequenceNumber = new byte[4];
	//Data to be received from receiver
	private static byte[] data = new byte[Helper.DATA_LENGTH];
	private static int receivedLen = 0; //number of bytes of data received in total

	/**
	 * This method checks for the correctness of the received data packet
	 * It verifies integrity check, packet type, sequence Number inorderness, payload length
	 * It also increases sequenceNumber if data is received correctly and appends the received data
	 * to the total data received 
	 * 
	 * 
	 * @param receivedMessage Data to be verified
	 * @return true if packet is received correctly,else false
	 */
	public static boolean recvPacketCorrectness(byte[] receivedMessage){

		byte packetType = receivedMessage[0];
		byte[] receivedSequenceNumber = sequenceNumberExtract(receivedMessage);
		byte[] payload = payloadExtract(receivedMessage);
		int payloadLength = receivedMessage[5];

		System.out.print("Received packet with sequence Number :");
		for(int i=0;i<4;i++)
			System.out.print(receivedSequenceNumber[i]+" ");
		System.out.println();

		//verifying integrity bytes with locally calculated bytes
		if(!(integrityCheckVerify(receivedMessage)))
			return false;
		//verifying whether it is an inorder sequenceNumber
		if(!((Helper.equalityCheck(sequenceNumber,receivedSequenceNumber)
				||Helper.equalityCheck(previousSequenceNumber,receivedSequenceNumber))))
			return false;
		//verifying whether packet type is either 0x55h or 0xaah and payload length is less than MPS
		if(!((packetType==(byte)0x55 || packetType==(byte)0xaa) && Helper.MPS>=(int)payloadLength))
			return false;

		else{
			//if sequence number of received packet is equal to the expected, increase the sequence number
			if(Helper.equalityCheck(sequenceNumber,receivedSequenceNumber)){
				for(int i=0;i<4;i++){
					previousSequenceNumber[i] = sequenceNumber[i];
				}

				for(int i=0;i<payloadLength;i++){
					sequenceNumber = Helper.increment(sequenceNumber);
				}

				appendData(data,receivedLen,payload);
				receivedLen = receivedLen+payload.length;
			}
			return true;
		}
	}

	/**
	 * This method extracts the sequence number from the received packet
	 * 
	 * @param message This is the data from which from sequence number is extracted
	 * @return sequenceNumber sequence number of the packet received
	 */
	public static byte[] sequenceNumberExtract(byte[] message){
		byte[] sequenceNumber = new byte[4];
		for(int i=1;i<5;i++){
			sequenceNumber[i-1] = message[i];
		}
		return sequenceNumber;
	}

	/**
	 * This method extracts payload from the received packet
	 * 
	 * @param message This is the data from which from sequence number is extracted
	 * @return payload Payload of the packet received
	 */
	public static byte[] payloadExtract(byte[] message){
		int length = (int)message[5]; 
		byte[] payload = new byte[length];
		for(int i=6;i<length+6;i++){
			payload[i-6] = message[i];
		}
		return payload;
	}

	/**
	 * This method is used in other method to append payload to total data
	 * 
	 * @param data Total data
	 * @param receivedLen payload length
	 * @param payload payload to be appended
	 */
	public static void appendData(byte[] data,int receivedLen,byte[] payload){
		int length = receivedLen+payload.length;
		for(int i=receivedLen;i<length;i++){
			data[i] = payload[i-receivedLen];
		}
	}

	/**
	 * This method takes sequenceNumber as input, increments it locally,
	 * prepends packet type and calculates the integrity check and appends it to input
	 * 
	 * @param sentMessage This is the payload to be added in packet
	 * @return sentMessage This is the packet to be sent to receiver
	 */
	public static byte[] frameSentMessage(byte[] sentMessage) {

		//Header fields of ACK
		byte packetType = (byte) 0xff;
		byte[] ackNumber = new byte[4];

		//integrity check
		byte[] integrityCheck = new byte[4];
		byte[] temp = new byte[5];
		for(int i=0;i<4;i++){
			ackNumber[i] = sequenceNumber[i];
		}
		//ackNumber = Helper.increment(ackNumber);
		for(int i=0; i<5; i++) {
			if(i == 0){
				sentMessage[i] = packetType;
				temp[i] = packetType;
			}
			if(i>0 && i<5){
				sentMessage[i] = ackNumber[i-1];
				temp[i] = ackNumber[i-1];
			}	
		}
		integrityCheck = Helper.integrityCheckCalculate(temp); //calculating integrity check
		//appending it to the input
		for(int i=5; i<9; i++) {
			sentMessage[i] = integrityCheck[i-5];
		}
		return sentMessage;
	}



	/**
	 * This method is used verify the integrity check to locally calculated integrity check
	 * 
	 * @param message The data for which integrity check is to be verified
	 * @return true if integrity check equals locally calculated one, else false
	 */
	public static boolean integrityCheckVerify(byte[] message){

		int length = message.length;
		byte[] integrityBytes = new byte[4];
		for(int i=0;i<4;i++){
			integrityBytes[i] = message[length-4+i];
		}
		byte[] temp = new byte[length-4];
		for(int i=0;i<temp.length;i++){
			temp[i] = message[i];
		}
		byte[] check = Helper.integrityCheckCalculate(temp);				
		return Helper.equalityCheck(integrityBytes,check);	
	}

	/**
	 * This is the main method which receives 500 bytes of random data from the transmitter.
	 * It receives segments of data and verifies the data before sending ACK packet
	 * Appends the data to total data and prints it after the last packet is received and acknowledged
	 * 
	 * @param args unused
	 * @return Nothing
	 */
	public static void main(String[] args) {

		final int RECEIVED_MSG_SIZE = 100;  //maximum size of packet that can be received
		final int SENT_MSG_SIZE = 9;       // sent message size

		// server socket, listening on port serverPortNum
		DatagramSocket serverSocket = null;
		try {
			serverSocket = new DatagramSocket(Helper.SERVER_PORT_NUM);
		} catch (SocketException e) {
			System.out.println("ERROR IN DatagramserverReceiverSocket ");
			System.exit(1);
		}

		// send and receive data buffers
		byte[] torecv = new byte[RECEIVED_MSG_SIZE];
		byte[] sentMessage = new byte[SENT_MSG_SIZE];

		// tosend and toreceive UDP packets
		DatagramPacket receivePacket = new DatagramPacket(torecv,
				torecv.length);
		DatagramPacket sentPacket = new DatagramPacket(sentMessage, sentMessage.length);

		for(int i=0;i<4;i++){
			sequenceNumber[i] = Helper.INITIAL_SEQUENCE[i];
			previousSequenceNumber[i] = Helper.INITIAL_SEQUENCE[i];
		} 

		int isDataPrint = 0; //To make sure that data is printed only once if the Last ACK isn't transmitted properly

		while(true) {

			//receive the packet from transmitter
			try {
				serverSocket.receive(receivePacket);
			} catch (IOException e) {
				System.out.println("ERROR IN DatagramgramserverReceiverSocket ");
			}

			//copying the received message to array
			byte[] receivedMessage = new byte[receivePacket.getLength()];
			int length = receivePacket.getLength();
			for(int i=0;i<length;i++){
				receivedMessage[i] = torecv[i];
			}

			//extracting client address and port
			InetAddress clientAddress = receivePacket.getAddress();                 
			int clientPort = receivePacket.getPort();   

			// setting up the response UDP packet object
			sentPacket.setAddress(clientAddress);   // destination IP address
			sentPacket.setPort(clientPort); 		// destination port number
			sentPacket.setLength(SENT_MSG_SIZE);    // actual data length

			//Verify whether data is received correctly before sending ACK
			if(recvPacketCorrectness(receivedMessage)){

				try {
					sentMessage = frameSentMessage(sentMessage); //framing the ACK
					serverSocket.send(sentPacket);  // sending the response to the client

					//if last packet, close the socket and print the data
					if(receivedMessage[0] == (byte)0xaa && isDataPrint == 0){
						System.out.println("Received the total data from IP Address: "+clientAddress+" and Port No: "+clientPort);
						for(int i=0;i<data.length;i++){
							System.out.print(data[i]+" ");
						}
						isDataPrint = 1;
						serverSocket.close();
						System.exit(0);
					}
				} catch (IOException e) {
					System.out.println("ERROR IN DatagramgramserverSENDSocket ");
				}
			}	
		}
	}//main
}//class