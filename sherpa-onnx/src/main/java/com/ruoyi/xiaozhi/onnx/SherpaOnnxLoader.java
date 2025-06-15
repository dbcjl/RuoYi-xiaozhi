package com.ruoyi.xiaozhi.onnx;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;

/**
 * 本地库加载器，用于加载 onnxruntime 和 sherpa-onnx-jni 库
 * 会从 resources 中提取库文件到临时目录，然后调用 System.load 加载
 * 若临时目录已存在，则直接复用并覆盖目标文件
 *
 * @author ruoyi-xiaozhi
 */
@Slf4j
public class SherpaOnnxLoader {

    private SherpaOnnxLoader() {}

    /** 系统架构标识，例如 linux-x64 */
    private static final String OS_ARCH_STR = initOsArch();

    /** 是否已加载本地库 */
    private static boolean loaded = false;

    /** 所需加载的本地库名称列表 */
    private static final String[] LIB_NAMES = new String[]{"onnxruntime", "sherpa-onnx-jni"};

    /** 本地库提取路径 */
    private static final Path BASE_TEMP_DIR = Paths.get(System.getProperty("java.io.tmpdir"), "xiaozhi-sherpa-onnx-java");

    /**
     * 初始化加载本地库
     */
    public static synchronized void init() {
        if (loaded) {
            return;
        }

        // 确保临时目录存在
        File dirFile = BASE_TEMP_DIR.toFile();
        if (!dirFile.exists()) {
            if (!dirFile.mkdirs()) {
                throw new IllegalStateException("无法创建临时目录: " + BASE_TEMP_DIR);
            }
        }

        try {
            // 循环加载每个本地库
            for (String libName : LIB_NAMES) {
                load(libName);
            }
            loaded = true;
        } catch (Exception e) {
            throw new IllegalStateException("加载本地库失败", e);
        }
    }

    /**
     * 加载单个本地库
     *
     * @param library 库名
     */
    private static void load(String library) {
        Optional<File> extractedPath = extractFromResources(library);
        if (extractedPath.isPresent()) {
            System.load(extractedPath.get().getAbsolutePath());
            log.info("已加载本地库 '{}'", library);
        } else {
            log.error("未找到本地库资源 '{}'", library);
        }
    }

    /**
     * 从资源中提取本地库到指定目录
     * 若目标文件已存在，也会覆盖写入
     *
     * @param library 本地库名称
     * @return 提取后的本地库文件
     */
    private static Optional<File> extractFromResources(String library) {
        String libraryFileName = mapLibraryName(library);
        String resourcePath = "/native/" + OS_ARCH_STR + '/' + libraryFileName;
        File targetFile = SherpaOnnxLoader.BASE_TEMP_DIR.resolve(libraryFileName).toFile();

        try (InputStream is = SherpaOnnxLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                return Optional.empty();
            }

            // 复制资源到目标文件（覆盖）
            try (FileOutputStream os = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[4096];
                int readBytes;
                while ((readBytes = is.read(buffer)) != -1) {
                    os.write(buffer, 0, readBytes);
                }
            }

            log.info("已提取本地库 '{}' 到目录 {}", library, targetFile.getAbsolutePath());
            return Optional.of(targetFile);
        } catch (IOException e) {
            log.error("提取本地库 '{}' 失败", library, e);
            return Optional.empty();
        } finally {
            cleanUp(targetFile);
        }
    }

    /**
     * 初始化系统架构字符串
     * @return 例如 linux-x64, win-x64
     */
    private static String initOsArch() {
        String os = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        String arch = System.getProperty("os.arch", "generic").toLowerCase(Locale.ENGLISH);
        String detectedOS;
        String detectedArch;

        if (os.contains("mac") || os.contains("darwin")) {
            detectedOS = "osx";
        } else if (os.contains("win")) {
            detectedOS = "win";
        } else if (os.contains("nux")) {
            detectedOS = "linux";
        } else if (isAndroid()) {
            detectedOS = "android";
        } else {
            throw new IllegalStateException("不支持的操作系统: " + os);
        }

        if (arch.startsWith("amd64") || arch.startsWith("x86_64")) {
            detectedArch = "x64";
        } else if (arch.startsWith("x86")) {
            detectedArch = "x86";
        } else if (arch.startsWith("aarch64")) {
            detectedArch = "aarch64";
        } else if (arch.startsWith("ppc64")) {
            detectedArch = "ppc64";
        } else if (isAndroid()) {
            detectedArch = arch;
        } else {
            throw new IllegalStateException("不支持的系统架构: " + arch);
        }

        return detectedOS + '-' + detectedArch;
    }

    /**
     * 判断是否为 Android 系统
     * @return 是否为 Android
     */
    private static boolean isAndroid() {
        return System.getProperty("java.vendor", "generic").equals("The Android Project");
    }

    /**
     * 映射库名称为平台相关的文件名
     * @param library 库名
     * @return 映射后的文件名
     */
    private static String mapLibraryName(String library) {
        return System.mapLibraryName(library).replace("jnilib", "dylib");
    }

    /**
     * 程序退出时删除指定文件
     * @param file 要删除的文件
     */
    private static void cleanUp(File file) {
        if (file != null && file.exists()) {
            log.debug("程序退出时删除文件: {}", file);
            file.deleteOnExit();
        }
    }

}

