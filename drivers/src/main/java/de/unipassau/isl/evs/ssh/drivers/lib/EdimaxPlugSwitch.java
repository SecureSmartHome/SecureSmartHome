/*
 * MIT License
 *
 * Copyright (c) 2016.
 * Bucher Andreas, Fink Simon Dominik, Fraedrich Christoph, Popp Wolfgang,
 * Sell Leon, Werli Philemon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.AbstractComponent;
import de.unipassau.isl.evs.ssh.core.schedule.ExecutionServiceComponent;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

/**
 * Class to control EDIMAX WiFi Smart Plug Switch
 *
 * @author Wolfram Gottschlich
 * @author Niko Fink
 * @author Wolfgang Popp
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
     * @see #setOn(boolean)
     */
    public Future<Boolean> setOnAsync(final boolean on) {
        final Future<Boolean> future = requireComponent(ExecutionServiceComponent.KEY).submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return setOn(on);
            }
        });
        if (CoreConstants.TRACK_STATISTICS) {
            future.addListener(new TimingListener());
        }
        return future;
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
     * @see #isOn()
     */
    public Future<Boolean> isOnAsync() {
        final Future<Boolean> future = requireComponent(ExecutionServiceComponent.KEY).submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return isOn();
            }
        });
        if (CoreConstants.TRACK_STATISTICS) {
            future.addListener(new TimingListener());
        }
        return future;
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
                connection.disconnect();
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
        } else {
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

    private static class TimingListener implements FutureListener<Boolean> {
        private final long started = System.nanoTime();

        @Override
        public void operationComplete(Future<Boolean> future) throws Exception {
            Log.v(TAG, "Network operation complete after " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) + "ms");
        }
    }
}
