#ifndef HASHING_H_
#define HASHING_H_

#include "hmac_sha256/sha256.hpp"

struct hashing {
private:
  size_t i, j, k, l;
  uint32_t xs[16];
  uint8_t B[132];
  uint32_t X[32];
  uint32_t V[32768];
  Sha256Context context;
public:
  uint8_t H[SHA256_HASH_SIZE]; //cache of hashing
  hashing();
  ~hashing();
  void xorSalsa8();
  void hash(uint8_t*, uint32_t);
};
void hashN(uint8_t*, uint8_t[SHA256_HASH_SIZE]);

#endif //HASHING_H_