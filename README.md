# Data-transfer-application
  A distributed networking application in Java consisting of a transmitter and a receiver that can ensure reliable data transfer and cryptographic authentication. The application uses Java’s UDP sockets (classes DatagramPacket and DatagramSocket and their methods) and provides the necessary reliable data transfer functionality on the top of UDP’s unreliable communication services by implementing the data transfer protocol described below. The data transfer is one-directional with data flowing from the transmitter to the receiver.
  
  Receiver.java implements the receiver
  Transmitter.java implements the transmitter
  Helper.java contains methods that are common to transmitter and receiver
  RC4.java encrypts and decrypts the data using RC4 algorithm
  SecretKeyGenerator.java generates a secret key, used in RC4 algorithm
  
  
