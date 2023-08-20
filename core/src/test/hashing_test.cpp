#include <hashing.hpp>
#include <iostream>
#include <cstdint>
#include <cstdio>
#include <cstring>

static inline void hexToBiner(const char *hex, void *out, size_t len_byte) {
    for (size_t i = 0; i < len_byte; i++) {
        sscanf(hex + (i*2), "%2hhx", (uint8_t*)out+i);
    }
}
bool hashing_test() {
    std::cout << "Hashing Test Start" << std::endl;
    const struct dat {
        const char *header;
        const char *nonce;
        const char *result;
    } test_data[] = {
        {
            "01000000f615f7ce3b4fc6b8f61e8f89aedb1d0852507650533a9e3b10b9bbcc30639f279fcaa86746e1ef52d3edb3c4ad8259920d509bd073605c9bf1d59983752a6b06b817bb4ea78e011d",
            "012d59d4",
            "d9eb8663ffec241c2fb118adb7de97a82c803b6ff46d57667935c81001000000"
        }
    };
    hashing hp;
    uint8_t header[76];
    uint32_t nonce;
    uint8_t expected[SHA256_HASH_SIZE];
    for(const dat &d : test_data) {
        hexToBiner(d.header, header, 76);
        hexToBiner(d.nonce, &nonce, 4);
        hexToBiner(d.result, expected, SHA256_HASH_SIZE);
        hp.hash(header, nonce);
        if (memcmp(hp.H, expected) == 0) {
            std::cout << "*** TEST SUCCESS ***" << std::endl;
        } else {
            std::cout << "*** TEST FAILED ***" << std::endl;
        }
    }
    std::cout << "Hashing Test Ended" << std::endl;
    return true;
}