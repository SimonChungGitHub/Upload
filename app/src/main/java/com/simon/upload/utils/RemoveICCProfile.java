package com.simon.upload.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 移除 jpg 圖片 ICC_PROFILE 內容
 * 舊版 windows 圖片瀏覽程式無法開啟含有 ICC_PROFILE 內容的圖片
 */
public class RemoveICCProfile {
    private final String Tag = "ICC_PROFILE";
    private final Path path;

    public RemoveICCProfile(Path path) {
        this.path = path;
        if (searchTag()) removeTag();
    }

    private boolean searchTag() {
        try (FileInputStream fis = new FileInputStream(path.toFile());
             FileChannel fc = fis.getChannel()) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024 * 1024);
            while (fc.read(byteBuffer) != -1) {
                byteBuffer.flip();
                String searchString = new String(byteBuffer.array());
                if (searchString.contains(Tag)) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void removeTag() {
        Path temp = Paths.get(path.toString().toLowerCase().replace(".jpg", ""));
        try {
            Files.copy(path, temp, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (FileInputStream fis = new FileInputStream(temp.toFile());
             FileChannel readChannel = fis.getChannel();
             FileOutputStream fos = new FileOutputStream(path.toFile());
             FileChannel writeChannel = fos.getChannel()) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(256 * 1024);
            while (readChannel.read(byteBuffer) != -1) {
                String searchString = new String(byteBuffer.array());
                if (searchString.contains(Tag)) {
                    ByteBuffer byteBuffer2 = ByteBuffer.wrap(byteBuffer.array());
                    byteBuffer.clear();
                    boolean marker = false;
                    byte byte_0xFF = 0;
                    for (byte b : byteBuffer2.array()) {
                        if (marker) {
                            marker = false;
                            if ((b & 0xFF) != 0xE2) {
                                byteBuffer.put(byte_0xFF);
                                byteBuffer.put(b);
                            }
                        } else if ((b & 0xFF) == 0xFF) {
                            marker = true;
                            byte_0xFF = b;
                        } else {
                            byteBuffer.put(b);
                        }
                    }
                }
                byteBuffer.flip();
                writeChannel.write(byteBuffer);
                byteBuffer.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                Files.delete(temp);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
