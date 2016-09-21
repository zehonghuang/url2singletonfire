package com.hongframe;

import com.hongframe.url2html.URL2SingletonHtml;
import org.apache.commons.lang3.StringUtils;

import java.io.*;

/**
 * Created by huangzehong on 16/9/19.
 */
public  class PhantomjsUtils {

    private static String jsPath;
    private static String customPath;

    static {
        String path = new File(URL2SingletonHtml.class.getResource("/").getPath()).getPath();
        jsPath = path +  System.getProperty("file.separator") + "classes" + System.getProperty("file.separator") + "phantom_load_web_page2.js ";
        File f = new File(jsPath);
        if (!f.exists()) {
            path = StringUtils.substringBeforeLast(path, System.getProperty("file.separator"));
            jsPath = path +  System.getProperty("file.separator") + "classes" + System.getProperty("file.separator") + "phantom_load_web_page2.js ";
        }
    }

    /**
     * 用phantomjs获取渲染后的页面内容
     * @param url 页面地址
     * @param imgPath 保存截图文件的地址
     * @return 页面html
     * @throws IOException
     */
    public static String toHTML(String url, String imgPath) throws IOException {

        InputStream is = null;
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        Runtime runtime = Runtime.getRuntime();
        String encoding = System.getProperty("file.encoding");

        String cmd = null;
        if(customPath != null && !customPath.isEmpty()) {
            cmd = customPath + " --output-encoding=" + encoding + " " + jsPath + url + " " + imgPath;
        }
        else {
            cmd = "phantomjs --output-encoding=" + encoding + " " + jsPath + url + " " + imgPath;
        }

        Process process = runtime.exec(cmd);
        is = process.getInputStream();
        br = new BufferedReader(new InputStreamReader(is));

        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
        System.out.println("phantomjs end....");
        return sb.toString();
    }

    /**
     * 自定义phantomjs命令路径
     * @param path 命令路径。如：/usr/local/bin/phantomjs
     */
    public static void setPhantomjsPath(String path) {
        customPath = path;
    }

    /**
     * 自定义js文件路径
     * @param path js文件路径。如 /data/js/xxxxxx.jd
     */
    public static void setJsPath(String path) {
        if(path != null && !path.isEmpty())
            jsPath = path;
    }

}
