package steganographyRS;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.nio.file.Path;

public class FileHandle {

	/**
	 * <p>
	 * Transform a vector of signed bytes into an unsigned integer vector
	 * <p>
	 * 
	 * @param file File to be transform
	 * @return unsignedInt: Vector containing the unsigned bytes
	 */
	protected static int[] byteToIntUnsigned(byte[] file) {
		int fileLen = file.length;
		int intValue = 0;
		int[] unsignedInt = new int[fileLen];
		for (int i = 0; i < fileLen; i++) {
			intValue = file[i];
			if (intValue < 0) {
				intValue = intValue + 256;
			}
			unsignedInt[i] = intValue;
		}
		return unsignedInt;
	}

	/**
	 * <p>
	 * Reads the bytes of a user-entered file and returns its conversion in an
	 * unsigned integer vector
	 * <p>
	 * 
	 * @param file The file to be transform into unsigned integer
	 * @return unsignedInt: Vector containing the conversion
	 * @throws IOException: if for any reason the file it's unavailable in the disk
	 */
	protected static int[] signedToUnsigned(String file) throws IOException {
		Path path = Paths.get(file);
		byte[] bytesArquivoLido = Files.readAllBytes(path);
		int[] unsignedInt = byteToIntUnsigned(bytesArquivoLido);
		return unsignedInt;
	}

	/**
	 * <p>
	 * Unsigned int to signed byte
	 * <p>
	 * 
	 * @param unsignedInt Integer vector to be transform
	 * @return signedByte: The transformed signed byte
	 * @throws IOException: if for any reason the file it's unavailable in the disk
	 */
	protected static byte[] unsignedToSigned(int[] unsignedInt) throws IOException {
		int intByte = 0;
		int size = unsignedInt.length;
		byte[] signedByte = new byte[size];
		for (int i = 0; i < signedByte.length; ++i) {
			intByte = unsignedInt[i];
			signedByte[i] = intUnToSig(intByte);
		}
		return signedByte;
	}

	/**
	 * <p>
	 * Transforms a single unsigned integer into signed integer
	 * <p>
	 * 
	 * @param unsigned The unsigned byte
	 * @return intvalue: The signed int
	 */
	private static byte intUnToSig(int unsigned) {
		byte intvalue = 0;
		if (unsigned <= 256) {
			intvalue = (byte) unsigned;
			if (intvalue > 127) {
				intvalue = (byte) (intvalue - 256);
			}
		}
		return intvalue;
	}

	/**
	 * <p>
	 * Identifies the directory, file name, and file extension from a user-entered
	 * file. Used to write to disk in encoding, decoding, correction and hash
	 * preserving the original file name
	 * <p>
	 * 
	 * @param absolutePath String containing the absolute path of the file
	 * @return directoryFileExtension: an array of string where the index [0]
	 *         indicates the directory, the index [1] the file name and the index
	 *         [2] the file extension
	 */
	protected static String[] nameExtension(String absolutePath) {
		File f = new File(absolutePath);

		String fileExtension = "";
		int in = f.getAbsolutePath().lastIndexOf("\\");
		if (in > -1) {
			fileExtension = f.getAbsolutePath().substring(in + 1);
		}
		// Directory
		String directory = absolutePath.replace(fileExtension, "");

		// Get name and extension
		int extensionIndex = fileExtension.lastIndexOf('.');
		int sizeNameExtension = fileExtension.length();
		String extension = fileExtension.substring(extensionIndex, sizeNameExtension);
		String file = fileExtension.substring(0, extensionIndex);

		// Stores directory, file name, and file extension in an array of strings
		String[] directoryFileExtension = new String[3];
		directoryFileExtension[0] = directory;
		directoryFileExtension[1] = file;
		directoryFileExtension[2] = extension;

		return directoryFileExtension;
	}

	/**
	 * <p>
	 * Write file to disk
	 * <p>
	 * 
	 * @param fileBytes    vector of bytes to be written
	 * @param absolutePath location where the file will be saved
	 * @param fileSufix    the suffix that the file will receive after the name and
	 *                     before the extension
	 * @return newFile the generated file
	 * @throws IOException: if for any reason the file it's unavailable in the disk
	 */
	protected static File writeFile(byte[] fileBytes, String absolutePath, String fileSufix) throws IOException {

		String[] directoryFileExtension = nameExtension(absolutePath);
		String directory = directoryFileExtension[0];
		String file = directoryFileExtension[1];
		String extension = directoryFileExtension[2];
		String fullFile = directory + file + "_" + fileSufix + extension;

		// Writes the file with the given name and read extension
		File newFile = new File(fullFile);
		FileOutputStream stream = new FileOutputStream(newFile);
		stream.write(fileBytes);
		stream.close();
		return newFile;
	}

	/**
	 * <p>
	 * Corrupts t% of file with random bytes
	 * <p>
	 * 
	 * @param file the vector of the file that will be corrupted
	 * @param t    the number of errors that will be generated for every n bytes
	 * @param k    Information symbols each vector needs a separate increment
	 *             variable
	 */
	protected static void corruption(byte[] file, byte[] secretFile, int t, int k) {

		SecureRandom random = new SecureRandom();
		int interactions = file.length / k;
		int remainder = file.length % k;
		int incrementData = 0;
		int incrementSecret = 0;
		int incrementCorruption = 0;
		int startRmndCopy = file.length - remainder;
		int randomIndex = random.nextInt(k);
		int randomIndexCorru = 0;
		byte[] tempCorru = new byte[k];
		byte[] tempSecret = new byte[t];
		byte[] corruption = new byte[t];
		random.nextBytes(corruption);

		// The original vector is divided into a vector of k positions, according to the
		// number of iterations
		for (int x = 0; x < interactions; x++) {

			System.arraycopy(file, incrementData, tempCorru, 0, k);
			incrementData += k;
			System.arraycopy(secretFile, incrementSecret, tempSecret, 0, t);
			incrementSecret += t;

			// Every k symbol of the original file, corrupted t random positions of these k
			// positions
			for (int n = 0; n < t; n++) {
				randomIndexCorru = random.nextInt(corruption.length);
				System.arraycopy(tempSecret, randomIndexCorru, tempCorru, randomIndex, 1);
			}
			// Returns the original vector, now corrupted t random positions every k symbols
			System.arraycopy(tempCorru, 0, file, incrementCorruption, k);
			incrementCorruption += k;
		}
		if (remainder > 0) {
			byte[] remainderTempCorr = new byte[remainder];
			System.arraycopy(file, startRmndCopy, remainderTempCorr, 0, remainder);
			for (int m = 0; m < remainder; m++) {
				randomIndexCorru = random.nextInt(corruption.length);
				System.arraycopy(secretFile, randomIndexCorru, file, startRmndCopy, 1);
			}
		}
	}

	/**
	 * <p>
	 * Erases the encoded, the decoded, redundancy and hash files on the disk
	 * <p>
	 * 
	 * @param encoded    The file encoded by the RS encoder
	 * @param decoded    The file decoded by the RS decoder
	 * @param hash       The hash generated in the encoder process
	 * @param redundancy The error correction symbols, stored in a file
	 * @throws ReedSolomonException: If the files are not available to be erase
	 */
	protected static void eraseFiles(String absolutePath, String decoded, String hash, String redundancy)
			throws ReedSolomonException {

		try {
			Path pathFile = Paths.get(absolutePath);
			Path pathDecoded = Paths.get(decoded);
			Path pathHash = Paths.get(hash);
			Path pathRedundancy = Paths.get(redundancy);

			Files.delete(pathFile);
			Files.delete(pathDecoded);
			Files.delete(pathHash);
			Files.delete(pathRedundancy);
			System.out.println("The files has been successful erased" + "\n");

		} catch (Exception e) {
			throw new ReedSolomonException("Error while erasing files");
		}
	}
}