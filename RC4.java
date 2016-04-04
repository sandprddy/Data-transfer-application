
/**
 * This program implements RC4 algorithm
 * 
 * @author Group 13 {Navanitha Rao(114203236), Sandeep R Panuganti(114440747)} 
 * @since 2015-11-28
 */
public class RC4 {
	private static byte[] S = new byte[256];
	private static byte[] T = new byte[256];

	/**
	 * This method initializes T and S matrices
	 * 
	 * @param K is the secret key
	 */
	public static void initialization(byte[] K){
		int i;
		int keylen = K.length;
		for(i=0;i<256;i++){
			S[i] = (byte)i;
			T[i] = K[i%keylen];
		}
	}

	/**
	 * This method does initial permutation
	 */
	public static void initialPermutation(){

		int i=0,j = 0;
		for(i=0;i<256;i++){
			j = (j+S[i]+T[i]) & 0xff;
			byte temp = S[i];
			S[i] = S[j];
			S[j] = temp;
		}
	}

	/**
	 * This method does the encryption of the data
	 * 
	 * @param data is the data to be incremented
	 * @param K is the secret key
	 * @return encrypted data
	 */
	public static byte[] encrypt(byte[] data, byte[] K){
		int len = data.length;
		byte[] encryptData = new byte[len];

		initialization(K);

		initialPermutation();

		/*cipher generation*/
		int i=0,j=0;
		int x = len;
		while(x>0){
			i = (i+1) & 0xff;
			j = (j+S[i]) & 0xff;
			byte temp = S[i];
			S[i] = S[j];
			S[j] = temp;
			int t = (S[i]+S[j]) & 0xff;
			byte k = S[t];
			encryptData[len-x] = (byte) (k^data[len-x]);
			x--;
		}
		return encryptData;
	}
}
