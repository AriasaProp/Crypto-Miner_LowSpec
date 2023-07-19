#include <cstdlib>
#include <iostream>

bool hmac_sha256_test ();

int main () {
  bool testResult = true;

  testResult = hmac_sha256_test ();

  return testResult ? EXIT_SUCCESS : EXIT_FAILURE;
}
