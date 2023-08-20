
extern bool hmac_sha256_test ();
extern bool hashing_test ();

int main () {
  
  if (!hashing_test ()) goto failed_state;
  if (!hmac_sha256_test ()) goto failed_state;
  
  return 0;
failed_state:
  return 1;
}
