#include "hashing.hpp"
#include <cstring>

#define _rotl(value, bits) (((value) >> (bits)) | ((value) << (32 - (bits))))

hashing::hashing(){}
hashing::~hashing(){}
void hashing::xorSalsa8() {
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
      xs[4] ^= _rotl(xs[0] + xs[12], 7);
      xs[8] ^= _rotl(xs[4] + xs[0], 9);
      xs[12] ^= _rotl(xs[8] + xs[4], 13);
      xs[0] ^= _rotl(xs[12] + xs[8], 18);
      xs[9] ^= _rotl(xs[5] + xs[1], 7);
      xs[13] ^= _rotl(xs[9] + xs[5], 9);
      xs[1] ^= _rotl(xs[13] + xs[9], 13);
      xs[5] ^= _rotl(xs[1] + xs[13], 18);
      xs[14] ^= _rotl(xs[10] + xs[6], 7);
      xs[2] ^= _rotl(xs[14] + xs[10], 9);
      xs[6] ^= _rotl(xs[2] + xs[14], 13);
      xs[10] ^= _rotl(xs[6] + xs[2], 18);
      xs[3] ^= _rotl(xs[15] + xs[11], 7);
      xs[7] ^= _rotl(xs[3] + xs[15], 9);
      xs[11] ^= _rotl(xs[7] + xs[3], 13);
      xs[15] ^= _rotl(xs[11] + xs[7], 18);
      xs[1] ^= _rotl(xs[0] + xs[3], 7);
      xs[2] ^= _rotl(xs[1] + xs[0], 9);
      xs[3] ^= _rotl(xs[2] + xs[1], 13);
      xs[0] ^= _rotl(xs[3] + xs[2], 18);
      xs[6] ^= _rotl(xs[5] + xs[4], 7);
      xs[7] ^= _rotl(xs[6] + xs[5], 9);
      xs[4] ^= _rotl(xs[7] + xs[6], 13);
      xs[5] ^= _rotl(xs[4] + xs[7], 18);
      xs[11] ^= _rotl(xs[10] + xs[9], 7);
      xs[8] ^= _rotl(xs[11] + xs[10], 9);
      xs[9] ^= _rotl(xs[8] + xs[11], 13);
      xs[10] ^= _rotl(xs[9] + xs[8], 18);
      xs[12] ^= _rotl(xs[15] + xs[14], 7);
      xs[13] ^= _rotl(xs[12] + xs[15], 9);
      xs[14] ^= _rotl(xs[13] + xs[12], 13);
      xs[15] ^= _rotl(xs[14] + xs[13], 18);
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
      xs[4] ^= _rotl(xs[0] + xs[12], 7);
      xs[8] ^= _rotl(xs[4] + xs[0], 9);
      xs[12] ^= _rotl(xs[8] + xs[4], 13);
      xs[0] ^= _rotl(xs[12] + xs[8], 18);
      xs[9] ^= _rotl(xs[5] + xs[1], 7);
      xs[13] ^= _rotl(xs[9] + xs[5], 9);
      xs[1] ^= _rotl(xs[13] + xs[9], 13);
      xs[5] ^= _rotl(xs[1] + xs[13], 18);
      xs[14] ^= _rotl(xs[10] + xs[6], 7);
      xs[2] ^= _rotl(xs[14] + xs[10], 9);
      xs[6] ^= _rotl(xs[2] + xs[14], 13);
      xs[10] ^= _rotl(xs[6] + xs[2], 18);
      xs[3] ^= _rotl(xs[15] + xs[11], 7);
      xs[7] ^= _rotl(xs[3] + xs[15], 9);
      xs[11] ^= _rotl(xs[7] + xs[3], 13);
      xs[15] ^= _rotl(xs[11] + xs[7], 18);
      xs[1] ^= _rotl(xs[0] + xs[3], 7);
      xs[2] ^= _rotl(xs[1] + xs[0], 9);
      xs[3] ^= _rotl(xs[2] + xs[1], 13);
      xs[0] ^= _rotl(xs[3] + xs[2], 18);
      xs[6] ^= _rotl(xs[5] + xs[4], 7);
      xs[7] ^= _rotl(xs[6] + xs[5], 9);
      xs[4] ^= _rotl(xs[7] + xs[6], 13);
      xs[5] ^= _rotl(xs[4] + xs[7], 18);
      xs[11] ^= _rotl(xs[10] + xs[9], 7);
      xs[8] ^= _rotl(xs[11] + xs[10], 9);
      xs[9] ^= _rotl(xs[8] + xs[11], 13);
      xs[10] ^= _rotl(xs[9] + xs[8], 18);
      xs[12] ^= _rotl(xs[15] + xs[14], 7);
      xs[13] ^= _rotl(xs[12] + xs[15], 9);
      xs[14] ^= _rotl(xs[13] + xs[12], 13);
      xs[15] ^= _rotl(xs[14] + xs[13], 18);
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
void hashing::hash(uint8_t* header, uint32_t nonce) {
  memcpy(B, header, 76);
  memcpy(B + 76, &nonce, 4);
   
  Sha256Initialise(&context);
  memset(B+80, 0, 3);

  for (i = 0; i < 4; i++) {
      B[83] = i + 1;
      Sha256Update(&context, B, 84);
      Sha256Finalise(&context, H);
      memcpy(X+(i*8), H, 32);
  }

  for (i = 0; i < 32768; i += 32) {
      memcpy(V + i, X, 32);
      xorSalsa8();
  }

  for (i = 0; i < 1024; i++) {
      k = (X[16] & 0x3ff) << 5;
      for (j = 0; j < 32; j++) {
          X[j] ^= V[k + j];
      }
      xorSalsa8();
  }
  memcpy(B, X, 128);
  B[131] = 1;
  Sha256Update(&context, B, 132);
  Sha256Finalise(&context, H);
}

void hashN(uint8_t* header, uint8_t H[SHA256_HASH_SIZE]) {
    uint8_t B[132];
    uint32_t X[32];
    uint32_t V[32768];
    uint32_t xs[16];
    
    Sha256Context context;
    size_t i, j, k, l;
    memcpy(B, header, 80);
    Sha256Initialise(&context);
    memset(B+80, 0, 3);
    
    for (i = 0; i < 4; i++) {
        B[83] = i + 1;
        Sha256Update(&context, B, 84);
        Sha256Finalise(&context, H);
        memcpy(X+(i*8), H, 32);
    }

    for (i = 0; i < 32768; i += 32) {
        memcpy(V + i, X, 32);
        //xorSalsa8
        {
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
                xs[4] ^= _rotl(xs[0] + xs[12], 7);
                xs[8] ^= _rotl(xs[4] + xs[0], 9);
                xs[12] ^= _rotl(xs[8] + xs[4], 13);
                xs[0] ^= _rotl(xs[12] + xs[8], 18);
                xs[9] ^= _rotl(xs[5] + xs[1], 7);
                xs[13] ^= _rotl(xs[9] + xs[5], 9);
                xs[1] ^= _rotl(xs[13] + xs[9], 13);
                xs[5] ^= _rotl(xs[1] + xs[13], 18);
                xs[14] ^= _rotl(xs[10] + xs[6], 7);
                xs[2] ^= _rotl(xs[14] + xs[10], 9);
                xs[6] ^= _rotl(xs[2] + xs[14], 13);
                xs[10] ^= _rotl(xs[6] + xs[2], 18);
                xs[3] ^= _rotl(xs[15] + xs[11], 7);
                xs[7] ^= _rotl(xs[3] + xs[15], 9);
                xs[11] ^= _rotl(xs[7] + xs[3], 13);
                xs[15] ^= _rotl(xs[11] + xs[7], 18);
                xs[1] ^= _rotl(xs[0] + xs[3], 7);
                xs[2] ^= _rotl(xs[1] + xs[0], 9);
                xs[3] ^= _rotl(xs[2] + xs[1], 13);
                xs[0] ^= _rotl(xs[3] + xs[2], 18);
                xs[6] ^= _rotl(xs[5] + xs[4], 7);
                xs[7] ^= _rotl(xs[6] + xs[5], 9);
                xs[4] ^= _rotl(xs[7] + xs[6], 13);
                xs[5] ^= _rotl(xs[4] + xs[7], 18);
                xs[11] ^= _rotl(xs[10] + xs[9], 7);
                xs[8] ^= _rotl(xs[11] + xs[10], 9);
                xs[9] ^= _rotl(xs[8] + xs[11], 13);
                xs[10] ^= _rotl(xs[9] + xs[8], 18);
                xs[12] ^= _rotl(xs[15] + xs[14], 7);
                xs[13] ^= _rotl(xs[12] + xs[15], 9);
                xs[14] ^= _rotl(xs[13] + xs[12], 13);
                xs[15] ^= _rotl(xs[14] + xs[13], 18);
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
                xs[4] ^= _rotl(xs[0] + xs[12], 7);
                xs[8] ^= _rotl(xs[4] + xs[0], 9);
                xs[12] ^= _rotl(xs[8] + xs[4], 13);
                xs[0] ^= _rotl(xs[12] + xs[8], 18);
                xs[9] ^= _rotl(xs[5] + xs[1], 7);
                xs[13] ^= _rotl(xs[9] + xs[5], 9);
                xs[1] ^= _rotl(xs[13] + xs[9], 13);
                xs[5] ^= _rotl(xs[1] + xs[13], 18);
                xs[14] ^= _rotl(xs[10] + xs[6], 7);
                xs[2] ^= _rotl(xs[14] + xs[10], 9);
                xs[6] ^= _rotl(xs[2] + xs[14], 13);
                xs[10] ^= _rotl(xs[6] + xs[2], 18);
                xs[3] ^= _rotl(xs[15] + xs[11], 7);
                xs[7] ^= _rotl(xs[3] + xs[15], 9);
                xs[11] ^= _rotl(xs[7] + xs[3], 13);
                xs[15] ^= _rotl(xs[11] + xs[7], 18);
                xs[1] ^= _rotl(xs[0] + xs[3], 7);
                xs[2] ^= _rotl(xs[1] + xs[0], 9);
                xs[3] ^= _rotl(xs[2] + xs[1], 13);
                xs[0] ^= _rotl(xs[3] + xs[2], 18);
                xs[6] ^= _rotl(xs[5] + xs[4], 7);
                xs[7] ^= _rotl(xs[6] + xs[5], 9);
                xs[4] ^= _rotl(xs[7] + xs[6], 13);
                xs[5] ^= _rotl(xs[4] + xs[7], 18);
                xs[11] ^= _rotl(xs[10] + xs[9], 7);
                xs[8] ^= _rotl(xs[11] + xs[10], 9);
                xs[9] ^= _rotl(xs[8] + xs[11], 13);
                xs[10] ^= _rotl(xs[9] + xs[8], 18);
                xs[12] ^= _rotl(xs[15] + xs[14], 7);
                xs[13] ^= _rotl(xs[12] + xs[15], 9);
                xs[14] ^= _rotl(xs[13] + xs[12], 13);
                xs[15] ^= _rotl(xs[14] + xs[13], 18);
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
    }

    for (i = 0; i < 1024; i++) {
        k = (X[16] & 0x3ff) << 5;
        for (j = 0; j < 32; j++) {
            X[j] ^= V[k + j];
        }
        //xorSalsa8
        {
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
                xs[4] ^= _rotl(xs[0] + xs[12], 7);
                xs[8] ^= _rotl(xs[4] + xs[0], 9);
                xs[12] ^= _rotl(xs[8] + xs[4], 13);
                xs[0] ^= _rotl(xs[12] + xs[8], 18);
                xs[9] ^= _rotl(xs[5] + xs[1], 7);
                xs[13] ^= _rotl(xs[9] + xs[5], 9);
                xs[1] ^= _rotl(xs[13] + xs[9], 13);
                xs[5] ^= _rotl(xs[1] + xs[13], 18);
                xs[14] ^= _rotl(xs[10] + xs[6], 7);
                xs[2] ^= _rotl(xs[14] + xs[10], 9);
                xs[6] ^= _rotl(xs[2] + xs[14], 13);
                xs[10] ^= _rotl(xs[6] + xs[2], 18);
                xs[3] ^= _rotl(xs[15] + xs[11], 7);
                xs[7] ^= _rotl(xs[3] + xs[15], 9);
                xs[11] ^= _rotl(xs[7] + xs[3], 13);
                xs[15] ^= _rotl(xs[11] + xs[7], 18);
                xs[1] ^= _rotl(xs[0] + xs[3], 7);
                xs[2] ^= _rotl(xs[1] + xs[0], 9);
                xs[3] ^= _rotl(xs[2] + xs[1], 13);
                xs[0] ^= _rotl(xs[3] + xs[2], 18);
                xs[6] ^= _rotl(xs[5] + xs[4], 7);
                xs[7] ^= _rotl(xs[6] + xs[5], 9);
                xs[4] ^= _rotl(xs[7] + xs[6], 13);
                xs[5] ^= _rotl(xs[4] + xs[7], 18);
                xs[11] ^= _rotl(xs[10] + xs[9], 7);
                xs[8] ^= _rotl(xs[11] + xs[10], 9);
                xs[9] ^= _rotl(xs[8] + xs[11], 13);
                xs[10] ^= _rotl(xs[9] + xs[8], 18);
                xs[12] ^= _rotl(xs[15] + xs[14], 7);
                xs[13] ^= _rotl(xs[12] + xs[15], 9);
                xs[14] ^= _rotl(xs[13] + xs[12], 13);
                xs[15] ^= _rotl(xs[14] + xs[13], 18);
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
                xs[4] ^= _rotl(xs[0] + xs[12], 7);
                xs[8] ^= _rotl(xs[4] + xs[0], 9);
                xs[12] ^= _rotl(xs[8] + xs[4], 13);
                xs[0] ^= _rotl(xs[12] + xs[8], 18);
                xs[9] ^= _rotl(xs[5] + xs[1], 7);
                xs[13] ^= _rotl(xs[9] + xs[5], 9);
                xs[1] ^= _rotl(xs[13] + xs[9], 13);
                xs[5] ^= _rotl(xs[1] + xs[13], 18);
                xs[14] ^= _rotl(xs[10] + xs[6], 7);
                xs[2] ^= _rotl(xs[14] + xs[10], 9);
                xs[6] ^= _rotl(xs[2] + xs[14], 13);
                xs[10] ^= _rotl(xs[6] + xs[2], 18);
                xs[3] ^= _rotl(xs[15] + xs[11], 7);
                xs[7] ^= _rotl(xs[3] + xs[15], 9);
                xs[11] ^= _rotl(xs[7] + xs[3], 13);
                xs[15] ^= _rotl(xs[11] + xs[7], 18);
                xs[1] ^= _rotl(xs[0] + xs[3], 7);
                xs[2] ^= _rotl(xs[1] + xs[0], 9);
                xs[3] ^= _rotl(xs[2] + xs[1], 13);
                xs[0] ^= _rotl(xs[3] + xs[2], 18);
                xs[6] ^= _rotl(xs[5] + xs[4], 7);
                xs[7] ^= _rotl(xs[6] + xs[5], 9);
                xs[4] ^= _rotl(xs[7] + xs[6], 13);
                xs[5] ^= _rotl(xs[4] + xs[7], 18);
                xs[11] ^= _rotl(xs[10] + xs[9], 7);
                xs[8] ^= _rotl(xs[11] + xs[10], 9);
                xs[9] ^= _rotl(xs[8] + xs[11], 13);
                xs[10] ^= _rotl(xs[9] + xs[8], 18);
                xs[12] ^= _rotl(xs[15] + xs[14], 7);
                xs[13] ^= _rotl(xs[12] + xs[15], 9);
                xs[14] ^= _rotl(xs[13] + xs[12], 13);
                xs[15] ^= _rotl(xs[14] + xs[13], 18);
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
    }
    memcpy(B, X, 128);
    B[131] = 1;
    Sha256Update(&context, B, 132);
    Sha256Finalise(&context, H);
}
