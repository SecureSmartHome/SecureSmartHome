package de.unipassau.isl.evs.ssh.core.messaging.payload;

/**
 * Payload class for weather warnings
 *
 * @author bucher
 */
public class WeatherPayload {

    boolean warning; //should user be warned?
    String warnText; //what type of warning is it?

    public WeatherPayload (boolean warning, String warnText){
        this.warning = warning;
        this.warnText = warnText;
    }

    /**
     * Returns a boolean if there is a warning for the user from the Weather Service.
     *
     * @return boolean warning
     */
    public boolean getWarning (){ return warning; }

    /**
     * The String contains the message from the Weather Service. So the message to the user also
     * contains information about why the user should close his window.
     *
     * @return String warnText
     */
    public String getWarnText () { return warnText; }

}
