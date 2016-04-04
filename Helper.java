
/**
 * Helper program contains common methods and variables used in Transmitter and Receiver programs 
 * Secret Key is generated randomly and fixed in this program which is used by Transmitter and receiver
 * Initial sequence is kept as 0
 * 
 * @author Group 13 {Navanitha Rao(114203236), Sandeep R Panuganti(114440747)} 
 * @since 2015-11-28
 */
public class Helper {
	//SecretKey generated randomly and distributed  to transmitter and receiver
	public static final byte[] SECRET_KEY = { -35, -44, -124, 15, 55, -8, 33,	 
			-107, -116, 37, 89, -30, -20, 65, -10, 50};

	//Initial sequence number taken as 0
	public static final byte[] INITIAL_SEQUENCE = {0, 0, 0, 1};

	//Total data Length to be sent
	public static final  int DATA_LENGTH = 500;

	//Maximum payload size
	public static final int MPS = 30;

	//Receive message size
	public static final int REC_MSG_SIZE = 9;

	// server port number - same as the port that the server is listening on!
	public static final int SERVER_PORT_NUM = 10688;

	/**
	 * This method increments an array of bytes by 1
	 * 
	 * @param A
	 * @return A This is the incremented array
	 */
	public static byte[] increment(byte[] A) {
		for (int i = (A.length - 1); i >= 0; i--) {
			if(A[i] < 127) {
				A[i]++;
				break;	

			}
			else {
				A[i] = 0;
				A[i-1]++;
			}
		}
		return A;
	}

	/**
	 * This Method compares two byte arrays of same length.
	 * It equates each element of data1 to corresponding element in data2
	 * 
	 * @param data1 This is one of two arrays to be equated
	 * @param data2 This is one of two arrays to be equated
	 * @return true if data1 equals data2, else false
	 */
	public static boolean equalityCheck(byte[] data1,byte[] data2){
		for(int i=0;i<data1.length;i++){
			if(data1[i] != data2[i])
				return false;
		}
		return true;
	}

	/**
	 * This method encrypts the data calculates the integrity check of the encrypted data 
	 * 
	 * @param message This is the message for which integrity check is to be calculated
	 * @return integrityCheck This is the calculated integrity check of the data
	 */
	public static byte[] integrityCheckCalculate(byte[] message){

		//padding zeroes if necessary
		int length =0;
		if((message.length)%4 != 0)
			length = (4-(message.length)%4);
		byte[] temp = new byte[message.length + length];
		for(int i=0;i<temp.length;i++){
			if(i>=0 && i<message.length)
				temp[i] = message[i];
			else
				temp[i] = 0;
		}

		//encrypting the header+payload+padded zeroes
		temp = RC4.encrypt(temp, SECRET_KEY);

		//XOR'ing the bytes and compressing it to 4 bytes
		byte[] integrityCheck = {temp[0],temp[1],temp[2],temp[3]};
		for(int i=4;i<temp.length-3;i++){
			integrityCheck[0] = (byte) (integrityCheck[0] ^ temp[i]);
			integrityCheck[1] = (byte) (integrityCheck[1] ^ temp[i+1]);
			integrityCheck[2] = (byte) (integrityCheck[2] ^ temp[i+2]);
			integrityCheck[3] = (byte) (integrityCheck[3] ^ temp[i+3]);
		}
		return integrityCheck;
	}

}
