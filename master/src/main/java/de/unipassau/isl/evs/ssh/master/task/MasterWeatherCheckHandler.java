package de.unipassau.isl.evs.ssh.master.task;

import android.net.http.AndroidHttpClient;
import de.unipassau.isl.evs.ssh.core.handler.MessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.IncomingDispatcher;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import org.apache.http.client.HttpClient;
import org.apache.http.protocol.HTTP;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Task/Handler that periodically checks the records of weather data provider and issues notifications based on a configured set of rules.
 */
public class MasterWeatherCheckHandler implements Task, MessageHandler {

    @Override
    public void run() {
        //TODO implement
    }

    @Override
    public void handle(Message.AddressedMessage message) {

    }

    @Override
    public void handlerAdded(IncomingDispatcher dispatcher, String routingKey) {

    }

    @Override
    public void handlerRemoved(String routingKey) {

    }
}