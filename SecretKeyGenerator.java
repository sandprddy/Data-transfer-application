
import java.util.Random;
import java.security.SecureRandom;

/**
 * This program generates 128 bits secret key which is distributed to Transmitter and receiver
 * 
 * @author Sandeep Reddy
 * @since 2015-11-28
 */
public class SecretKeyGenerator {
	private static final int SEC_KEY_SIZE = 16;
	public static void main(String[] args) {

		//generating random data
		Random ranNum = new SecureRandom();
		byte[] K = new byte[SEC_KEY_SIZE]; 
		ranNum.nextBytes(K);

		//Printing the secret random key
		System.out.println("\nThe secretKey bytes: ");
		for(int i=0; i<SEC_KEY_SIZE;i++){
			System.out.print(K[i]+"	");
		}
	}//main

}//class
