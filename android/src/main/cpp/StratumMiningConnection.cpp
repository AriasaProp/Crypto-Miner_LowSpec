#include <jni.h>
#include <cstdint>
#include <pthread.h>
#include <iostream>
#include <cstring>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <unistd.h>

static int stratumSocket = -1;
static char *buffer = nullptr;

bool onload_StratumMiningConnection (JNIEnv *) {
  buffer = new char[1024*1024]{};
  return true;
}
void onunload_StratumMiningConnection (JNIEnv *) {
  delete[] buffer;
  buffer = nullptr;
}

#define JNIF(R, M) extern "C" JNIEXPORT R JNICALL Java_com_ariasaproject_cmls_connection_StratumMiningConnection_##M

JNIF(jboolean, connectN)
(JNIEnv *env, jobject, jstring uri, jint port) {
  if (stratumSocket >= 0) return true;
  stratumSocket = socket(AF_INET, SOCK_STREAM, 0);
  if (stratumSocket < 0) return false;
  const char *nuri = env->GetStringUTFChars(uri, JNI_FALSE);
  struct sockaddr_in serverAddress = {
    .sin_family = AF_INET,
    .sin_port = htons(port),
    .sin_addr = {
      .s_addr = inet_addr(nuri),
    }
  };
  env->ReleaseStringUTFChars(uri, nuri);
  size_t tries = 0;
  while (connect(stratumSocket, (struct sockaddr*)&serverAddress, sizeof(serverAddress)) < 0) {
    if (++tries >= 3) return false;
  }
  return true;
}


JNIF(jboolean, send)
(JNIEnv *env, jobject, jstring msg) {
  if (stratumSocket < 0) return false;
  const char *nmsg = env->GetStringUTFChars(msg, JNI_FALSE);
  size_t sended = 0, fullMsg = env->GetStringUTFLength(msg), tries = 0;
  while (sended < fullMsg) {
    size_t sending = send(stratumSocket, nmsg, fullMsg - sended, 0);
    if (!sending) tries++;
    if (tries >= 3) return false;
    sended += sending;
  }
  env->ReleaseStringUTFChars(msg, nmsg);
  return true;
}

JNIF(jstring, recv)
(JNIEnv *env, jobject) {
  if (stratumSocket < 0) return 0;
  memset(buffer, 0, sizeof(buffer));
  size_t received = 0, tries = 0;
  do {
    received = recv(stratumSocket, buffer, sizeof(buffer), 0);
    if (!received) tries++;
  } while (tries < 3);
  return env->NewStringUTF(buffer);
}

JNIF(void, disconnectN)
(JNIEnv *, jobject) {
  if (stratumSocket < 0) return;
  close(stratumSocket);
  stratumSocket = -1;
}
