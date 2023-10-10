package wikicrawl;

import org.xbill.DNS.Message;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URL;
import java.util.Base64;

/**
 * Created by alex on 27/11/2018.
 */
public class DohResolver {

    String host;

    public DohResolver(String host) {
        this.host = host;
    }

    public Message query(Message query, Integer timeout) throws IOException {
        String encodedQuery = Base64.getUrlEncoder().withoutPadding().encodeToString(query.toWire());
        Message response = makeGetRequest(encodedQuery, timeout);

        return response;
    }

    private Message makeGetRequest(String encodedQuery, Integer timeout) throws IOException {
        HttpsURLConnection con = null;
        try {
            URL myurl = new URL(host + "?dns=" + encodedQuery);
            con = (HttpsURLConnection) myurl.openConnection();
            con.setDoOutput(true);
            con.setRequestMethod("GET");
            con.setConnectTimeout(timeout);
            con.setRequestProperty("User-Agent", "Java client");
            con.setRequestProperty("content-type", "application/dns-message");

            byte[] readBytes = new byte[65535];
            con.getInputStream().read(readBytes);

            String cache_control = con.getHeaderField("cache-control");
            String maxAgeString = cache_control.replace("max-age=", "");
            Integer maxAge = new Integer(maxAgeString);

            Message response = new Message(readBytes);

            return response;
        } finally {
            con.disconnect();
        }
    }
}
