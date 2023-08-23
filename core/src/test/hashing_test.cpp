#include <hashing.hpp>
#include <iostream>
#include <iomanip>
#include <cstdint>
#include <cstdio>
#include <cstring>

static void toBinary(const char* s, uint8_t* data, size_t len) {
    for (size_t i = 0, j = 0; i < len; i++, j += 2) {
        sscanf(s+j, "%2hhx", data+i);
    }
}
static void toHex(const uint8_t* b, char* result, size_t len) {
    for (size_t i = 0, j = 0; i < len; i++, j += 2) {
        snprintf(result+j, 3, "%02x", b[i]);
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
    uint8_t header[76];
    uint8_t expected[SHA256_HASH_SIZE];
    hashing hp;
    for(const dat &d : test_data) {
        toBinary(d.header, header, 80);
        toBinary(d.expected, expected, SHA256_HASH_SIZE);
        
        if (memcmp(hp.H, expected, SHA256_HASH_SIZE) != 0) {
            std::cout << "Header length : " << strlen(d.header) << std::endl;
            char resHex[SHA256_HASH_SIZE*2];
            toHex(hp.H, resHex, SHA256_HASH_SIZE);
            std::cout << "Result  : " << resHex << std::endl;
            toHex(expected, resHex, SHA256_HASH_SIZE);
            std::cout << "Expected: " << resHex << std::endl;
            std::cout << "**** Hashing Test Failed ****" << std::endl;
            return false;
        }
        
    }
    std::cout << "**** Hashing Test Success ****" << std::endl;
    return true;
}