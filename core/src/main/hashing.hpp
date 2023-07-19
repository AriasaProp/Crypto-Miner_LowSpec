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
  SHA256_HASH H;
  Sha256Context context;
public:
  hashing();
  ~hashing();
  void xorSalsa8();
  void hash(uint8_t*, uint32_t, uint8_t*);
};
void hashN(uint8_t*, uint8_t*);

#endif //HASHING_H_