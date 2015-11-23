#include <termios.h>
#include <stdio.h>
#include <string.h> //memset
#include <fcntl.h> // O_RDWR, etc.
#include <stdlib.h>
#include "jni.h"

struct termios ios; //parameters for IO access
int fd; // file descriptor

void initSerial() {
    LOGD("initializing serial interface");
    fd = -1;
    fd = open(port0, O_RDWR | O_NOCTTY | O_NONBLOCK);
    if (fd < 0) {
        LOGW("/dev/ttyUSB0 open fail\n");
        fd = open(port1, O_RDWR | O_NOCTTY | O_NONBLOCK);
    }
    if (fd < 0) {
        LOGW("/dev/ttyUSB1 open fail\n");
        fd = open(port2, O_RDWR | O_NOCTTY | O_NONBLOCK);
    }
    if (fd < 0) {
        LOGE("/dev/ttyUSB2 open fail...giving up!\n");
        return;
    }

    memset(&ios, 0, sizeof(ios));
    tcgetattr(fd, &ios);

    cfsetispeed(&ios, baudrate);
    cfsetospeed(&ios, baudrate);

    ios.c_cflag |= CS8;
    ios.c_iflag |= IGNBRK;
    ios.c_iflag &= ~(BRKINT | ICRNL | IMAXBEL | IXON);
    ios.c_oflag &= ~(OPOST | ONLCR);
    ios.c_lflag &= ~(ISIG | ICANON | IEXTEN | ECHO | ECHOE | ECHOK |
                     ECHOCTL | ECHOKE);
    ios.c_lflag |= NOFLSH;
    ios.c_cflag &= ~CRTSCTS;

    tcflush(fd, TCIFLUSH);
    tcsetattr(fd, TCSANOW, &ios);
    LOGD("serial interface initialized");
}

extern "C" {

void Java_de_unipassau_isl_evs_ssh_drivers_lib_Weather_initSerialInterface() {
    initSerial();
}

void Java_de_unipassau_isl_evs_ssh_drivers_lib_Weather_readData(JNIEnv *env, jobject thiz) {
    LOGD("readData()");

    jclass thisWeather = env->GetObjectClass(thiz);
    jfieldID temp1ID = env->GetFieldID(thisWeather, "temp1", "D");
    jfieldID pressureID = env->GetFieldID(thisWeather, "pressure", "D");

    int j;
    for (j = 0; j < 9; j++) {
        //LOGD("%d: reading data", j);
        readBuf[0] = '0';
        int readResult = 0;
        while (readBuf[0] != 'w') readResult = read(fd, readBuf, 1); //look for delimiter

        int i = 1, wait = 0;
        read(fd, readBuf, 1);
        char code = readBuf[0];
        while (readBuf[0] != '\e') {
            readResult = read(fd, readBuf, 1);
            if (readResult == 1) {
                readBuf[i] = readBuf[0];
                i++;
            } else if (readResult == -1) {
                wait++;
            } else
                LOGW("Unexpected read result: %d", readResult);
            //LOGD("%d: %i = %c (readResult=%d)", i, (int)readBuf[i], readBuf[i], readResult);
        }
        readBuf[i - 1] = '\0';
        LOGD("code %c: data %s (wait=%d)", code, &readBuf[1], wait);
        switch (code) {
            case '0':
                temperature1 = atof(&readBuf[1]);
                LOGD("Found value for temperature1: %f", temperature1);
                env->SetDoubleField(thiz, temp1ID, temperature1);
                break;
            case '1':
                pressure = atof(&readBuf[1]);
                LOGD("Found value for pressure: %f", pressure);
                env->SetDoubleField(thiz, pressureID, pressure);
                break;
            case '2':
                altitude = atof(&readBuf[1]);
                LOGD("Found value for altitude: %f", altitude);
                env->SetDoubleField(thiz, env->GetFieldID(thisWeather, "altitude", "D"), altitude);
                break;
            case '3':
                temperature2 = atof(&readBuf[1]);
                LOGD("Found value for temperature2: %f", temperature2);
                env->SetDoubleField(thiz, env->GetFieldID(thisWeather, "temp2", "D"), temperature2);
                break;
            case '4':
                humidity = atof(&readBuf[1]);
                LOGD("Found value for humidity: %f", humidity);
                env->SetDoubleField(thiz, env->GetFieldID(thisWeather, "humidity", "D"), humidity);
                break;
            case '5':
                uv = atof(&readBuf[1]);
                LOGD("Found value for UV: %f", uv);
                env->SetDoubleField(thiz, env->GetFieldID(thisWeather, "uv", "D"), uv);
                break;
            case '6':
                visible = atoi(&readBuf[1]);
                LOGD("Found value for visible light: %d", visible);
                env->SetIntField(thiz, env->GetFieldID(thisWeather, "visible", "I"), visible);
                break;
            case '7':
                ir = atoi(&readBuf[1]);
                LOGD("Found value for infrared: %d", ir);
                env->SetIntField(thiz, env->GetFieldID(thisWeather, "ir", "I"), ir);
                break;
            default:
                LOGW("Unknown code: %c", code);
        }
    }
}

void Java_de_unipassau_isl_evs_ssh_drivers_lib_Weather_close() {
    close(fd);
}

/*jdouble Java_de_unipassau_isl_evs_ssh_drivers_lib_Weather_getTemperature1() {
    return temperature1;
}

jdouble Java_de_unipassau_isl_evs_ssh_drivers_lib_Weather_getTemperature2() {
    return temperature2;
}

jdouble Java_de_unipassau_isl_evs_ssh_drivers_lib_Weather_getPressure() {
    return pressure;
}

jdouble Java_de_unipassau_isl_evs_ssh_drivers_lib_Weather_getAltitude() {
    return altitude;
}

jdouble Java_de_unipassau_isl_evs_ssh_drivers_lib_Weather_getHumidity() {
    return humidity;
}

jdouble Java_de_unipassau_isl_evs_ssh_drivers_lib_Weather_getUV() {
    return uv;
}

jint Java_de_unipassau_isl_evs_ssh_drivers_lib_Weather_getVisibleLight() {
    return visible;
}

jint Java_de_unipassau_isl_evs_ssh_drivers_lib_Weather_getInfrared() {
    return ir;
}*/

}
