#include <termios.h>
#include <string.h> //memset
#include <fcntl.h> // O_RDWR, etc.
#include <stdlib.h>
#include "ssh-drivers.h"
#include "jni.h"

struct termios ios; //parameters for IO access
int fd; // file descriptor

double atof(const char *nptr) {
    return (strtod(nptr, NULL));
}

double atof(const char *nptr, const char *eptr) {
    return (strtod(nptr, NULL));
}



extern "C" {
JNIEXPORT void JNICALL
Java_de_unipassau_isl_evs_ssh_drivers_lib_WeatherSensor_initSerialInterface(JNIEnv *env, jobject instance) {
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

JNIEXPORT void JNICALL
Java_de_unipassau_isl_evs_ssh_drivers_lib_WeatherSensor_readData(JNIEnv *env, jobject instance) {
    LOGD("readData()");

    jclass thisWeather = env->GetObjectClass(instance);

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
            case '5':
                temperature1 = atof(&readBuf[1]);
                LOGD("Found value for temperature1: %f", temperature1);
                env->SetDoubleField(instance, env->GetFieldID(thisWeather, "temp1", "D"), temperature1);
                break;
            case '1':
                pressure = atof(&readBuf[1], &readBuf[3]);
                LOGD("Found value for pressure: %f", pressure);
                env->SetDoubleField(instance, env->GetFieldID(thisWeather, "pressure", "D"), pressure);
                break;
            case '0':
                altitude = atof(&readBuf[1], &readBuf[3]);
                LOGD("Found value for altitude: %f", altitude);
                env->SetDoubleField(instance, env->GetFieldID(thisWeather, "altitude", "D"), altitude);
                break;
            case '7':
                temperature2 = atof(&readBuf[1]);
                LOGD("Found value for temperature2: %f", temperature2);
                env->SetDoubleField(instance, env->GetFieldID(thisWeather, "temp2", "D"), temperature2);
                break;
            case '6':
                humidity = atof(&readBuf[1]);
                LOGD("Found value for humidity: %f", humidity);
                env->SetDoubleField(instance, env->GetFieldID(thisWeather, "humidity", "D"), humidity);
                break;
            case '2':
                uv = atof(&readBuf[1]);
                LOGD("Found value for UV: %f", uv);
                env->SetDoubleField(instance, env->GetFieldID(thisWeather, "uv", "D"), uv);
                break;
            case '3':
                visible = atoi(&readBuf[1]);
                LOGD("Found value for visible light: %d", visible);
                env->SetIntField(instance, env->GetFieldID(thisWeather, "visible", "I"), visible);
                break;
            case '4':
                ir = atoi(&readBuf[1]);
                LOGD("Found value for infrared: %d", ir);
                env->SetIntField(instance, env->GetFieldID(thisWeather, "ir", "I"), ir);
                break;
            default:
                LOGW("Unknown code: %c", code);
        }
    }
}

JNIEXPORT void JNICALL
Java_de_unipassau_isl_evs_ssh_drivers_lib_WeatherSensor_close(JNIEnv *env, jobject instance) {
    close(fd);
}
}