package com.hongframe.entity;

import net.sf.jmimemagic.*;
import org.apache.commons.codec.binary.Base64;

/**
 * @author zehong.hongframe.huang@gmail.com, 有事也别来找我
 * @author huangzehong.me
 * 2016年9月18日
 */
public class Image {
    private byte[] bytes;

    private String base64;

    private String contentType;

    private String imageType;


    public Image(byte[] bytes) {
        super();
        this.bytes = bytes;
    }

    public Image(byte[] bytes, String contentType) {
        super();
        this.bytes = bytes;
        this.contentType = contentType;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public String getBase64(boolean withType) {
        if (this.base64 == null) {
            this.base64 = new String(Base64.encodeBase64(this.bytes));
        }
        if (withType && this.imageType == null) {
            try {
                MagicMatch match = Magic.getMagicMatch(this.bytes);
                this.imageType = match.getMimeType();
            } catch (MagicParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (MagicMatchNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (MagicException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        if(withType){
            return "data:"+this.imageType + ";base64,"+this.base64;
        }else{
            return this.base64;
        }
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

}
