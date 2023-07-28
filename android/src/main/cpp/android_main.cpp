#include "hashing.hpp"
#include <jni.h>
#include <cstring>
#include <cstdint>
#include <endian.h>
#include <netinet/in.h>

#ifndef __BYTE_ORDER__
#define __BYTE_ORDER__ __ORDER_LITTLE_ENDIAN__
#endif

#define JNIH(R, M) extern "C" JNIEXPORT R JNICALL Java_com_ariasaproject_cmls_Constants_##M

JNIH(jbyteArray, hash) (JNIEnv *env, jclass, jbyteArray head) {
    jbyte* header = env->GetByteArrayElements(head, NULL);
    uint8_t *ret = new uint8_t[SHA256_HASH_SIZE];
    hashN((uint8_t*)header, ret);
    env->ReleaseByteArrayElements(head, header, JNI_ABORT);
    jbyteArray result = env->NewByteArray(SHA256_HASH_SIZE);
    env->SetByteArrayRegion(result, 0, SHA256_HASH_SIZE, reinterpret_cast<const jbyte*>(ret));
    delete[] ret;
    return result;
}
JNIH(jbyteArray, hash2) (JNIEnv *env, jclass, jbyteArray head) {
    jbyte* header = env->GetByteArrayElements(head, NULL);
    uint8_t *ret = new uint8_t[SHA256_HASH_SIZE];
    hashN((uint8_t*)header, ret);
    env->ReleaseByteArrayElements(head, header, JNI_ABORT);
    jbyteArray result = env->NewByteArray(SHA256_HASH_SIZE);
    env->SetByteArrayRegion(result, 0, SHA256_HASH_SIZE, reinterpret_cast<const jbyte*>(ret));
    delete[] ret;
    return result;
}
JNIH(jlong, initHasher) (JNIEnv *, jclass) {
    return (jlong) (new hashing);
}
JNIH(jbyteArray, nativeHashing) (JNIEnv *env, jclass, jlong o, jbyteArray head, jint nonce) {
    jbyte* header = env->GetByteArrayElements(head, NULL);
    hashing *hp = (hashing*)o;
    uint8_t *ret = new uint8_t[SHA256_HASH_SIZE];
    hp->hash((uint8_t*)header, (uint32_t)nonce, ret);
    env->ReleaseByteArrayElements(head, header, JNI_ABORT);
    jbyteArray result = env->NewByteArray(SHA256_HASH_SIZE);
    env->SetByteArrayRegion(result, 0, SHA256_HASH_SIZE, reinterpret_cast<const jbyte*>(ret));
    delete[] ret;
    return result;
}
JNIH(void, destroyHasher) (JNIEnv *, jclass, jlong o) {
    delete ((hashing*)o);
}


