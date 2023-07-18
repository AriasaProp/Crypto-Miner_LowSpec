#include "hmac_sha256.hpp"

#include <cassert>
#include <iomanip>
#include <iostream>
#include <sstream>
#include <string>
#include <tuple>
#include <vector>

// 256 bit, 32 byte
#define SHA256_HASH_SIZE 32

typedef std::vector<std::tuple<std::string, std::string, std::string>> TestData_t;

bool hmac_sha256_test1 () {
  // Test vectors from RFC4231, https://tools.ietf.org/html/rfc4231#section-4
  const TestData_t test_vectors = {
      // Key      Data      HMAC
      {
          "\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b\x0b"
          "\x0b\x0b\x0b",
          "\x48\x69\x20\x54\x68\x65\x72\x65",
          "b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7",
      },
      {
          /* Test with a key shorter than the length of the HMAC output. */
          "\x4a\x65\x66\x65",
          "\x77\x68\x61\x74\x20\x64\x6f\x20\x79\x61\x20\x77\x61\x6e\x74\x20\x66"
          "\x6f\x72\x20\x6e\x6f\x74\x68\x69\x6e\x67\x3f",
          "5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843",
      },
      {
          /* Test with a combined length of key and data that is larger than 64
             bytes (= block-size of SHA-224 and SHA-256). */
          "\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa\xaa"
          "\xaa\xaa\xaa",
          "\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd"
          "\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd"
          "\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd\xdd",
          "773ea91e36800e46854db8ebd09181a72959098b3ef8c122d9635514ced565fe",
      },
      {
          /* Test with a combined length of key and data that is larger than 64
             bytes (= block-size of SHA-224 and SHA-256). */
          "\x01\x02\x03\x04\x05\x06\x07\x08\x09\x0a\x0b\x0c\x0d\x0e\x0f\x10\x11"
          "\x12\x13\x14\x15\x16\x17\x18\x19",
          "\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd"
          "\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd"
          "\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd\xcd",
          "82558a389a443c0ea4cc819899f2083a85f0faa3e578f8077a2e3ff46729665b",
      },
      {
          /* Test with a truncation of output to 128 bits. */
          "\x0c\x0c\x0c\x0c\x0c\x0c\x0c\x0c\x0c\x0c\x0c\x0c\x0c\x0c\x0c\x0c\x0c"
          "\x0c\x0c\x0c",
          "\x54\x65\x73\x74\x20\x57\x69\x74\x68\x20\x54\x72\x75\x6e\x63\x61\x74"
          "\x69\x6f\x6e",
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
  {
    for (auto tvec : test_vectors) {
      std::vector<uint8_t> out (std::get<2> (tvec).size () / 2);
      hmac_sha256 (std::get<0> (tvec).data (), std::get<0> (tvec).size (), std::get<1> (tvec).data (), std::get<1> (tvec).size (), out.data (), out.size ());
      // verify
      {
        const std::string &expected = std::get<2> (tvec);
        std::stringstream ss;
        for (uint8_t i : out) {
          ss << std::hex << std::setfill ('0') << std::setw (2) << (int)i;
        }
        if (expected != ss.str ()) {
          std::cout << "*** TEST FAILED ***: \n\t" << ss.str () << " != \n\t" << expected << std::endl;
          return false;
        }
      }
    }
  }
  std::cout << "Test Ended" << std::endl;
  return true;
}

static const std::string expectedHex = "4b393abced1c497f8048860ba1ede46a23f1ff5209b18e9c428bddfbb690aad8";

bool hmac_sha256_test2 () {
  const std::string str_data = "Hello World!";
  const std::string str_key = "super-secret-key";
  std::stringstream ss_result;
  // Allocate memory for the HMAC
  std::vector<uint8_t> out (SHA256_HASH_SIZE);
  // Call hmac-sha256 function
  hmac_sha256 (str_key.data (), str_key.size (), str_data.data (), str_data.size (), out.data (), out.size ());
  // Convert `out` to string with std::hex
  for (uint8_t x : out) {
    ss_result << std::hex << std::setfill ('0') << std::setw (2) << (int)x;
  }
  // Print out the result
  std::cout << "Message: " << str_data << std::endl;
  std::cout << "Key: " << str_key << std::endl;
  std::cout << "HMAC: " << ss_result.str () << std::endl;

  return (ss_result.str () == expectedHex);
}

bool hmac_sha256_test () {
  bool result = true;
  result = hmac_sha256_test1 ()
      result = hmac_sha256_test2 () return result;
}