#ifndef SHA256_H_
#define SHA256_H_

#include <cstdint>
#include <cstdio>

struct Sha256Context {
  uint64_t length;
  uint32_t state[8];
  uint32_t curlen;
  uint8_t buf[64];
};

#define SHA256_HASH_SIZE (256 / 8)

struct SHA256_HASH {
  uint8_t bytes[SHA256_HASH_SIZE];
};

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//  PUBLIC FUNCTIONS
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//  Sha256Initialise
//
//  Initialises a SHA256 Context. Use this to initialise/reset a context.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
void Sha256Initialise (Sha256Context *Context // [out]
);

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//  Sha256Update
//
//  Adds data to the SHA256 context. This will process the data and update the
//  internal state of the context. Keep on calling this function until all the
//  data has been added. Then call Sha256Finalise to calculate the hash.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
void Sha256Update (Sha256Context *Context, // [in out]
                   void const *Buffer,     // [in]
                   uint32_t BufferSize     // [in]
);

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//  Sha256Finalise
//
//  Performs the final calculation of the hash and returns the digest (32 byte
//  buffer containing 256bit hash). After calling this, Sha256Initialised must
//  be used to reuse the context.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
void Sha256Finalise (Sha256Context *Context, // [in out]
                     SHA256_HASH *Digest     // [out]
);

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//  Sha256Calculate
//
//  Combines Sha256Initialise, Sha256Update, and Sha256Finalise into one
//  function. Calculates the SHA256 hash of the buffer.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
void Sha256Calculate (void const *Buffer,  // [in]
                      uint32_t BufferSize, // [in]
                      SHA256_HASH *Digest  // [in]
);
#endif // SHA256_H_