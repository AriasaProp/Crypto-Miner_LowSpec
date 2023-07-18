#include <jni.h>

#define JNIF(R, M) extern "C" JNIEXPORT R JNICALL Java_com_ariasaproject_cmls_MainActivity_##M

JNIF(jstring, callNative) (JNIEnv *env, jobject) {
    return env->NewStringUTF("Hello World from Native!");
}
#define JNIF(R, M) extern "C" JNIEXPORT R JNICALL Java_com_ariasaproject_cmls_hasher_Hasher_##M

JNIF(jbyteArray, nativeHashing) (JNIEnv *env, jclass, jbyteArray, jint) {
    
    return 0;
}


