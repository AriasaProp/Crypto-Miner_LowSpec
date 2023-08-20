#include <hashing.hpp>
#include <iostream>
#include <iomanip>
#include <cstdint>
#include <cstdio>
#include <cstring>

static inline void hexToBiner(const char *hex, uint8_t *out, size_t len_byte) {
    for (size_t i = 0, j = 0; i < len_byte; i++, j += 2) {
        sscanf(hex + j, "%2hhx", out+i);
    }
}
bool hashing_test() {
    std::cout << "Hashing Test Start" << std::endl;
    const struct dat {
        const char *header;
        const char *expected;
    } test_data[] = {
        {
            "01000000f615f7ce3b4fc6b8f61e8f89aedb1d0852507650533a9e3b10b9bbcc30639f279fcaa86746e1ef52d3edb3c4ad8259920d509bd073605c9bf1d59983752a6b06b817bb4ea78e011d012d59d4",
            "d9eb8663ffec241c2fb118adb7de97a82c803b6ff46d57667935c81001000000"
        }
    };
    uint8_t header[80];
    uint8_t expected[SHA256_HASH_SIZE];
    uint8_t H[SHA256_HASH_SIZE];
    for(const dat &d : test_data) {
        hexToBiner(d.header, header, 80);
        hexToBiner(d.expected, expected, SHA256_HASH_SIZE);
        hashN(header, H);
        if (memcmp(H, expected, SHA256_HASH_SIZE) == 0) {
            std::cout << "*** TEST SUCCESS ***" << std::endl;
        } else {
            std::cout << "Result: ";
            for (int i = 0; i < SHA256_HASH_SIZE; i++) {
                std::cout << std::setfill('0') << std::setw(2) << std::hex << static_cast<int>(H[i]);
            }
            std::cout << std::endl;
            std::cout << "Expected: ";
            for (int i = 0; i < SHA256_HASH_SIZE; i++) {
                std::cout << std::setfill('0') << std::setw(2) << std::hex << static_cast<int>(expected[i]);
            }
            std::cout << std::endl;
            std::cout << "*** TEST FAILED ***" << std::endl;
        }
    }
    std::cout << "Hashing Test Ended" << std::endl;
    return true;
}