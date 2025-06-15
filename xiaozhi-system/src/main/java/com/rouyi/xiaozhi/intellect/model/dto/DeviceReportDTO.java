package com.rouyi.xiaozhi.intellect.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 设备固件信息上报
 */
@Data
public class DeviceReportDTO {

    /** 板子固件版本号 */
    private Integer version;

    /** 闪存大小（单位：字节） */
    @JsonProperty("flash_size")
    private Integer flashSize;

    /** 最小空闲堆内存（字节） */
    @JsonProperty("minimum_free_heap_size")
    private Integer minimumFreeHeapSize;

    /** 设备MAC地址 */
    @JsonProperty("mac_address")
    private String macAddress;

    /** 设备唯一ID */
    private String uuid;

    /** 芯片型号 */
    @JsonProperty("chip_model_name")
    private String chipModelName;

    /** 芯片信息 */
    @JsonProperty("chip_info")
    private ChipInfo chipInfo;

    /** 应用程序信息 */
    private Application application;

    /** 分区表列表 */
    @JsonProperty("partition_table")
    private List<Partition> partitionTable;

    /** 当前运行的 OTA 分区信息 */
    private OtaInfo ota;

    /** 板子配置信息 */
    private BoardInfo board;

    /** 芯片信息 */
    @Data
    public static class ChipInfo {

        /** 芯片模型代码 */
        private Integer model;

        /** 核心数 */
        private Integer cores;

        /** 硬件修订版本 */
        private Integer revision;

        /** 芯片功能标志位 */
        private Integer features;
    }

    /** 板子编译信息 */
    @Data
    public static class Application {

        /** 名称 */
        private String name;

        /** 应用版本号 */
        private String version;

        /** 编译时间（UTC ISO格式） */
        @JsonProperty("compile_time")
        private String compileTime;

        /** ESP-IDF 版本号 */
        @JsonProperty("idf_version")
        private String idfVersion;

        /** ELF 文件 SHA256 校验 */
        @JsonProperty("elf_sha256")
        private String elfSha256;
    }

    /** 分区信息*/
    @Data
    public static class Partition {
        /** 分区标签名 */
        private String label;

        /** 分区类型 */
        private Integer type;

        /** 子类型 */
        private Integer subtype;

        /** 起始地址 */
        private Integer address;

        /** 分区大小 */
        private Integer size;
    }

    /** OTA信息 */
    @Data
    public static class OtaInfo {
        /** 当前OTA标签 */
        private String label;
    }

    /** 板子连接和网络信息 */
    @Data
    public static class BoardInfo {
        /** 板子类型 */
        private String type;

        /** 连接的 Wi-Fi SSID */
        private String ssid;

        /** Wi-Fi 信号强度（RSSI） */
        private Integer rssi;

        /** Wi-Fi 信道 */
        private Integer channel;

        /** IP 地址 */
        private String ip;

        /** MAC 地址 */
        private String mac;
    }

}
