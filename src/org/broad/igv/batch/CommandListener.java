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
package org.broad.igv.batch;

import java.awt.Frame;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.channels.ClosedByInterruptException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.broad.igv.Globals;
import org.broad.igv.PreferenceManager;
import org.broad.igv.ui.IGV;

public class CommandListener implements Runnable {

    private static Logger log = Logger.getLogger(CommandListener.class);

    private static CommandListener listener;

    private int port = -1;
    private int portMultiRange = -1;
    private ServerSocket serverSocket = null;
    private Socket clientSocket = null;
    private Thread listenerThread;
    boolean halt = false;
    
    static long lastActivityReport;
    static boolean enableReportActivity = true;

    public static synchronized void start(int port, int portMultiRange) {
        listener = new CommandListener(port, portMultiRange);
        listener.listenerThread.start();
    }


    public static synchronized void halt() {
        if (listener != null) {
            listener.halt = true;
            listener.listenerThread.interrupt();
            listener.closeSockets();
            listener = null;
        }
    }

    private CommandListener(int port, int portMultiRange) {
        this.port = port;
        this.portMultiRange = portMultiRange;
        listenerThread = new Thread(this);
    }

    /**
     * Loop forever, processing client requests synchronously.  The server is single threaded, because in most cases
     * we would not know how to process commands synchronously
     */
    public void run() {

        CommandExecutor cmdExe = new CommandExecutor();

        try {
        	if ( portMultiRange <= 0 )
        		serverSocket = new ServerSocket(port);
        	else
        	{
            	log.info("multiUsernameKey: " + multiUsernameKey());
            	log.info("multiLivePort: " + multiLivePort());
            	BindException		lastException = null;
            	int					portOffset = multiHash(multiUsernameKey());
            	int					basePort = port;
            	for ( int bindAttempt = 0 ; bindAttempt < multiPortWindowSize() ; bindAttempt++ )
            	{
            		port = basePort + ((portOffset + bindAttempt) % multiPortWindowSize());
            		
    	        	try
    	        	{
    	        		serverSocket = new ServerSocket(port);
    	        		break;
    	        	}
    	        	catch (BindException e)
    	        	{
    	        		lastException = e;
    	        	}
            	}
            	if ( serverSocket == null )
            		throw lastException;
        	}
            log.info("Listening on port " + port);

            while (true) {
                clientSocket = serverSocket.accept();
                processClientSession(cmdExe);
                if (clientSocket != null) {
                    try {
                        clientSocket.close();
                        clientSocket = null;
                    } catch (IOException e) {
                        log.error("Error in client socket loop", e);
                    }
                }
            }


        } catch (java.net.BindException e) {
            log.error(e);
        } catch (ClosedByInterruptException e) {
            log.error(e);

        } catch (IOException e) {
            if (!halt) {
                log.error("IO Error on port socket ", e);
            }
        }
    }

    /**
     * Process a client session.  Loop continuously until client sends the "halt" message, or closes the connection.
     *
     * @param cmdExe
     * @throws IOException
     */
    private void processClientSession(CommandExecutor cmdExe) throws IOException {
        PrintWriter out = null;
        BufferedReader in = null;

        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String inputLine;

            while (!halt && (inputLine = in.readLine()) != null) {

                String cmd = inputLine;
                if (cmd.startsWith("GET")) {
                    String command = null;
                    Map<String, String> params = null;
                    String[] tokens = inputLine.split(" ");
                    if (tokens.length < 2) {
                        sendHTTPResponse(out, "ERROR unexpected command line: " + inputLine);
                        return;
                    } else {
                        String[] parts = tokens[1].split("\\?");
                        if (parts.length < 2) {
                            sendHTTPResponse(out, "ERROR unexpected command line: " + inputLine);
                            return;
                        } else {
                            command = parts[0];
                            params = parseParameters(parts[1]);
                        }
                    }

                    // Consume the remainder of the request, if any.  This is important to free the connection.
                    String nextLine = in.readLine();
                    while (nextLine != null && nextLine.length() > 0) {
                        nextLine = in.readLine();
                    }

                    // If a callback (javascript) function is specified write it back immediately.  This function
                    // is used to cancel a timeout handler
                    String callback = params.get("callback");
                    if (callback != null) {
                        sendHTTPResponse(out, callback);
                    }

                    String		result = processGet(command, params, cmdExe);
                    if ( command.equals("/user") || command.equals("/exit") || command.equals("/idle") )
                    	sendHTTPResponse(out, result);
                    else
                    {
                    	if ( result != null && !result.equals("OK") )
                    		log.error("result: " + result);
	                    // If no callback was specified write back a "no response" header
	                    if (callback == null) {
	                        sendHTTPResponse(out, null);
	                    }
                    }

                    // http sockets are used for one request onle
                    return;

                } else {
                    // Port command
                    Globals.setBatch(true);
                    Globals.setSuppressMessages(true);
                    final String response = cmdExe.execute(inputLine);
                    out.println(response);
                }
            }
        } catch (IOException e) {
            log.error("Error processing client session", e);
        } finally {
            Globals.setSuppressMessages(false);
            Globals.setBatch(false);
            if (out != null) out.close();
            if (in != null) in.close();
        }
    }

    private void closeSockets() {
        if (clientSocket != null) {
            try {
                clientSocket.close();
                clientSocket = null;
            } catch (IOException e) {
                log.error("Error closing clientSocket", e);
            }
        }

        if (serverSocket != null) {
            try {
                serverSocket.close();
                serverSocket = null;
            } catch (IOException e) {
                log.error("Error closing server socket", e);
            }
        }
    }

    private static final String CRNL = "\r\n";
    private static final String CONTENT_TYPE = "Content-Type: ";
    private static final String HTTP_RESPONSE = "HTTP/1.1 200 OK";
    private static final String HTTP_NO_RESPONSE = "HTTP/1.1 204 No Response";
    private static final String CONTENT_LENGTH = "Content-Length: ";
    private static final String CONTENT_TYPE_TEXT_HTML = "text/html";
    private static final String CONNECTION_CLOSE = "Connection: close";

    private void sendHTTPResponse(PrintWriter out, String result) {

        out.println(result == null ? HTTP_NO_RESPONSE : HTTP_RESPONSE);
        if (result != null) {
            out.print(CONTENT_TYPE + CONTENT_TYPE_TEXT_HTML);
            out.print(CRNL);
            out.print(CONTENT_LENGTH + (result.length()));
            out.print(CRNL);
            out.print(CONNECTION_CLOSE);
            out.print(CRNL);
            out.print(CRNL);
            out.print(result);
            out.print(CRNL);
        }
        out.close();
    }

    /**
     * Process an http get request.
     */

    private String processGet(String command, Map<String, String> params, CommandExecutor cmdExe) throws IOException {

    	try
    	{
    		enableReportActivity = false;
    		
	        String result = "OK";
	        final Frame mainFrame = IGV.getMainFrame();
	
	        // Trick to force window to front, the setAlwaysOnTop works on a Mac,  toFront() does nothing.
	        if ( !command.equals("/user") && !command.equals("/exit") && !command.equals("/idle") )
	        {
		        mainFrame.toFront();
		        mainFrame.setAlwaysOnTop(true);
		        mainFrame.setAlwaysOnTop(false);
	        }
	        if (command.equals("/load")) {
	            if (params.containsKey("file")) {
	                String genomeID = params.get("genome");
	                String mergeValue = params.get("merge");
	                String locus = params.get("locus");
	                if (genomeID != null) {
	                    IGV.getFirstInstance().selectGenomeFromList(genomeID);
	                }
	                if(genomeID != null) genomeID = URLDecoder.decode(genomeID);
	                if(mergeValue != null) mergeValue = URLDecoder.decode(mergeValue);
	                if(locus != null) locus = URLDecoder.decode(locus);
	
	
	                // Default for merge is "false" for session files,  "true" otherwise
	                String file = params.get("file");
	                boolean merge;
	                if (mergeValue != null) {
	                    // Explicit setting
	                    merge = mergeValue.equalsIgnoreCase("true");
	                } else if (file.endsWith(".xml") || file.endsWith(".php") || file.endsWith(".php3")) {
	                    // Session file
	                    merge = false;
	                } else {
	                    // Data file
	                    merge = true;
	                }
	
	                String name = params.get("name");
	
	                result = cmdExe.loadFiles(file, locus, merge, name);
	            } else {
	                return ("ERROR Parameter \"file\" is required");
	            }
	        } else if (command.equals("/reload") || command.equals("/goto")) {
	            String locus = params.get("locus");
	            IGV.getFirstInstance().goToLocus(locus);
	        } else if (command.equals("/gc") ) {
	            System.gc();
	        } else if (command.equals("/redraw") ) {
	            IGV.getInstance().doRefresh();
	        } else if (command.equals("/user") ) {
	        	result = multiUsernameKey();
	        } else if (command.equals("/exit") ) {
	        	result = "OK";
	        	
	        	Timer		timer = new Timer();
	        	timer.schedule(new TimerTask() {
					
					public void run() {
						System.exit(0);
					}
				}, 1000);
	        	
	        } else if (command.equals("/idle") ) {
	        	result = Long.toString(System.currentTimeMillis() - lastActivityReport);
	        	
	        } else {
	            return ("ERROR Unknown command: " + command);
	        }
	
	        return result;
    	}
    	finally
    	{
    		enableReportActivity = true;
    	}
    }

    /**
     * Parse the html parameter string into a set of key-value pairs.  Parameter values are
     * url decoded with the exception of the "locus" parameter.
     *
     * @param parameterString
     * @return
     */
    private Map<String, String> parseParameters(String parameterString) {
        HashMap<String, String> params = new HashMap();
        String[] kvPairs = parameterString.split("&");
        for (String kvString : kvPairs) {
            String[] kv = kvString.split("=");
            if (kv.length == 1) {
                params.put(kv[0], null);
            } else {
                String key = kv[0];
                // Special treatment of locus string, need to preserve encoding of spaces
                String value = key.equals("locus") ? kv[1] : URLDecoder.decode(kv[1]);
                params.put(kv[0], value);
            }
        }
        return params;

    }
    public int multiLivePortTimeout()
    {
    	return 2500;
    }
    
    public String multiUsername()
    {
    	String		username = System.getProperty("user.name");
    	
    	return username;
    }
        
    public String multiDisplay()
    {
    	String		username = System.getenv("DISPLAY");
    	
    	return username;
    }
        
    public String multiUsernameKey()
    {
    	return multiUsername() + "_" + multiDisplay();
    }
        
    public int multiPortWindowSize()
    {
    	// this must be larger then the number of expected users. it is best that this number will be prime
    	return portMultiRange;
    }
    
    public int multiHash(String text)
    {
    	int		sum = 0;
    	
    	for ( int n = 0 ; n < text.length() ; n++ )
    		sum += text.charAt(n);
    	
    	return sum % multiPortWindowSize();
    }
    
    public int multiLivePort()
    {
    	log.info("multiUsernameKey: " + multiUsernameKey());
    	
    	final int			portOffset = multiHash(multiUsernameKey());
    	final int			basePort = port;
    	final String[]		bindData = new String[multiPortWindowSize()];
    	final Thread[]		bindThread = new Thread[multiPortWindowSize()];
    	
    	for ( int bindAttemptOuter = 0 ; bindAttemptOuter < multiPortWindowSize() ; bindAttemptOuter++ )
    	{
    		final int bindAttempt = bindAttemptOuter;
    		
    		bindThread[bindAttempt] = new Thread(new Runnable() {
				
				public void run() {

					URL					url = null;
					int					port = -1;
					try
					{
		        		port = basePort + ((portOffset + bindAttempt) % multiPortWindowSize());
						url = new URL("http://localhost:" + port + "/user?a=b");
			    		//log.info("url: " + url);
					}
		    		catch (MalformedURLException e)
		    		{
		    			// this is a programmer error
		    			e.printStackTrace();
		    			return;
		    		}
					
		    		try
		    		{
			    		
			    		URLConnection 		conn = url.openConnection();
			    		conn.setConnectTimeout(multiLivePortTimeout());
			    		conn.setReadTimeout(multiLivePortTimeout());
			    		InputStream			is = conn.getInputStream();
			    		String			content = (new BufferedReader(new InputStreamReader(is))).readLine();
			    		log.info("port: " + port + " content: " + content);
			    		
			    		if ( content != null )
			    			bindData[bindAttempt] = "" + port + "," + content;
		    		}
		    		catch (IOException e)
		    		{
		    			// this is probably a port on which no one is listening. or there is a timeout
		    			log.debug("url: " + url + ", exception: " + e.getMessage());	
		    		}
				}
			});
    		bindThread[bindAttempt].start();
    	}

    	// wait for threads to complete
    	for ( int bindAttempt = 0 ; bindAttempt < multiPortWindowSize() ; bindAttempt++ )
    	{
    		try {
    			bindThread[bindAttempt].join();
    		} catch (InterruptedException e) {}
    	}

    	// scan for a result
    	for ( int bindAttempt = 0 ; bindAttempt < multiPortWindowSize() ; bindAttempt++ )
    		if ( bindData[bindAttempt] != null )
    		{
    			log.info("bindData[" + bindAttempt + "]: " +  bindData[bindAttempt]);
    			
    			String[]		toks = bindData[bindAttempt].split(",");
    			if ( toks.length == 2 && toks[1].equals(multiUsernameKey()) )
    			{
    				int		foundPort = Integer.parseInt(toks[0]);
    				
    				log.info("found port: " + foundPort);
    				
    				return foundPort;
    			}
    		}
    	
    	// if here, not found
    	return -1;
    }


	public static CommandListener getListener() {
		
		if ( listener == null )
		{
            // Command listener thread
            final PreferenceManager preferenceManager = PreferenceManager.getInstance();
            
            int port = preferenceManager.getAsInt(PreferenceManager.PORT_NUMBER);
            int portMultiRange = preferenceManager.getAsInt(PreferenceManager.PORT_MULTI_RANGE);
            
            listener = new CommandListener(port, portMultiRange);
		}
		
		return listener;
	}
	
	public static void reportActivity()
	{
		if ( enableReportActivity )
			lastActivityReport = System.currentTimeMillis();
	}

}
