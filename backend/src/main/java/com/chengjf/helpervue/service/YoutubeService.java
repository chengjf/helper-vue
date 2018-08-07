package de.jonashackt.springbootvuejs.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.NullNode;
import de.jonashackt.springbootvuejs.domain.ScrapeInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class YoutubeService {

    private static final Pattern patYouTubePageLink = Pattern.compile("(http|https)://(www\\.|m.|)youtube\\.com/watch\\?v=(.+?)( |\\z|&)");
    private static final Pattern patYouTubeShortLink = Pattern.compile("(http|https)://(www\\.|)youtu.be/(.+?)( |\\z|&)");

    private static final Pattern patVariableFunction = Pattern.compile("([{; =])([a-zA-Z$][a-zA-Z0-9$]{0,2})\\.([a-zA-Z$][a-zA-Z0-9$]{0,2})\\(");
    private static final Pattern patFunction = Pattern.compile("([{; =])([a-zA-Z$_][a-zA-Z0-9$]{0,2})\\(");
    private static final Pattern patSignatureDecFunction = Pattern.compile("\"signature\",(.{1,3}?)\\(.{1,10}?\\)");
    private static final Pattern IP_PATTERN = Pattern.compile(
            "^(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])\\.(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])\\.(\\d{1,2}|1\\d\\d|2[0-4]" +
                    "\\d|25[0-5])\\.(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])$");

    private static final Map<Integer, Format> FORMAT_MAP = new LinkedHashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String YT_CONFIG = "ytplayer.config\\s*=\\s*([^\\n]+});";
    private static final String YT_CONFIG_PLAYER = "ytplayer.config\\s*=\\s*([^\\n]+});ytplayer";

    static {
        // http://en.wikipedia.org/wiki/YouTube#Quality_and_formats

        /**
         {'itag': '38', 'container': 'MP4', 'video_resolution': '3072p', 'video_encoding': 'H.264', 'video_profile': 'High', 'video_bitrate': '3.5-5', 'audio_encoding': 'AAC', 'audio_bitrate': '192'},
         #{'itag': '85', 'container': 'MP4', 'video_resolution': '1080p', 'video_encoding': 'H.264', 'video_profile': '3D', 'video_bitrate': '3-4', 'audio_encoding': 'AAC', 'audio_bitrate': '192'},
         {'itag': '46', 'container': 'WebM', 'video_resolution': '1080p', 'video_encoding': 'VP8', 'video_profile': '', 'video_bitrate': '', 'audio_encoding': 'Vorbis', 'audio_bitrate': '192'},
         {'itag': '37', 'container': 'MP4', 'video_resolution': '1080p', 'video_encoding': 'H.264', 'video_profile': 'High', 'video_bitrate': '3-4.3', 'audio_encoding': 'AAC', 'audio_bitrate': '192'},
         #{'itag': '102', 'container': 'WebM', 'video_resolution': '720p', 'video_encoding': 'VP8', 'video_profile': '3D', 'video_bitrate': '', 'audio_encoding': 'Vorbis', 'audio_bitrate': '192'},
         {'itag': '45', 'container': 'WebM', 'video_resolution': '720p', 'video_encoding': 'VP8', 'video_profile': '', 'video_bitrate': '2', 'audio_encoding': 'Vorbis', 'audio_bitrate': '192'},
         #{'itag': '84', 'container': 'MP4', 'video_resolution': '720p', 'video_encoding': 'H.264', 'video_profile': '3D', 'video_bitrate': '2-3', 'audio_encoding': 'AAC', 'audio_bitrate': '192'},
         {'itag': '22', 'container': 'MP4', 'video_resolution': '720p', 'video_encoding': 'H.264', 'video_profile': 'High', 'video_bitrate': '2-3', 'audio_encoding': 'AAC', 'audio_bitrate': '192'},
         {'itag': '120', 'container': 'FLV', 'video_resolution': '720p', 'video_encoding': 'H.264', 'video_profile': 'Main@L3.1', 'video_bitrate': '2', 'audio_encoding': 'AAC', 'audio_bitrate': '128'}, # Live streaming only
         {'itag': '44', 'container': 'WebM', 'video_resolution': '480p', 'video_encoding': 'VP8', 'video_profile': '', 'video_bitrate': '1', 'audio_encoding': 'Vorbis', 'audio_bitrate': '128'},
         {'itag': '35', 'container': 'FLV', 'video_resolution': '480p', 'video_encoding': 'H.264', 'video_profile': 'Main', 'video_bitrate': '0.8-1', 'audio_encoding': 'AAC', 'audio_bitrate': '128'},
         #{'itag': '101', 'container': 'WebM', 'video_resolution': '360p', 'video_encoding': 'VP8', 'video_profile': '3D', 'video_bitrate': '', 'audio_encoding': 'Vorbis', 'audio_bitrate': '192'},
         #{'itag': '100', 'container': 'WebM', 'video_resolution': '360p', 'video_encoding': 'VP8', 'video_profile': '3D', 'video_bitrate': '', 'audio_encoding': 'Vorbis', 'audio_bitrate': '128'},
         {'itag': '43', 'container': 'WebM', 'video_resolution': '360p', 'video_encoding': 'VP8', 'video_profile': '', 'video_bitrate': '0.5', 'audio_encoding': 'Vorbis', 'audio_bitrate': '128'},
         {'itag': '34', 'container': 'FLV', 'video_resolution': '360p', 'video_encoding': 'H.264', 'video_profile': 'Main', 'video_bitrate': '0.5', 'audio_encoding': 'AAC', 'audio_bitrate': '128'},
         #{'itag': '82', 'container': 'MP4', 'video_resolution': '360p', 'video_encoding': 'H.264', 'video_profile': '3D', 'video_bitrate': '0.5', 'audio_encoding': 'AAC', 'audio_bitrate': '96'},
         {'itag': '18', 'container': 'MP4', 'video_resolution': '270p/360p', 'video_encoding': 'H.264', 'video_profile': 'Baseline', 'video_bitrate': '0.5', 'audio_encoding': 'AAC', 'audio_bitrate': '96'},
         {'itag': '6', 'container': 'FLV', 'video_resolution': '270p', 'video_encoding': 'Sorenson H.263', 'video_profile': '', 'video_bitrate': '0.8', 'audio_encoding': 'MP3', 'audio_bitrate': '64'},
         #{'itag': '83', 'container': 'MP4', 'video_resolution': '240p', 'video_encoding': 'H.264', 'video_profile': '3D', 'video_bitrate': '0.5', 'audio_encoding': 'AAC', 'audio_bitrate': '96'},
         {'itag': '13', 'container': '3GP', 'video_resolution': '', 'video_encoding': 'MPEG-4 Visual', 'video_profile': '', 'video_bitrate': '0.5', 'audio_encoding': 'AAC', 'audio_bitrate': ''},
         {'itag': '5', 'container': 'FLV', 'video_resolution': '240p', 'video_encoding': 'Sorenson H.263', 'video_profile': '', 'video_bitrate': '0.25', 'audio_encoding': 'MP3', 'audio_bitrate': '64'},
         {'itag': '36', 'container': '3GP', 'video_resolution': '240p', 'video_encoding': 'MPEG-4 Visual', 'video_profile': 'Simple', 'video_bitrate': '0.175', 'audio_encoding': 'AAC', 'audio_bitrate': '36'},
         {'itag': '17', 'container': '3GP', 'video_resolution': '144p', 'video_encoding': 'MPEG-4 Visual', 'video_profile': 'Simple', 'video_bitrate': '0.05', 'audio_encoding': 'AAC', 'audio_bitrate': '24'},
         **/
        FORMAT_MAP.put(38, new Format(38, Format.Container.MP4, "3072p", Format.VCodec.H264, "High", "3.5-5", Format.ACodec.AAC, "192"));
        //        FORMAT_MAP.put(46, new Format(46, Container.WebM, "1080p", VCodec.VP8, "", "", ACodec.VORBIS, "192"));
        FORMAT_MAP.put(37, new Format(37, Format.Container.MP4, "1080p", Format.VCodec.H264, "High", "3-4.3", Format.ACodec.AAC, "192"));
        //        FORMAT_MAP.put(45, new Format(45, Container.WebM, "720p", VCodec.VP8, "", "2", ACodec.VORBIS, "192"));
        FORMAT_MAP.put(22, new Format(22, Format.Container.MP4, "720p", Format.VCodec.H264, "High", "2-3", Format.ACodec.AAC, "192"));
        FORMAT_MAP.put(120, new Format(120, Format.Container.FLV, "720p", Format.VCodec.H264, "Main@L3.1", "2", Format.ACodec.AAC, "128"));
        //        FORMAT_MAP.put(44, new Format(44, Container.WebM, "480p", VCodec.VP8, "", "1", ACodec.VORBIS, "128"));
        FORMAT_MAP.put(35, new Format(35, Format.Container.FLV, "480p", Format.VCodec.H264, "Main", "0.8-1", Format.ACodec.AAC, "128"));
        //        FORMAT_MAP.put(43, new Format(43, Container.WebM, "360p", VCodec.VP8, "", "0.5", ACodec.VORBIS, "128"));
        FORMAT_MAP.put(34, new Format(34, Format.Container.FLV, "360p", Format.VCodec.H264, "Main", "0.5", Format.ACodec.AAC, "128"));
        FORMAT_MAP.put(18, new Format(18, Format.Container.MP4, "270p/360p", Format.VCodec.H264, "Baseline", "0.5", Format.ACodec.AAC, "96"));
        FORMAT_MAP.put(6, new Format(6, Format.Container.FLV, "270p", Format.VCodec.H263, "", "0.8", Format.ACodec.MP3, "64"));
        //        FORMAT_MAP.put(13, new Format(13, Container.GP3, "", VCodec.MPEG4, "", "0.5", ACodec.AAC, ""));
        FORMAT_MAP.put(5, new Format(5, Format.Container.FLV, "240p", Format.VCodec.H263, "", "0.25", Format.ACodec.MP3, "64"));
        //        FORMAT_MAP.put(36, new Format(36, Container.GP3, "240p", VCodec.MPEG4, "Simple", "0.175", ACodec.AAC, "36"));
        //        FORMAT_MAP.put(17, new Format(17, Container.GP3, "144p", VCodec.MPEG4, "Simple", "0.05", ACodec.AAC, "24"));
    }

    // json parser
    static {
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }


    private boolean useHttp = false;

    public static void main(String[] args) throws Exception {

    }

    private static String parseQueryParam(String url, String param) {
        if (StringUtils.isNotEmpty(url) && StringUtils.isNotEmpty(param)) {
            try {
                if (!url.startsWith("http:")) {
                    url = "https://youtube.com?" + url;
                }
                List<NameValuePair> parse = URLEncodedUtils.parse(new URI(url), StandardCharsets.UTF_8.name());
                return parse.stream().filter(n -> n.getName().equals(param)).findFirst().map(NameValuePair::getValue).orElse("");
            } catch (URISyntaxException e) {
                log.error("parse query param error: url:{} param:{}", url, param, e);
            }
        }
        return null;
    }

    public static String matchs(String text, Pattern... patterns) {
        if (patterns == null) {
            return null;
        }
        if (patterns.length == 1) {
            Pattern pattern = patterns[0];
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } else {
            StringBuilder builder = new StringBuilder();
            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(text);
                builder.append(matcher.group(1));
            }
            return builder.toString();
        }
        return null;
    }

    private static Map<String, String> parseQueryParam(String url) {
        Map<String, String> map = new HashMap(0);

        if (StringUtils.isEmpty(url)) {
            return map;
        }
        if (!url.startsWith("http")) {
            url = "http://www.youtube.com?" + url;
        }
        try {
            List<NameValuePair> parse = URLEncodedUtils.parse(new URI(url), StandardCharsets.UTF_8.name());
            int size = parse.size();
            map = new HashMap(size);
            for (NameValuePair nameValuePair : parse) {
                map.put(nameValuePair.getName(), nameValuePair.getValue());
            }
            return map;
        } catch (URISyntaxException e) {
            log.error("parse query param error: url:{}", url, e);
        }
        return map;
    }

    /**
     * 解析http响应结果
     *
     * @param response
     * @return
     */
    private static String parseReponse(CloseableHttpResponse response) {
        try {
            HttpEntity httpEntity = response.getEntity();
            if (httpEntity != null) {
                return EntityUtils.toString(httpEntity, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.error("parse response error: response:{}", response, e);
        }
        return null;
    }

    private static String decipherSignature(String js, String encSignature) {
        log.info("get youtube video real source signature: js:{} encSignature:{}", js, encSignature);
        String decipher = null;
        try {
            String decipherFunctionName;
            String decipherFunctions;
            String javascriptFile = js;

            Matcher mat = patSignatureDecFunction.matcher(javascriptFile);
            if (mat.find()) {
                decipherFunctionName = mat.group(1);

                Pattern patMainVariable = Pattern.compile("(var |\\s|,|;)" + decipherFunctionName.replace("$", "\\$") +
                        "(=function\\((.{1,3})\\)\\{)");

                String mainDecipherFunct;

                mat = patMainVariable.matcher(javascriptFile);
                if (mat.find()) {
                    mainDecipherFunct = "var " + decipherFunctionName + mat.group(2);
                } else {
                    Pattern patMainFunction = Pattern.compile("function " + decipherFunctionName.replace("$", "\\$") +
                            "(\\((.{1,3})\\)\\{)");
                    mat = patMainFunction.matcher(javascriptFile);
                    if (!mat.find())
                        return null;
                    mainDecipherFunct = "function " + decipherFunctionName + mat.group(2);
                }

                int startIndex = mat.end();

                for (int braces = 1, i = startIndex; i < javascriptFile.length(); i++) {
                    if ((braces == 0) && ((startIndex + 5) < i)) {
                        mainDecipherFunct += javascriptFile.substring(startIndex, i) + ";";
                        break;
                    }
                    if (javascriptFile.charAt(i) == '{')
                        braces++;
                    else if (javascriptFile.charAt(i) == '}')
                        braces--;
                }
                decipherFunctions = mainDecipherFunct;
                // Search the main function for extra functions and variables
                // needed for deciphering
                // Search for variables
                mat = patVariableFunction.matcher(mainDecipherFunct);
                while (mat.find()) {
                    String variableDef = "var " + mat.group(2) + "={";
                    if (decipherFunctions.contains(variableDef)) {
                        continue;
                    }
                    startIndex = javascriptFile.indexOf(variableDef) + variableDef.length();
                    for (int braces = 1, i = startIndex; i < javascriptFile.length(); i++) {
                        if (braces == 0) {
                            decipherFunctions += variableDef + javascriptFile.substring(startIndex, i) + ";";
                            break;
                        }
                        if (javascriptFile.charAt(i) == '{')
                            braces++;
                        else if (javascriptFile.charAt(i) == '}')
                            braces--;
                    }
                }
                // Search for functions
                mat = patFunction.matcher(mainDecipherFunct);
                while (mat.find()) {
                    String functionDef = "function " + mat.group(2) + "(";
                    if (decipherFunctions.contains(functionDef)) {
                        continue;
                    }
                    startIndex = javascriptFile.indexOf(functionDef) + functionDef.length();
                    for (int braces = 0, i = startIndex; i < javascriptFile.length(); i++) {
                        if ((braces == 0) && ((startIndex + 5) < i)) {
                            decipherFunctions += functionDef + javascriptFile.substring(startIndex, i) + ";";
                            break;
                        }
                        if (javascriptFile.charAt(i) == '{')
                            braces++;
                        else if (javascriptFile.charAt(i) == '}')
                            braces--;
                    }
                }
                decipher = getDecipherViaWebView(encSignature, decipherFunctions, decipherFunctionName);
            }
        } catch (Throwable t) {
            log.error("get youtube video real source signature exception: encSignature:{} js:{}", encSignature, js, t);
        }
        log.info("get youtube video real source signature result: encSignature:{} js:{} decipher:{}", encSignature, js, decipher);
        return decipher;
    }

    private static String getDecipherViaWebView(String encSignature, String decipherFunctions, String decipherFunctionName) {
        log.info("get youtube video signature via js engine: encSignature:{} decipherFunctions:{} decipherFunctionName:{}", encSignature, decipherFunctions, decipherFunctionName);
        String decipher = null;
        try {
            StringBuilder stb = new StringBuilder(decipherFunctions + " function decipher(");
            stb.append("){return ");
            stb.append(decipherFunctionName).append("('").append(encSignature).
                    append("')");

            stb.append("};decipher();");
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
            try {
                log.info("get youtube video signature via js engine, js code:{} encSignature:{} decipherFunctions:{} decipherFunctionName:{}",
                        stb, encSignature, decipherFunctions, decipherFunctionName);
                Object eval = engine.eval(stb.toString());
                decipher = (String) eval;
            } catch (ScriptException e) {
                log.error("get youtube video signature via js engine,  execute js code exception: js code:{} encSignature:{} decipherFunctions:{} decipherFunctionName:{}",
                        stb, encSignature, decipherFunctions, decipherFunctionName, e);
            }
        } catch (Throwable t) {
            log.info("et youtube video signature via js engine exception: encSignature:{} decipherFunctions:{} decipherFunctionName:{}",
                    encSignature, decipherFunctions, decipherFunctionName, t);

        }
        return decipher;
    }

    private static JsonNode getYTConfig(String content) {
        Pattern compile = Pattern.compile(YT_CONFIG);
        String matchs = matchs(content, compile);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        JsonNode node = NullNode.getInstance();
        try {
            node = objectMapper.readTree(matchs);
        } catch (Throwable t) {
            log.error("YTConfig parse error: content:{}", content, t);
        }
        return node;
    }

    private static JsonNode getYTConfigWithPlayer(String content) {
        Pattern compile = Pattern.compile(YT_CONFIG_PLAYER);
        String matchs = matchs(content, compile);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        JsonNode node = NullNode.getInstance();
        try {
            node = objectMapper.readTree(matchs);
        } catch (IOException e) {
        }
        return node;
    }

    /**
     * 解析信息构建youtube视频流信息
     *
     * @param streamInfo
     * @return
     */
    private static YTStream buildYTStream(Map<String, String> streamInfo) {
        YTStream ytStream = new YTStream();
        Integer itag = Integer.parseInt(streamInfo.get("itag"));
        ytStream.setItag(itag);
        ytStream.setUrl(streamInfo.get("url"));
        ytStream.setSig(streamInfo.get("sig"));
        ytStream.setS(streamInfo.get("s"));
        ytStream.setQuality(streamInfo.get("quality"));
        ytStream.setType(streamInfo.get("type"));
        ytStream.setMime(streamInfo.get("mime"));
        ytStream.setContainer(streamInfo.get("container"));
        Format format = FORMAT_MAP.get(itag);
        ytStream.setFormat(format);
        return ytStream;
    }

    /**
     * 目前只支持mp4和flv
     *
     * @param container
     * @return
     */
    private static String getFormat(Format.Container container) {
        if (container == null) {
            return null;
        }
        switch (container) {
            case MP4:
                return "mp4";
            case FLV:
                return "flv";
        }
        return null;
    }

    private HttpHost getHttpProxy(String proxy) {
        if (StringUtils.isNotBlank(proxy) && isRightfulProxy(proxy)) {
            String[] pro = proxy.split(":");
            return new HttpHost(pro[0], NumberUtils.toInt(pro[1]));
        }
        return null;
    }

    private boolean isRightfulProxy(String proxy) {
        log.info("isRightfulProxy proxy:{}", proxy);
        if (StringUtils.isEmpty(proxy)) {
            return false;
        }
        if (!proxy.contains(":") && !(proxy.split(":").length != 2)) {
            return false;
        }
        Matcher m = IP_PATTERN.matcher(proxy.split(":")[0]);
        boolean result = m.matches();
        log.info("isRightfulProxy proxy:{}, result:{}", proxy, result);
        return result;
    }

    /**
     * 获取http的get请求结果
     *
     * @param url
     * @return
     */
    private String httpGet(String url) {
        String result = "";
        try {
            CloseableHttpClient httpClient = HttpClients.custom().build();
            HttpGet request = new HttpGet(url);
            RequestConfig.Builder builder = RequestConfig.custom().setSocketTimeout(5000).setConnectTimeout(5000);
            RequestConfig build = builder.build();
            request.setConfig(build);

            CloseableHttpResponse response = httpClient.execute(request);
            result = parseReponse(response);
        } catch (Exception e) {
            log.error("http get error. url:{}", url, e);
        }
        return result;
    }


    public ScrapeInfo parseWeb(String linkUrl) throws Exception {
        log.info("start parseWeb linkUrl:{}", linkUrl);
        ScrapeInfo videoInfo = new ScrapeInfo();
        videoInfo.setLinkUrl(linkUrl);
        videoInfo = extract(linkUrl, videoInfo);
        log.info("end parseWeb videoInfo:{}", videoInfo);
        return videoInfo;
    }

    private ScrapeInfo extract(String url, ScrapeInfo scrapeInfo) {
        log.info("extract youtube start url:{} scrapeInfo:{}", url, scrapeInfo);
        String videoId = getVideoId(url);
        if (StringUtils.isNotEmpty(videoId)) {
            scrapeInfo = getVideoInfo(videoId, scrapeInfo);
        }
        log.info("extract youtube end url:{} scrapeInfo:{}", url, scrapeInfo);
        return scrapeInfo;
    }

    /**
     * 获取视频id
     *
     * @param url
     * @return
     */
    private String getVideoId(String url) {
        log.info("get youtube video id url:{}", url);
        if (url == null) {
            return null;
        }
        String videoId = null;
        Matcher mat = patYouTubePageLink.matcher(url);
        if (mat.find()) {
            videoId = mat.group(3);
        } else {
            mat = patYouTubeShortLink.matcher(url);
            if (mat.find()) {
                videoId = mat.group(3);
            } else if (url.matches("\\p{Graph}+?")) {
                videoId = url;
            }
        }
        log.info("get youtube video id result url:{} videoId:{}", url, videoId);
        return videoId;
    }

    /**
     * 获取视频信息
     *
     * @param videoId
     * @param scrapeInfo
     * @return
     */
    private ScrapeInfo getVideoInfo(String videoId, ScrapeInfo scrapeInfo) {
        try {
            log.info("get youtube video metadata videoId:{} scrapeInfo:{}", videoId, scrapeInfo);
            String ytbInfoUrl = (useHttp) ? "http://" : "https://";
            ytbInfoUrl += "www.youtube.com/get_video_info?video_id=" + videoId + "&eurl="
                    + URLEncoder.encode("https://youtube.googleapis.com/v/" + videoId, "UTF-8");

            log.info("get youtube video metadata url:{} videoId:{}", ytbInfoUrl, videoId);

            String streamMap = httpGet(ytbInfoUrl);
            if (StringUtils.isNotEmpty(streamMap)) {
                scrapeInfo = parseVideoMeta(videoId, streamMap, scrapeInfo);
            }
        } catch (Throwable t) {
            log.error("get youtube video metadata exception: videoId:{} scrapInfo:{}", videoId, scrapeInfo, t);
            throw new RuntimeException("parse error", t);
        }
        log.info("get youtube video metadata result: videoId:{} scrapeInfo:{}", videoId, scrapeInfo);
        return scrapeInfo;
    }

    private ScrapeInfo parseVideoMeta(String videoId, String videoInfo, ScrapeInfo scrapeInfo) throws UnsupportedEncodingException {
        log.info("parse youtube video metadata. videoInfo:{} scrapInfo:{}", videoInfo, scrapeInfo);
        try {
            String[] streamList = null;
            String htmlPlayer = null;
            JsonNode ytConfig = null;
            Map<Integer, YTStream> streams = new HashMap<>();

            String status = parseQueryParam(videoInfo, "status");
            if (StringUtils.isEmpty(status)) {
                log.error("parse youtube video metadata. Unknown status.");
            } else if ("ok".equals(status)) {
                String useCipherSignature = parseQueryParam(videoInfo, "use_cipher_signature");
                if (StringUtils.isEmpty(useCipherSignature) || "False".equals(useCipherSignature)) {
                    scrapeInfo.setTitle(parseQueryParam(videoInfo, "title"));
                    String lengthSeconds = parseQueryParam(videoInfo, "length_seconds");
                    scrapeInfo.setDuration(Integer.parseInt(lengthSeconds));
                    scrapeInfo.setCoverPic(parseQueryParam(videoInfo, "thumbnail_url"));
                    String url = "https://www.youtube.com/watch?v=" + videoId;
                    try {
                        String videoPage = httpGet(url);
                        ytConfig = getYTConfig(videoPage);
                        htmlPlayer = "https://www.youtube.com" + ytConfig.get("assets").get("js").asText();
                        streamList = ytConfig.get("args").get("url_encoded_fmt_stream_map").asText().split(",");
                    } catch (Throwable t) {
                        htmlPlayer = null;
                        streamList = parseQueryParam(videoInfo, "url_encoded_fmt_stream_map").split(",");
                    }
                } else {
                    // Parse video page instead
                    String url = "https://www.youtube.com/watch?v=" + videoId;
                    try {
                        String videoPage = httpGet(url);
                        ytConfig = getYTConfig(videoPage);
                        scrapeInfo.setTitle(ytConfig.get("args").get("title").asText());
                        scrapeInfo.setCoverPic(ytConfig.get("args").get("thumbnail_url").asText());
                        scrapeInfo.setDuration(ytConfig.get("args").get("length_seconds").asInt());
                        htmlPlayer = "https://www.youtube.com" + ytConfig.get("assets").get("js").asText();
                        streamList = ytConfig.get("args").get("url_encoded_fmt_stream_map").asText().split(",");
                    } catch (Throwable t) {
                        htmlPlayer = null;
                        streamList = null;
                    }

                }

            } else if ("fail".equals(status)) {
                String errorCode = parseQueryParam(videoInfo, "errorcode");
                if ("150".equals(errorCode)) {
                    String url = "https://www.youtube.com/watch?v=" + videoId;
                    try {
                        String videoPage = httpGet(url);
                        ytConfig = getYTConfigWithPlayer(videoPage);
                        JsonNode titleNode = ytConfig.get("args").get("title");
                        if (titleNode != null) {
                            scrapeInfo.setTitle(ytConfig.get("args").get("title").asText());
                            scrapeInfo.setCoverPic(ytConfig.get("args").get("thumbnail_url").asText());
                            scrapeInfo.setDuration(ytConfig.get("args").get("length_seconds").asInt());
                            htmlPlayer = "https://www.youtube.com" + ytConfig.get("assets").get("js").asText();
                            streamList = ytConfig.get("args").get("url_encoded_fmt_stream_map").asText().split(",");
                        } else {
                            log.error("The uploader has not made this video available in your country.");
                            return scrapeInfo;
                        }
                    } catch (Throwable t) {
                        htmlPlayer = null;
                        streamList = null;
                    }
                } else if ("100".equals(errorCode)) {
                    log.error("This video does not exist.");
                    return scrapeInfo;

                } else {
                    log.error("%s", parseQueryParam(videoInfo, "reason"));
                    return scrapeInfo;
                }

            } else {
                log.error("parse youtbe video metadata. Invalid status.");
                return scrapeInfo;
            }

            // youtube live
            JsonNode liveStreamNode = ytConfig.get("args").get("livestream");
            JsonNode livePlaybackNode = ytConfig.get("args").get("live_playback");
            boolean liveStreamNodeCheck = (liveStreamNode != null) && "1".equals(liveStreamNode.asText());
            boolean livePlaybackNodeCheck = (livePlaybackNode != null) && "1".equals(livePlaybackNode.asText());
            if (liveStreamNodeCheck || livePlaybackNodeCheck) {
                log.info("Youtube live stream return");
                return scrapeInfo;
            }

            // extract stream list
            if (streamList != null) {
                for (String st : streamList) {
                    Map<String, String> map = parseQueryParam(st);
                    Integer itag = Integer.parseInt(map.get("itag"));
                    YTStream ytStream = buildYTStream(map);
                    streams.put(itag, ytStream);
                }
            }

            // find best source url
            for (Map.Entry<Integer, Format> integerFormatEntry : FORMAT_MAP.entrySet()) {
                Integer key = integerFormatEntry.getKey();
                if (streams.containsKey(key)) {
                    YTStream ytStream = streams.get(key);
                    log.info("get best youtube quality: key:{} stream:{}", key, ytStream);
                    String src = ytStream.getUrl();
                    if (StringUtils.isNotEmpty(ytStream.getSig())) {
                        String sig = ytStream.getSig();
                        src += "&signature=" + sig;
                    } else if (StringUtils.isNotEmpty(ytStream.getS())) {
                        String js = httpGet(htmlPlayer);
                        String s = ytStream.getS();
                        String sig = decipherSignature(js, s);
                        src += "&signature=" + sig;
                    }
                    scrapeInfo.setSourceUrl(src);
                    scrapeInfo.setSize(getVideoSize(src));
                    scrapeInfo.setFormat(getFormat(ytStream.getFormat().getContainer()));
                    break;
                }
            }
        } catch (Throwable t) {
            log.error("parse youtube video metadata exception: scrapInfo:{}", scrapeInfo, t);
        }
        log.info("parse youtube video metadata result: scrapInfo:{}", scrapeInfo);
        return scrapeInfo;
    }

    private Integer getVideoSize(String sourceUrl) throws Exception {
        URL url = new URL(sourceUrl);
        //得到UrlConnection对象
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        //声明请求方式
        conn.setRequestMethod("GET");
        //设置连接超时
        conn.setConnectTimeout(1000);
        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            //如果结果是200，就代表他的请求是成功的
            //得到服务器端传回来的数据，相对客户端为输入流
            return conn.getContentLength();
        }
        return null;
    }

    /**
     * youtube视频流信息
     */
    static class YTStream {
        private Integer itag;
        private String url;
        private String sig;
        private String s;
        private String quality;
        private String type;
        private String mime;
        private String container;
        private String src;
        private Format format;

        YTStream() {
        }

        public Integer getItag() {
            return itag;
        }

        public void setItag(Integer itag) {
            this.itag = itag;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getSig() {
            return sig;
        }

        public void setSig(String sig) {
            this.sig = sig;
        }

        public String getS() {
            return s;
        }

        public void setS(String s) {
            this.s = s;
        }

        public String getQuality() {
            return quality;
        }

        public void setQuality(String quality) {
            this.quality = quality;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getMime() {
            return mime;
        }

        public void setMime(String mime) {
            this.mime = mime;
        }

        public String getContainer() {
            return container;
        }

        public void setContainer(String container) {
            this.container = container;
        }

        public String getSrc() {
            return src;
        }

        public void setSrc(String src) {
            this.src = src;
        }

        public Format getFormat() {
            return format;
        }

        public void setFormat(Format format) {
            this.format = format;
        }

        @Override
        public String toString() {
            return "YTStream{" +
                    "itag='" + itag + '\'' +
                    ", url='" + url + '\'' +
                    ", sig='" + sig + '\'' +
                    ", s='" + s + '\'' +
                    ", quality='" + quality + '\'' +
                    ", type='" + type + '\'' +
                    ", mime='" + mime + '\'' +
                    ", container='" + container + '\'' +
                    ", src='" + src + '\'' +
                    ", format=" + format +
                    '}';
        }
    }

    /**
     * youtube 视频格式
     */
    static class Format {

        private int itag;
        private Container container;

        private String videoResolution;
        private VCodec vCodec;
        private String videoProfile;
        private String videoBitrate;

        private ACodec aCodec;
        private String audioBitrate;

        public Format(int itag, Container container, String videoResolution, VCodec vCodec, String videoProfile, String videoBitrate, ACodec aCodec, String audioBitrate) {
            this.itag = itag;
            this.container = container;
            this.videoResolution = videoResolution;
            this.vCodec = vCodec;
            this.videoProfile = videoProfile;
            this.videoBitrate = videoBitrate;
            this.aCodec = aCodec;
            this.audioBitrate = audioBitrate;
        }

        public int getItag() {
            return itag;
        }

        public void setItag(int itag) {
            this.itag = itag;
        }

        public Container getContainer() {
            return container;
        }

        public void setContainer(Container container) {
            this.container = container;
        }

        public String getVideoResolution() {
            return videoResolution;
        }

        public void setVideoResolution(String videoResolution) {
            this.videoResolution = videoResolution;
        }

        public VCodec getvCodec() {
            return vCodec;
        }

        public void setvCodec(VCodec vCodec) {
            this.vCodec = vCodec;
        }

        public ACodec getaCodec() {
            return aCodec;
        }

        public void setaCodec(ACodec aCodec) {
            this.aCodec = aCodec;
        }

        public String getVideoProfile() {
            return videoProfile;
        }

        public void setVideoProfile(String videoProfile) {
            this.videoProfile = videoProfile;
        }

        public String getVideoBitrate() {
            return videoBitrate;
        }

        public void setVideoBitrate(String videoBitrate) {
            this.videoBitrate = videoBitrate;
        }

        public String getAudioBitrate() {
            return audioBitrate;
        }

        public void setAudioBitrate(String audioBitrate) {
            this.audioBitrate = audioBitrate;
        }

        @Override
        public String toString() {
            return "Format{" +
                    "itag=" + itag +
                    ", container='" + container + '\'' +
                    ", videoResolution='" + videoResolution + '\'' +
                    ", vCodec=" + vCodec +
                    ", aCodec=" + aCodec +
                    ", videoProfile='" + videoProfile + '\'' +
                    ", videoBitrate='" + videoBitrate + '\'' +
                    ", audioBitrate='" + audioBitrate + '\'' +
                    '}';
        }

        enum VCodec {
            H263, H264, MPEG4, VP8, VP9, NONE
        }

        enum ACodec {
            MP3, AAC, VORBIS, OPUS, NONE
        }

        enum Container {
            MP4, WebM, FLV, GP3
        }
    }
}
