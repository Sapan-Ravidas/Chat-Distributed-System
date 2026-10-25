package com.sapan.chat.message.service;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sapan.chat.message.exception.MessageException;

@Service
public class ImageStorageService {

	private static final int MAX_IMAGE_EDGE = 1920;
	private static final long MAX_IMAGE_PIXELS = 25_000_000L;

	private final Path imageDirectory;

	public ImageStorageService(@Value("${chat.media.directory:./data/images}") String imageDirectory) {
		this.imageDirectory = Path.of(imageDirectory).toAbsolutePath().normalize();
	}

	public StoredImage store(MultipartFile upload) {
		if (upload == null || upload.isEmpty()) {
			throw new MessageException(HttpStatus.BAD_REQUEST, "An image file is required.");
		}
		if (upload.getSize() > 10L * 1024 * 1024) {
			throw new MessageException(HttpStatus.PAYLOAD_TOO_LARGE, "Images must be 10 MB or smaller.");
		}

		String declaredMediaType = upload.getContentType();
		if (!"image/jpeg".equalsIgnoreCase(declaredMediaType) && !"image/png".equalsIgnoreCase(declaredMediaType)) {
			throw new MessageException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only JPEG and PNG images are supported.");
		}

		byte[] original;
		try {
			original = upload.getBytes();
		} catch (IOException exception) {
			throw new MessageException(HttpStatus.BAD_REQUEST, "The uploaded image could not be read.");
		}

		DecodedImage decoded = decode(original);
		if (!decoded.mediaType().equalsIgnoreCase(declaredMediaType)) {
			throw new MessageException(HttpStatus.BAD_REQUEST, "The declared content type does not match the image.");
		}

		byte[] bytes;
		try {
			bytes = encodeJpeg(resize(decoded.image()));
		} catch (IOException exception) {
			throw new MessageException(HttpStatus.INTERNAL_SERVER_ERROR, "The image could not be compressed.");
		}

		String imageKey = UUID.randomUUID().toString();
		try {
			Files.createDirectories(imageDirectory);
			Files.write(imageDirectory.resolve(imageKey + ".jpg"), bytes,
					StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
		} catch (IOException exception) {
			throw new MessageException(HttpStatus.INTERNAL_SERVER_ERROR, "The compressed image could not be stored.");
		}
		return new StoredImage(imageKey, decoded.mediaType(), original.length, bytes.length);
	}

	public Path resolve(String imageKey) {
		Path image = imageDirectory.resolve(imageKey + ".jpg").normalize();
		if (!image.getParent().equals(imageDirectory)) {
			throw new MessageException(HttpStatus.NOT_FOUND, "Image not found.");
		}
		return image;
	}

	public void delete(String imageKey) {
		try {
			Files.deleteIfExists(resolve(imageKey));
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to remove an uncommitted image file.", exception);
		}
	}

	private BufferedImage resize(BufferedImage source) {
		double scale = Math.min(1.0, (double) MAX_IMAGE_EDGE
				/ Math.max(source.getWidth(), source.getHeight()));
		int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
		int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
		BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		var graphics = target.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		graphics.drawImage(source, 0, 0, width, height, null);
		graphics.dispose();
		return target;
	}

	private DecodedImage decode(byte[] original) {
		try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(original))) {
			if (input == null) {
				throw new MessageException(HttpStatus.BAD_REQUEST, "The uploaded file is not a valid image.");
			}
			Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
			if (!readers.hasNext()) {
				throw new MessageException(HttpStatus.BAD_REQUEST, "The uploaded file is not a valid image.");
			}

			ImageReader reader = readers.next();
			try {
				reader.setInput(input, true, true);
				String format = reader.getFormatName().toLowerCase(Locale.ROOT);
				String mediaType = switch (format) {
					case "jpg", "jpeg" -> "image/jpeg";
					case "png" -> "image/png";
					default -> throw new MessageException(
							HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only JPEG and PNG images are supported.");
				};
				int width = reader.getWidth(0);
				int height = reader.getHeight(0);
				if ((long) width * height > MAX_IMAGE_PIXELS) {
					throw new MessageException(HttpStatus.PAYLOAD_TOO_LARGE, "Image dimensions are too large.");
				}
				BufferedImage image = reader.read(0);
				if (image == null) {
					throw new MessageException(HttpStatus.BAD_REQUEST, "The uploaded file is not a valid image.");
				}
				return new DecodedImage(image, mediaType);
			} finally {
				reader.dispose();
			}
		} catch (IOException exception) {
			throw new MessageException(HttpStatus.BAD_REQUEST, "The uploaded file is not a valid image.");
		}
	}

	private byte[] encodeJpeg(BufferedImage image) throws IOException {
		Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
		if (!writers.hasNext()) {
			throw new IOException("No JPEG encoder is available.");
		}

		ImageWriter writer = writers.next();
		try (ByteArrayOutputStream output = new ByteArrayOutputStream();
				ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
			writer.setOutput(imageOutput);
			ImageWriteParam parameters = writer.getDefaultWriteParam();
			parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			parameters.setCompressionQuality(0.82f);
			writer.write(null, new IIOImage(image, null, null), parameters);
			imageOutput.flush();
			return output.toByteArray();
		} finally {
			writer.dispose();
		}
	}

	public record StoredImage(String imageKey, String originalMediaType, long originalSize, long storedSize) {
	}

	private record DecodedImage(BufferedImage image, String mediaType) {
	}
}
