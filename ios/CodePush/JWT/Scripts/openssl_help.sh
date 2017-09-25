exit(0)

# generate rsa private key.
openssl genrsa -out private_1.pem

# request certificate with private key in pem format.
openssl req -new -key private_1.pem -out cert_1.csr

# extract to pem certificate in csr format.
openssl pkcs7 -in cert_1.csr -print_certs -out certs.pem

# extract to cer certificate from csr with private key in pem.
openssl x509 -req -in cert_1.csr -signkey private_1.pem -out cert_1.cer

# extract to p12 certificate cert_1.crt with private key in pem.
openssl pkcs12 -export -in cert_1.crt -inkey private_1.pem -out cert_1.p12

# extract public key from pem file with cetificate and private key.
openssl rsa -in private_256_right.pem -pubout -out public_256_right.pem

# extract p12 to pem with all data ( nodes )
openssl pkcs12 -in private_256_right.p12 -out private_256_right.pem -nodes

# generate ecdsa private key.
openssl ecparam -genkey -name prime256v1  -out ec256-private.pem

# generate ecdsa public key.
openssl ec -in ec256-private.pem -pubout -out ec256-public.pem