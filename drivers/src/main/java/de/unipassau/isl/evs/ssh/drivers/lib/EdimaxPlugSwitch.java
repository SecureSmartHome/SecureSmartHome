package de.unipassau.isl.evs.ssh.drivers.lib;
/**
 * Class to control EDIMAX WiFi Smart Plug Switch
 *
 * @author Wolfram Gottschlich
 * @version 1.0
 */

import android.util.Base64;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.container.Container;

public class EdimaxPlugSwitch implements Component {
    public static final Key<EdimaxPlugSwitch> KEY = new Key<>(EdimaxPlugSwitch.class);

    String URL;
    String USER;
    String PASS;

    @Override
    public void init(Container container) {
        //No need to do anything here
    }

    @Override
    public void destroy() {
        //No need to do anything here
    }

    /**
     * Constructor
     *
     * @param address IP address of the EDIMAX Smart Plug
     * @param port    Port
     * @param user    user-name of to access the EDIMAX Smart Plug
     * @param pw      password to access the EDIMAX Smart Plug
     */
    public EdimaxPlugSwitch(String address, int port, String user, String pw) {
        USER = user;
        PASS = pw;
        URL = "http://" + address + ":" + port + "/smartplug.cgi";
    }

    /**
     * Constructor that uses default port, user and password
     *
     * @param address IP address of the EDIMAX Smart Plug
     */
    public EdimaxPlugSwitch(String address) {
        USER = "admin";
        PASS = "1234";
        URL = "http://" + address + ":" + 10000 + "/smartplug.cgi";
    }


    /**
     * Switches the Smart Plug on
     *
     * @return false in case of an error
     * @throws IOException
     * @throws EvsIoException
     */
    public boolean switchOn() throws IOException, EvsIoException {
        boolean ret = false;
        String response;
        String onXML = "<?xml version=\"1.0\" encoding=\"utf-8\"?><SMARTPLUG id=\"edimax\"><CMD id=\"setup\"><Device.System.Power.State>ON</Device.System.Power.State></CMD></SMARTPLUG>";
        response = excutePost(URL, onXML);
        if (response != null) {
            ret = true;
        }
        return ret;
    }


    /**
     * Switches the Smart Plug off
     *
     * @return false in case of an error
     * @throws IOException
     * @throws EvsIoException
     */
    public boolean switchOff() throws IOException, EvsIoException {
        boolean ret = false;
        String response;
        String onXML = "<?xml version=\"1.0\" encoding=\"utf-8\"?><SMARTPLUG id=\"edimax\"><CMD id=\"setup\"><Device.System.Power.State>OFF</Device.System.Power.State></CMD></SMARTPLUG>";
        response = excutePost(URL, onXML);
        if (response != null) {
            ret = true;
        }
        return ret;
    }

    /**
     * Checks the current status of the Smart Plug
     *
     * @return true if the Smart Plug is On
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws EvsIoException
     */
    public boolean isOn() throws IOException, ParserConfigurationException, SAXException, EvsIoException {
        boolean ret = false;
        String response;
        String onXML = "<?xml version=\"1.0\" encoding=\"UTF8\"?><SMARTPLUG id=\"edimax\"><CMD id=\"get\"><Device.System.Power.State></Device.System.Power.State></CMD></SMARTPLUG>";
        response = excutePost(URL, onXML);
        if (response != null) {
            System.out.println("Response: " + response);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource isRes = new InputSource(new StringReader(response));
            Document document = builder.parse(isRes);
            System.out.println("XML result: " + document.getElementsByTagName("Device.System.Power.State").item(0).getTextContent());
            if (document != null && document.getElementsByTagName("Device.System.Power.State").getLength() > 0) {
                NodeList tempElementList = document.getElementsByTagName("Device.System.Power.State");
                if (tempElementList.item(0).getTextContent().toLowerCase().equals("on")) {
                    ret = true;
                } else {
                    ret = false;
                }

            } else {
                throw new EvsIoException("Could not parse response");
            }

            ret = true;
        }
        return ret;
    }

    /**
     * Executes post requests
     *
     * @param targetURL    The URL for the request
     * @param urlParameters The body for the post request
     * @return Response of the request
     * @throws IOException
     * @throws Exception
     */
    private String excutePost(String targetURL, String urlParameters) throws IOException {
        HttpURLConnection connection = null;
        try {
            //Create connection
            URL url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/xml");
            connection.setConnectTimeout(2000);
            String userpass = USER + ":" + PASS;
            String basicAuth = "Basic " + Base64.encodeToString(userpass.getBytes(), Base64.NO_WRAP);
            connection.setRequestProperty("Authorization", basicAuth);
            connection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            //Send request
            DataOutputStream wr = new DataOutputStream(
                    connection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.close();

            //Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder(); // or StringBuffer if not Java 5+
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();
        } catch (IOException e) {
            throw e;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
