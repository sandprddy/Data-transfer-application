
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Random;

/**
 * Transmitter program generates 500 bytes of random and transmits it. 
 * This program uses Java's UDP sockets (classes DatagramPacket and DatagramSocket and their methods)
 * and provides a reliable data transfer functionality on the top of UDP’s unreliable communication 
 * services by implementing a data transfer protocol
 * 
 * @author Group 13 {Navanitha Rao(114203236), Sandeep R Panuganti(114440747)} 
 * @since 2015-11-28
 */
public class Transmitter {

	//Data to be transmitted
	private static byte[] data = new byte[Helper.DATA_LENGTH];

	//Header fields
	private static byte packetHeader;
	private static byte[] sequenceNumber = new byte[4];
	private static byte payloadLength = 0;

	//integrity field
	private static byte[] integrityCheck = new byte[4];

	/**
	 * This method takes payload as input, prepends respective header fields
	 * It also calculates and appends integrity check
	 * 
	 * @param payload This is the payload to be added in packet
	 * @return sentMessage This is the packet to be sent to receiver
	 */
	public static byte[] framePacket(byte[] payload) {

		byte[] sentMessage = new byte[payload.length + 10];

		byte[] temp = new byte[payload.length+6];
		for(int i=0; i<temp.length; i++) {
			if(i == 0){
				temp[i] = packetHeader;
				sentMessage[i] = packetHeader;
			}
			if(i>0 && i<5){
				temp[i] = sequenceNumber[i-1];
				sentMessage[i] = sequenceNumber[i-1];
			}	
			if(i == 5){
				temp[i] = payloadLength;
				sentMessage[i] = payloadLength;
			}
			if(i>5 && i<(sentMessage.length)){
				temp[i] = payload[i-6];
				sentMessage[i] = payload[i-6];
			}
		}

		//calculating integrity check
		integrityCheck = Helper.integrityCheckCalculate(temp);

		//copying integrity check bytes to last 4 bytes
		for(int i=(sentMessage.length-4);i<sentMessage.length;i++){
			sentMessage[i] = integrityCheck[i-(sentMessage.length-4)];
		}
		return sentMessage;
	}

	/**
	 * This method sends a packet and receives ACK message. 
	 * If ACK isn't received correctly,it resends the data maximum of three times.
	 * 
	 * @param sentMessage  This is the message to be sent to receiver
	 * @param clientSocket 
	 * @param server This is the address of server
	 * @return receiveCheck Equal to 1 if data is sent successfully or else equal to 0
	 */
	public static int sendPacket(byte[] sentMessage, DatagramSocket clientSocket, InetAddress server){

		int maxCount = 4; 	 //maximum number of times a packet can be sent
		int tryCount = 0;    //number of times present packet is sent
		int receiveCheck = 0;//receiveCheck = 1 indicates acknowledgement packet is received correctly
		int timer = 1000;   //initially timer is set to 1000ms

		// creating the receive UDP packet
		byte[] receivedMessage = new byte[Helper.REC_MSG_SIZE];
		DatagramPacket receivedPacket = new DatagramPacket(receivedMessage,receivedMessage.length);
		// creating the UDP packet to be sent
		DatagramPacket sentPacket;
		//ACK isn't received correctly, resend the packet for a maximum of 3 times
		do {
			try {
				// setting the timeout for the socket
				clientSocket.setSoTimeout(timer);

				sentPacket = new DatagramPacket(sentMessage, sentMessage.length,
						server, Helper.SERVER_PORT_NUM);
				// sending the UDP packet to the server
				clientSocket.send(sentPacket);

				// the timeout timer starts ticking here
				clientSocket.receive(receivedPacket);

				//if the ACK packet is received from unknown host exception is thrown
				if(!receivedPacket.getAddress().equals(server))
					throw new IOException("RECEIVED FROM UNKNOWN HOST");

				//Verifies the ACK packet received. receiveCheck=1 indicates a correct ACK 
				receiveCheck = ackCorrectness(receivedMessage,(int)sentMessage[5]);
			} catch(SocketTimeoutException e )	{
				System.out.print("TIMEOUT. ");
				timer = timer * 2;
			} catch (IOException e) {
				System.out.println(e);
			}
			//if ACK isn't received correctly, double the timer
			tryCount++;
			if(receiveCheck==0)
				System.out.println("RESENDING COUNT: "+tryCount);
		} while(receiveCheck == 0 && tryCount < maxCount-1);

		return receiveCheck;
	}

	/**
	 * This method verifies the correctness of the received ACK message
	 *  
	 * @param message This is the received ACK message from receiver
	 * @return 1 if message is correct or else 0
	 */
	public static int ackCorrectness(byte[] message,int sentMessageLen){

		byte[] temp = new byte[5];
		for(int i=0;i<temp.length;i++){
			temp[i] = message[i];
		}

		//checks if locally calculated integrity check equals received integrity check field
		byte[] integrityBytes = Helper.integrityCheckCalculate(temp);		
		for(int i=0;i<integrityBytes.length;i++){
			if(integrityBytes[i] != message[i+5])
				return 0;
		}
		//checks if ack packet is of correct ack number and packet type
		if(!(message[0]==(byte)0xff))
			return 0;
		byte[] temp1 = Arrays.copyOf(sequenceNumber, sequenceNumber.length);
		for(int i=0;i<sentMessageLen;i++){
			temp1 = Helper.increment(temp1);
		}
		for(int i =0;i<4;i++){
			if(!(temp1[i]==message[i+1]))
				return 0;
		}
		return 1;
	}	

	/**
	 * This is the main method which creates 500 bytes of random data.
	 * It splits it in such a way that maximum length of data block is 30 bytes and transmits it .
	 * It receives the acknowledgement from receiver for each correctly send packet
	 * Prints COMMUNICATION ERROR and exits with status 1 unsuccessful transmission
	 * 
	 * @param args unused
	 * @return Nothing
	 */
	public static void main(String[] args) {

		//Random data to be transmitted
		Random randGen = new Random();
		randGen.nextBytes(data);

		InetAddress server = null;
		// creating the UDP client socket (randomly chosen client port number)
		DatagramSocket clientSocket = null;
		// creating the IP address object for the server machine
		// method : loop back the request to the same machine
		try {
			server = InetAddress.getLocalHost();

			clientSocket = new DatagramSocket();
		} catch (UnknownHostException e) {
			System.out.println(e + "Unable to determine local host address");
		} catch (SocketException e) {
			System.out.println(e + "ERROR IN DATAGRAMSOCKET CREATION");
		}

		packetHeader = (byte)0x55;//until last packet this field remains 0x55
		int datalen = Helper.MPS;//until last packet this field remains MPS
		int dataSent = 0; //number of bytes out of 500 bytes that are transmitted

		//initializing sequenceNumber to INITIAL_SEQUENCE
		for(int i=0;i<sequenceNumber.length;i++){
			sequenceNumber[i] = Helper.INITIAL_SEQUENCE[i];
		}

		System.out.println("Sending the data to Receiver");
		while(datalen > 0){

			System.out.print("Sending a packet with sequence Number: ");
			for(int i=0;i<4;i++)
				System.out.print(sequenceNumber[i]+" ");
			System.out.println("Payload is: ");
			for(int i=dataSent;i<dataSent+datalen;i++)
				System.out.print(data[i]+" ");
			System.out.println();	

			//extracting payload from 500 bytes of data
			payloadLength = (byte)datalen;
			byte[] payload = new byte[datalen];
			for(int i=0;i<datalen;i++){
				payload[i] = data[dataSent+i];
			}

			//framing the send packet
			byte[] sentMessage = framePacket(payload);
			//Transmitting the send packet
			int receiveCheck = sendPacket(sentMessage,clientSocket,server);

			if(receiveCheck == 1) {     //if packet is sent successfully
				for(int i=0;i<datalen;i++){
					sequenceNumber = Helper.increment(sequenceNumber);
				}
				System.out.println("ACK received. Packet is sent successfully");
				dataSent = dataSent+datalen;
			}
			else {                       //if packet isn't sent successfully
				System.out.println("COMMUNICATION ERROR");
				System.exit(1);
			}
			//if remaining data to send to receiver is less than MPS, 
			if(data.length-dataSent < Helper.MPS){
				datalen = data.length-dataSent;
				packetHeader = (byte) 0xaa;
			}	
		}
		System.out.println("Data is sent successfully :-)");
	}//main

}//class
