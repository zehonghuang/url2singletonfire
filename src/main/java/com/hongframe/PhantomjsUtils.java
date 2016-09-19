package com.hongframe;

import com.hongframe.url2html.URL2SingletonHtml;
import org.apache.commons.lang3.StringUtils;

import java.io.*;

/**
 * Created by huangzehong on 16/9/19.
 */
public class PhantomjsUtils {

    private static String phantomjsPath;

    static {
        String path = new File(URL2SingletonHtml.class.getResource("/").getPath()).getPath();
        phantomjsPath = path +  System.getProperty("file.separator") + "classes" + System.getProperty("file.separator") + "phantom_load_web_page2.js ";
        File f = new File(phantomjsPath);
        if (!f.exists()) {
            path = StringUtils.substringBeforeLast(path, System.getProperty("file.separator"));
            phantomjsPath = path +  System.getProperty("file.separator") + "classes" + System.getProperty("file.separator") + "phantom_load_web_page2.js ";
        }
    }

    public static String toHTML(String url, String imgPath) throws IOException {

        InputStream is = null;
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        Runtime runtime = Runtime.getRuntime();

        Process process = runtime.exec("phantomjs " + phantomjsPath + url + " " + imgPath);
        is = process.getInputStream();
        br = new BufferedReader(new InputStreamReader(is));

        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
        System.out.println("phantomjs end....");
        return sb.toString();
    }

}