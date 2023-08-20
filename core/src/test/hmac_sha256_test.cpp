#include <sha256.hpp>

#include <cassert>
#include <iomanip>
#include <iostream>
#include <sstream>
#include <string>
#include <cstring>
#include <cstdlib>
#include <stddef.h>

// 256 bit, 32 byte
#define SHA256_HASH_SIZE 32
// 512 bit, 64 byte
#define SHA256_BLOCK_SIZE 64

//HMac test

// Wrapper for sha256
static void *sha256 (const void *data, const size_t datalen, void *out, const size_t outlen) {
  size_t sz;
  uint8_t H[SHA256_HASH_SIZE];
  Sha256Context ctx;

  Sha256Initialise (&ctx);
  Sha256Update (&ctx, data, datalen);
  Sha256Finalise (&ctx, H);

  sz = (outlen > SHA256_HASH_SIZE) ? SHA256_HASH_SIZE : outlen;
  return memcpy (out, H, sz);
}

// Concatenate X & Y, return hash.
static void *H (const void *x, const size_t xlen, const void *y, const size_t ylen, void *out, const size_t outlen) {
  size_t buflen = (xlen + ylen);
  uint8_t *buf = new uint8_t[buflen];

  memcpy (buf, x, xlen);
  memcpy (buf + xlen, y, ylen);
  void *result = sha256 (buf, buflen, out, outlen);

  delete[] buf;
  return result;
}

// Declared in hmac_sha256.h
static size_t hmac_sha256 (const void *key, const size_t keylen, const void *data, const size_t datalen, void *out, const size_t outlen) {
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

bool hmac_sha256_test () {
    std::cout << "HMac SHA256 Test Start" << std::endl;
    // Test Data from RFC4231, https://tools.ietf.org/html/rfc4231#section-4
    const struct dat {
        const char *key;
        const char *data;
        const char *expected;
    } test_data[] = {
        // Key      Data      HMAC
        {
            "super-secret-key",
            "Hello World!",
            "4b393abced1c497f8048860ba1ede46a23f1ff5209b18e9c428bddfbb690aad8",
        },
        {
            "\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b",
            "\x48\x69\x20\x54\x68\x65\x72\x65",
            "b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7",
        },
        {
            /* Test with a key shorter than the length of the HMAC output. */
            "\x4a\x65\x66\x65",
            "\x77\x68\x61\x74\x20\x64\x6f\x20\x79\x61\x20\x77\x61\x6e\x74\x20\x66\x6f\x72\x20\x6e\x6f\x74\x68\x69\x6e\x67\x3f",
            "5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843",
        },
        {
            /* Test with a combined length of key and data that is larger than 64
               bytes (= block-size of SHA-224 and SHA-256). */
            "\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa",
            "\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd",
            "773ea91e36800e46854db8ebd09181a72959098b3ef8c122d9635514ced565fe",
        },
        {
            /* Test with a combined length of key and data that is larger than 64
               bytes (= block-size of SHA-224 and SHA-256). */
            "\x01\x02\x03\x04\x05\x06\x07\x08\x09\x0a\x0b\x0c\x0d\x0e\x0f\x10\x11\x12\x13\x14\x15\x16\x17\x18\x19",
            "\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd",
            "82558a389a443c0ea4cc819899f2083a85f0faa3e578f8077a2e3ff46729665b",
        },
        {
            /* Test with a truncation of output to 128 bits. */
            "\x0c\x0c\x0c\x0c\x0c\x0c\x0c\x0c\x0c\x0c\x0c\x0c\x0c\x0c\x0c\x0c\x0c\x0c\x0c\x0c",
            "\x54\x65\x73\x74\x20\x57\x69\x74\x68\x20\x54\x72\x75\x6e\x63\x61\x74\x69\x6f\x6e",
            "a3b6167473100ee06e0c796c2955552b",
        },
        {
            /* Test with a key larger than 128 bytes (= block-size of SHA-384 and
               SHA-512). */
            "\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
            "\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
            "\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
            "\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
            "\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
            "\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
            "\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
            "\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa",
            "\x54\x65\x73\x74\x20\x55\x73\x69\x6e\x67\x20\x4c\x61\x72\x67\x65\x72"
            "\x20\x54\x68\x61\x6e\x20\x42\x6c\x6f\x63\x6b\x2d\x53\x69\x7a\x65\x20"
            "\x4b\x65\x79\x20\x2d\x20\x48\x61\x73\x68\x20\x4b\x65\x79\x20\x46\x69"
            "\x72\x73\x74",
            "60e431591ee0b67f0d8a26aacbf5b77f8e0bc6213728c5140546040f0ee37f54",
        },
        {
            /* Test with a key and data that is larger than 128 bytes (=
               block-size of SHA-384 and SHA-512). */
            "\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
            "\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
            "\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
            "\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
            "\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
            "\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
            "\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
            "\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa",
            "\x54\x68\x69\x73\x20\x69\x73\x20\x61\x20\x74\x65\x73\x74\x20\x75\x73"
            "\x69\x6e\x67\x20\x61\x20\x6c\x61\x72\x67\x65\x72\x20\x74\x68\x61\x6e"
            "\x20\x62\x6c\x6f\x63\x6b\x2d\x73\x69\x7a\x65\x20\x6b\x65\x79\x20\x61"
            "\x6e\x64\x20\x61\x20\x6c\x61\x72\x67\x65\x72\x20\x74\x68\x61\x6e\x20"
            "\x62\x6c\x6f\x63\x6b\x2d\x73\x69\x7a\x65\x20\x64\x61\x74\x61\x2e\x20"
            "\x54\x68\x65\x20\x6b\x65\x79\x20\x6e\x65\x65\x64\x73\x20\x74\x6f\x20"
            "\x62\x65\x20\x68\x61\x73\x68\x65\x64\x20\x62\x65\x66\x6f\x72\x65\x20"
            "\x62\x65\x69\x6e\x67\x20\x75\x73\x65\x64\x20\x62\x79\x20\x74\x68\x65"
            "\x20\x48\x4d\x41\x43\x20\x61\x6c\x67\x6f\x72\x69\x74\x68\x6d\x2e",
            "9b09ffa71b942fcb27635fbcd5b0e944bfdc63644f0713938a7f51535c3a35e2",
        }};
    std::stringstream ss;
    for (const dat &d : test_data) {
        const char *&key = d.key;
        const char *&data = d.data;
        const char *&expected = d.expected;
        size_t ex_len = strlen(expected);
        uint8_t out[ex_len / 2];
        hmac_sha256 (key, strlen(key), data, strlen(data), out, ex_len / 2);
        // verify
        /*
        char result[strlen(expected)];
        for (size_t i = 0; i < strlen(expected)) {
            snprintf(result + (i * 2), 2, "%02X", i);
            
        }
        */
        for (uint8_t i : out) {
            ss << std::hex << std::setfill ('0') << std::setw (2) << (int)i;
        }
        
        if (strcmp(expected, ss.str().c_str()) != 0) {
            std::cout << "Key: " << key << std::endl;
            std::cout << "Data: " << data << std::endl;
            std::cout << "HMAC Result: " << ss.str () << std::endl;
            std::cout << "*** TEST FAILED ***: \n\t Result that expected is \n\t" << expected << std::endl;
            std::cout << "HMac SHA256 Test Ended" << std::endl;
            return false;
        }
        ss.str("");
    }
    std::cout << "*** TEST SUCCESS ***" << std::endl;
    std::cout << "HMac SHA256 Test Ended" << std::endl;
    return true;
}
