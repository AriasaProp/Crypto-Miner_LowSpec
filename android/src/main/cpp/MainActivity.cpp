#include <jni.h>

static jclass m_class;
static jmethodID m_callOfCall;

bool onload_MainActivity (JNIEnv *env) {
  m_class = env->FindClass ("com/ariasaproject/cmls/MainActivity");
  if (!m_class) goto failed_section;
  m_callOfCall = env->GetMethodID (m_class, "callOfCall", "()Ljava/lang/String;"); // Get method ID
  if (!m_callOfCall) goto failed_section;

  return true;
failed_section:
  return false;
}
void onunload_MainActivity (JNIEnv *) {
  m_class = NULL;
  m_callOfCall = NULL;
}

#define JNIF(R, M) extern "C" JNIEXPORT R JNICALL Java_com_ariasaproject_cmls_MainActivity_##M

JNIF (jstring, callHello)
(JNIEnv *env, jobject obj) {
  jstring javaString = (jstring)env->CallObjectMethod (obj, m_callOfCall);
  const char *nativeString = env->GetStringUTFChars (javaString, nullptr);
  return env->NewStringUTF (nativeString);
}