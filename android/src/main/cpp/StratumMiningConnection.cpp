#include <jni.h>
#include <cstdint>
#include <pthread.h>
#include <iostream>
#include <cstring>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <unistd.h>
#include <string>
#include <vector>

static int stratumSocket = -1;
static char *buffer = nullptr;

bool onload_StratumMiningConnection (JNIEnv *) {
  buffer = new char[1048575]{}; // aprox 1Mbyte
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
  const char *nuri = env->GetStringUTFChars(uri, JNI_FALSE);
  struct hostent *host = gethostbyname(nuri);
  env->ReleaseStringUTFChars(uri, nuri);
  if (!host) return false;
  stratumSocket = socket(AF_INET, SOCK_STREAM, 0);
  if (stratumSocket < 0) return false;
  struct sockaddr_in serverAddress = {
    .sin_family = AF_INET,
    .sin_port = htons(port),
    .sin_addr = *((struct in_addr *)host->h_addr),
  };
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
static size_t lastBuffer = 0;
JNIF(jobjectArray, recv)
(JNIEnv *env, jobject) {
  if (stratumSocket < 0) return 0;
  size_t received = 0, tries = 0;
  do {
    received = recv(stratumSocket, buffer-lastBuffer, sizeof(buffer)-lastBuffer, 0);
    if (!received) tries++;
  } while (tries < 3);
  if (tries >= 3) return 0;
  //string each line
  std::string data(buffer);
  std::vector<std::string> datas;
  size_t endPos = 0, startPos = 0;
  while (startPos != std::string::npos) {
    endPos = data.find('\n', startPos);
    if (endPos == std::string::npos) {
      break;
    }
    datas.push_back(data.substr(startPos, endPos - startPos));
    startPos = endPos + 1;
  }
  if (startPos < received) {
    size_t remain = strlen(buffer+startPos);
    if (remain) {
      memmove(buffer, buffer+startPos, remain);
    }
    lastBuffer = startPos;
  }
  if (datas.empty()) return 0;

  jobjectArray result = env->NewObjectArray(datas.size(), env->FindClass("java/lang/String"), nullptr);
  for (size_t i = 0; i < datas.size(); ++i) {
      env->SetObjectArrayElement(result, i, env->NewStringUTF(datas[i].c_str()));
  }
  return result;
}

JNIF(void, disconnectN)
(JNIEnv *, jobject) {
  if (stratumSocket < 0) return;
  close(stratumSocket);
  stratumSocket = -1;
}
