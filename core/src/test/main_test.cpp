#include <iostream>
#include <cstdlib>

bool base_test();
bool hmac_sha256_test();


int main() {
    bool testResult = true;
    
    testResult = base_test();
    testResult = hmac_sha256_test();
    
    return testResult ? EXIT_SUCCESS : EXIT_FAILURE;
}
