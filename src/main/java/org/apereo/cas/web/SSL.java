package org.apereo.cas.web;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import javax.net.ssl.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class SSL extends SimpleClientHttpRequestFactory {
    public SSL(){
        this(5000,15000);
    }
    public SSL(int readTimout,int connTimeout){
        this.setReadTimeout(readTimout);
        this.setConnectTimeout(connTimeout);
    }
    @Override
    protected void prepareConnection(HttpURLConnection connection, String httpMethod)
            throws IOException {
        if (connection instanceof HttpsURLConnection) {
        	HttpsURLConnection conn =(HttpsURLConnection)connection; 
            try {
            	conn.setHostnameVerifier(new HostnameVerifier() {
            		 @Override
            	        public boolean verify(String s, SSLSession sslSession) {
            	            return true;
            	        }
            	});
            	conn.setSSLSocketFactory(createSslSocketFactory());
            }
            catch (Exception ex) {
                
            }
        }
        super.prepareConnection(connection, httpMethod);
    }

    private SSLSocketFactory createSslSocketFactory() throws Exception {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[] { new SkipX509TrustManager() },
                new SecureRandom());
        return context.getSocketFactory();
    }

    private static class SkipX509TrustManager implements X509TrustManager {

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        	//return new X509Certificate[0];
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

    }

}


