package de.unipassau.isl.evs.ssh.drivers.lib;


import android.util.Base64;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;

/**
 * Class to control EDIMAX WiFi Smart Plug Switch
 *
 * @author Wolfram Gottschlich
 * @author Niko Fink
 * @version 2.0
 */
public class EdimaxPlugSwitch extends AbstractComponent {
    private static final String TAG = EdimaxPlugSwitch.class.getSimpleName();
    private static final String XML_SET = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
            "<SMARTPLUG id=\"edimax\">" +
            "<CMD id=\"setup\">" +
            "<Device.System.Power.State>" +
            "%s" +
            "</Device.System.Power.State>" +
            "</CMD>" +
            "</SMARTPLUG>";
    private static final String XML_GET = "<?xml version=\"1.0\" encoding=\"UTF8\"?>" +
            "<SMARTPLUG id=\"edimax\">" +
            "<CMD id=\"get\">" +
            "<Device.System.Power.State>" +
            "</Device.System.Power.State>" +
            "</CMD>" +
            "</SMARTPLUG>";

    private final String url;
    private final String user;
    private final String password;

    /**
     * Constructor
     *
     * @param address  IP address of the EDIMAX Smart Plug
     * @param port     Port
     * @param user     user-name of to access the EDIMAX Smart Plug
     * @param password password to access the EDIMAX Smart Plug
     */
    public EdimaxPlugSwitch(String address, int port, String user, String password) {
        this.user = user;
        this.password = password;
        url = "http://" + address + ":" + port + "/smartplug.cgi";
    }

    /**
     * Constructor that uses default port, user and password
     *
     * @param address IP address of the EDIMAX Smart Plug
     */
    public EdimaxPlugSwitch(String address) {
        this(address, 10000, "admin", "1234");
    }

    /**
     * Switches the Smart Plug on
     *
     * @return {@code true} if the set operation was successful
     * @throws IOException
     */
    public boolean setOn(boolean on) throws IOException {
        String command = String.format(XML_SET, on ? "ON" : "OFF");
        String response = executePost(url, command);
        Log.d(TAG, "Response: " + response);
        return parseResponseSet(response);
    }

    /**
     * Checks the current status of the Smart Plug
     *
     * @return true if the Smart Plug is On
     * @throws IOException
     */
    public boolean isOn() throws IOException {
        final String response = executePost(url, XML_GET);
        Log.d(TAG, "Response: " + response);
        return parseResponseGet(response);
    }

    /**
     * Executes post requests
     *
     * @param targetURL     The URL for the request
     * @param urlParameters The body for the post request
     * @return Response of the request
     * @throws IOException
     */
    private String executePost(String targetURL, String urlParameters) throws IOException {
        HttpURLConnection connection = null;
        try {
            //Create connection
            URL url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/xml");
            connection.setConnectTimeout(2000);
            String userpass = user + ":" + password;
            String basicAuth = "Basic " + Base64.encodeToString(userpass.getBytes(), Base64.NO_WRAP);
            connection.setRequestProperty("Authorization", basicAuth);
            connection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
            connection.setUseCaches(false);
            connection.setDoOutput(true);

            //Send request
            try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                wr.writeBytes(urlParameters);
            }

            //Get Response
            StringBuilder response = new StringBuilder();
            try (BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = rd.readLine()) != null) {
                    response.append(line);
                    response.append('\n');
                }
            }
            return response.toString();
        } finally {
            if (connection != null) {
                connection.disconnect(); //TODO Niko: check if keeping the connection open leads to a speedup (Wolfi, 2016-01-06)
            }
        }
    }

    private boolean parseResponseSet(String response) throws IOException {
        // Parse the response String
        final Document document = getDocument(response);

        // Extract response status
        Element setup = document.getElementById("setup");
        if (setup != null) {
            String status = setup.getTextContent();
            Log.d(TAG, "Status of set: " + status);
            return status.toLowerCase().equals("ok");
        }
        else {
            throw new IOException("Response missing id 'setup'");
        }
    }

    private boolean parseResponseGet(String response) throws IOException {
        // Parse the response String
        final Document document = getDocument(response);

        // Extract the status
        final NodeList elements = document.getElementsByTagName("Device.System.Power.State");
        if (elements.getLength() > 0) {
            final String state = elements.item(0).getTextContent();
            Log.d(TAG, "XML document: " + state);
            return state != null && state.toLowerCase().equals("on");
        } else {
            throw new IOException("Response missing Element with tag 'Device.System.Power.State'");
        }
    }

    private Document getDocument(String response) throws IOException {
        try {
            final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(response)));
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Could not parse response", e);
        }
    }
}
