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
extern bool onload_Constants (JNIEnv *);
extern bool onload_CpuMiningWorker (JNIEnv *);
extern bool onload_StratumMiningConnection (JNIEnv *);

jint JNI_OnLoad (JavaVM *vm, void *) {
  JNIEnv *env;
  if (
    (vm->GetEnv ((void **)&env, JNI_VERSION_1_6) != JNI_OK) ||
    !onload_Constants (env) ||
    !onload_CpuMiningWorker (env) ||
    !onload_StratumMiningConnection (env) 
  ) return JNI_ERR;

  global_jvm = vm;
  return JNI_VERSION_1_6;
}

extern void onunload_Constants (JNIEnv *);
extern void onunload_CpuMiningWorker (JNIEnv *);
extern void onunload_StratumMiningConnection (JNIEnv *);

void JNI_OnUnload (JavaVM *vm, void *) {
  JNIEnv *env;
  if (vm->GetEnv ((void **)&env, JNI_VERSION_1_6) != JNI_OK)
    return;

  onunload_Constants (env);
  onunload_CpuMiningWorker (env);
  onunload_StratumMiningConnection (env);

  global_jvm = nullptr;
}
