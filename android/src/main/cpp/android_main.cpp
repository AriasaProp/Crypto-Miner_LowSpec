
// native MainActivity.java
#include <jni.h>

extern "C" JNIEXPORT jstring JNICALL Java_com_ariasaproject_cmls_MainActivity_callNative (JNIEnv *env, jobject) {
  return env->NewStringUTF("Hello World from Native!");
}


