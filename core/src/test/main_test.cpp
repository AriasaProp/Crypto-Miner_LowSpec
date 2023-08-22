
extern bool hmac_sha256_test ();
extern bool hashing_test ();
extern bool json_test ();

int main () {
  
  if (!hashing_test ()) goto failed_state;
  if (!hmac_sha256_test ()) goto failed_state;
  if (!json_test ()) goto failed_state;
  
  return 0;
failed_state:
  return 1;
}
