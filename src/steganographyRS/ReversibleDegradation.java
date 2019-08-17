package steganographyRS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import steganographyRS.FileHandle;

/**
 * <p>
 * Implements the meaningless reversible degradation, with Reed-Solomon and Near
 * Field Communication
 * </p>
 * With m = 8, Default Primitive Polynomial=D^8 + D^4 + D^3 + D^2 + 1; Integer
 * Representation=285. A PC card reader is necessary for the correct
 * implementation of the meaningless reversible degradation.
 * 
 * @author Kevin de Santana
 *
 */

public class ReversibleDegradation {

	/**
	 * <p>
	 * Implements the reed-solomon encoder and errases degradationPercent of random
	 * bytes of the file, every n bytes in the file
	 * <p>
	 * Encoder routine, the last iteration is treated separately, when there is rest
	 * in the division totalSymbols / k The coding is performed every 255 symbols of
	 * the total of symbols of the file the original vector and divided into vectors
	 * of 255, at the end of the coding are recorded two files: Coded_file_name and
	 * Redundant_file_name The encrypted file and the original corrupted file t
	 * positions, redundancy are the symbols of Correction generated at each
	 * iteration of for, of each vector of the original file.
	 * 
	 * @param absolutePath       string representing the file to be degradeted
	 *                           (receives any kind of extension, i.e, .pdf, .word,
	 *                           .jpg and etc.)
	 * @param degradationPercent total percentage of the file to be degradeted (up
	 *                           to a maximum of 50%). Notice that the higher the
	 *                           percent, the more the algorithm takes to encoder
	 *                           the file.
	 * @throws IOException:              if for any reason the data to be degradeted
	 *                                   it's unavailable in the disk
	 * @throws ReedSolomonException:     exception to handle the error correction
	 *                                   code
	 * @throws CardException:            pc card reader default exception, if for
	 *                                   any reason there is no card in the reader
	 *                                   or if the reader itself is unavailable
	 * @throws NoSuchAlgorithmException: default sha256 exception
	 */
	protected void encoder(String absolutePath, String secretInformation, int degradationPercent)
			throws IOException, ReedSolomonException, NoSuchAlgorithmException {

		int k = 0;
		switch (degradationPercent) {
		case 5:
			k = 229;
			break;
		case 10:
			k = 203;
			break;
		case 15:
			k = 177;
			break;
		case 20:
			k = 151;
			break;
		case 25:
			k = 125;
			break;
		case 50:
			k = 5;
			break;
		}
		System.out.println("System ready to encode" + "\n");

		int n = 255, t = ((n - k) / 2), redundacy = n - k;
		int incrementRS8C = 0, incrementTotalVector = 0, incrementCorrVec = 0;
		Path path = Paths.get(absolutePath);
		Path pathSecretFile = Paths.get(secretInformation);		
		byte[] bytesFromFile = Files.readAllBytes(path);
		byte[] secretFile = Files.readAllBytes(pathSecretFile);
		int totalSymbols = bytesFromFile.length;
		int iteractionsRS = totalSymbols / k;
		int remainingRS = totalSymbols % k;
		int[] intUnsigned = FileHandle.signedToUnsigned(absolutePath);
		int[] encodedRS8 = new int[totalSymbols];
		int[] redundancySmb = new int[((iteractionsRS + 1) * redundacy)];
		int[] rs8c = new int[255];
		int sourcePosRemain = totalSymbols - remainingRS;
		int destPosRdd = iteractionsRS * redundacy;

		GenericGF gf = new GenericGF(285, 256, 1);
		ReedSolomonEncoder encoder = new ReedSolomonEncoder(gf);

		System.out.println("Encoding..." + "\n");

		// At each iteration the rs8c receives k symbols from the original file
		// The remaining n-k positions are reserved for generating the correction
		// symbols of these k symbols
		for (int h = 0; h < iteractionsRS; h++) {
			System.arraycopy(intUnsigned, incrementRS8C, rs8c, 0, k);
			incrementRS8C += k;

			// Once filled in, the correction symbols of the RS8C vector are generated from
			// the encoder processing
			encoder.encode(rs8c, redundacy);

			// The k symbols encoded by the RS8 at each iteration are stored in a single
			// vector
			System.arraycopy(rs8c, 0, encodedRS8, incrementTotalVector, k);
			incrementTotalVector += k;

			// The n-k correction symbols encoded by RS8 are stored in a single vector
			System.arraycopy(rs8c, k, redundancySmb, incrementCorrVec, redundacy);
			incrementCorrVec += redundacy;

		}
		// Treatment for the case where remainingRS > 0
		// Additional vector is created only when there is remainder in the division
		if (remainingRS > 0) {
			int[] remainingRS8C = new int[255]; // New vector for the remainder symbols
			// Copy of k vector remainder symbols file for the remainingRS8C
			System.arraycopy(intUnsigned, sourcePosRemain, remainingRS8C, 0, remainingRS);
			// Encoding of the remainder symbols, the coding is done independently of the
			// rest, will always be coded k symbols (even the zeros)
			encoder.encode(remainingRS8C, redundacy);
			// Copy of k vector remainder symbols file for the encodedRS8
			System.arraycopy(remainingRS8C, 0, encodedRS8, sourcePosRemain, remainingRS);
			// Copy of remainder correction symbols for the redundancySmb
			System.arraycopy(remainingRS8C, k, redundancySmb, destPosRdd, redundacy);
		}

		if (Arrays.equals(intUnsigned, encodedRS8) != true) {
			throw new ReedSolomonException(
					"Error in coding, coded vector is not equal to file vector (in unsigned integer)");
		} else {
			System.out.println("File successfully encoded! " + "\n");
		}

		// Create vector of bytes already encoded by the encoder and corrupted t
		// positions trocar por insercao de bytes especificos
		byte[] vetorCodificadoSigned = FileHandle.unsignedToSigned(encodedRS8);
		FileHandle.corruption(vetorCodificadoSigned, secretFile, t, k);

		// Create byte vector of redundancy symbols
		byte[] vetorCorrecaoSigned = FileHandle.unsignedToSigned(redundancySmb);

		// Write encoded and corrupted file
		FileHandle.writeFile(vetorCodificadoSigned, absolutePath, "Encoded");
		FileHandle.writeFile(vetorCorrecaoSigned, absolutePath, "Redundancy");

	}

	/**
	 * <p>
	 * Implements the reed-solomon decoder and correction of degradationPercent of
	 * the random erased bytes of the file, every n bytes in the file
	 * <p>
	 * 
	 * @param encodedFile        The file encoded and corrupted
	 * @param redundancyFile     The file containing the redundacy symbols for
	 *                           correction
	 * @param hashEncoder        The generated hash from the card whom encoded the
	 *                           file
	 * @param degradationPercent Corruption percentage of the file (must match the
	 *                           one in the encoding process)
	 * @throws IOException:              if for any reason the data to be degradeted
	 *                                   it's unavailable in the disk
	 * @throws ReedSolomonException:     exception to handle the error correction
	 *                                   code
	 * @throws CardException:            pc card reader default exception, if for
	 *                                   any reason there is no card in the reader
	 *                                   or if the reader itself is unavailable
	 * @throws NoSuchAlgorithmException: default sha256 exception
	 */
	protected void decoder(String absolutePath, String encodedFile, String redundancyFile, int degradationPercent)
			throws IOException, ReedSolomonException, NoSuchAlgorithmException {

		int k = 0;
		switch (degradationPercent) {
		case 5:
			k = 229;
			break;
		case 10:
			k = 203;
			break;
		case 15:
			k = 177;
			break;
		case 20:
			k = 151;
			break;
		case 25:
			k = 125;
			break;
		case 50:
			k = 5;
			break;
		}

		//String decoded = "Z:\\\\@Projeto-Degradacao-Corretiva\\\\Testes-com-RS-GF(2^16)\\\\TCC1_v1.0.2_Encoded_Decoded.pdf";

		System.out.println("Decoding..." + "\n");
		boolean writeFile = true;
		// With 15% error: k = 177 Bytes n-k = 78 Redundancy bytes t = 39
		int n = 255, t = ((n - k) / 2), totalRddSymb = (n - k), destPos = 0;
		int srcPosDec = 0, incrementRdd = 0;
		Path path = Paths.get(encodedFile);
		byte[] bytesFile = Files.readAllBytes(path);
		int totalSymb = bytesFile.length;
		int iteractionsRS = totalSymb / k;
		int remainderRS = totalSymb % k;
		int[] rs8d = new int[255];
		int destPosRS8D = rs8d.length - totalRddSymb;
		int incrementRemainder = totalSymb - remainderRS;
		int srcPosRemainder = iteractionsRS * totalRddSymb;
		int[] file = FileHandle.signedToUnsigned(encodedFile);
		int[] decoderRS8D = new int[totalSymb];

		GenericGF gf = new GenericGF(285, 256, 1);
		ReedSolomonDecoder decoder = new ReedSolomonDecoder(gf);

		// Recovers encoded file and redundancy
		int[] redundancySymb = new int[((iteractionsRS + 1) * totalRddSymb)];
		redundancySymb = FileHandle.signedToUnsigned(redundancyFile);
		int[] fromEncoded = FileHandle.signedToUnsigned(encodedFile);

		// Initiate the decoding process
		for (int h = 0; h < iteractionsRS; h++) {

			// Break single vector of symbols encoded in 255 vectors
			// Copy 177 information symbols to the vectorRS8D
			System.arraycopy(fromEncoded, incrementRdd, rs8d, 0, k);
			incrementRdd += k;

			// Concatenating Data with Redundancy and Decoding
			System.arraycopy(redundancySymb, destPos, rs8d, destPosRS8D, totalRddSymb);
			destPos += totalRddSymb;

			// Decoder
			decoder.decode(rs8d, totalRddSymb);

			// Save what was decoded into a single vector to write the decoded file
			System.arraycopy(rs8d, 0, decoderRS8D, srcPosDec, k);
			srcPosDec += k;
		}
		// Remainder
		if (remainderRS > 0) {
			int[] rs8dRemainder = new int[255]; // decoder remainder
			// Copy k rest symbols to rs8dRemainder
			System.arraycopy(fromEncoded, incrementRemainder, rs8dRemainder, 0, remainderRS);
			// Copy n-k rest symbols to rs8dRemainder
			System.arraycopy(redundancySymb, srcPosRemainder, rs8dRemainder, k, totalRddSymb);
			// Decoder process of the remainder
			decoder.decode(rs8dRemainder, totalRddSymb);
			// Copy of k remainder symbols for single vector
			System.arraycopy(rs8dRemainder, 0, decoderRS8D, incrementRemainder, remainderRS);
			// Copy of n-k remainder correction symbols for single vector
			System.arraycopy(rs8dRemainder, k, redundancySymb, srcPosRemainder, totalRddSymb);
		}

		if (writeFile == true) {
			// Save the decoded file
			byte[] decodificado = FileHandle.unsignedToSigned(decoderRS8D);
			FileHandle.writeFile(decodificado, encodedFile, "Decoded");
			System.out.println("File decoded and avaliable in disk" + "\n");
		}
		writeFile = false;

	}
}
