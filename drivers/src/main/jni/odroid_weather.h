#include <android/log.h>

#define port0 "/dev/ttyUSB0"
#define port1 "/dev/ttyUSB1"
#define port2 "/dev/ttyUSB2"
#define baudrate B500000
#define BUFF_SIZE 32
#define LOG_TAG "libodroid_weather"

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

char readBuf[BUFF_SIZE];
double temperature1, temperature2, altitude, humidity, uv, pressure;
int visible, ir;


