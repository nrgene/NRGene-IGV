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

package org.broad.igv.ui;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.broad.igv.batch.CommandListener;
import org.broad.igv.ui.util.EncryptionUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class exists solely to provide backward compatibility.
 *
 * @author jrobinso
 * @date Apr 9, 2011
 */
public class IGVMainFrame {

  private static Logger log = Logger.getLogger(IGVMainFrame.class);

  static private final String JAVA_PROP_JNLP_ARGS = "jnlp.args";
 // static private final String SYS_ENV_APPSTREAM_ARGS = "AppStream_Session_Context";
  static private final String SYS_ENV_APPSTREAM_ARGS = "APPSTREAM_SESSION_CONTEXT";
  static private final String SYS_ENV_USER_DEFINED_GENOMES_TEMPLATE = "Nrgene_UserDefinedGenomes_Template";
  static private final String SYS_ENV_MERGE = "Nrgene_Merge";
  static private final String SYS_ENV_REUSE = "Nrgene_Reuse";
  static private final String SYS_ENV_AUTHTOKEN = "Nrgene_AuthToken";

  private static final String XML_SESSION_URL_JSON_FIELD = "xmlSessionUrl";
  private static final String AUTH_TOKEN_JSON_FIELD = "authToken";
  private static final String REUSE_JSON_FIELD = "reuse";
  private static final String MERGE_JSON_FIELD = "merge";
  private static final String LOCUS_JSON_FIELD = "locus";
  private static final String SESSION_FILE_URL_FIELD = "sessionFileUrl";
  private static final String ENCRYPTION_KEY_FIELD = "encryptionKey";

  private static boolean appStreamMaximized;
  private static String authToken;
  private static EncryptionUtils encryptionUtils = new EncryptionUtils();

  public static void main(String[] args) {
    dumpSystemProperties();
    dumpSystemEnvironment();

    String sessionContext = null;  // SessionContext comes either from jnlp or from app-stream
    if (System.getProperty(JAVA_PROP_JNLP_ARGS) != null) {
      sessionContext = System.getProperty(JAVA_PROP_JNLP_ARGS);
    } else if (System.getenv().containsKey(SYS_ENV_APPSTREAM_ARGS)) {
      sessionContext = System.getenv(SYS_ENV_APPSTREAM_ARGS);
      appStreamMaximized = true;
    }

    if (StringUtils.isEmpty(sessionContext)) {
      Main.main(args);
      return;
    }

    authToken = System.getenv(SYS_ENV_AUTHTOKEN);  // Default
    boolean merge = Boolean.parseBoolean(System.getenv(SYS_ENV_MERGE));  // Default
    boolean reuse = Boolean.parseBoolean(System.getenv(SYS_ENV_REUSE));  // Default
    String locus = "";  // Default
    String igvInputFile;

    // First we need to decide if we're parsing a json sessionContext or a comma-separated predefined-position
    // sessionContext. We decide which it is according to the first character in the string.
    log.info("Session context is: '" + sessionContext + "'");
    if (sessionContext.startsWith("{")) {
      log.info("Session context is json");
      try {
        JSONObject sessionContextJson = new JSONObject(sessionContext);
        URL sessionJsonFileUrl = new URL(sessionContextJson.getString(SESSION_FILE_URL_FIELD));
        String encryptionKeyBase64 = sessionContextJson.getString(ENCRYPTION_KEY_FIELD);
        log.info("Session json file url: " + sessionJsonFileUrl);
        EncryptionUtils.Key encryptionKey = EncryptionUtils.Key.getFromBase64String(encryptionKeyBase64);
        byte[] encryptedSessionJsonData = IOUtils.toByteArray(sessionJsonFileUrl.openStream());
        byte[] decryptedData = encryptionUtils.decrypt(encryptedSessionJsonData, encryptionKey);
        JSONObject sessionJson = new JSONObject(new String(decryptedData, Charset.forName("UTF-8")));
        igvInputFile = sessionJson.getString(XML_SESSION_URL_JSON_FIELD);  // Mandatory parameter
        if (sessionJson.has(AUTH_TOKEN_JSON_FIELD)) {
          authToken = sessionJson.getString(AUTH_TOKEN_JSON_FIELD);
        }
        if (sessionJson.has(REUSE_JSON_FIELD)) {
          reuse = sessionJson.getBoolean(REUSE_JSON_FIELD);
        }
        if (sessionJson.has(MERGE_JSON_FIELD)) {
          merge = sessionJson.getBoolean(MERGE_JSON_FIELD);
        }
        if (sessionJson.has(LOCUS_JSON_FIELD) && !sessionJson.isNull(LOCUS_JSON_FIELD)) {
          locus = sessionJson.getString(LOCUS_JSON_FIELD);
        }
      } catch (JSONException e) {
        log.error("Invalid json while reading session context", e);
        throw new RuntimeException(e);
      } catch (IOException e) {
        log.error("Cannot download file reading session context", e);
        throw new RuntimeException(e);
      }
    } else {
      log.info("Session context is not json");

      // Should be comma-separated
      // First argument is expected to be the xml url
      // Second argument, if exists, is the auth token
      final String[] sessionArgs = sessionContext.split(",");

      // the first argument is the session file
      igvInputFile = sessionArgs[0];

      if (igvInputFile.matches(".*(\\?|\\&)merge=1($|\\&.*)")) {
        merge = true;
      }
      if (igvInputFile.matches(".*(\\?|\\&)reuse=1($|\\&.*)")) {
        reuse = true;
      }

      // extract locus
      Matcher m = Pattern.compile("(\\?|\\&)locus=(.*?)($|\\&.*)").matcher(igvInputFile);
      if (m.find()) {
        locus = m.group(2);
      }
      if (sessionArgs.length > 1) {
        authToken = sessionArgs[1];
      }
    }
    log.info(String
        .format("Done parsing session context. igvInputFile: '%s' ; merge: '%s' ; reuse: '%s' ; " +
                "locus: '%s'", igvInputFile, merge, reuse, locus));
    if (merge || reuse) {
      // communicate with live instance
      // find out if already running
      int port = CommandListener.getListener().multiLivePort();
      if (port > 0) {
        URL url = null;
        try {
          // build url
          url = new URL(String.format("http://localhost:%d/load?file=%s&merge=%s&locus=%s",
              port, URLEncoder.encode(igvInputFile, "UTF-8"), Boolean.toString(merge), locus));

          // send command, read first line from stream
          (new BufferedReader(new InputStreamReader(url.openStream()))).readLine();

          // if here, we have communicated with the live instance, we are done here
          System.exit(0);
        } catch (IOException e) {
          // if here, we failed. continue with creating a new instance
          log.warn("failed to communicate w/ instance: url: " + url + ", exception: " + e.getMessage());
        }
      }
    }

    Main.main(new String[]{igvInputFile});
  }


  public static void initializeUserDefinedGenomes(File file) {
    log.info("file: " + file.getAbsolutePath() + ", exists: " + file.exists());

    // init?
    if ((!file.exists() || file.length() == 0) &&
        !StringUtils.isEmpty(System.getenv(SYS_ENV_USER_DEFINED_GENOMES_TEMPLATE))) {
      File template = new File(System.getenv(SYS_ENV_USER_DEFINED_GENOMES_TEMPLATE));
      log.info("initializing from template: " + template.getAbsolutePath());

      FileInputStream is = null;
      FileOutputStream os = null;
      List<Closeable> closeables = new LinkedList<Closeable>();
      try {
        closeables.add(is = new FileInputStream(template));
        closeables.add(os = new FileOutputStream(file));
        IOUtils.copy(is, os);

      } catch (IOException e) {
        log.warn("failed to initialize user-defined-genomes", e);
      } finally {
        for (Closeable closeable : closeables) {
          try {
            closeable.close();
          } catch (IOException e) {
            log.warn("failed to close cloeable: " + closeable, e);
          }
        }
      }
    }
  }

  private static void dumpSystemProperties() {
    log.info("**** System Properties:");
    for (Object key : System.getProperties().keySet()) {
      log.info(key.toString() + "=" + System.getProperty(key.toString()));
    }
  }

  private static void dumpSystemEnvironment() {
    log.info("**** System Environment:");
    for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
      log.info(entry.getKey() + "=" + entry.getValue());
    }
  }

  public static boolean isAppStreamMaximized() {
    return appStreamMaximized;
  }

  public static boolean hasAuthToken() {
    return !StringUtils.isEmpty(authToken);
  }

  public static String getAuthToken() {
    return authToken;
  }
}
