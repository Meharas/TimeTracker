package timetracker.client;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.SchemePortResolver;
import org.apache.http.conn.UnsupportedSchemeException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import timetracker.ServicePath;
import timetracker.TimeTracker;
import timetracker.TimeTrackerConstants;
import timetracker.log.Log;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;

/**
 * Http-Client f?r die Anfragen an Youtrack
 */
public class Client
{
    private static String token;
    private static String userId;
    private static String host;
    private static String scheme;
    private static int port;
    private static HttpClient httpClient;

    private static final Matcher USER_ID_MATCHER = TimeTrackerConstants.USER_ID_PATTERN.matcher(TimeTrackerConstants.STRING_EMPTY);

    private Client()
    {
    }

    /**
     * Setzt die UserId entweder aus den Properties, oder wenn noch nicht vorhanden, vom Youtrack
     * @param properties Properties
     * @throws URISyntaxException Wenn das Parsen als URI-Referenz schief ging
     * @throws IOException I/O Error
     */
    public static void setUserID(final Properties properties) throws URISyntaxException, IOException
    {
        Client.userId = properties != null ? properties.getProperty(TimeTrackerConstants.YOUTRACK_USERID) : null;
        if (Client.userId != null && !Client.userId.isEmpty())
        {
            USER_ID_MATCHER.reset(Client.userId);
            if(USER_ID_MATCHER.matches())
            {
                return;
            }
        }
        requestUserID();
    }

    /**
     * Ermittelt die Nutzer-ID
     * @throws URISyntaxException Wenn das Parsen als URI-Referenz schief ging
     * @throws IOException I/O Error
     */
    private static void requestUserID() throws IOException, URISyntaxException
    {
        final URIBuilder builder = getUserURIBuilder();
        final HttpResponse response = execute(builder);
        if (response == null)
        {
            return;
        }
        final String userId = getID(response, TimeTrackerConstants.YOUTRACK_USERID);
        setUserID(userId);
    }

    /**
     * Liefert die interne ID eines Issues. F?r manche Operationen, z.b. Commands, kann nicht mit dem Issue gearbeitet werden
     * @param ticket Issue
     * @return Interne ID des Issues
     * @throws URISyntaxException Wenn das Parsen als URI-Referenz schief ging
     * @throws IOException Verbindungsproblem
     */
    public static String getIssueID(final String ticket) throws URISyntaxException, IOException
    {
        final URIBuilder getIssueBuilder = Client.getURIBuilder(ServicePath.ISSUE, ticket);
        final HttpGet request = new HttpGet(getIssueBuilder.build());
        final HttpResponse response = Client.executeRequest(request);
        if (response == null)
        {
            return null;
        }
        return Client.getID(response, null);
    }

    public static String getValueFromJson(final String ticket, final String fields, final String attribute) throws IOException, URISyntaxException
    {
        final URIBuilder builder = getURIBuilder(ServicePath.ISSUE, ticket, new BasicNameValuePair(TimeTrackerConstants.FIELDS, fields));
        final HttpResponse response = execute(builder);
        if (response == null)
        {
            return null;
        }
        final StatusLine statusLine = response.getStatusLine();
        if(statusLine.getStatusCode() != HttpStatus.SC_OK)
        {
            return null;
        }
        final boolean isCustomField = TimeTrackerConstants.ISSUE_CUSTOM_FIELDS.equalsIgnoreCase(fields);
        return getValueFromJson(response, attribute, isCustomField);
    }

    /**
     * Liefert den Wert eines "id"-Feldes aus dem Response
     * @param response Response
     * @param saveWithKey Schl?ssel, unter welchem die ermittelte ID abgespeichert werden soll
     * @return ID als String
     * @throws IOException I/O Error beim Schreiben in den Outputstream
     */
    public static String getID(final HttpResponse response, final String saveWithKey) throws IOException
    {
        final String value = getValueFromJson(response, "id", false);
        if(value != null && saveWithKey != null)
        {
            TimeTracker.saveSetting(value, saveWithKey);
        }
        return value;
    }

    private static HttpResponse execute(final URIBuilder builder) throws IOException, URISyntaxException
    {
        final HttpUriRequest request = new HttpGet(builder.build());
        return executeRequest(request);
    }

    /**
     * Liefert einen URIBuilder zur Ermittlung der User-ID
     * @return URIBuilder
     */
    private static URIBuilder getUserURIBuilder()
    {
        return getURIBuilder(ServicePath.USER, null, new BasicNameValuePair("fields", "id"));
    }

    /**
     * Liefert einen URIBuilder
     * @param path Rest-Endpoint
     * @param ticket Ticket
     * @return URIBuilder
     */
    public static URIBuilder getURIBuilder(final ServicePath path, final String ticket, final NameValuePair... parameters)
    {
        final URIBuilder builder = new URIBuilder();
        if(path != ServicePath.USER && !checkUserId())
        {
            Log.severe("User id {0} does not match", getUserId());
            return builder;
        }

        builder.setScheme(getScheme());
        builder.setHost(getHost());

        if(getPort() != -1)
        {
            builder.setPort(getPort());
        }

        if (ticket == null)
        {
            builder.setPath(path.restEndPoint);
        }
        else
        {
            builder.setPath(String.format(path.restEndPoint, ticket));
        }
        if(parameters != null && parameters.length > 0)
        {
            builder.setParameters(parameters);
        }
        return builder;
    }

    private static boolean checkUserId()
    {
        final String userId = getUserId();
        if(userId != null)
        {
            USER_ID_MATCHER.reset(userId);
        }
        if(userId == null || !USER_ID_MATCHER.matches())
        {
            Log.warning("User id {0} does not match", userId);
            try
            {
                requestUserID();
            }
            catch (final IOException | URISyntaxException ex)
            {
                Log.severe(ex.getMessage(), ex);
            }
        }
        USER_ID_MATCHER.reset(userId);
        return USER_ID_MATCHER.matches();
    }


    /**
     * Liefert einen Wert aus dem Json
     * @param response Response
     * @param key Schl?ssel im Json, dessen Wert ermittelt werden soll
     * @return Wert aus dem Json
     * @throws IOException I/O Error
     */
    private static String getValueFromJson(final HttpResponse response, final String key, final boolean isCustomField) throws IOException
    {
        String result = null;
        final StatusLine statusLine = response.getStatusLine();
        final int status = statusLine.getStatusCode();
        if (status == HttpStatus.SC_OK)
        {
            JsonParser parser = null;
            try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
            {
                response.getEntity().writeTo(outputStream);
                outputStream.flush();

                final String msg = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
                Log.info(msg);

                final JsonFactory jsonFactory = new JsonFactory();
                parser = jsonFactory.createParser(msg);
                result = getValueFromParser(parser, key, isCustomField);
            }
            finally
            {
                if (parser != null)
                {
                    parser.close();
                }
            }
        }
        return result;
    }

    /**
     * Liefert den Wert zum ?bergebenen Schl?ssel
     * @param parser JsonParser
     * @param key Schl?ssel
     * @param isCustomField <code>true</code>, wenn es sich um ein CustomField handelt. Dann steht der Wert woanders. Sonst <code>false</code>
     * @return Wert zum ?bergebenen Schl?ssel
     * @throws IOException I/O Error
     */
    private static String getValueFromParser(final JsonParser parser, final String key, final boolean isCustomField) throws IOException
    {
        boolean start = false;
        while ((parser.nextToken()) != null)
        {
            if (start)
            {
                final String name = parser.getCurrentName();
                if(!"name".equalsIgnoreCase(name))
                {
                    continue;
                }
                return parser.nextTextValue();
            }
            final String name = parser.getCurrentName();
            if (key.equalsIgnoreCase(name))
            {
                return parser.nextTextValue();
            }
            else if(isCustomField && key.equalsIgnoreCase(parser.getText()))
            {
                start = true;
            }
        }
        return null;
    }

    public static void setClient(final HttpClient httpClient)
    {
        Client.httpClient = httpClient;
    }

    /**
     * Setzt den Request ab
     *
     * @param request Request
     * @return Response
     * @throws IOException in case of a problem or the connection was aborted
     */
    public static HttpResponse executeRequest(final HttpUriRequest request) throws IOException
    {
        initHttpClient();
        return httpClient.execute(request);
    }

    private static void initHttpClient() throws IOException
    {
        if(httpClient != null)
        {
            return;
        }

        final SchemePortResolver portResolver = new DefaultSchemePortResolver();
        final String scheme = getScheme();
        try
        {
            portResolver.resolve(new HttpHost(getHost(), getPort(), scheme));
        }
        catch (final UnsupportedSchemeException e)
        {
            Log.severe(e.getMessage(), e);
            return;
        }

        final RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.create();
        if("https".equalsIgnoreCase(scheme))
        {
            final SSLContext sslContext = createSSLContext();
            registryBuilder.register(scheme, new SSLConnectionSocketFactory(sslContext));
        }
        else
        {
            registryBuilder.register(scheme, PlainConnectionSocketFactory.getSocketFactory());
        }

        final RequestConfig requestConfig = RequestConfig.custom()
                .setExpectContinueEnabled(true)
                .setRedirectsEnabled(false)
                .build();

        final ConnectionConfig.Builder builder = ConnectionConfig.custom()
                .setBufferSize(8192)
                .setCharset(StandardCharsets.UTF_8);

        final PoolingHttpClientConnectionManager conman = new PoolingHttpClientConnectionManager(registryBuilder.build());
        conman.setDefaultMaxPerRoute(48);
        conman.setMaxTotal(256);

        final HttpClientBuilder httpClient = HttpClientBuilder.create()
                .setConnectionManager(conman)
                .setDefaultConnectionConfig(builder.build())
                .setDefaultRequestConfig(requestConfig)
                .setSchemePortResolver(portResolver)
                .setDefaultHeaders(Arrays.asList(new BasicHeader(HttpHeaders.AUTHORIZATION, "Bearer " + Client.getToken()),
                                                 new BasicHeader(HttpHeaders.ACCEPT, TimeTrackerConstants.MIMETYPE_JSON),
                                                 new BasicHeader(HttpHeaders.CONTENT_TYPE, TimeTrackerConstants.MIMETYPE_JSON),
                                                 new BasicHeader(HttpHeaders.CACHE_CONTROL, TimeTrackerConstants.NO_CACHE)));
        setClient(httpClient.build());
    }

    private static SSLContext createSSLContext() throws IOException
    {
        final String certFileName = Optional.ofNullable(TimeTracker.getProperties()).map(props -> props.getProperty(TimeTrackerConstants.YOUTRACK_CERT)).orElse(null);
        if(certFileName == null || certFileName.isEmpty())
        {
            final String msg = String.format("Value of property %s missing.", TimeTrackerConstants.YOUTRACK_CERT);
            JOptionPane.showMessageDialog(TimeTracker.getTimeTracker(), msg);
            throw new IOException(msg);
        }

        final File certificate = new File(TimeTracker.getHome() + "security\\" + certFileName);
        if(!certificate.exists())
        {
            final String msg = "Certificate file missing.";
            JOptionPane.showMessageDialog(TimeTracker.getTimeTracker(), msg);
            throw new IOException(msg);
        }

        final SSLContext sslContext;
        try
        {
            final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            final KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null);

            try(final FileInputStream inputStream = new FileInputStream(certificate))
            {
                final X509Certificate cert = (X509Certificate) certificateFactory.generateCertificate(inputStream);
                final String alias = cert.getSubjectX500Principal().getName();
                trustStore.setCertificateEntry(alias, cert);
            }

            final TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
            tmf.init(trustStore);

            final TrustManager[] trustManagers = tmf.getTrustManagers();
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, null);
        }
        catch (final IOException e)
        {
            throw e;
        }
        catch (final Exception e)
        {
            final String msg = TimeTracker.getMessage(e);
            System.err.println(msg);
            JOptionPane.showMessageDialog(TimeTracker.getTimeTracker(), msg);
            throw new IOException(msg, e.getCause());
        }
        return sslContext;
    }

    /**
     * Liefert den URI-Builder f?r Commands
     * @return URIBuilder
     */
    public static URIBuilder getCommandURIBuilder()
    {
        return getURIBuilder(ServicePath.COMMAND, null);
    }

    /**
     * Loggt einen Response aus
     * @param response Response
     * @throws IOException I/O Error
     */
    public static void logResponse(final HttpResponse response) throws IOException
    {
        final StatusLine statusLine = response.getStatusLine();
        final int status = statusLine.getStatusCode();

        try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
        {
            response.getEntity().writeTo(outputStream);
            outputStream.flush();

            final String msg = outputStream.toString();
            if (status == HttpStatus.SC_OK)
            {
                Log.info(msg);
                Log.info("Success.");
            }
            else
            {
                Log.warning(msg);
            }
        }
    }

    public static String getTicketSummary(final String text)
    {
        try
        {
            final String summary = getIssueSummary(text);
            if(summary != null)
            {
                return text + TimeTrackerConstants.STRING_SPACE + summary;
            }
        }
        catch (final URISyntaxException | IOException ex)
        {
            Log.severe(ex.getMessage(), ex);
        }
        return text;
    }

    /**
     * Ermittelt die Beschreibung zu einem Ticket
     * @param text Ticket
     * @return die Beschreibung zu einem Ticket
     * @throws URISyntaxException Invalide URL
     * @throws IOException I/O Error
     */
    private static String getIssueSummary(String text) throws URISyntaxException, IOException
    {
        if (!TimeTracker.matches(text))
        {
            return null;
        }
        final String ticket = TimeTracker.MATCHER.group(1);
        text = text.replace(ticket, TimeTrackerConstants.STRING_EMPTY);
        if(!text.trim().isEmpty())
        {
            //Sollte der Nutzer was eigenes hingeschrieben haben, so sollte das nicht ersetzt werden
            return null;
        }
        return getSummaryFromJson(ticket);
    }

    /**
     * Liefert den Ticket-Status
     * @param text Ticket, ggf. mit Beschreibung
     * @return Ticket-Status
     * @throws URISyntaxException Invalide URL
     * @throws IOException I/O Error
     */
    public static String getIssueState(final String text) throws URISyntaxException, IOException
    {
        if (!TimeTracker.matches(text))
        {
            return null;
        }
        return Client.getValueFromJson(TimeTracker.MATCHER.group(1), TimeTrackerConstants.ISSUE_CUSTOM_FIELDS, TimeTrackerConstants.ISSUE_STATE);
    }

    private static String getSummaryFromJson(final String ticket) throws IOException, URISyntaxException
    {
        return Client.getValueFromJson(ticket, TimeTrackerConstants.ISSUE_SUMMARY, TimeTrackerConstants.ISSUE_SUMMARY);
    }

    public static String getToken()
    {
        return Client.token;
    }

    public static boolean hasToken()
    {
        return Client.token != null && !Client.token.isEmpty();
    }

    public static void setToken(final String token)
    {
        Client.token = token;
    }

    public static String getUserId()
    {
        return Client.userId;
    }

    public static void setUserID(final String userId)
    {
        Client.userId = userId;
    }

    public static String getHost()
    {
        return Client.host;
    }

    public static void setHost(final String host)
    {
        Client.host = host;
    }

    public static String getScheme()
    {
        return scheme;
    }

    public static void setScheme(final String scheme)
    {
        Client.scheme = scheme;
    }

    public static int getPort()
    {
        return Client.port;
    }

    public static void setPort(final int port)
    {
        Client.port = port;
    }
}