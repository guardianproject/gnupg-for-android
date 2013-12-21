/*
 * Copyright (C) 2011 Senecaso
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openintents.openpgp.keyserver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import android.text.Html;

public class HkpKeyServer extends KeyServer {
    private static class HttpError extends Exception {
        private static final long serialVersionUID = 1718783705229428893L;
        private int mCode;
        private String mData;

        public HttpError(int code, String data) {
            super("" + code + ": " + data);
            mCode = code;
            mData = data;
        }

        public int getCode() {
            return mCode;
        }

        public String getData() {
            return mData;
        }
    }

    private String mHost;
    private short mPort = 11371;

    // example:
    // pub 2048R/<a
    // href="/pks/lookup?op=get&search=0x887DF4BE9F5C9090">9F5C9090</a>
    // 2009-08-17 <a
    // href="/pks/lookup?op=vindex&search=0x887DF4BE9F5C9090">Jörg Runge
    // &lt;joerg@joergrunge.de&gt;</a>
    public static Pattern PUB_KEY_LINE = Pattern
            .compile(
                    "pub +([0-9]+)([a-z]+)/.*?0x([0-9a-z]+).*? +([0-9-]+) +(.+)[\n\r]+((?:    +.+[\n\r]+)*)",
                    Pattern.CASE_INSENSITIVE);
    public static Pattern USER_ID_LINE = Pattern.compile("^   +(.+)$", Pattern.MULTILINE
            | Pattern.CASE_INSENSITIVE);

    public HkpKeyServer(String host) {
        mHost = host;
    }

    public HkpKeyServer(String host, short port) {
        mHost = host;
        mPort = port;
    }

    static private String readAll(InputStream in, String encoding) throws IOException {
        ByteArrayOutputStream raw = new ByteArrayOutputStream();

        byte buffer[] = new byte[1 << 16];
        int n = 0;
        while ((n = in.read(buffer)) != -1) {
            raw.write(buffer, 0, n);
        }

        if (encoding == null) {
            encoding = "utf8";
        }
        return raw.toString(encoding);
    }

    private String submitQuery(String request) throws QueryException, HttpError {
        InetAddress ips[];
        try {
            ips = InetAddress.getAllByName(mHost);
        } catch (UnknownHostException e) {
            throw new QueryException(e.toString());
        }
        for (int i = 0; i < ips.length; ++i) {
            try {
                String url = "http://" + ips[i].getHostAddress() + ":" + mPort + request;
                URL realUrl = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) realUrl.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(25000);
                conn.connect();
                int response = conn.getResponseCode();
                if (response >= 200 && response < 300) {
                    return readAll(conn.getInputStream(), conn.getContentEncoding());
                } else {
                    String data = readAll(conn.getErrorStream(), conn.getContentEncoding());
                    throw new HttpError(response, data);
                }
            } catch (MalformedURLException e) {
                // nothing to do, try next IP
            } catch (IOException e) {
                // nothing to do, try next IP
            }
        }

        throw new QueryException("querying server(s) for '" + mHost + "' failed");
    }

    @Override
    public ArrayList<KeyInfo> search(String searchString) throws QueryException, TooManyResponses,
            InsufficientQuery {
        ArrayList<KeyInfo> results = new ArrayList<KeyInfo>();

        if (searchString.length() < 3) {
            throw new InsufficientQuery();
        }

        String encodedQuery;
        try {
            encodedQuery = URLEncoder.encode(searchString, "utf8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
        final String request = "/pks/lookup?op=index&search=" + encodedQuery;

        String data = null;
        try {
            data = submitQuery(request);
        } catch (HttpError e) {
            if (e.getCode() == 404) {
                return results;
            } else {
                if (e.getData().toLowerCase(Locale.ENGLISH).contains("no keys found")) {
                    return results;
                } else if (e.getData().toLowerCase(Locale.ENGLISH).contains("too many")) {
                    throw new TooManyResponses();
                } else if (e.getData().toLowerCase(Locale.ENGLISH).contains("insufficient")) {
                    throw new InsufficientQuery();
                }
            }
            throw new QueryException("querying server(s) for '" + mHost + "' failed");
        }
        Matcher matcher = PUB_KEY_LINE.matcher(data);
        while (matcher.find()) {
            KeyInfo info = new KeyInfo();
            info.size = Integer.parseInt(matcher.group(1));
            info.algorithm = matcher.group(2);
            info.setFingerprint(matcher.group(3));
            String chunks[] = matcher.group(4).split("-");
            info.creationDate = new GregorianCalendar(Integer.parseInt(chunks[0]),
                    Integer.parseInt(chunks[1]), Integer.parseInt(chunks[2])).getTime();
            info.userIds = new ArrayList<String>();
            if (matcher.group(5).startsWith("*** KEY REVOKED ***")) {
                info.isRevoked = true;
            } else {
                info.isRevoked = false;
                String tmp = matcher.group(5).replaceAll("<.*?>", "");
                tmp = Html.fromHtml(tmp).toString();
                info.userIds.add(tmp);
            }
            if (matcher.group(6).length() > 0) {
                Matcher matcher2 = USER_ID_LINE.matcher(matcher.group(6));
                while (matcher2.find()) {
                    String tmp = matcher2.group(1).replaceAll("<.*?>", "");
                    tmp = Html.fromHtml(tmp).toString();
                    info.userIds.add(tmp);
                }
            }
            results.add(info);
        }

        return results;
    }

    @Override
    public String get(long keyId) throws QueryException {
        HttpClient client = new DefaultHttpClient();
        try {
            HttpGet get = new HttpGet("http://" + mHost + ":" + mPort
                    + "/pks/lookup?op=get&search=0x" + KeyInfo.hexFromKeyId(keyId));

            HttpResponse response = client.execute(get);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new QueryException("not found");
            }

            HttpEntity entity = response.getEntity();
            InputStream is = entity.getContent();
            String data = readAll(is, EntityUtils.getContentCharSet(entity));
            Matcher matcher = PGP_PUBLIC_KEY.matcher(data);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (IOException e) {
            e.printStackTrace();
            // nothing to do, better luck on the next keyserver
        } finally {
            client.getConnectionManager().shutdown();
        }

        return null;
    }

    @Override
    public void add(String armoredText) throws AddKeyException {
        HttpClient client = new DefaultHttpClient();
        try {
            HttpPost post = new HttpPost("http://" + mHost + ":" + mPort + "/pks/add");

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("keytext", armoredText));
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            HttpResponse response = client.execute(post);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new AddKeyException();
            }
        } catch (IOException e) {
            e.printStackTrace();
            // nothing to do, better luck on the next keyserver
        } finally {
            client.getConnectionManager().shutdown();
        }
    }
}
