#include "hmac_sha256/sha256.hpp"
#include <cstring>
#include <jni.h>

#define JNIF(R, M) extern "C" JNIEXPORT R JNICALL Java_com_ariasaproject_cmls_MainActivity_##M

JNIF (jstring, callNative)
(JNIEnv *env, jobject) {
  return env->NewStringUTF ("Hello World from Native!");
}
#define JNIF(R, M) extern "C" JNIEXPORT R JNICALL Java_com_ariasaproject_cmls_hasher_Hasher_##M

JNIF (jbyteArray, nativeHashing)
(JNIEnv *env, jclass, jbyteArray head, jint nonce) {
  uint8_t *header = static_cast<uint8_t *> (env->GetByteArrayElements (head, NULL));

  uint8_t ret = new uint8_t[SHA256_HASH_SIZE];
  hash (header, nonce, ret);
  env->ReleaseByteArrayElements (head, header, JNI_ABORT);

  jbyteArray result = env->NewByteArray (len);
  env->SetByteArrayRegion (result, 0, len, (const jbyte *)ret);
  delete[] ret;
  return result;
}

const int ARRAY_SIZE = 132;
const int X_SIZE = 32;
const int V_SIZE = 32768;

uint32_t rotateLeft (uint32_t value, int shift) {
  return (value << shift) | (value >> (32 - shift));
}
void xorSalsa8 (uint32_t X[X_SIZE]) {
  uint32_t xs[15];
  xs[0] = (X[0] ^= X[16]);
  xs[1] = (X[1] ^= X[17]);
  xs[2] = (X[2] ^= X[18]);
  xs[3] = (X[3] ^= X[19]);
  xs[4] = (X[4] ^= X[20]);
  xs[5] = (X[5] ^= X[21]);
  xs[6] = (X[6] ^= X[22]);
  xs[7] = (X[7] ^= X[23]);
  xs[8] = (X[8] ^= X[24]);
  xs[9] = (X[9] ^= X[25]);
  xs[10] = (X[10] ^= X[26]);
  xs[11] = (X[11] ^= X[27]);
  xs[12] = (X[12] ^= X[28]);
  xs[13] = (X[13] ^= X[29]);
  xs[14] = (X[14] ^= X[30]);
  xs[15] = (X[15] ^= X[31]);
  for (l = 0; l < 4; l++) { // 8/2
    xs[4] ^= rotateLeft (xs[0] + xs[12], 7);
    xs[8] ^= rotateLeft (xs[4] + xs[0], 9);
    xs[12] ^= rotateLeft (xs[8] + xs[4], 13);
    xs[0] ^= rotateLeft (xs[12] + xs[8], 18);
    xs[9] ^= rotateLeft (xs[5] + xs[1], 7);
    xs[13] ^= rotateLeft (xs[9] + xs[5], 9);
    xs[1] ^= rotateLeft (xs[13] + xs[9], 13);
    xs[5] ^= rotateLeft (xs[1] + xs[13], 18);
    xs[14] ^= rotateLeft (xs[10] + xs[6], 7);
    xs[2] ^= rotateLeft (xs[14] + xs[10], 9);
    xs[6] ^= rotateLeft (xs[2] + xs[14], 13);
    xs[10] ^= rotateLeft (xs[6] + xs[2], 18);
    xs[3] ^= rotateLeft (xs[15] + xs[11], 7);
    xs[7] ^= rotateLeft (xs[3] + xs[15], 9);
    xs[11] ^= rotateLeft (xs[7] + xs[3], 13);
    xs[15] ^= rotateLeft (xs[11] + xs[7], 18);
    xs[1] ^= rotateLeft (xs[0] + xs[3], 7);
    xs[2] ^= rotateLeft (xs[1] + xs[0], 9);
    xs[3] ^= rotateLeft (xs[2] + xs[1], 13);
    xs[0] ^= rotateLeft (xs[3] + xs[2], 18);
    xs[6] ^= rotateLeft (xs[5] + xs[4], 7);
    xs[7] ^= rotateLeft (xs[6] + xs[5], 9);
    xs[4] ^= rotateLeft (xs[7] + xs[6], 13);
    xs[5] ^= rotateLeft (xs[4] + xs[7], 18);
    xs[11] ^= rotateLeft (xs[10] + xs[9], 7);
    xs[8] ^= rotateLeft (xs[11] + xs[10], 9);
    xs[9] ^= rotateLeft (xs[8] + xs[11], 13);
    xs[10] ^= rotateLeft (xs[9] + xs[8], 18);
    xs[12] ^= rotateLeft (xs[15] + xs[14], 7);
    xs[13] ^= rotateLeft (xs[12] + xs[15], 9);
    xs[14] ^= rotateLeft (xs[13] + xs[12], 13);
    xs[15] ^= rotateLeft (xs[14] + xs[13], 18);
  }
  X[0] += xs[0];
  X[1] += xs[1];
  X[2] += xs[2];
  X[3] += xs[3];
  X[4] += xs[4];
  X[5] += xs[5];
  X[6] += xs[6];
  X[7] += xs[7];
  X[8] += xs[8];
  X[9] += xs[9];
  X[10] += xs[10];
  X[11] += xs[11];
  X[12] += xs[12];
  X[13] += xs[13];
  X[14] += xs[14];
  X[15] += xs[15];

  xs[0] = (X[16] ^= X[0]);
  xs[1] = (X[17] ^= X[1]);
  xs[2] = (X[18] ^= X[2]);
  xs[3] = (X[19] ^= X[3]);
  xs[4] = (X[20] ^= X[4]);
  xs[5] = (X[21] ^= X[5]);
  xs[6] = (X[22] ^= X[6]);
  xs[7] = (X[23] ^= X[7]);
  xs[8] = (X[24] ^= X[8]);
  xs[9] = (X[25] ^= X[9]);
  xs[10] = (X[26] ^= X[10]);
  xs[11] = (X[27] ^= X[11]);
  xs[12] = (X[28] ^= X[12]);
  xs[13] = (X[29] ^= X[13]);
  xs[14] = (X[30] ^= X[14]);
  xs[15] = (X[31] ^= X[15]);
  for (l = 0; l < 4; l++) { // 8/2
    xs[4] ^= rotateLeft (xs[0] + xs[12], 7);
    xs[8] ^= rotateLeft (xs[4] + xs[0], 9);
    xs[12] ^= rotateLeft (xs[8] + xs[4], 13);
    xs[0] ^= rotateLeft (xs[12] + xs[8], 18);
    xs[9] ^= rotateLeft (xs[5] + xs[1], 7);
    xs[13] ^= rotateLeft (xs[9] + xs[5], 9);
    xs[1] ^= rotateLeft (xs[13] + xs[9], 13);
    xs[5] ^= rotateLeft (xs[1] + xs[13], 18);
    xs[14] ^= rotateLeft (xs[10] + xs[6], 7);
    xs[2] ^= rotateLeft (xs[14] + xs[10], 9);
    xs[6] ^= rotateLeft (xs[2] + xs[14], 13);
    xs[10] ^= rotateLeft (xs[6] + xs[2], 18);
    xs[3] ^= rotateLeft (xs[15] + xs[11], 7);
    xs[7] ^= rotateLeft (xs[3] + xs[15], 9);
    xs[11] ^= rotateLeft (xs[7] + xs[3], 13);
    xs[15] ^= rotateLeft (xs[11] + xs[7], 18);
    xs[1] ^= rotateLeft (xs[0] + xs[3], 7);
    xs[2] ^= rotateLeft (xs[1] + xs[0], 9);
    xs[3] ^= rotateLeft (xs[2] + xs[1], 13);
    xs[0] ^= rotateLeft (xs[3] + xs[2], 18);
    xs[6] ^= rotateLeft (xs[5] + xs[4], 7);
    xs[7] ^= rotateLeft (xs[6] + xs[5], 9);
    xs[4] ^= rotateLeft (xs[7] + xs[6], 13);
    xs[5] ^= rotateLeft (xs[4] + xs[7], 18);
    xs[11] ^= rotateLeft (xs[10] + xs[9], 7);
    xs[8] ^= rotateLeft (xs[11] + xs[10], 9);
    xs[9] ^= rotateLeft (xs[8] + xs[11], 13);
    xs[10] ^= rotateLeft (xs[9] + xs[8], 18);
    xs[12] ^= rotateLeft (xs[15] + xs[14], 7);
    xs[13] ^= rotateLeft (xs[12] + xs[15], 9);
    xs[14] ^= rotateLeft (xs[13] + xs[12], 13);
    xs[15] ^= rotateLeft (xs[14] + xs[13], 18);
  }
  X[16] += xs[0];
  X[17] += xs[1];
  X[18] += xs[2];
  X[19] += xs[3];
  X[20] += xs[4];
  X[21] += xs[5];
  X[22] += xs[6];
  X[23] += xs[7];
  X[24] += xs[8];
  X[25] += xs[9];
  X[26] += xs[10];
  X[27] += xs[11];
  X[28] += xs[12];
  X[29] += xs[13];
  X[30] += xs[14];
  X[31] += xs[15];
}
void hash (const uint8_t *header, int nonce, uint8_t *result) {
  uint8_t B[ARRAY_SIZE];
  uint32_t X[X_SIZE];
  uint32_t V[V_SIZE];
  SHA256_HASH H;

  memcpy (B, header, 76);
  B[76] = (nonce >> 24) & 0xFF;
  B[77] = (nonce >> 16) & 0xFF;
  B[78] = (nonce >> 8) & 0xFF;
  B[79] = nonce & 0xFF;

  Sha256Context context;
  Sha256Initialise (&context);
  B[80] = 0;
  B[81] = 0;
  B[82] = 0;

  size_t i, j, k;

  for (i = 0; i < 4; i++) {
    B[83] = i + 1;
    Sha256Update (&context, B, 84);
    Sha256Finalise (&context, &H);

    for (j = 0; j < 8; j++) {
      X[i * 8 + j] = (H[j * 4] & 0xFF) | (H[j * 4 + 1] & 0xFF) << 8 | (H[j * 4 + 2] & 0xFF) << 16 | (H[j * 4 + 3] & 0xFF) << 24;
    }
  }

  for (i = 0; i < V_SIZE; i += 32) {
    memcpy (V + i, X, 32);
    xorSalsa8 (X);
  }

  for (i = 0; i < 1024; i++) {
    k = (X[16] & 1023) * 32;
    for (j = 0; j < 32; j++) {
      X[j] ^= V[k + j];
    }
    xorSalsa8 (X);
  }

  for (i = 0; i < 32; i++) {
    B[i * 4] = X[i] & 0xFF;
    B[i * 4 + 1] = (X[i] >> 8) & 0xFF;
    B[i * 4 + 2] = (X[i] >> 16) & 0xFF;
    B[i * 4 + 3] = (X[i] >> 24) & 0xFF;
  }

  B[131] = 1;

  Sha256Update (&context, B, ARRAY_SIZE);
  Sha256Finalise (&context, &H);

  memcpy (result, H, SHA256_HASH_SIZE);
}
