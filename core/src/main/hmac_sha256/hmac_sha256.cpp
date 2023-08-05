#include "hmac_sha256.hpp"
#include "sha256.hpp"

#include <cstdlib>
#include <cstring>

#define SHA256_BLOCK_SIZE 64

// Wrapper for sha256
static void *sha256 (const void *data, const size_t datalen, void *out, const size_t outlen) {
  size_t sz;
  uint8_t H[SHA256_HASH_SIZE];
  Sha256Context ctx;

  Sha256Initialise (&ctx);
  Sha256Update (&ctx, data, datalen);
  Sha256Finalise (&ctx, H);

  sz = (outlen > SHA256_HASH_SIZE) ? SHA256_HASH_SIZE : outlen;
  return memcpy(out, H, sz);
}

// Concatenate X & Y, return hash.
static void *H (const void *x, const size_t xlen, const void *y, const size_t ylen, void *out, const size_t outlen) {
  size_t buflen = (xlen + ylen);
  uint8_t *buf = new uint8_t[buflen];

  memcpy (buf, x, xlen);
  memcpy (buf + xlen, y, ylen);
  void *result = sha256(buf, buflen, out, outlen);

  delete[] buf;
  return result;
}

// Declared in hmac_sha256.h
size_t hmac_sha256 (const void *key, const size_t keylen, const void *data, const size_t datalen, void *out, const size_t outlen) {
  uint8_t k[SHA256_BLOCK_SIZE];
  uint8_t k_ipad[SHA256_BLOCK_SIZE];
  uint8_t k_opad[SHA256_BLOCK_SIZE];
  uint8_t ihash[SHA256_HASH_SIZE];
  uint8_t ohash[SHA256_HASH_SIZE];
  size_t sz;
  int i;

  memset (k, 0, sizeof (k));
  memset (k_ipad, 0x36, SHA256_BLOCK_SIZE);
  memset (k_opad, 0x5c, SHA256_BLOCK_SIZE);

  if (keylen > SHA256_BLOCK_SIZE) {
    sha256 (key, keylen, k, sizeof (k));
  } else {
    memcpy (k, key, keylen);
  }

  for (i = 0; i < SHA256_BLOCK_SIZE; i++) {
    k_ipad[i] ^= k[i];
    k_opad[i] ^= k[i];
  }

  H (k_ipad, sizeof (k_ipad), data, datalen, ihash, sizeof (ihash));
  H (k_opad, sizeof (k_opad), ihash, sizeof (ihash), ohash, sizeof (ohash));

  sz = (outlen > SHA256_HASH_SIZE) ? SHA256_HASH_SIZE : outlen;
  memcpy (out, ohash, sz);
  return sz;
}

