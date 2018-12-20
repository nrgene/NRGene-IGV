/*
 * Copyright (c) 2007-2011 by The Broad Institute of MIT and Harvard.  All Rights Reserved.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 *
 * THE SOFTWARE IS PROVIDED "AS IS." THE BROAD AND MIT MAKE NO REPRESENTATIONS OR
 * WARRANTES OF ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED, INCLUDING,
 * WITHOUT LIMITATION, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT OR OTHER DEFECTS, WHETHER
 * OR NOT DISCOVERABLE.  IN NO EVENT SHALL THE BROAD OR MIT, OR THEIR RESPECTIVE
 * TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR ANY DAMAGES
 * OF ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR CONSEQUENTIAL DAMAGES,
 * ECONOMIC DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER
 * THE BROAD OR MIT SHALL BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT
 * SHALL KNOW OF THE POSSIBILITY OF THE FOREGOING.
 */

package org.broad.igv.util;

import java.awt.Frame;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.broad.igv.PreferenceManager;
import org.broad.igv.gs.GSUtils;
import org.broad.igv.nrgene.api.ApiRequest;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.IGVMainFrame;
import org.broad.igv.util.stream.IGVUrlHelper;
import org.broad.tribble.util.SeekableHTTPStream;
import org.broad.tribble.util.ftp.FTPClient;
import org.broad.tribble.util.ftp.FTPStream;
import org.broad.tribble.util.ftp.FTPUtils;

import sun.misc.BASE64Encoder;

/**
 * Wrapper utility class... for interacting with HttpURLConnection.
 *
 * @author Jim Robinson
 * @date 9/22/11
 */
public class HttpUtils {

    private static Logger log = Logger.getLogger(HttpUtils.class);

    public static boolean byteRangeTested = false;
    public static boolean byteRangeTestSuccess = false;
    private static HttpUtils instance;
    private static String lastErrorText;

    public static String getLastErrorText() {
		return lastErrorText;
	}

	public static void setLastErrorText(String lastErrorText) {
		HttpUtils.lastErrorText = lastErrorText;
	}

	private ProxySettings proxySettings = null;
    private final int MAX_REDIRECTS = 5;

	private Map<String, String> niuActualUrls = new LinkedHashMap<String, String>();
	
	private static Boolean			trustAllCerts = null;

    static {
        synchronized (HttpUtils.class) {
            instance = new HttpUtils();
            CookieHandler.setDefault(new CookieManager());
        }
    }

    /**
     * @return the single instance
     */
    public static HttpUtils getInstance() {
        return instance;
    }

    /**
     * Constructor
     */
    private HttpUtils() {
        Authenticator.setDefault(new IGVAuthenticator());

    }


    public static boolean isURL(String string) {
        String lcString = string.toLowerCase();
        return lcString.startsWith("http://") || lcString.startsWith("https://") || lcString.startsWith("ftp://")
                || lcString.startsWith("file://");
    }

    /**
     * Test to see if this client can successfully retrieve a portion of a remote file using the byte-range header.
     * This is not a test of the server, but the client.  In some environments the byte-range header gets removed
     * by filters after the request is made by IGV.
     *
     * @return
     */
    public static boolean testByteRange() {

        try {
            String testURL = "http://www.broadinstitute.org/igvdata/annotations/seq/hg19/chr1.txt";
            byte[] expectedBytes = {'C', 'A', 'G', 'C', 'T', 'A', 'A', 'T', 'T', 'T', 'T', 'G', 'T', 'A', 'T', 'T',
                    'T', 'T', 'T', 'A', 'G', 'T', 'A', 'G', 'A', 'G', 'T'};


            SeekableHTTPStream str = new SeekableHTTPStream(new IGVUrlHelper(new URL(testURL)));
            str.seek(161032764);
            byte[] buffer = new byte[expectedBytes.length];
            str.read(buffer, 0, expectedBytes.length);

            for (int i = 0; i < expectedBytes.length; i++) {
                if (buffer[i] != expectedBytes[i]) {
                    log.info("Byte range test failed -- problem with client network environment.");
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            log.error("Error while testing byte range " + e.getMessage());
            // We could not reach the test server, so we can't know if this client can do byte-range tests or
            // not.  Take the "optimistic" view.
            return true;
        }
    }


    public static boolean useByteRange(URL url) {

        // Get explicit user setting
        boolean userByteRangeSetting = PreferenceManager.getInstance().getAsBoolean(PreferenceManager.USE_BYTE_RANGE);
        if (userByteRangeSetting == false) {
            // No means no!
            return false;
        }

        // We can test byte-range success for broad hosted data. We can't know if they work or not in other
        // environments (e.g. intranets)
        if (url.getHost().contains("broadinstitute.org")) {
            // Test broad urls for successful byte range requests.
            if (!byteRangeTested) {
                byteRangeTestSuccess = testByteRange();
                byteRangeTested = true;   // <= to prevent testing again
            }
            return byteRangeTestSuccess;
        } else {
            return true;
        }

    }

    /**
     * Always use this function and not directly get the Content-Length header, because if url is an amazon
     * api gateway, it sometimes passes a wrong value or 0 in the Content-Length header, and the correct value is
     * given in the x-amzn-Remapped-Content-Length header . This function reads both fields if exists and returns
     * the correct value.
     */
    public static String getContentLengthFromConnection(HttpURLConnection con) {
        String contentLength1 = con.getHeaderField("Content-Length");
        String contentLength2 = con.getHeaderField("x-amzn-Remapped-Content-Length");
        String result;
        if (StringUtils.isEmpty(contentLength1)) {
            result = StringUtils.isEmpty(contentLength2) ? null : contentLength2;
        } else {
            result = StringUtils.isEmpty(contentLength2) ? contentLength1 : contentLength2;
        }
        log.info("Content length for con " + con.getURL().toString() + " is: " + result);
        return result;
    }

    public void shutdown() {
        // Do any cleanup required here
    }

    /**
     * Return the contents of the url as a String.  This method should only be used for queries expected to return
     * a small amount of data.
     *
     * @param url
     * @return
     */
    public String getContentsAsString(URL url) throws IOException {

        InputStream is = null;
        HttpURLConnection conn = openConnection(url, null);

        try {
            is = conn.getInputStream();
            return readContents(is);

        } finally {
            if (is != null) is.close();
        }

    }

    public HttpURLConnection openConnectionRaw(URL url) throws IOException {
        return openConnection(url, null);
    }

    public InputStream openConnectionStream(URL url) throws IOException {
    	return openConnectionStream(url, (ResourceLocator)null);
    }
    
    public InputStream openConnectionStream(URL url, ResourceLocator locator) throws IOException {
        if (url.getProtocol().toLowerCase().equals("ftp")) {
            String userInfo = url.getUserInfo();
            String host = url.getHost();
            String file = url.getPath();
            FTPClient ftp = FTPUtils.connect(host, userInfo, new UserPasswordInputImpl());
            ftp.pasv();
            ftp.retr(file);
            return new FTPStream(ftp);

        } else {
            HttpURLConnection	conn = openConnection(url, null);
            InputStream			is = null;
            
            if ( locator != null )
            {
            	int				code = conn.getResponseCode();
            	if ( code >= 300 )
            	{
            		locator.setHttpErrorCode(code);
            		locator.setHttpErrorMessage(conn.getResponseMessage());
            	}
            	
            	if ( ApiRequest.isApiUrlWithResultNotReadyYet(url, code) )
            	{
            	    log.info("Found api url with result not ready yet. Issuing an api request");
	            	ApiRequest			apiRequest = new ApiRequest(url, conn);
	            	
	            	apiRequest.postRequest(true);
	            	
	            	locator.setApiRequest(apiRequest);
            	}
            }
            
            if ( is == null )
            	is = conn.getInputStream();
            return is;
        }
    }

    public InputStream openConnectionStream(URL url, Map<String, String> requestProperties) throws IOException {
        HttpURLConnection conn = openConnection(url, requestProperties);
        return conn.getInputStream();
    }


    public boolean resourceAvailable(URL url) {

        if (url.getProtocol().toLowerCase().equals("ftp")) {
            return FTPUtils.resourceAvailable(url);
        }

        try {
            HttpURLConnection conn = openConnection(url, null, "HEAD");
            int code = conn.getResponseCode();
            if (ApiRequest.isNewAPI(url) && url.getPath().endsWith(".idx") && code == 202)
                return false; // otherwise IGV starts to try and read this stuff too early
            return (code == 200) || (ApiRequest.isNiu() && (code == 202));
        } catch (IOException e) {
            return false;
        }
    }

    public long getContentLength(URL url) throws IOException {
        String contentLengthString = "";

        HttpURLConnection conn = openConnection(url, null, "HEAD");
        int code = conn.getResponseCode();
        // TODO -- check code
        contentLengthString = getContentLengthFromConnection(conn);
        if (contentLengthString == null) {
            return -1;
        } else {
            return Long.parseLong(contentLengthString);
        }
    }

    public void updateProxySettings() {
        boolean useProxy;
        String proxyHost;
        int proxyPort = -1;
        boolean auth = false;
        String user = null;
        String pw = null;

        PreferenceManager prefMgr = PreferenceManager.getInstance();
        useProxy = prefMgr.getAsBoolean(PreferenceManager.USE_PROXY);
        proxyHost = prefMgr.get(PreferenceManager.PROXY_HOST, null);
        try {
            proxyPort = Integer.parseInt(prefMgr.get(PreferenceManager.PROXY_PORT, "-1"));
        } catch (NumberFormatException e) {
            proxyPort = -1;
        }
        auth = prefMgr.getAsBoolean(PreferenceManager.PROXY_AUTHENTICATE);
        user = prefMgr.get(PreferenceManager.PROXY_USER, null);
        String pwString = prefMgr.get(PreferenceManager.PROXY_PW, null);
        if (pwString != null) {
            pw = Utilities.base64Decode(pwString);
        }

        proxySettings = new ProxySettings(useProxy, user, pw, auth, proxyHost, proxyPort);
    }

    public boolean downloadFile(String url, File outputFile) throws IOException {

        log.info("Downloading " + url + " to " + outputFile.getAbsolutePath());

        HttpURLConnection conn = openConnection(new URL(url), null);

        long contentLength = -1;
        String contentLengthString = getContentLengthFromConnection(conn);
        if (contentLengthString != null) {
            contentLength = Long.parseLong(contentLengthString);
        }


        log.info("Content length = " + contentLength);

        InputStream is = null;
        OutputStream out = null;

        try {
            is = conn.getInputStream();
            out = new FileOutputStream(outputFile);

            byte[] buf = new byte[64 * 1024];
            int downloaded = 0;
            int bytesRead = 0;
            while ((bytesRead = is.read(buf)) != -1) {
                out.write(buf, 0, bytesRead);
                downloaded += bytesRead;
            }
            log.info("Download complete.  Total bytes downloaded = " + downloaded);
        } finally {
            if (is != null) is.close();
            if (out != null) {
                out.flush();
                out.close();
            }
        }
        long fileLength = outputFile.length();

        return contentLength <= 0 || contentLength == fileLength;
    }


    public void uploadGenomeSpaceFile(String uri, File file, Map<String, String> headers) throws IOException {

        HttpURLConnection urlconnection = null;
        OutputStream bos = null;

        URL url = new URL(uri);
        urlconnection = openConnection(url, headers, "PUT");
        urlconnection.setDoOutput(true);
        urlconnection.setDoInput(true);

        bos = new BufferedOutputStream(urlconnection.getOutputStream());
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        int i;
        // read byte by byte until end of stream
        while ((i = bis.read()) > 0) {
            bos.write(i);
        }
        bos.close();
        int responseCode = urlconnection.getResponseCode();

        // Error messages below.
        if (responseCode >= 400) {
            String message = readErrorStream(urlconnection);
            throw new IOException("Error uploading " + file.getName() + " : " + message);
        }
    }


    public String createGenomeSpaceDirectory(URL url, String body) throws IOException {

        HttpURLConnection urlconnection = null;
        OutputStream bos = null;

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        headers.put("Content-Length", String.valueOf(body.getBytes().length));

        urlconnection = openConnection(url, headers, "PUT");
        urlconnection.setDoOutput(true);
        urlconnection.setDoInput(true);

        bos = new BufferedOutputStream(urlconnection.getOutputStream());
        bos.write(body.getBytes());
        bos.close();
        int responseCode = urlconnection.getResponseCode();

        // Error messages below.
        StringBuffer buf = new StringBuffer();
        InputStream inputStream = null;

        if (responseCode >= 200 && responseCode < 300) {
            inputStream = urlconnection.getInputStream();
        } else {
            inputStream = urlconnection.getErrorStream();
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String nextLine;
        while ((nextLine = br.readLine()) != null) {
            buf.append(nextLine);
            buf.append('\n');
        }
        inputStream.close();

        if (responseCode >= 200 && responseCode < 300) {
            return buf.toString();
        } else {
            throw new IOException("Error creating GS directory: " + buf.toString());
        }
    }

    ;

    /**
     * Code for disabling SSL certification
     */
    private static void disableCertificateValidation() {
    	
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
        	log.warn("", e);
        }

    }

    private String readContents(InputStream is) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(is);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int b;
        while ((b = bis.read()) >= 0) {
            bos.write(b);
        }
        return new String(bos.toByteArray());
    }

    private String readErrorStream(HttpURLConnection connection) throws IOException {
        InputStream inputStream = null;

        try {
            inputStream = connection.getErrorStream();
            return readContents(inputStream);
        } finally {
            if (inputStream != null) inputStream.close();
        }


    }

    private HttpURLConnection openConnection(URL url, Map<String, String> requestProperties) throws IOException {
        return openConnection(url, requestProperties, "GET");
    }

    private HttpURLConnection openConnection(URL url, Map<String, String> requestProperties, String method) throws IOException {
        return openConnection(url, requestProperties, method, 0);
    }

    /**
     * The "real" connection method
     *
     * @param url
     * @param requestProperties
     * @param method
     * @return
     * @throws java.io.IOException
     */
    private synchronized HttpURLConnection openConnection(
            URL url, Map<String, String> requestProperties, String method, int redirectCount) throws IOException {
    	
    	// disable cert checking
    	addTrustAllCerts();
    	
    	// translate?
    	if ( ApiRequest.isNiu() )
    	{ 	
    		String		externalForm = niuActualUrls.get(url.toExternalForm());
    		if ( externalForm != null )
    		{
    			log.debug("actualizing: " + url + " -> " + externalForm);
    			url = new URL(externalForm);
    		}
    	}
    	
        boolean useProxy = proxySettings != null && proxySettings.useProxy && proxySettings.proxyHost != null &&
                proxySettings.proxyPort > 0;

        HttpURLConnection conn = null;
        if (useProxy) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxySettings.proxyHost, proxySettings.proxyPort));
            conn = (HttpURLConnection) url.openConnection(proxy);

            if (proxySettings.auth && proxySettings.user != null && proxySettings.pw != null) {
                byte[] bytes = (proxySettings.user + ":" + proxySettings.pw).getBytes();
                String encodedUserPwd = (new BASE64Encoder()).encode(bytes);
                conn.setRequestProperty("Proxy-Authorization", "Basic " + encodedUserPwd);
            }
        } else {
            conn = (HttpURLConnection) url.openConnection();
        }

        if (GSUtils.isGenomeSpace(url)) {

            String token = GSUtils.getGSToken();
            if (token != null) conn.setRequestProperty("Cookie", "gs-token=" + token);

            // The GenomeSpace server requires the Accept header below, unless doing a GET for an uploadurl.  Setting
            // the header in that case results in a 406 error.
            if (!url.toString().contains("datamanager/uploadurls")) {
                conn.setRequestProperty("Accept", "application/json,application/text");
            }

        }
        
        // add security properties
        addSecurityRequestProperties(conn);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(300000);
        conn.setRequestMethod(method);
        conn.setUseCaches(false);
        conn.setRequestProperty("Connection", "close");

        if (requestProperties != null) {
            for (Map.Entry<String, String> prop : requestProperties.entrySet()) {
                conn.setRequestProperty(prop.getKey(), prop.getValue());
            }
        }

        if (method.equals("PUT")) {
            return conn;
        } else {
            int code = conn.getResponseCode();
            log.info("code: " + code + " for " + url + ", properties: " + requestProperties);
            if (code >= 200 && code < 300) {
            	
            	// a niu hack, catch actual url
            	if ( ApiRequest.isNiu() && method.equals("HEAD") )
            	{
            		niuActualUrls .put(url.toExternalForm(), conn.getURL().toExternalForm());
            	}
            	
            	
            	// A genome-space hack.  We want to catch redirects from the identity server to grab the cookie
                // and write it to the .gstoken file.  This is required for the GS single-sign on model
                if (GSUtils.isGenomeSpace(url)) {
                    try {
                        java.util.List<HttpCookie> cookies = ((CookieManager) CookieManager.getDefault()).getCookieStore().get(url.toURI());
                        if (cookies != null) {
                            for (HttpCookie cookie : cookies) {
                                if (cookie.getName().equals("gs-token")) {
                                    GSUtils.setGSToken(cookie.getValue());
                                } else if (cookie.getName().equals("gs-username")) {
                                    GSUtils.setGSUser(cookie.getValue());
                                }
                            }
                        }
                    } catch (URISyntaxException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            }

            // Redirects.  These can occur even if followRedirects == true if there is a change in protocol,
            // for example http -> https.
            if (code >= 300 && code < 400) {

                if (redirectCount > MAX_REDIRECTS) {
                    throw new IOException("Too many redirects");
                }

                String newLocation = conn.getHeaderField("Location");
                log.debug("Redirecting to " + newLocation);

                return openConnection(new URL(newLocation), requestProperties, method, redirectCount++);
            }

            // TODO -- handle other response codes.
            else if (code >= 400) {

                String message;
                if (code == 404) {
                    message = "File not found: " + url.toString();
                    throw new FileNotFoundException(message);
                } else {
                    message = conn.getResponseMessage();
                }
                String details = readErrorStream(conn);
                
                String			text = "Server returned error code: " + code + " (" + message + ")";
                if ( !StringUtils.isEmpty(details) )
                {
                	int			lengthLimit = 512;
                	
                	if ( details.length() > lengthLimit )
                		details = details.substring(0, lengthLimit - 4) + " ...";
                	
                	text += "\n\nError Text:\n" + addLinebreaks(details, 120);
                	
                	setLastErrorText(text);
                }

                throw new IOException(text);
            }
        }
        return conn;
    }

    private static synchronized void addTrustAllCerts() 
    {
    	// already set?
    	if ( trustAllCerts != null )
    		return;
    	
    	// get conf
    	trustAllCerts = PreferenceManager.getInstance().getAsBoolean(PreferenceManager.SEC_TRUST_ALL_CERTS, false);
    	
    	// disable?
    	if ( trustAllCerts )
    		disableCertificateValidation();
	}

	private static void addSecurityRequestProperties(HttpURLConnection conn)
    {
		for ( int index = 1 ; index < 100 ; index++ )
		{
	    	// get key & value
	    	String			suffix = "_" + index;
	    	String			key = PreferenceManager.getInstance().get(PreferenceManager.SEC_HEADER_KEY + suffix, null);
	    	String			value = PreferenceManager.getInstance().get(PreferenceManager.SEC_HEADER_VALUE + suffix, null);
	    	if ( StringUtils.isEmpty(key) || StringUtils.isEmpty(value) )
	    		break;
	    	log.debug("key: " + key + ", value: " + value);
	    	
	    	// set prop
	    	conn.setRequestProperty(key, value);
		}
		
		// add auth token
		if ( IGVMainFrame.hasAuthToken() )
		{
			String		key = PreferenceManager.getInstance().get(PreferenceManager.SEC_HEADER_AUTH_TOKEN, PreferenceManager.SEC_HEADER_AUTH_TOKEN_DEFVAL);
			String		value = IGVMainFrame.getAuthToken();
			
	    	log.debug("key: " + key + ", value: " + value);
	    	conn.setRequestProperty(key, value);
		}
	}

	public String addLinebreaks(String input, int maxLineLength) {
        StringTokenizer tok = new StringTokenizer(input, " ");
        StringBuilder output = new StringBuilder(input.length());
        int lineLen = 0;
        while (tok.hasMoreTokens()) {
            String word = tok.nextToken();

            if (lineLen + word.length() > maxLineLength) {
                output.append("\n");
                lineLen = 0;
            }
            if ( lineLen != 0 )
            	output.append(" ");
            output.append(word);
            lineLen += word.length();
        }
        return output.toString();
    }
    
    public static class ProxySettings {
        boolean auth = false;
        String user;
        String pw;
        boolean useProxy;
        String proxyHost;
        int proxyPort = -1;

        public ProxySettings(boolean useProxy, String user, String pw, boolean auth, String proxyHost, int proxyPort) {
            this.auth = auth;
            this.proxyHost = proxyHost;
            this.proxyPort = proxyPort;
            this.pw = pw;
            this.useProxy = useProxy;
            this.user = user;
        }
    }

    /**
     * The default authenticator
     */
    public class IGVAuthenticator extends Authenticator {

        /**
         * Called when password authentcation is needed.
         *
         * @return
         */
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {

            RequestorType type = getRequestorType();
            URL url = this.getRequestingURL();

            boolean isProxyChallenge = type == RequestorType.PROXY;
            if (isProxyChallenge) {
                if (proxySettings.auth && proxySettings.user != null && proxySettings.pw != null) {
                    return new PasswordAuthentication(proxySettings.user, proxySettings.pw.toCharArray());
                }
            }

            Frame owner = IGV.hasInstance() ? IGV.getMainFrame() : null;

            boolean isGenomeSpace = GSUtils.isGenomeSpace(url);

            LoginDialog dlg = new LoginDialog(owner, isGenomeSpace, url.toString(), isProxyChallenge);
            dlg.setVisible(true);
            if (dlg.isCanceled()) {
                return null;
            } else {
                final String userString = dlg.getUsername();
                final char[] userPass = dlg.getPassword();

                if (isProxyChallenge) {
                    proxySettings.user = userString;
                    proxySettings.pw = new String(userPass);
                }

                return new PasswordAuthentication(userString, userPass);
            }
        }
    }
}
