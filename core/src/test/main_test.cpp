#include <iostream>
#include <cstdlib>

bool base_test();


int main() {
    bool testResult = true;
    
    testResult |= base_test();
    
    return testResult ? EXIT_SUCCESS : EXIT_FAILURE;
}
