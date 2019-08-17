package steganographyRS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class Sandbox {

	public static void main(String[] args) throws IOException {
		String arquivo = "F:\\Decoy.jpg";
		Path path = Paths.get(arquivo);
		byte[] image = Files.readAllBytes(path);
		System.out.println(Arrays.toString(image));
		System.out.println(image.length % 52);

	}

}
