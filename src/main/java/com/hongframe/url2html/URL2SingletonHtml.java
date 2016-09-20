package com.hongframe.url2html;

import com.hongframe.PhantomjsUtils;
import com.hongframe.entity.Image;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * url指定页面转化为单个html文件，并截图
 * @author zehong.hongframe.huang@gmail.com, 有事也别来找我
 * @author huangzehong.me 2016年9月14日
 */
public class URL2SingletonHtml {

    /** 网页编码 */
    private String strEncoding = null;

    private Map<String, String> map = new HashMap<String, String>();

    public static void main(String[] args) throws IOException {
        new URL2SingletonHtml("http://www.5aitou.com","D:\\5aitou.html", "D:\\ihurong.png", "UTF-8");
    }

    /**
     * url指定页面转化为单个html文件，并截图
     * @param url 指定页面的url
     * @param htmlPath html文件的保存路径
     * @param imgPath 截图文件的保存路径
     * @param encode
     * @throws IOException
     */
    public URL2SingletonHtml(String url, String htmlPath, String imgPath, String encode) throws IOException {

        try {
            String html =PhantomjsUtils.toHTML(url, imgPath);
            if(html.contains("charset=")) {
                strEncoding = html.split("charset=(\")")[1];
                strEncoding = strEncoding.substring(0, strEncoding.indexOf("\""));
            }
            else {
                strEncoding = encode;
            }
            System.out.println("into compile");
            compile(new URL(url), html);
            saveAsFileWriter(replaceHtml(html, map), htmlPath);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return;
        }
    }

    /**
     * 方法说明：执行下载操作<br>
     * 输入参数：strWeb 网页地址; strText 网页内容; strFilePath 保存路径<br>
     * 返回类型：boolean<br>
     */
    public boolean compile(URL url, String html) {
        if (url == null || html == null) {
            return false;
        }
        HashMap<String, String> urlMap = new HashMap<String, String>();

        Document doc = Jsoup.parse(html);
        Elements scriptElements = doc.select("link[href]");
        scriptElements.addAll(doc.select("script[src]"));
        Elements imgElements = doc.select("img[src]");

        URL strWebB = extractBaseURL(doc.select("base[href]"));
        if (strWebB == null || strWebB.equals("")) {
            strWebB = url;
        }
        extractAllScriptElements(urlMap, strWebB, scriptElements);
        extractAllImageElements(urlMap, strWebB, imgElements);

        // System.out.println(map);

        return true;

    }

    /**
     * 方法说明：下载文件操作<br>
     * 输入参数：url 文件路径<br>
     * 返回类型：byte[]<br>
     */
    private byte[] downBinaryFile(String urlStr) {
        try {
            URL url = new URL(urlStr);
            URLConnection conn = url.openConnection();
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.101 Safari/537.36");
            // String contentType = this.strType;
            int contentLength = conn.getContentLength();
            if (contentLength > 0) {
                InputStream raw = conn.getInputStream();
                InputStream in = new BufferedInputStream(raw);
                byte[] data = new byte[contentLength];
                int bytesRead = 0;
                int offset = 0;
                while (offset < contentLength) {
                    bytesRead = in.read(data, offset, data.length - offset);
                    if (bytesRead == -1) {
                        break;
                    }
                    offset += bytesRead;
                }
                in.close();
                raw.close();
                return data;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    /**
     * 方法说明：抽取基础URL地址<br>
     * 输入参数：nodes 网页标签集合<br>
     * 返回类型：URL<br>
     */
    private URL extractBaseURL(Elements elements) {
        if(elements == null)
            return null;
        String href = null;
        for(Element element : elements) {
            href = element.attr("href");
            if (href != null && href.length() > 0) {
                try {
                    return new URL(href);
                } catch (MalformedURLException e) {
                    e.printStackTrace();

                }
            }
        }
        return null;
    }

    /**
     * 方法说明：抽取网页包含的css,js链接
     * @param urlMap
     * @param url
     * @param elements
     */
    private void extractAllScriptElements(HashMap<String, String> urlMap, URL url, Elements elements) {

        for(Element e : elements) {
            String src0href = e.attr("src");
            if("".equals(src0href)) {
                src0href = e.attr("href");
            }
            String absoluteURL = makeAbsoluteURL(url, src0href);
            if(absoluteURL != null && !urlMap.containsKey(absoluteURL)) {
                urlMap.put(absoluteURL, src0href);
                byte[] bs = downBinaryFile(absoluteURL);
                map.put(src0href, bs == null ? null : new String(bs));
            }
        }

    }

    /**
     * 方法说明：抽取网页包含的图像链接<br>
     * @param urlMap
     * @param url
     * @param elements
     */
    private void extractAllImageElements(HashMap<String, String> urlMap, URL url, Elements elements) {

        for(Element e : elements) {
            String src = e.attr("src");
            String absoluteURL = makeAbsoluteURL(url, src);
            if(absoluteURL != null && !urlMap.containsKey(absoluteURL)) {
                urlMap.put(absoluteURL, src);
                byte[] bs = downBinaryFile(absoluteURL);
                map.put(src, bs == null ? null : new Image(bs).getBase64(true));
            }
        }
    }

    private String replaceHtml(String html, Map<String, String> linkMap) {
        if (StringUtils.isBlank(html))
            return html;

        Document doc = Jsoup.parse(html);
        Elements scriptElements = doc.select("script[src]");
        for (Element e : scriptElements) {
            String src = e.attr("src");
            if (map.containsKey(src)) {
                e.removeAttr("src");
                String text = map.get(src);
                e.attr("data-src", src);
                e.attr("type", "text/javascript");
                if (text != null)
                    e.text(text);
            }
        }

        Elements cssElements = doc.select("link[href]");
        for (Element e : cssElements) {
            String href = e.attr("href");
            if (map.containsKey(href)) {
                e.remove();
                Element e1 = doc.appendElement("style");
                e1.attr("type", "text/css");
                e1.attr("data-src", href);
                String text = map.get(href);
                if (text != null)
                    e1.text(text);
            }
        }

        Elements imgElements = doc.select("img[src]");
        for (Element e : imgElements) {
            String href = e.attr("src");
            if (map.containsKey(href)) {
                String text = map.get(href);
                e.attr("data-src", href);
                if (text != null)
                    e.attr("src", map.get(href));
            }
        }

        return doc.html();
    }

    private void saveAsFileWriter(String content, String filePath) {

        try {
            File file = new File(filePath);
            File path = file.getParentFile();
            FileUtils.forceMkdir(path);
            FileUtils.writeStringToFile(file, content, strEncoding);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 方法说明：相对路径转绝对路径<br>
     * 输入参数：strWeb 网页地址; innerURL 相对路径链接<br>
     * 返回类型：绝对路径链接<br>
     */
    public String makeAbsoluteURL(URL strWeb, String innerURL) {

        // TODO Auto-generated method stub
        // 去除后缀(即参数去掉)
        // int pos = innerURL.indexOf("?");
        // if (pos != -1) {
        // innerURL = innerURL.substring(0, pos);
        // }
        if (strWeb == null || strWeb.equals("")) {
            if (innerURL.startsWith("//")) {
                innerURL = "http:" + innerURL;
            }
        }
        if (innerURL != null && innerURL.toLowerCase().indexOf("http") == 0) {
            return innerURL;
        }
        URL linkUri = null;
        try {
            linkUri = new URL(strWeb, innerURL);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;

        }

        String absURL = linkUri.toString();
        absURL = absURL.replace("../", "");
        absURL = absURL.replace("./", "");

        return absURL;

    }

}