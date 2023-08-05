#ifndef SHA256_H_
#define SHA256_H_

#include <cstdint>
#include <cstdio>

struct Sha256Context {
  uint32_t curlen;
  uint64_t length;
  uint32_t state[8];
  uint8_t buf[64];
};

#define SHA256_HASH_SIZE 32

void Sha256Initialise (Sha256Context *Context);
void Sha256Update (Sha256Context *Context, void const *Buffer, uint32_t BufferSize);
void Sha256Finalise (Sha256Context *Context, SHA256_HASH *Digest);
void Sha256Calculate (void const *Buffer, uint32_t BufferSize, SHA256_HASH *Digest);
#endif // SHA256_H_