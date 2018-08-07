package de.jonashackt.springbootvuejs.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Setter
@Getter
@ToString
public class ScrapeInfo implements Serializable {
    private static final long serialVersionUID = 3349918418604561618L;

    /**
     * 外链URL
     */
    private String linkUrl;
    /**
     * 视频来源网站
     */
    private String sourceType;
    /**
     * 封面图片Name
     */
    private String coverPic;
    /**
     * 视频大小 byte
     */
    private Integer size;

    /**
     * 视频源地址   是指第三方的视频源
     */
    private String sourceUrl;

    /**
     * 视频主题
     */
    private String title;
    /**
     * 视频时长 ms
     */
    private Integer duration;
    /**
     * 视频格式
     */
    private String format;

    /**
     * 是否使用代理
     */
    private String proxy;

    private String cookie;
}

