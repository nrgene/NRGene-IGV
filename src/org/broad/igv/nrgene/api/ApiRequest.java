package org.broad.igv.nrgene.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.broad.igv.util.HttpUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class ApiRequest {

	private static Logger log = Logger.getLogger(ApiRequest.class);
    
    private static boolean niu = true; /// New-Indexed-Url enabler
    
	public enum State {
		INIT,						// initialized, not posted yet
		POSTED,						// posted to server
		PENDING,						// reported pending by server
		READY,						// reported done/ready by server
		ERROR						// in error
	};
	
    public enum QueryStatusValue {
        InProgress, 
        Success, 
        Fail, 
        NotExist
    }

    private static final String JSON_FIELD_RESULT_URL = "resultUrl";
	private static final String JSON_FIELD_STATUS = "status";
	private static final String JSON_FIELD_STATUS_INFO = "statusInfo";

	static final int	CODE_OK			= 200; 
	static final int	CODE_ACCEPTED	= 202;
	static final int	CODE_SEE_OTHER	= 303;
	
	private URL			url;
	private HttpUtils httpUtils;
	private State		state = State.INIT;
	
	private URL			statusUrl;
	private URL			dataUrl;
	private URL			dataIndexUrl;
	
	private int			lastCode;
	
	private long		sleepMsec = 2000;
	
	private Thread		thread;

	
	public ApiRequest(URL url)
	{
		this.url = url;
		this.httpUtils = HttpUtils.getInstance();
	}
	

	public ApiRequest(URL url, HttpURLConnection conn) throws IOException
	{
		this(url);

		if (isNewAPI(url))
			statusUrl = url;
		else
			statusUrl = new URL(conn.getHeaderField("Location"));
		setState(State.PENDING);
	}

	public static boolean isNewAPI(URL url) {
		return url.toExternalForm().contains("/genomagic-api/");
	}

	@Override
	public String toString()
	{
		StringBuilder		sb = new StringBuilder();
		
		sb.append(getState());
		if ( getUrl() != null )
			sb.append(" url: " + getUrl());
		if ( getStatusUrl() != null )
			sb.append(" status: " + getStatusUrl());
		if ( getDataUrl() != null )
			sb.append(" data: " + getDataUrl());
		
		return sb.toString();
	}
	
	public synchronized void postRequest(boolean autoStatus) throws IOException
	{
		// post if not already
		if ( getState() == State.INIT )
		{
			// create connection
			HttpURLConnection		conn = httpUtils.openConnectionRaw(url);
			conn.setInstanceFollowRedirects(false);
	
			
			// connect, get status
			setState(State.POSTED);
			conn.connect();
			lastCode = conn.getResponseCode();
			log.info("code: " + lastCode + " when accessing url: " + url);

			if (isNewAPI(url)) {
				if (lastCode == CODE_ACCEPTED) {
					statusUrl = url;
					setState(State.PENDING);
				} else if (lastCode == CODE_OK) {
					dataUrl = url;
					setState(State.READY);
				} else {
					setState(State.ERROR);
				}
			} else {
				// redirecting to status?
				if (lastCode == CODE_ACCEPTED) {
					statusUrl = new URL(conn.getHeaderField("Location"));
					setState(State.PENDING);
				} else if (lastCode == CODE_SEE_OTHER) {
					dataUrl = new URL(conn.getHeaderField("Location"));
					setState(State.READY);
				} else
					setState(State.ERROR);
			}
			conn.disconnect();
		}
		
		// kick off auto status?
		if ( getState() == State.PENDING && autoStatus )
		{
			thread = new Thread(new Runnable() {
				
				@Override
				public void run() 
				{
					while ( getState() == State.PENDING )
					{
						try 
						{
							Thread.sleep(sleepMsec);
							checkStatus();
						} catch (Exception e) {
							setState(State.ERROR);
							log.error("Error when accessing url: " + statusUrl +". Original request url is: " + url,e);
							break;
						}
					}
					thread = null;
				}
			});
			
			thread.start();
		}
	}
	
	private void checkStatus() throws IOException {
		if(isNewAPI(statusUrl))
			checkStatusNewAPI();
		else
			checkStatusOldAPI();
	}

	private synchronized void checkStatusNewAPI() throws IOException {
		if (getState() != State.PENDING)
			return;

		HttpURLConnection conn =  null;
		try {
			conn = httpUtils.openConnectionRaw(statusUrl);
			conn.connect();
			lastCode = conn.getResponseCode();
			log.info("code: " + lastCode + " when accessing statusUrl: " + statusUrl);

			// redirecting to status?
			if (lastCode == CODE_OK) {
				dataUrl = url;
				setState(State.READY);
			} else if (lastCode >= 400) {
				String errMsg = extractErrorMessageFromJSON(conn);
				throw new IOException("Return code: " + lastCode + " from: " + statusUrl + ", message: " + errMsg);
			}
		} finally {
			if (conn != null) conn.disconnect();
		}
	}

	private String extractErrorMessageFromJSON(HttpURLConnection connection) {
		try {
			return new JSONObject(new JSONTokener(connection.getErrorStream())).getJSONObject("error").getString("message");
		} catch (Exception e) {
			return "";
		}
	}

	private synchronized void checkStatusOldAPI() throws IOException {
		if ( getState() != State.PENDING )
			return;

		// create connection
		HttpURLConnection		conn = (HttpURLConnection)statusUrl.openConnection();
		conn.setInstanceFollowRedirects(false);
		
		// connect, get status
		conn.connect();
		lastCode = conn.getResponseCode();
		log.info("code: " + lastCode + " when accessing statusUrl: " + statusUrl);
		
		// redirecting to status?
		if ( lastCode == CODE_OK )
		{
			try {
				// get parse status json
				InputStream			is = conn.getInputStream();
				JSONTokener			tokenizer = new JSONTokener(is);
				JSONObject			json = new JSONObject(tokenizer);
				
				log.info("json: " + json);
				if ( json.has(JSON_FIELD_STATUS_INFO) )
				{
					JSONObject statusInfo = json.getJSONArray(JSON_FIELD_STATUS_INFO).getJSONObject(0);
					String queryStatusValueStr = statusInfo.getString(JSON_FIELD_STATUS);
					QueryStatusValue queryStatusValue = QueryStatusValue.valueOf(queryStatusValueStr);
					switch (queryStatusValue) {
					case Success:
						String resultUrl = statusInfo.getString(JSON_FIELD_RESULT_URL);
						if ( !StringUtils.isEmpty(resultUrl) )
						{
							dataUrl = new URL(resultUrl);
							setState(State.READY);
						} else { 
							throw new IOException("Unexpected empty '" + JSON_FIELD_RESULT_URL + "' json field value");
						}
						break;
					case Fail:
					case NotExist:
						throw new IOException("Api request status is :  " + queryStatusValue.toString());

					case InProgress:
						break;
					}
				} else { 
					throw new IOException("Unexpected json structure:  missing '" + JSON_FIELD_STATUS_INFO + "' field");
				}

			} catch (JSONException e) {
				throw new IOException(e);
			}
			
		} else if ( lastCode == CODE_SEE_OTHER ) {
			dataUrl = new URL(conn.getHeaderField("Location"));			
			setState(State.READY);
		} else {
			throw new IOException("Return code: " + lastCode + " from: " + statusUrl);
		}	
		conn.disconnect();
	}
	
	public synchronized void cancel()
	{
		if ( thread != null )
		{
			Thread.interrupted();
			thread = null;
		}
	}
	
	public static boolean isApiUrlWithResultNotReadyYet(URL url, int lastCode)
	{
		return url.getPath().contains("genomagic-api") && lastCode == CODE_ACCEPTED;
	}
	
	public static void main(String[] args) throws IOException, InterruptedException
	{
		int			randLimit = 10000;
		Random		random = new Random();
		
		boolean		autoStatus = true;
		
		for ( String arg : args )
		{
			URL					url = new URL(arg.replace("$R", Integer.toString(random.nextInt(randLimit))));
			ApiRequest			apiRequest = new ApiRequest(url);
			
			// posting
			System.out.println("Posting " + url + " ...");
			apiRequest.postRequest(autoStatus);
			
			// loop until read or error
			while ( true )
			{
				// get state
				State			state = apiRequest.getState();
				System.out.println(" " + state + " (" + apiRequest.getLastCode() + ")");
				if ( apiRequest.getState() != State.PENDING )
					break;
				
				// wait and poll status
				Thread.sleep(apiRequest.getSleepMsec());
				System.out.println(" " + apiRequest.getStatusUrl());
				if ( !autoStatus )
					apiRequest.checkStatus();
			}
			
			// if ready, show data url
			if ( apiRequest.getState() == State.READY )
			{
				System.out.println(" dataUrl: " + apiRequest.getDataUrl());
				if ( apiRequest.getDataIndexUrl() != null )
					System.out.println(" dataIndexUrl: " + apiRequest.getDataIndexUrl());
			}
		}
	}
	
	public URL getUrl() {
		return url;
	}

	public synchronized State getState() {
		return state;
	}

	public synchronized void setState(State state) {
		this.state = state;
	}
	public URL getStatusUrl() {
		return statusUrl;
	}

	public URL getDataUrl() {
		return dataUrl;
	}

	public URL getDataIndexUrl() {
		return dataIndexUrl;
	}

	public int getLastCode() {
		return lastCode;
	}

	public long getSleepMsec() {
		return sleepMsec;
	}


	public static boolean isNiu() {
		return niu;
	}


	public static String extractCleanPath(String path) 
	{
		if ( StringUtils.isEmpty(path) )
			return path;
		
		// remove params
		int			index = path.indexOf('?');
		if ( index >= 0 )
			path = path.substring(0, index);
		
		// leave only last path element
		index = path.lastIndexOf(File.separator);
		if ( index >= 0 )
			path = path.substring(index + 1);
		
		return path;
	}


	public static String buildIndexPath(String path, String indexExtension)
	{
		if ( StringUtils.isEmpty(path) )
			return path;
		
		// remove params
		int			index = path.indexOf('?');
		if ( index >= 0 )
			path = path.substring(0, index) + indexExtension + path.substring(index);
		else
			path = path + indexExtension;
		
		return path;
	}


	public static boolean isApiUrl(String path) 
	{
		if ( !ApiRequest.isNiu() )
			return false;
		
		// this is a guess
		if ( StringUtils.isEmpty(path) )
			return false;
		
		return path.startsWith("http") && (path.indexOf("genomagic-api") > 0);
	}

}
