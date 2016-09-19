package com.hongframe.url2mhtml;

import com.hongframe.entity.Image;
import org.apache.commons.lang3.StringUtils;
import org.htmlparser.Parser;
import org.htmlparser.Tag;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.lexer.Lexer;
import org.htmlparser.lexer.Page;
import org.htmlparser.util.DefaultParserFeedback;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.MimetypesFileTypeMap;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 * Created by huangzh on 2016/9/19.
 */
public class URL2Mhtml {
    /** 网页编码 */
    private String strEncoding = null;

    // mht格式附加信息
    private String from = "lishigui@126.com";
    private String to = "lishigui@126.com";
    private String subject = "blog.csdn.net/lishigui";
    private String cc;
    private String bcc;

    private static String phantomjsPath = "";

    static {
        String path = new File(URL2Mhtml.class.getResource("/").getPath()).getPath();
        phantomjsPath = path + "\\classes" + System.getProperty("file.separator") + "phantomjs_fetcher.js ";
        File f = new File(phantomjsPath);
        if (!f.exists()) {
            path = StringUtils.substringBeforeLast(path, System.getProperty("file.separator"));
            phantomjsPath = path + "\\classes" + System.getProperty("file.separator") + "phantom_load_web_page2.js ";
        }
    }

    public static void main(String[] args) throws IOException {
        new URL2Mhtml("http://www.5aitou.com","C:\\", "c:\\ihurong.png");
    }

    /**
     * 构造方法：初始化<br>
     * 输入参数：url 网页地址;  strFilePath 保存路径<br>
     * @throws IOException
     */
    public URL2Mhtml(String url, String htmlPath, String imgPath) throws IOException {

        try {
            byte[] bText = null;
            //取得页面内容
            bText = toHtml(url, imgPath).getBytes();
            String html = new String(bText);
            System.out.println(html);
            strEncoding = html.split("charset=(\")")[1];
            strEncoding = strEncoding.substring(0, strEncoding.indexOf("\""));
            try {
                html = new String(bText, 0, bText.length, strEncoding);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (html == null){
                return;
            }
            compile(new URL(url),html,htmlPath);

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return;
        }
    }

    public String toHtml(String url, String imgPath) throws IOException {

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
        return sb.toString();
    }

    /**
     * 方法说明：执行下载操作<br>
     * 输入参数：strWeb 网页地址; strText 网页内容; strFilePath 保存路径<br>
     * 返回类型：boolean<br>
     */
    public boolean compile(URL url, String html, String htmlPath) {
        if (url == null || html == null || htmlPath == null){
            return false;
        }
        HashMap urlMap = new HashMap();
        NodeList nodes = new NodeList();
        try {
            Parser parser = createParser(html);
            nodes = parser.parse(null);
        } catch (ParserException e) {
            e.printStackTrace();
        }

        Document doc = Jsoup.parse(html);
        Elements scriptElements = doc.select("link[href]");
        scriptElements.addAll(doc.select("script[src]"));
        Elements imgElements = doc.select("img[src]");

        URL strWebB = extractBaseURL(doc.select("base[href]"));
        if(strWebB == null || strWebB.equals("")){
            strWebB = url;
        }
        List urlScriptList = extractAllScriptElements(urlMap, strWebB, scriptElements);

        List urlImageList = extractAllImageElements(nodes, urlMap, strWebB, imgElements);
        if(strWebB == null || strWebB.equals("")){
            for (Iterator iter = urlMap.entrySet().iterator(); iter.hasNext();) {
                Map.Entry entry = (Map.Entry) iter.next();
                String key = (String) entry.getKey();
                String val = (String) entry.getValue();
                html = html.replace(val, key);
            }
        }

        try {
            createMhtArchive(html, urlScriptList, urlImageList, url, htmlPath);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;

    }

    /**
     * 方法说明：下载文件操作<br>
     * 输入参数：url 文件路径<br>
     * 返回类型：byte[]<br>
     */
    public  byte[] downBinaryFile(String url){
        System.out.println(url);
        try {
            URL cUrl = new URL(url);
            URLConnection uc = cUrl.openConnection();
            uc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.101 Safari/537.36");
            // String contentType = this.strType;
            int contentLength = uc.getContentLength();
            if (contentLength > 0) {
                InputStream raw = uc.getInputStream();
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
     * 方法说明：建立HTML parser<br>
     * 输入参数：inputHTML 网页文本内容<br>
     * 返回类型：HTML parser<br>
     */
    private Parser createParser(String inputHTML) {
        Lexer mLexer = new Lexer(new Page(inputHTML));
        return new Parser(mLexer, new DefaultParserFeedback(
                DefaultParserFeedback.QUIET));
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
     * 方法说明：抽取网页包含的css,js链接<br>
     * 输入参数：nodes 网页标签集合; urlMap 已存在的url集合<br>
     * 返回类型：css,js链接的集合<br>
     */
    private List<List<String>> extractAllScriptElements(HashMap urlMap,
                                                        URL url, Elements elements) {

        List<List<String>> urlList = new ArrayList();
        for(Element e : elements) {
            String src0href = e.attr("src");
            if("".equals(src0href)) {
                src0href = e.attr("href");
            }
            String absoluteURL = makeAbsoluteURL(url, src0href);
            if(absoluteURL != null && !urlMap.containsKey(absoluteURL)) {
                urlMap.put(absoluteURL, src0href);
                List<String> urlInfo = new ArrayList();
                urlInfo.add(src0href);
                urlInfo.add(absoluteURL);
                urlList.add(urlInfo);
            }
        }
        return urlList;
    }

    /**
     * 方法说明：抽取网页包含的图像链接<br>
     * 输入参数：nodes 网页标签集合; urlMap 已存在的url集合; strWeb 网页地址<br>
     * 返回类型：图像链接集合<br>
     */
    private List<List<String>> extractAllImageElements(NodeList nodes, HashMap urlMap,
                                              URL url, Elements elements) {

        List<List<String>> urlList = new ArrayList();
        for(Element e : elements) {
            String src = e.attr("src");
            String absoluteURL = makeAbsoluteURL(url, src);
            if(absoluteURL != null && !urlMap.containsKey(absoluteURL)) {
                urlMap.put(absoluteURL, src);
                List<String> urlInfo = new ArrayList();
                urlInfo.add(src);
                urlInfo.add(absoluteURL);
                urlList.add(urlInfo);
            }
        }
        return urlList;
    }

    /**
     * 方法说明：相对路径转绝对路径<br>
     * 输入参数：strWeb 网页地址; innerURL 相对路径链接<br>
     * 返回类型：绝对路径链接<br>
     */
    public  String makeAbsoluteURL(URL strWeb, String innerURL) {

        // TODO Auto-generated method stub
        // 去除后缀(即参数去掉)
//        int pos = innerURL.indexOf("?");
//        if (pos != -1) {
//            innerURL = innerURL.substring(0, pos);
//        }
        if(strWeb == null || strWeb.equals("")){
            if(innerURL.startsWith("//")){
                innerURL = "http:"+innerURL;
            }
        }
        if (innerURL != null
                && innerURL.toLowerCase().indexOf("http") == 0) {
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
        System.out.println(absURL);

        return absURL;

    }

    /**
     * 方法说明：创建mht文件<br>
     * 输入参数：content 网页文本内容; urlScriptList 脚本链接集合; urlImageList 图片链接集合
     * strWeb 网页地址； strFilePath 保存路径<br>
     * 返回类型：<br>
     */
    private void createMhtArchive(String content, List urlScriptList,
                                  List urlImageList, URL strWeb, String strFilePath) throws Exception {

        // Instantiate a Multipart object
        MimeMultipart mp = new MimeMultipart("related");

        Properties properties = new Properties();
        // 设置系统属性
        properties = System.getProperties();
        properties.put("mail.smtp.host", "smtp.126.com");
        properties.put("mail.smtp.auth", "true");
        // 邮件会话对象
        Session session = Session.getDefaultInstance(properties,
                new Email_auth(from, ""));

        // props.put("mail.smtp.host", smtp);
        MimeMessage msg = new MimeMessage(session);

        // set mailer
        msg.setHeader("X-Mailer", "Code Manager .SWT");

        // set from
        if (from != null) {
            msg.setFrom(new InternetAddress(from));
        }

        // set subject
        if (subject != null) {
            msg.setSubject(subject);
        }

        // to
        if (to != null) {
            InternetAddress[] toAddresses = getInetAddresses(to);
            msg.setRecipients(Message.RecipientType.TO, toAddresses);

        }

        // cc
        if (cc != null) {
            InternetAddress[] ccAddresses = getInetAddresses(cc);
            msg.setRecipients(Message.RecipientType.CC, ccAddresses);
        }

        // bcc
        if (bcc != null) {
            InternetAddress[] bccAddresses = getInetAddresses(bcc);
            msg.setRecipients(Message.RecipientType.BCC, bccAddresses);
        }

        // 设置网页正文
        MimeBodyPart bp = new MimeBodyPart();
        bp.setText(content, strEncoding);
        bp.addHeader("Content-Type", "text/html;charset=" + strEncoding);
        bp.addHeader("Content-Location", strWeb.toString());
        mp.addBodyPart(bp);

        int urlCount = urlScriptList.size();

        for (int i = 0; i < urlCount; i++) {

            bp = new MimeBodyPart();
            ArrayList urlInfo = (ArrayList) urlScriptList.get(i);
            String absoluteURL = urlInfo.get(1).toString();

            bp.addHeader("Content-Location",javax.mail.internet.MimeUtility
                    .encodeWord(java.net.URLDecoder.decode(absoluteURL, strEncoding)));

            DataSource source = new AttachmentDataSource(absoluteURL, "text");
            bp.setDataHandler(new DataHandler(source));

            mp.addBodyPart(bp);

        }

        urlCount = urlImageList.size();

        for (int i = 0; i < urlCount; i++) {

            bp = new MimeBodyPart();
            ArrayList urlInfo = (ArrayList) urlImageList.get(i);

            // String url = urlInfo.get(0).toString();
            String absoluteURL = urlInfo.get(1).toString();
            bp.addHeader("Content-Location",javax.mail.internet.MimeUtility
                    .encodeWord(java.net.URLDecoder.decode(absoluteURL, strEncoding)));

            DataSource source = new AttachmentDataSource(absoluteURL, "image");
            bp.setDataHandler(new DataHandler(source));

            mp.addBodyPart(bp);
        }
        msg.setContent(mp);
        // write the mime multi part message to a file
        msg.writeTo(new FileOutputStream(strFilePath+"//"+strWeb.toString().split("/")[strWeb.toString().split("/").length-1]+".mht"));
        // Transport.send(msg);
    }

    private InternetAddress[] getInetAddresses(String emails) throws Exception {
        ArrayList list = new ArrayList();
        StringTokenizer tok = new StringTokenizer(emails, ",");
        while (tok.hasMoreTokens()) {
            list.add(tok.nextToken());
        }
        int count = list.size();
        InternetAddress[] addresses = new InternetAddress[count];
        for (int i = 0; i < count; i++) {
            addresses[i] = new InternetAddress(list.get(i).toString());
        }
        return addresses;

    }

    class AttachmentDataSource implements DataSource {

        private MimetypesFileTypeMap map = new MimetypesFileTypeMap();
        private String strUrl;
        private String strType;
        private byte[] dataSize = null;

        /**
         *
         * This is some content type maps.
         */
        private Map normalMap = new HashMap();
        {
            // Initiate normal mime type map
            // Images
            normalMap.put("image", "image/jpeg");
            normalMap.put("text", "text/plain");

        }

        public AttachmentDataSource(String strUrl, String strType) {
            this.strType = strType;
            this.strUrl = strUrl;
            strUrl = strUrl.trim();
            strUrl = strUrl.replaceAll(" ", "%20");
            dataSize = downBinaryFile(strUrl);

        }

        public String getContentType() {
            return getMimeType(getName());
        }

        public String getName() {
            char separator = File.separatorChar;
            if (strUrl.lastIndexOf(separator) >= 0)
                return strUrl.substring(strUrl.lastIndexOf(separator) + 1);
            return strUrl;

        }

        private String getMimeType(String fileName) {
            String type = (String) normalMap.get(strType);
            if (type == null) {
                try {
                    type = map.getContentType(fileName);
                } catch (Exception e) {
                }
                if (type == null) {
                    type = "application/octet-stream";
                }
            }
            return type;

        }

        public InputStream getInputStream() throws IOException {
            if (dataSize == null)
                dataSize = new byte[0];
            return new ByteArrayInputStream(dataSize);
        }

        public OutputStream getOutputStream() throws IOException {
            return new java.io.ByteArrayOutputStream();
        }

    }

    class Email_auth extends Authenticator {

        String auth_user;
        String auth_password;

        public Email_auth() {
            super();
        }

        public Email_auth(String user, String password) {
            super();
            setUsername(user);
            setUserpass(password);

        }

        public void setUsername(String username) {
            auth_user = username;
        }

        public void setUserpass(String userpass) {
            auth_password = userpass;
        }

        public PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(auth_user, auth_password);
        }

    }

}
