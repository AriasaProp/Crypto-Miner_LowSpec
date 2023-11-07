#include "hashing.hpp"
#include <cstdint>
#include <cstring>
#include <endian.h>
#include <jni.h>
#include <string>

#ifndef __BYTE_ORDER__
#define __BYTE_ORDER__ __ORDER_LITTLE_ENDIAN__
#endif

JavaVM *global_jvm;
extern bool onload_MainActivity (JNIEnv *);
extern bool onload_Constants (JNIEnv *);
extern bool onload_CpuMiningWorker (JNIEnv *);

jint JNI_OnLoad (JavaVM *vm, void *) {
  JNIEnv *env;
  if (
    (vm->GetEnv (reinterpret_cast<void **> (&env), JNI_VERSION_1_6) != JNI_OK) ||
    !onload_MainActivity (env) ||
    !onload_Constants (env) ||
    !onload_CpuMiningWorker (env)
  ) return JNI_ERR;

  global_jvm = vm;
  return JNI_VERSION_1_6;
}

extern void onunload_MainActivity (JNIEnv *);
extern void onunload_Constants (JNIEnv *);
extern void onunload_CpuMiningWorker (JNIEnv *);

void JNI_OnUnload (JavaVM *vm, void *) {
  JNIEnv *env;
  if (vm->GetEnv (reinterpret_cast<void **> (&env), JNI_VERSION_1_6) != JNI_OK)
    return;

  onunload_MainActivity (env);
  onunload_Constants (env);
  onunload_CpuMiningWorker (env);

  global_jvm = nullptr;
}
