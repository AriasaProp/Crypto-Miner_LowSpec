#include <jni.h>
#include <cstdint>
#include "hashing.hpp"

static uint8_t ret[SHA256_HASH_SIZE];

bool onload_Constants(JNIEnv *) {
    
}
void onunload_Constants(JNIEnv *) {
    
}

#define JNIF(R, M) extern "C" JNIEXPORT R JNICALL Java_com_ariasaproject_cmls_Constants_##M

JNIF(jbyteArray, hash) (JNIEnv *env, jclass, jbyteArray head) {
    jbyte* header = env->GetByteArrayElements(head, NULL);
    hashN((uint8_t*)header, ret);
    env->ReleaseByteArrayElements(head, header, JNI_ABORT);
    jbyteArray result = env->NewByteArray(SHA256_HASH_SIZE);
    env->SetByteArrayRegion(result, 0, SHA256_HASH_SIZE, reinterpret_cast<const jbyte*>(ret));
    return result;
}
JNIF(jbyteArray, hash2) (JNIEnv *env, jclass, jbyteArray head) {
    jbyte* header = env->GetByteArrayElements(head, NULL);
    hashN((uint8_t*)header, ret);
    env->ReleaseByteArrayElements(head, header, JNI_ABORT);
    jbyteArray result = env->NewByteArray(SHA256_HASH_SIZE);
    env->SetByteArrayRegion(result, 0, SHA256_HASH_SIZE, reinterpret_cast<const jbyte*>(ret));
    return result;
}
JNIF(jlong, initHasher) (JNIEnv *, jclass) {
    return (jlong) (new hashing);
}
JNIF(jboolean, nativeHashing) (JNIEnv *env, jclass, jlong o, jbyteArray head, jint nonce, jbyteArray target) {
    jbyte* header = env->GetByteArrayElements(head, NULL);
    hashing *hp = (hashing*)o;
    hp->hash((uint8_t*)header, (uint32_t)nonce);
    env->ReleaseByteArrayElements(head, header, JNI_ABORT);
    jbyte* t = env->GetByteArrayElements(target, NULL);
    uint8_t *tar = (uint8_t*)t;
    size_t i = SHA256_HASH_SIZE;
    jboolean result = false;
    while (--i) {
        uint8_t &a = hp->H[i], &b = tar[i];
        if (a != b) {
            result = (a < b);
            break;
        }
    }
    env->ReleaseByteArrayElements(target, t, JNI_ABORT);
    return result;
}
JNIF(void, destroyHasher) (JNIEnv *, jclass, jlong o) {
    delete ((hashing*)o);
}
